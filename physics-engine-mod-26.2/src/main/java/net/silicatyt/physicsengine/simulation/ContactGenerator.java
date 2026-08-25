package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.*;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleBiFunction;

public final class ContactGenerator { // TODO: This whole file is a mess and needs a rewrite and some optimizations
    private interface Boundary {
        double distance(ClipPoint p);
    }

    private static final double PREVIOUS_AXIS_PREFERENCE_MULTIPLIER = 0.9; // The currently chosen axis has to clearly win against the previous one, otherwise prefer the old one

    private static final int[][] FACE_CORNER_INDICES_IN_WINDING_ORDER = { // In winding order (important for Sutherland-Hodgman clipping)
            {0, 1, 3, 2}, // -X
            {4, 5, 7, 6}, // +X
            {0, 1, 5, 4}, // -Y
            {2, 3, 7, 6}, // +Y
            {0, 2, 6, 4}, // -Z
            {1, 3, 7, 5}  // +Z
    };
    private static final List<ToDoubleBiFunction<List<ContactPointCandidate>, ContactPointCandidate>> REDUCTION_SCORERS = List.of(
            (_, candidate) -> candidate.penetrationDepth(),
            (chosen, candidate) -> distanceSquared2D(chosen.getFirst(), candidate),
            (chosen, candidate) -> triangleAreaDoubled2D(chosen.getFirst(), chosen.get(1), candidate),
            (chosen, candidate) -> Math.max(
                    triangleAreaDoubled2D(chosen.getFirst(), chosen.get(1), candidate),
                    Math.max(
                            triangleAreaDoubled2D(chosen.getFirst(), chosen.get(2), candidate), // TODO: In the datapack, re-use the intermediate results for the 2 common shared points
                            triangleAreaDoubled2D(chosen.get(1), chosen.get(2), candidate)
                    )
            )
    );

    private static final int[][] EDGE_STARTING_POINT_INDICES_FOR_AXIS = {
            {0,1,2,3},
            {0,1,4,5},
            {0,2,4,6}
    };

    private static final double CLIP_EPSILON = 1e-9;

    public static Optional<Manifold> generateManifold(PhysicsObject a, PhysicsObject b, SatResult collision, double dx, double dy, double dz) {
        boolean persistedAxisPreferred = isPersistedAxisPreferred(collision) && collision.persistedAxisData().isPresent(); // "isPresent" only to get rid of compiler warnings
        PersistedAxisData persistedAxisData = persistedAxisPreferred ? collision.persistedAxisData().get() : null;

        AxisData chosenAxisData = persistedAxisPreferred ? persistedAxisData : collision.candidateAxisData();
        boolean axisFacingB = persistedAxisPreferred ? persistedAxisData.isFacingB() : isAxisFacingB(chosenAxisData.axis(), dx, dy, dz);

        Optional<ContactState> contact;
        boolean axisFacingOutward = false; // Default = false as an unused placeholder if it's an edge-edge contact
        int chosenAxisIndex = chosenAxisData.index();

        Manifold lastTickManifold = collision.lastTickManifold().orElse(null);
        Manifold sameAxisManifold = (lastTickManifold != null && lastTickManifold.persistedAxisIndex == chosenAxisIndex)
                ? lastTickManifold : null;

        if (chosenAxisIndex < 6) {
            axisFacingOutward = persistedAxisPreferred ? persistedAxisData.isFacingOutward() : (chosenAxisIndex < 3) == axisFacingB;
            contact = generateContactPointFace(a, b, chosenAxisData, sameAxisManifold, axisFacingOutward, dx, dy, dz);
        } else {
            contact = generateContactEdgeEdge(a, b, chosenAxisData, sameAxisManifold, axisFacingB, dx, dy, dz);
        }

        return contact.isEmpty() ? Optional.empty() : Optional.of(new Manifold(
                a, b, contact.get(), chosenAxisIndex, axisFacingOutward, axisFacingB, dx, dy, dz // TODO: Invert the offset if pointFace and a is the reference
        ));
    }

    private static boolean isPersistedAxisPreferred(SatResult collision) {
        if (collision.persistedAxisData().isEmpty()) return false;

        AxisData persistedAxis = collision.persistedAxisData().get();
        AxisData candidateAxis = collision.candidateAxisData();

        if (persistedAxis.index() == candidateAxis.index()) return true; // Not necessary for correctness

        return persistedAxis.overlap() * PREVIOUS_AXIS_PREFERENCE_MULTIPLIER < candidateAxis.overlap();
    }

    public static boolean isAxisFacingB(Vector3dc axis, double dx, double dy, double dz) { return axis.dot(dx, dy, dz) < 0; } // TODO: Re-use results from earlier in the datapack

    // Face
    private static Optional<ContactState> generateContactPointFace(PhysicsObject a, PhysicsObject b, AxisData axisData, Manifold previousManifold, boolean axisFacingOutward, double dx, double dy, double dz) {
        // TODO: Split into several methods, single responsibility principle
        // TODO: Cleanup, improve variable names, add comments with the original formulas etc. This whole method is a pure mess because I wanted to apply the same optimizations as in the datapack
        int axisIndex = axisData.index();

        // Select reference & incident objects
        boolean referenceObjectIsA = axisIndex < 3;
        PhysicsObject referenceObject = referenceObjectIsA ? a : b;
        PhysicsObject incidentObject = referenceObjectIsA ? b : a;

        // Get outward facing contact normal
        Vector3dc outwardContactNormal = axisFacingOutward ? axisData.axis() : new Vector3d(axisData.axis()).negate(); // Re-assignment should be fine, because getAxis() runs before that. But technically it's not 100% future-proof

        // Get reference face
        int referenceAxisIndex = referenceObjectIsA ? axisIndex : axisIndex - 3; // 0-2 regardless of whether it's objectA or objectB (TODO: This is already calculated earlier in the SAT)
        int referenceFaceIndex = calculateFaceIndex(referenceAxisIndex, axisFacingOutward);

        // Get reference tangent axes
        int referenceTangentIndexA = referenceAxisIndex == 0 ? 1 : 0;
        int referenceTangentIndexB = referenceAxisIndex == 2 ? 1 : 2;
        Vector3dc referenceTangentA = referenceObject.getAxis(referenceTangentIndexA);
        Vector3dc referenceTangentB = referenceObject.getAxis(referenceTangentIndexB);

        // Get incident face (most antiparallel to contact normal)
        int incidentAxisIndex = 0;
        double maxAbsProjection = -1.0;
        boolean isProjectionNegative = false;
        double incidentAxisNormalProjection0 = 0.0, incidentAxisNormalProjection1 = 0.0, incidentAxisNormalProjection2 = 0.0;
        for (int i = 0; i < 3; i++) {
            double projection = outwardContactNormal.dot(incidentObject.getAxis(i)); // TODO (MAYBE): In the datapack, optimize this calculation by taking advantage of the SAT's cross products and their lengthSquared. "a.dot(b)^2 = 1 - |a.cross(b)|^2"
            if (i == 0) incidentAxisNormalProjection0 = projection; else if (i == 1) incidentAxisNormalProjection1 = projection; else incidentAxisNormalProjection2 = projection;
            double absProjection = Math.abs(projection);
            if (absProjection < maxAbsProjection) continue;

            maxAbsProjection = absProjection;
            incidentAxisIndex = i;
            isProjectionNegative = projection < 0;
        }

        int incidentFaceIndex = calculateFaceIndex(incidentAxisIndex, isProjectionNegative);

        double incidentHalfExtentNormalProjection0 = incidentObject.getHalfExtent(0) * incidentAxisNormalProjection0;
        double incidentHalfExtentNormalProjection1 = incidentObject.getHalfExtent(1) * incidentAxisNormalProjection1;
        double incidentHalfExtentNormalProjection2 = incidentObject.getHalfExtent(2) * incidentAxisNormalProjection2;

        // Get incident points (in winding order) & project them onto the reference tangent frame
        double incidentToReferenceOffsetX = referenceObjectIsA ? -dx : dx; // incident - reference
        double incidentToReferenceOffsetY = referenceObjectIsA ? -dy : dy;
        double incidentToReferenceOffsetZ = referenceObjectIsA ? -dz : dz;

        double maxReferenceFaceProjectionTangentA = referenceObject.getHalfExtent(referenceTangentIndexA); // Corner 0 has the minimum position along both tangents of any face, but because I project a reference object corner onto its own axis, I can shortcut to the half extent. I also use "max" instead of "min" to avoid two negations.
        double maxReferenceFaceProjectionTangentB = referenceObject.getHalfExtent(referenceTangentIndexB);
        double constA = referenceTangentA.dot(incidentToReferenceOffsetX, incidentToReferenceOffsetY, incidentToReferenceOffsetZ) + maxReferenceFaceProjectionTangentA; // TODO: Dot product is already calculated in the SAT, pass it in instead
        double constB = referenceTangentB.dot(incidentToReferenceOffsetX, incidentToReferenceOffsetY, incidentToReferenceOffsetZ) + maxReferenceFaceProjectionTangentB;

        List<ClipPoint> clipPoints = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            int cornerIndex = FACE_CORNER_INDICES_IN_WINDING_ORDER[incidentFaceIndex][i];
            Vector3dc pos = new Vector3d(incidentObject.getCornerPosRelative(cornerIndex)); // New vector necessary because the contact's penetrationDepth may need to be calculated in future ticks, during accumulation. Re-assignment might be fine as well, because currently getCornerPosRelative() is called before that, so the vector automatically updates anyway.
            // TODO: I could get away with calculating the 4 corners here, instead of calculating all 8. But it probably doesn't matter much

            int signX = (cornerIndex & 4) != 0 ? 1 : -1;
            int signY = (cornerIndex & 2) != 0 ? 1 : -1;
            int signZ = (cornerIndex & 1) != 0 ? 1 : -1;

            double tangentProjectionA = pos.dot(referenceTangentA) + constA; // Formula: "pos.sub(minimum pos of reference object along tangent).dot(referenceTangentA)", but using only relative coordinates and heavily optimized to pre-calculate as much as possible and replace vector subtractions with double subtractions
            double tangentProjectionB = pos.dot(referenceTangentB) + constB;
            double normalProjection = signX*incidentHalfExtentNormalProjection0 + signY*incidentHalfExtentNormalProjection1 + signZ*incidentHalfExtentNormalProjection2; // Useful for calculating penetration depth more efficiently, otherwise not required
            clipPoints.add(new ClipPoint(tangentProjectionA, tangentProjectionB, normalProjection, pos, i)); // Original corner: ID is just the winding corner id of the face (0-3), which is also the incidentEdgeIndex
        }

        // Sutherland-Hodgman Clipping
        double referenceTangentScaleA = referenceObject.getScale(referenceTangentIndexA);
        double referenceTangentScaleB = referenceObject.getScale(referenceTangentIndexB);
        Boundary[] boundaries = { // TODO: Also make this static, like REDUCTION_SCORERS
                p -> referenceTangentScaleA - p.tangentProjectionA(),
                p -> p.tangentProjectionA(),
                p -> referenceTangentScaleB - p.tangentProjectionB(),
                p -> p.tangentProjectionB()
        };

        for (int i = 0; i < boundaries.length; i++) clipPoints = clip(clipPoints, boundaries[i], i); // TODO: Make it in-place to remove heap allocations

        // Calculate penetration depth & discard non-penetrating points
        double referenceHalfExtent = referenceObject.getHalfExtent(referenceAxisIndex);
        double offsetProjection = outwardContactNormal.dot(incidentToReferenceOffsetX, incidentToReferenceOffsetY, incidentToReferenceOffsetZ);
        double offsetReferencePointProjection = referenceHalfExtent - offsetProjection;

        List<ContactPointCandidate> candidates = new ArrayList<>(clipPoints.size());
        for (ClipPoint p : clipPoints) {
            double penetrationDepth = offsetReferencePointProjection - p.normalProjection();
            if (penetrationDepth > 0) candidates.add(new ContactPointCandidate(p, penetrationDepth));
        }

        if (candidates.isEmpty()) return Optional.empty();

        // Reduce to max 4 points
        candidates = reduce(candidates); // TODO: Make it in-place to remove heap allocation

        // Create contact state
        Vector3dc contactNormalFacingA = referenceObjectIsA ? new Vector3d(outwardContactNormal).negate() : outwardContactNormal;
        List<FaceContactPoint> contactPoints = new ArrayList<>(clipPoints.size()); // TODO: Avoid creating a new list object
        FaceContactState contact = new FaceContactState(a, b, referenceObjectIsA, incidentFaceIndex, referenceFaceIndex, contactNormalFacingA, contactPoints); // TODO: Avoid creating new FaceContactState & FaceContactPoint objects each time

        // Setup previous contactPoints for warm-starting
        List<FaceContactPoint> oldPoints = (previousManifold != null
                && previousManifold.persistedAxisFacingOutward == axisFacingOutward
                && previousManifold.state instanceof FaceContactState oldState)
                ? oldState.getPoints() : null;

        // Create contact points
        for (ContactPointCandidate p : candidates) {
            Vector3d contactPos = new Vector3d(outwardContactNormal).mul(p.penetrationDepth()) // TODO: In the datapack, optimize contactPos calculation by using all the pre-calculated variables if possible
                    .add(p.point().pos())
                    .add(incidentToReferenceOffsetX, incidentToReferenceOffsetY, incidentToReferenceOffsetZ); // TODO: Remove allocation
            FaceContactPoint point = new FaceContactPoint(p.point().id(), contact, contactPos, p.penetrationDepth());
            if (oldPoints != null) { // Carry over warm-start impulses
                for (FaceContactPoint old : oldPoints) {
                    if (old.id == point.id) {
                        point.setAccumulatedImpulse(old.getAccumulatedImpulse());
                        break;
                    }
                }
            }
            contactPoints.add(point);
        }

        return Optional.of(contact); // TODO: Can it happen that 0 contact points are generated? If not, remove "Optional". If yes, return empty in that case
    }

    private static int calculateFaceIndex(int axisIndex, boolean facingOutward) { return 2 * axisIndex + (facingOutward ? 1 : 0); }

    private static List<ClipPoint> clip(List<ClipPoint> clipPoints, Boundary b, int boundaryIndex) { // Sutherland-Hodgman Clipping (TODO: REWORK, remove "new()")
        List<ClipPoint> out = new ArrayList<>();
        for (int i = 0; i < clipPoints.size(); i++) {
            ClipPoint curr = clipPoints.get(i);
            ClipPoint next = clipPoints.get((i + 1) % clipPoints.size());
            double distanceCurr = b.distance(curr);
            double distanceNext = b.distance(next);
            boolean currInBounds = distanceCurr >= 0.0;
            boolean nextInBounds = distanceNext >= 0.0;
            if (currInBounds) out.add(curr);


            if (currInBounds != nextInBounds) {
                double denominator = distanceCurr - distanceNext;
                double t = distanceCurr / denominator;
                if (t > CLIP_EPSILON && t < 1.0 - CLIP_EPSILON) { // CLIP_EPSILON to guard against generating interpolated points that lie on corners, causing too much impulse for that corner
                    ClipPoint interpolated = new ClipPoint(
                            curr.tangentProjectionA() + t * (next.tangentProjectionA() - curr.tangentProjectionA()), // TODO: When a new point is added (interpolated), only calculate the projection onto the tangents that still have pending boundary checks
                            curr.tangentProjectionB() + t * (next.tangentProjectionB() - curr.tangentProjectionB()),
                            curr.normalProjection() + t * (next.normalProjection() - curr.normalProjection()),
                            curr.pos().lerp(next.pos(), t, new Vector3d()),
                            curr.id() * 10 + boundaryIndex + 5 // Packed into a single int (TODO: Clean up the formula)
                    );
                    out.add(interpolated);
                }
            }
        }
        return out;
    }

    private static List<ContactPointCandidate> reduce(List<ContactPointCandidate> candidates) { // TODO: Rework, remove "new()"
        List<ContactPointCandidate> remaining = new ArrayList<>(candidates);
        List<ContactPointCandidate> chosen = new ArrayList<>();
        for (ToDoubleBiFunction<List<ContactPointCandidate>, ContactPointCandidate> scorer : REDUCTION_SCORERS) {
            if (remaining.isEmpty()) break;
            ContactPointCandidate best = selectBestCandidate(chosen, remaining, scorer);
            chosen.add(best);
            remaining.remove(best);
        }
        return chosen;
    }

    private static ContactPointCandidate selectBestCandidate(List<ContactPointCandidate> chosen, List<ContactPointCandidate> remaining, ToDoubleBiFunction<List<ContactPointCandidate>, ContactPointCandidate> scorer) {
        if (remaining.size() == 1) return remaining.getFirst();
        ContactPointCandidate bestCandidate = null;
        double bestScore = -Double.MAX_VALUE;
        for (ContactPointCandidate candidate : remaining) {
            double score = scorer.applyAsDouble(chosen, candidate);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    private static double distanceSquared2D(ContactPointCandidate c1, ContactPointCandidate c2) {
        ClipPoint p1 = c1.point(), p2 = c2.point();
        double u = p1.tangentProjectionA() - p2.tangentProjectionA();
        double v = p1.tangentProjectionB() - p2.tangentProjectionB();
        return u * u + v * v;
    }

    private static double triangleAreaDoubled2D(ContactPointCandidate c1, ContactPointCandidate c2, ContactPointCandidate c3) {
        ClipPoint p1 = c1.point(), p2 = c2.point(), p3 = c3.point();
        double x1 = p1.tangentProjectionA(), y1 = p1.tangentProjectionB();
        double x2 = p2.tangentProjectionA(), y2 = p2.tangentProjectionB();
        double x3 = p3.tangentProjectionA(), y3 = p3.tangentProjectionB();
        return Math.abs((x2-x1)*(y3-y1) - (y2-y1)*(x3-x1)); // 2D cross-product
    }

    // Edge-edge
    private static Optional<ContactState> generateContactEdgeEdge(PhysicsObject a, PhysicsObject b, AxisData axisData, Manifold previousManifold, boolean axisFacingB, double dx, double dy, double dz) { // TODO: Apply mathematical optimizations (Re-use projections I did in the SAT etc)
        // Make contact normal face objectA
        Vector3dc contactNormal = axisFacingB ? new Vector3d(axisData.axis()).negate() : axisData.axis();

        // Get the respective edges (The one that's closest to the other object)
        int edgeA = getObjectEdgeIndex(a, axisData.index(), contactNormal, true);
        int edgeB = getObjectEdgeIndex(b, axisData.index(), contactNormal, false);

        // Calculate contact pos
        Vector3dc contactPos = calculateContactPosEdgeEdge(a, b, edgeA, edgeB, dx, dy, dz);

        // Calculate penetration depth
        double penetrationDepth = calculatePenetrationDepth(a, b, edgeA, edgeB, contactNormal, dx, dy, dz);

        // Create contact
        int id = calculateId(edgeA, edgeB);
        EdgeContactPoint newPoint = new EdgeContactPoint(id, contactNormal, contactPos, penetrationDepth);

        // Carry over warm-start impulse
        if (previousManifold != null && previousManifold.state instanceof EdgeContactPoint old && old.id == id) {
            newPoint.setAccumulatedImpulse(old.getAccumulatedImpulse());
        }

        return Optional.of(newPoint);
    }

    private static int getObjectEdgeIndex(PhysicsObject obj, int contactNormalAxisIndex, Vector3dc contactNormal, boolean isObjectA) { // TODO: Can this be optimized mathematically, using values I've calculated earlier?
        double projection;
        double maxProjection = -Double.MAX_VALUE;

        int axisIndex = getObjectAxisIndex(contactNormalAxisIndex, isObjectA);
        int[] edgeStartingPointIndices = EDGE_STARTING_POINT_INDICES_FOR_AXIS[axisIndex];
        int edgeIndex = -1;

        for (int i = 0; i < 4; i++) { // Which edge has the deepest projection (most positive) onto the contact normal? Basically "Which one is equal to projectionObjectA[1]", but I must consider floating point errors.
            projection = obj.getCornerPosRelative(edgeStartingPointIndices[i]).dot(contactNormal);
            if (isObjectA) projection *= -1; // Deepest projection for objectA is the most negative
            if (projection > maxProjection) {
                maxProjection = projection;
                edgeIndex = 4 * axisIndex + i; // Edges have IDs 0 - 11 (4 edges for x, 4 edges for y, and 4 edges for z)
            }
        }

        return edgeIndex;
    }

    private static int getObjectAxisIndex(int crossProductAxisIndex, boolean isObjectA) { // Takes axisIndex as used in the SAT (0-14), returns 0-2 for xyz
        if (crossProductAxisIndex < 6 || crossProductAxisIndex > 14) throw new IllegalArgumentException("Index does not match any cross product index (6-14).");
        if (isObjectA) return (crossProductAxisIndex - 6) / 3;
        return (crossProductAxisIndex - 6) % 3;
    }

    private static Vector3dc calculateContactPosEdgeEdge(PhysicsObject a, PhysicsObject b, int edgeIndexA, int edgeIndexB, double dx, double dy, double dz) { // TODO: Optimize, clean up, re-use already calculates values etc
        // Calculation: u (EdgeStartA), v (EdgeDirectionA = AxisA), m (EdgeStartB), n (EdgeDirectionB = AxisB)
        //              Point on EdgeA = u + s * v, Point on EdgeB = m + t * n
        //              A = v * v (Always 1 because v is normalized), B = n * n (Always 1 because n is normalized), C = v * n, D = v * (u - m), E = n * (u - m)
        //              s = (CE - BD) / (AB - CC), t = (AE - CD) / (AB - CC)
        Vector3dc axisA = a.getAxis(getAxisIndex(edgeIndexA));
        Vector3dc axisB = b.getAxis(getAxisIndex(edgeIndexB));

        Vector3dc edgeStartingPointA = new Vector3d(getEdgeStartingPoint(a, edgeIndexA)).add(dx, dy, dz);
        Vector3dc edgeStartingPointB = getEdgeStartingPoint(b, edgeIndexB);

        double c = axisA.dot(axisB);
        double denominator = 1.0 - c*c; // AB - CC
        Vector3d startingPointDifference = edgeStartingPointA.sub(edgeStartingPointB, new Vector3d());
        double d = axisA.dot(startingPointDifference);
        double e = axisB.dot(startingPointDifference);
        double s = (c*e - d) / denominator;
        double t = (e - c*d) / denominator;

        double axisAScale = a.getScale(getAxisIndex(edgeIndexA));
        double axisBScale = b.getScale(getAxisIndex(edgeIndexB));
        s = Math.clamp(s, 0, axisAScale);
        t = Math.clamp(t, 0, axisBScale);

        Vector3d pointEdgeA = new Vector3d(axisA);
        pointEdgeA.mul(s).add(edgeStartingPointA);

        Vector3d pointEdgeB = new Vector3d(axisB);
        pointEdgeB.mul(t).add(edgeStartingPointB);

        return new Vector3d(pointEdgeA).add(pointEdgeB).mul(0.5);
    }

    private static int getAxisIndex(int edgeIndex) { return edgeIndex / 4; }

    private static Vector3dc getEdgeStartingPoint(PhysicsObject obj, int edgeIndex) {
        int axisIndex = getAxisIndex(edgeIndex);
        int[] edgeStartingPointIndices = EDGE_STARTING_POINT_INDICES_FOR_AXIS[axisIndex];
        int edgeStartingPointIndex = edgeStartingPointIndices[edgeIndex - 4 * axisIndex];

        return obj.getCornerPosRelative(edgeStartingPointIndex);
    }

    private static double calculatePenetrationDepth(PhysicsObject a, PhysicsObject b, int edgeIndexA, int edgeIndexB, Vector3dc contactNormal, double dx, double dy, double dz) {
        return new Vector3d(getEdgeStartingPoint(b, edgeIndexB)).sub(getEdgeStartingPoint(a, edgeIndexA)).sub(dx, dy, dz).dot(contactNormal);
    }

    private static int calculateId(int edgeIndexA, int edgeIndexB) { return 10 * edgeIndexA + edgeIndexB; }

}

// TODO: Group the classes in "data" better
// TODO: Maybe split the ContactGenerator into 2 classes (or 3, because of isPersistedAxisPreferred and stuff). Package "simulation", or a sub-package?
// TODO: Maybe it's not clean to have "FaceContactState" and "EdgeContactPoint" both implement ContactState? Maybe I should add an explicit "EdgeContactState" for readability, even if it's not needed?
// TODO: For terrain collisions, I want to implement face culling & merging. But make sure it doesn't merge faces with different materials. Only "same material & face can connect"