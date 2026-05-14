package silicatyt.physics.simulation;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.Physics;
import silicatyt.physics.data.*;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.CollisionDetector.projectObjectOntoAxis;

public class ContactGenerator {
    // TODO: Maybe relocate these constants and rename them
    public static final double ACCUMULATION_PROJECTION_DISCARD_THRESHOLD = 0.7; // During accumulation, discard contacts from the previous tick whose contactNormal's projection onto the current tick's contact's contactNormal is less than this.
    public static final double ACCUMULATION_PROJECTION_DEACTIVATION_THRESHOLD = 0.9; // During accumulation, deactivate (but keep) contacts from the previous tick whose contactNormal's projection onto the current tick's contact's contactNormal is less than this.
    public static final double ACCUMULATION_MIN_PENETRATION_DEPTH_THRESHOLD = -0.1; // If penetrationDepth is smaller than this, the contact will be discarded during accumulation.
    public static final double ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD = 0.5; // When updating non-touching manifolds, discard them if the AABBs are separated by this amount
    public static final int ACCUMULATION_MAX_SEPARATION_TIME_THRESHOLD = 10; // When updating non-touching manifolds, discard them if they don't get in contact within X ticks

    private static final double CONTACT_EPSILON = 1e-7;

    public static Contact generateContact(PhysicsObject objectA, ObjectCollision collision) {
        if (collision.axisOfMinOverlapIndex() < 6) { return generateContactPointFace(objectA, collision); }
        return generateContactEdgeEdge(objectA, collision);
    }

    private static ContactPointFace generateContactPointFace(PhysicsObject objectA, ObjectCollision collision) {
        PhysicsObject objectB = collision.objectB();

        // Select faceObject and cornerObject
        boolean isObjectATheFaceObject = collision.axisOfMinOverlapIndex() < 3;
        PhysicsObject faceObject = isObjectATheFaceObject ? objectA : objectB;
        PhysicsObject cornerObject = isObjectATheFaceObject ? objectB : objectA;

        // Select the correct face and invert the contact normal if necessary
        Vector3d outwardFacingContactNormal = new Vector3d(collision.axisOfMinOverlap());
        int chosenFace = 11 + (collision.axisOfMinOverlapIndex() % 3) * 2;

        double[] faceObjectProjection = projectObjectOntoAxis(faceObject, outwardFacingContactNormal);
        double[] cornerObjectProjection = projectObjectOntoAxis(cornerObject, outwardFacingContactNormal);

        double penetrationDepthPositive = faceObjectProjection[1] - cornerObjectProjection[0];
        double penetrationDepthNegative = cornerObjectProjection[1] - faceObjectProjection[0];

        if (penetrationDepthNegative < penetrationDepthPositive) { // Choose the smaller depth: That's the one from the SAT.
            outwardFacingContactNormal.negate();
            chosenFace--; // The chosenFace is 10 for normal=-X, 11 for normal=+X, 12 for normal=-Y etc
        }

        // Try to find the deepest corner that matches
        int chosenCorner = tryToFindCorner(cornerObject, faceObject, chosenFace,  outwardFacingContactNormal); // TODO: Make it so it tries as the object with the smaller face area first (Small optimization)
        if (chosenCorner == -1) { // No corner found (Likely because a small object is perfectly parallel to a large floor object): Try swapping the roles as a fallback
            cornerObject = faceObject;
            faceObject = faceObject == objectA ? objectB : objectA;
            outwardFacingContactNormal.negate();
            if (chosenFace % 2 == 0) {
                chosenFace++;
            } else {
                chosenFace--;
            }
            chosenCorner = tryToFindCorner(cornerObject, faceObject, chosenFace,  outwardFacingContactNormal);
            if (chosenCorner == -1) { return null; } // Still no corner found: Give up
        }

        // Create contact
        if (faceObject == objectA) {
            return new ContactPointFace(objectA, objectB, chosenFace, chosenCorner);
        } else {
            return new ContactPointFace(objectA, objectB, chosenCorner, chosenFace);
        }
    }

    private static ContactEdgeEdge generateContactEdgeEdge(PhysicsObject objectA, ObjectCollision collision) {
        PhysicsObject objectB = collision.objectB();

        // Invert the contact normal if necessary
        Vector3d contactNormal = collision.axisOfMinOverlap();
        correctContactNormalDirectionEdgeEdge(contactNormal, objectA, objectB);

        // Get objectA's edge (The one that's closest to objectB)
        int featureA = getObjectEdgeIndex(objectA, collision.axisOfMinOverlapIndex(), contactNormal, true);

        // Get objectB's edge (The one that's closest to objectA)
        int featureB = getObjectEdgeIndex(objectB, collision.axisOfMinOverlapIndex(), contactNormal, false);

        // Create contact
        return new ContactEdgeEdge(objectA, objectB, featureA, featureB);
    }





    // Helper Methods (Point-Face)
    private static int tryToFindCorner(PhysicsObject cornerObject, PhysicsObject faceObject, int chosenFace, Vector3dc outwardFacingContactNormal) { // TODO: The fallback to "try the other object if both are perfectly parallel" would not be necessary with manifold clipping
        // Select the correct corner (The one with the most negative projection onto the normal)
        Vector3dc[] corners = cornerObject.getCornerPosAbsolute();
        double projection;
        double minProjection = Double.MAX_VALUE;
        int chosenCorner = -1;
        int normalAxisIndex = (chosenFace - 10) / 2;
        for (int i = 0; i < 8; i++) {
            if (isPointInsideTangentialBounds(corners[i], faceObject, normalAxisIndex)) { // Don't check for whether the corner is inside the boundaries for the contactNormal axis, because corners could pass through thin objects very easily, leading to 0 found corners otherwise.
                projection = corners[i].dot(outwardFacingContactNormal);
                if (projection < minProjection) {
                    minProjection = projection;
                    chosenCorner = i;
                }
            }
        }
        return chosenCorner;
    }

    private static boolean isPointInsideTangentialBounds(Vector3dc point, PhysicsObject obj, int normalAxisIndex) {
        if (normalAxisIndex < 0 || normalAxisIndex > 2) { throw new IllegalArgumentException("normalAxisIndex must be between 0 and 2."); }
        double pointProjection;
        double[] objProjection;

        for (int i = 0; i < 3; i++) {
            if (i == normalAxisIndex) { continue; }
            pointProjection = point.dot(obj.getAxis(i));
            objProjection = projectObjectOntoAxis(obj, obj.getAxis(i));
            if (objProjection[0] - CONTACT_EPSILON > pointProjection || pointProjection > objProjection[1] + CONTACT_EPSILON) { return false; } // Epsilon is necessary in case two objects are perfectly stacked ontop of each other (Safety net so that rounding errors do not make it fail)
        }
        return true;
    }





    // Helper Methods (Edge-Edge)
    public static void correctContactNormalDirectionEdgeEdge(Vector3d contactNormal, PhysicsObject objectA, PhysicsObject objectB) { // Modifies the input contactNormal, making sure it always points from B to A.
        Vector3d relativePosition = new Vector3d(objectA.getInternalPos()).sub(objectB.getInternalPos());
        double projection = relativePosition.dot(contactNormal);
        if (projection < 0) { contactNormal.negate(); }
    }

    private static int getObjectEdgeIndex(PhysicsObject object, int axisOfMinOverlapIndex, Vector3dc contactNormal, boolean isObjectA) {
        double projection;
        double maxProjection = -Double.MAX_VALUE;

        Vector3dc[] corners = object.getCornerPosAbsolute();
        int axisIndex = getObjectAxisIndex(axisOfMinOverlapIndex, isObjectA); // x (0), y (1) or z (2)
        int[] edgeStartingPointIndices = getEdgeStartingPointIndices(axisIndex);
        int feature = -1;

        for (int i = 0; i < 4; i++) { // Which edge has the deepest projection (most positive) onto the contact normal? Basically "Which one is equal to projectionObjectA[1]", but I must consider floating point errors.
            projection = corners[edgeStartingPointIndices[i]].dot(contactNormal);
            if (isObjectA) { projection *= -1; } // Deepest projection for objectA is the most negative TODO: I INVERTED THE CHECK AND THE COMMENT. IS THAT CORRECT?
            if (projection > maxProjection) {
                maxProjection = projection;
                feature = 20 + i + 4 * axisIndex; // Edges have IDs 20 - 31 (4 edges for x, 4 edges for y, and 4 edges for z)
            }
        }

        return feature;
    }

    private static int getObjectAxisIndex(int crossProductAxisIndex, boolean isObjectA) { // Takes axisIndex as used in the SAT (0-14). Returns 0, 1 or 2 for x, y or z.
        if (crossProductAxisIndex < 6 || crossProductAxisIndex > 14) { throw new IllegalArgumentException("Index does not match any cross product index (6-14)."); }
        if (isObjectA) {
            return (crossProductAxisIndex - 6) / 3;
        }
        return (crossProductAxisIndex - 6) % 3;
    }

    public static int[] getEdgeStartingPointIndices(int axisIndex) {
        if (axisIndex == 0) { // x
            return new int[]{0,1,2,3};
        }
        else if (axisIndex == 1) { // y
            return new int[]{0,1,4,5};
        }
        else if (axisIndex == 2) { // z
            return new int[]{0,2,4,6};
        }
        throw new IllegalArgumentException("axisIndex must be between 0 and 2.");
    }

}

// TODO: In point-face, generate a contact for every corner that matches, if there's not already a contact for that corner. Shouldn't be that expensive, but it would make landing flat on the floor much more stable.
// TODO: ^ do I need something like this for edge-edge too? How would that work?