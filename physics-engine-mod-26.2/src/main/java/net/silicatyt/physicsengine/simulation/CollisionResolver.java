package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.*;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.silicatyt.physicsengine.simulation.Main.DELTA_TIME;

public final class CollisionResolver {
    private static final int NOF_VELOCITY_RESOLUTION_ITERATIONS = 20;
    private static final int NOF_PENETRATION_RESOLUTION_ITERATIONS = 20;

    private static final double MIN_DELTA_IMPULSE = 0.0005; // TODO: Finetune by measuring
    private static final double MIN_DELTA_IMPULSE_SQUARED = MIN_DELTA_IMPULSE * MIN_DELTA_IMPULSE;
    private static final double MIN_PENETRATION_CORRECTION = 0.0005; // TODO: Finetune
    private static final double PENETRATION_SLOP = 0.01; // TODO: Finetune
    private static final double BAUMGARTE_FACTOR = 0.25; // TODO: Finetune
    private static final double INVERSE_BAUMGARTE_FACTOR = 1.0 / BAUMGARTE_FACTOR;
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.01; // TODO: Finetune

    // Setup
    public static void resolve(Island island) {
        for (PhysicsObject obj : island.objects()) obj.snapshotPreSolveVelocity(); // So targetVelocity can use the velocity from before warm-starting was applied. Additional loop required because applying warm-starting affects two objects, not just one.
        // TODO: ^ fix, currently it doesn't snapshot for static objects, because they're not part of island-objects()

        // Setup: Merge manifold contact points together into ResolvingContact wrappers with pre-calculated values & apply warm-starting
        List<ResolvingContact> allContacts = new ArrayList<>();
        for (Manifold m : island.manifolds()) {
            ManifoldSolverContext manifoldContext = buildManifoldContext(m);

            for (ContactPoint p : m.state.getPoints()) {
                ContactSolverContext contactContext = buildContactContext(p, m, manifoldContext);
                ContactSolverState state = new ContactSolverState();
                ResolvingContact c = new ResolvingContact(p, m, manifoldContext, contactContext, state);
                allContacts.add(c);

                state.accumulatedImpulseContactSpace.set(p.getAccumulatedImpulse());
                transformToContactSpace(state.accumulatedImpulseContactSpace, manifoldContext);

                // Warm-starting
                if (state.accumulatedImpulseContactSpace.lengthSquared() >= MIN_DELTA_IMPULSE_SQUARED) { // TODO: Because warm-starting doesn't always apply, the 1st iteration might under-solve potentially? Skip the check entirely?
                    applyImpulse(c, p.getAccumulatedImpulse(), state.accumulatedImpulseContactSpace);
                }
            }
        }

        // Velocity resolution
        allContacts.sort(Comparator.comparingDouble((ResolvingContact c) -> c.state().accumulatedImpulseContactSpace.x()).reversed()); // Sorted by warm-start impulse along contact normal (desc) for stack stability
        for (int i = 0; i < NOF_VELOCITY_RESOLUTION_ITERATIONS; i++) {
            double maxDeltaImpulseSquared = 0.0;
            for (ResolvingContact c : allContacts) {
                maxDeltaImpulseSquared = Math.max(maxDeltaImpulseSquared, resolveVelocity(c));
            }
            if (maxDeltaImpulseSquared < MIN_DELTA_IMPULSE_SQUARED) break; // Everything's already stable. That's another advantage of using islands, even if it weren't parallelized.
        }

        // Penetration resolution (No additional sort for penetrationDepth needed, as the previous sort works for this as well, because large penetration usually means large impulse. MAYBE it would be a little more stable, but for the datapack, it's likely not worth the overhead)
        for (int i = 0; i < NOF_PENETRATION_RESOLUTION_ITERATIONS; i++) {
            double maxPositionError = 0.0;
            for (ResolvingContact c : allContacts) {
                maxPositionError = Math.max(maxPositionError, resolvePenetration(c));
            }
            if (maxPositionError < MIN_PENETRATION_CORRECTION) break;
        }

        // Convert accumulatedImpulse back to world space (so it can be carried over to the next tick)
        for (ResolvingContact c : allContacts) {
            Vector3d accumulatedImpulseWorldSpace = c.state().accumulatedImpulseContactSpace;
            transformToWorldSpace(accumulatedImpulseWorldSpace, c.manifoldContext());
            c.point().setAccumulatedImpulse(accumulatedImpulseWorldSpace);
        }
    }

    private static ManifoldSolverContext buildManifoldContext(Manifold m) {
        Matrix3dc orthonormalBasis = buildOrthonormalBasis(m);
        double frictionCoefficient = calculateFrictionCoefficient(m);
        double restitutionCoefficient = calculateRestitutionCoefficient(m);
        return new ManifoldSolverContext(orthonormalBasis, frictionCoefficient, restitutionCoefficient);
    }

    private static ContactSolverContext buildContactContext(ContactPoint p, Manifold m, ManifoldSolverContext ctx) {
        Vector3dc contactPos = p.getPosition();
        boolean relativeToA = p.isPositionRelativeToA();

        Vector3dc relativeContactPosA = relativeToA ? contactPos : new Vector3d(contactPos).sub(m.offsetX, m.offsetY, m.offsetZ);
        Vector3dc relativeContactPosB = relativeToA ? new Vector3d(contactPos).add(m.offsetX, m.offsetY, m.offsetZ) : contactPos;

        Matrix3dc RA = buildRMatrix(relativeContactPosA, ctx.orthonormalBasis());
        Matrix3dc RB = buildRMatrix(relativeContactPosB, ctx.orthonormalBasis());

        Matrix3dc angularImpulseFactorA = m.a.getInverseInertiaTensorWorld().mul(RA, new Matrix3d());
        Matrix3dc angularImpulseFactorB = m.b.getInverseInertiaTensorWorld().mul(RB, new Matrix3d());

        EffectiveMass effectiveMass = buildEffectiveMass(m, RA, RB, angularImpulseFactorA, angularImpulseFactorB);
        double targetClosingVelocity = calculateTargetClosingVelocity(p, m, ctx, RA, RB);
        double biasVelocity = calculateBiasVelocity(p);
        return new ContactSolverContext(targetClosingVelocity, biasVelocity, relativeContactPosA, relativeContactPosB, RA, RB, angularImpulseFactorA, angularImpulseFactorB, effectiveMass);
    }

    private static void transformToContactSpace(Vector3d v, ManifoldSolverContext ctx) {
        ctx.orthonormalBasis().transformTranspose(v);
    }

    private static void transformToWorldSpace(Vector3d v, ManifoldSolverContext ctx) {
        ctx.orthonormalBasis().transform(v);
    }

    private static Matrix3dc buildOrthonormalBasis(Manifold m) { // TODO: Remove vector heap allocations
        Vector3dc normal = m.state.getNormal();
        Vector3d tangent1 = new Vector3d();
        if (Math.abs(normal.x()) > Math.abs(normal.y())) {
            // Use (z, 0, -x)
            tangent1.set(normal.z(), 0.0, -normal.x());

            double length = Math.sqrt(tangent1.x * tangent1.x + tangent1.z * tangent1.z);
            tangent1.x /= length;
            tangent1.z /= length;
        } else {
            // Use (0, -z, y)
            tangent1.set(0.0, -normal.z(), normal.y());

            double length = Math.sqrt(tangent1.y * tangent1.y + tangent1.z * tangent1.z);
            tangent1.y /= length;
            tangent1.z /= length;
        }
        Vector3d tangent2 = new Vector3d(normal).cross(tangent1);

        Matrix3d orthonormalBasis = new Matrix3d();
        orthonormalBasis.set(normal, tangent1, tangent2);
        return orthonormalBasis;
    }

    private static double calculateFrictionCoefficient(Manifold m) { return Math.sqrt(m.a.getFrictionCoefficient() * m.b.getFrictionCoefficient()); }

    private static double calculateRestitutionCoefficient(Manifold m) { return Math.sqrt(m.a.getRestitutionCoefficient() * m.b.getRestitutionCoefficient()); }

    private static double calculateTargetClosingVelocity(ContactPoint p, Manifold m, ManifoldSolverContext ctxManifold, Matrix3dc RA, Matrix3dc RB) {
        // targetClosingVelocity = -restitution * (closingVelocity + relativeVelocityFromAcceleration.dot(contactNormal))
        Vector3dc normal = p.getNormal();
        Vector3d armA = RA.getColumn(0, new Vector3d()); // TODO: Remove vector heap allocations
        Vector3d armB = RB.getColumn(0, new Vector3d());

        double closingVelocity = m.b.getPreSolveAngularVelocity().dot(armB) + m.b.getPreSolveLinearVelocity().dot(normal); // Using pre-solve velocity so it's not affected by warm-starting
        closingVelocity -= m.a.getPreSolveAngularVelocity().dot(armA) + m.a.getPreSolveLinearVelocity().dot(normal); // TODO: Remove the 2nd dot product by subtracting a's velocity from b's directly

        Vector3d relativeVelocityFromAcceleration = new Vector3d(m.b.getLinearVelocityFromAcceleration());
        relativeVelocityFromAcceleration.sub(m.a.getLinearVelocityFromAcceleration());

        double total = closingVelocity - relativeVelocityFromAcceleration.dot(normal); // TODO: Optimize by merging the dot product with the other ones

        if (total < RESTITUTION_ACTIVATION_SPEED_THRESHOLD) return 0.0;
        return -ctxManifold.restitutionCoefficient() * total;
    }

    private static double calculateBiasVelocity(ContactPoint p) {
        return Math.max(BAUMGARTE_FACTOR * (p.getPenetrationDepth() - PENETRATION_SLOP), 0.0) / DELTA_TIME;
    }

    private static EffectiveMass buildEffectiveMass(Manifold m, Matrix3dc RA, Matrix3dc RB, Matrix3dc angularImpulseFactorA, Matrix3dc angularImpulseFactorB) { // TODO: In the datapack, maybe use the regular vector form (single scalar per axis) if it doesn't have a stability or accuracy impact
        // TODO: Remove vector allocations, add helper method
        Vector3d armNormal = new Vector3d();
        Vector3d armTangent1 = new Vector3d();
        Vector3d armTangent2 = new Vector3d();
        Vector3d angularFactorNormal = new Vector3d();
        Vector3d angularFactorTangent1 = new Vector3d();
        Vector3d angularFactorTangent2 = new Vector3d();

        // ObjectA
        RA.getColumn(0, armNormal);
        RA.getColumn(1, armTangent1);
        RA.getColumn(2, armTangent2);
        angularImpulseFactorA.getColumn(0, angularFactorNormal);
        angularImpulseFactorA.getColumn(1, angularFactorTangent1);
        angularImpulseFactorA.getColumn(2, angularFactorTangent2);

        double angularNormal = angularFactorNormal.dot(armNormal);
        double angularTangent11 = angularFactorTangent1.dot(armTangent1);
        double angularTangent22 = angularFactorTangent2.dot(armTangent2);
        double angularTangent12 = angularFactorTangent1.dot(armTangent2); // Symmetric, so factorTangent2.dot(armTangent1) would be identical

        // ObjectB
        if (m.b.getInverseMass() != 0.0) {
            RB.getColumn(0, armNormal);
            RB.getColumn(1, armTangent1);
            RB.getColumn(2, armTangent2);
            angularImpulseFactorB.getColumn(0, angularFactorNormal);
            angularImpulseFactorB.getColumn(1, angularFactorTangent1);
            angularImpulseFactorB.getColumn(2, angularFactorTangent2);

            angularNormal += angularFactorNormal.dot(armNormal);
            angularTangent11 += angularFactorTangent1.dot(armTangent1);
            angularTangent22 += angularFactorTangent2.dot(armTangent2);
            angularTangent12 += angularFactorTangent1.dot(armTangent2);
        }

        double inverseMassSum = m.a.getInverseMass() + m.b.getInverseMass();
        double kNormal = inverseMassSum + angularNormal;
        double kTangent11 = inverseMassSum + angularTangent11;
        double kTangent22 = inverseMassSum + angularTangent22;
        double kTangent12 = angularTangent12; // No inverseMassSum here, because the two tangents are orthogonal

        double normalEffectiveMass = 1.0 / kNormal;
        double inverseDeterminant = 1.0 / (kTangent11 * kTangent22 - kTangent12 * kTangent12); // TODO: Maybe needs a guard to avoid division by 0? Shouldn't be a problem for cuboids, right?
        return new EffectiveMass(normalEffectiveMass,
                kTangent22 * inverseDeterminant,
                -kTangent12 * inverseDeterminant,
                kTangent11 * inverseDeterminant);
    }

    private static Matrix3d buildRMatrix(Vector3dc relativeContactPos, Matrix3dc basis) { // TODO: Remove vector allocations
        Matrix3d R = new Matrix3d();
        Vector3d axis = new Vector3d();
        Vector3d cross = new Vector3d();
        for (int i = 0; i < 3; i++) {
            basis.getColumn(i, axis);
            relativeContactPos.cross(axis, cross);
            R.setColumn(i, cross);
        }
        return R;
    }


    // Velocity resolution
    private static double resolveVelocity(ResolvingContact contact) {
        updateContactVelocity(contact);
        updateClosingVelocity(contact);

        Vector3dc contactVelocity = contact.state().velocity;
        Matrix3dc orthonormalBasis = contact.manifoldContext().orthonormalBasis();

        Vector3d column = new Vector3d();
        double contactVelocityContactSpaceX = contact.state().closingVelocity; // I avoid 'orthonormalBasis.transformTranspose(contactVelocity)' because the x component (closingVelocity) is already calculated
        double contactVelocityContactSpaceY = orthonormalBasis.getColumn(1, column).dot(contactVelocity);
        double contactVelocityContactSpaceZ = orthonormalBasis.getColumn(2, column).dot(contactVelocity);
        Vector3d contactVelocityContactSpace = column.set(contactVelocityContactSpaceX, contactVelocityContactSpaceY, contactVelocityContactSpaceZ);

        // Impulse
        double deltaVelocity = calculateDeltaVelocity(contact);
        Vector3d impulse = buildImpulse(contact, deltaVelocity, contactVelocityContactSpace); // Required impulse for velocity change

        // TODO: Maybe implement static friction (via friction anchors?)

        Vector3dc accumulatedImpulse = contact.state().accumulatedImpulseContactSpace;
        Vector3d combinedImpulse = new Vector3d(impulse).add(accumulatedImpulse);
        combinedImpulse.x = Math.max(0.0, combinedImpulse.x); // Clamp the combined impulse (incl. the warm-starting impulse), so the total impulse for this tick does not go in the negatives. I only apply combinedImpulse - accumulatedImpulse each iteration.

        // Friction
        double planarImpulseMagnitudeSquared = combinedImpulse.y*combinedImpulse.y + combinedImpulse.z*combinedImpulse.z;
        double maxFriction = contact.manifoldContext().frictionCoefficient() * combinedImpulse.x;

        if (planarImpulseMagnitudeSquared > maxFriction*maxFriction) { // Use dynamic friction
            double scalingFactor = maxFriction / Math.sqrt(planarImpulseMagnitudeSquared);
            combinedImpulse.y *= scalingFactor;
            combinedImpulse.z *= scalingFactor;
        }

        Vector3d deltaImpulse = new Vector3d(combinedImpulse).sub(accumulatedImpulse);
        double deltaImpulseSquared = deltaImpulse.lengthSquared();
        if (deltaImpulseSquared < MIN_DELTA_IMPULSE_SQUARED) return deltaImpulseSquared; // Early-out. Has to be done after clamping

        contact.state().accumulatedImpulseContactSpace.set(combinedImpulse);
        Vector3d deltaImpulseContactSpace = new Vector3d(deltaImpulse);
        orthonormalBasis.transform(deltaImpulse); // To world coordinates

        // Apply impulse
        applyImpulse(contact, deltaImpulse, deltaImpulseContactSpace);
        return deltaImpulseSquared;
    }

    private static void updateContactVelocity(ResolvingContact contact) {
        PhysicsObject a = contact.manifold().a;
        PhysicsObject b = contact.manifold().b;
        Vector3dc rA = contact.contactContext().relativeContactPosA();
        Vector3dc rB = contact.contactContext().relativeContactPosB();

        Vector3d velocityA = new Vector3d();
        Vector3d velocityB = contact.state().velocity;
        velocityA.set(a.getAngularVelocity()).cross(rA).add(a.getLinearVelocity());
        velocityB.set(b.getAngularVelocity()).cross(rB).add(b.getLinearVelocity());

        velocityB.sub(velocityA);
    }

    private static void updateClosingVelocity(ResolvingContact contact) {
        contact.state().closingVelocity = contact.state().velocity.dot(contact.point().getNormal());
    }

    private static double calculateDeltaVelocity(ResolvingContact contact) {
        return contact.state().closingVelocity - contact.contactContext().targetClosingVelocity();
    }

    private static Vector3d buildImpulse(ResolvingContact contact, double deltaVelocity, Vector3dc contactVelocityContactSpace) {
        EffectiveMass effectiveMass = contact.contactContext().effectiveMass();
        Vector3d impulse = new Vector3d();

        double tangentialVelocity1 = contactVelocityContactSpace.y();
        double tangentialVelocity2 = contactVelocityContactSpace.z();
        impulse.x = deltaVelocity * effectiveMass.normal();
        impulse.y = tangentialVelocity1 * effectiveMass.inverseK11() + tangentialVelocity2 * effectiveMass.inverseK12();
        impulse.z = tangentialVelocity1 * effectiveMass.inverseK12() + tangentialVelocity2 * effectiveMass.inverseK22();
        return impulse;
    }

    private static void applyImpulse(ResolvingContact contact, Vector3dc impulseWorldSpace, Vector3dc impulseContactSpace) { // TODO: Get rid of vector allocations
        PhysicsObject a = contact.manifold().a;
        a.addLinearVelocity(impulseWorldSpace.mul(a.getInverseMass(), new Vector3d()));
        a.addAngularVelocity(contact.contactContext().angularImpulseFactorA().transform(impulseContactSpace, new Vector3d()));

        PhysicsObject b = contact.manifold().b;
        if (b.getInverseMass() == 0.0) return;
        Vector3d negatedWorld = new Vector3d(impulseWorldSpace).negate();
        Vector3d negatedContact = new Vector3d(impulseContactSpace).negate();
        b.addLinearVelocity(negatedWorld.mul(b.getInverseMass()));
        b.addAngularVelocity(contact.contactContext().angularImpulseFactorB().transform(negatedContact));
    }

    // Penetration resolution
    private static double resolvePenetration(ResolvingContact contact) { // TODO: Split into helper methods, some code is re-used from other parts (contactVelocity, targetClosingVelocity, combinedImpulse vs accumulatedImpulse, ...)
        double biasVelocity = contact.contactContext().biasVelocity();
        double deltaVelocity = biasVelocity + calculateSplitImpulseClosingVelocity(contact);

        double positionError = Math.abs(deltaVelocity) * DELTA_TIME * INVERSE_BAUMGARTE_FACTOR;
        if (positionError < MIN_PENETRATION_CORRECTION) return positionError;

        double impulse = deltaVelocity * contact.contactContext().effectiveMass().normal(); // Only the component along the contact normal
        double accumulatedImpulse = contact.state().accumulatedSplitImpulse;

        double combinedImpulse = Math.max(0d, impulse + accumulatedImpulse);

        double deltaImpulse = combinedImpulse - accumulatedImpulse;
        contact.state().accumulatedSplitImpulse = combinedImpulse;

        Vector3d deltaImpulseWorldSpace = new Vector3d(contact.point().getNormal()).mul(deltaImpulse);

        // Apply impulse (Add it to split linear & angular velocities)
        applySplitImpulse(contact, deltaImpulseWorldSpace, deltaImpulse);
        return positionError;
    }

    private static double calculateSplitImpulseClosingVelocity(ResolvingContact contact) {
        Vector3dc normal = contact.point().getNormal();
        Matrix3dc RA = contact.contactContext().RA();
        Matrix3dc RB = contact.contactContext().RB();

        Vector3d armA = RA.getColumn(0, new Vector3d()); // TODO: Remove vector heap allocations
        Vector3d armB = RB.getColumn(0, new Vector3d());

        PhysicsObject a = contact.manifold().a;
        PhysicsObject b = contact.manifold().b;
        double closingVelocity = b.getSplitAngularVelocity().dot(armB) + b.getSplitLinearVelocity().dot(normal);
        closingVelocity -= a.getSplitAngularVelocity().dot(armA) + a.getSplitLinearVelocity().dot(normal); // TODO: Merge the dot products
        return closingVelocity;
    }

    private static void applySplitImpulse(ResolvingContact contact, Vector3dc impulseWorldSpace, double impulseMagnitude) { // TODO: Use helper methods to reduce duplicated code from applyImpulse()
        PhysicsObject a = contact.manifold().a;
        Vector3d angularImpulseFactorACol = contact.contactContext().angularImpulseFactorA().getColumn(0, new Vector3d());
        a.addSplitLinearVelocity(impulseWorldSpace.mul(a.getInverseMass(), new Vector3d()));
        a.addSplitAngularVelocity(angularImpulseFactorACol.mul(impulseMagnitude));

        PhysicsObject b = contact.manifold().b;
        if (b.getInverseMass() == 0.0) return;
        Vector3d angularImpulseFactorBCol = contact.contactContext().angularImpulseFactorB().getColumn(0, new Vector3d());
        b.addSplitLinearVelocity(impulseWorldSpace.mul(-b.getInverseMass(), new Vector3d()));
        b.addSplitAngularVelocity(angularImpulseFactorBCol.mul(-impulseMagnitude));
    }


}

// TODO: Is my naming convention clean (build for creating new objects, calculate for returning value, update for in-place)? Not used consistently (i.e., calculateImpulseLinearVelocity() in PhysicsObject)
// TODO: Maybe change effective mass from Vector3d to Matrix3d, should make it more stable (but also a bit more expensive...)
// TODO: Maybe make penetrationSlop or BAUMGARTE_FACTOR scale with the object sizes? Very small objects noticeably sink into the floor