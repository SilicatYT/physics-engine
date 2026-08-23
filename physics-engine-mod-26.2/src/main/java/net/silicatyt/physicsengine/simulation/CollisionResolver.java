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

    private static final double MIN_DELTA_VELOCITY = 0.0; // TODO: Finetune
    private static final double MIN_DELTA_VELOCITY_SQUARED = MIN_DELTA_VELOCITY * MIN_DELTA_VELOCITY;
    private static final double MIN_TANGENTIAL_VELOCITY_SQUARED = 0.0; // TODO: Finetune
    private static final double PENETRATION_SLOP = 0.0; // TODO: Finetune
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.0; // TODO: Finetune

    // Setup
    public static void resolve(Island island) {
        for (PhysicsObject obj : island.objects()) obj.snapshotPreSolveVelocity(); // So targetVelocity can use the velocity from before warm-starting was applied. Additional loop required because applying warm-starting affects two objects, not just one.

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
                if (state.accumulatedImpulseContactSpace.lengthSquared() >= MIN_DELTA_VELOCITY_SQUARED) {
                    applyImpulse(c, p.getAccumulatedImpulse());
                }
            }
        }

        // Velocity resolution
        allContacts.sort(Comparator.comparingDouble((ResolvingContact c) -> c.state().accumulatedImpulseContactSpace.x()).reversed()); // Sorted by warm-start impulse along contact normal (desc) for stack stability
        for (int i = 0; i < NOF_VELOCITY_RESOLUTION_ITERATIONS; i++) {
            for (ResolvingContact c : allContacts) resolveVelocity(c);
        }

        // Penetration resolution
        allContacts.sort(Comparator.comparingDouble((ResolvingContact c) -> c.point().getPenetrationDepth()).reversed()); // TODO: Check whether sorting by the previous tick's applied splitImpulse would be more stable. I'd need to move accumulatedSplitImpulse from ContactSolverState to ContactPoint in that case.
        for (int i = 0; i < NOF_PENETRATION_RESOLUTION_ITERATIONS; i++) {
            for (ResolvingContact c : allContacts) resolvePenetration(c);
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

        Vector3dc effectiveMass = buildEffectiveMass(p, m, RA, RB, angularImpulseFactorA, angularImpulseFactorB);
        double targetClosingVelocity = calculateTargetClosingVelocity(p, m, ctx, relativeContactPosA, relativeContactPosB);
        double biasVelocity = calculateBiasVelocity(p);
        return new ContactSolverContext(targetClosingVelocity, biasVelocity, relativeContactPosA, relativeContactPosB, angularImpulseFactorA, angularImpulseFactorB, effectiveMass);
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

    private static double calculateTargetClosingVelocity(ContactPoint p, Manifold m, ManifoldSolverContext ctxManifold, Vector3dc relativeContactPosA, Vector3dc relativeContactPosB) {
        // targetClosingVelocity = -restitution * (closingVelocity + relativeVelocityFromAcceleration.dot(contactNormal))
        Vector3dc normal = p.getNormal();
        Vector3d armA = new Vector3d(relativeContactPosA).cross(normal);
        Vector3d armB = new Vector3d(relativeContactPosB).cross(normal);

        double closingVelocity = m.a.getPreSolveAngularVelocity().dot(armA) + m.a.getPreSolveLinearVelocity().dot(normal); // Using pre-solve velocity so it's not affected by warm-starting
        closingVelocity -= m.b.getPreSolveAngularVelocity().dot(armB) + m.b.getPreSolveLinearVelocity().dot(normal);

        Vector3d relativeVelocityFromAcceleration = new Vector3d(m.a.getLinearVelocityFromAcceleration());
        relativeVelocityFromAcceleration.sub(m.b.getLinearVelocityFromAcceleration());

        double total = closingVelocity + relativeVelocityFromAcceleration.dot(normal);

        if (total > RESTITUTION_ACTIVATION_SPEED_THRESHOLD) return 0.0;
        return -ctxManifold.restitutionCoefficient() * total;
    }

    private static double calculateBiasVelocity(ContactPoint p) { return Math.max(p.getPenetrationDepth() - PENETRATION_SLOP, 0.0) / DELTA_TIME; } // TODO: Maybe multiply slop by penetration for more stable results?

    private static Vector3dc buildEffectiveMass(ContactPoint p, Manifold m, Matrix3dc RA, Matrix3dc RB, Matrix3dc angularImpulseFactorA, Matrix3dc angularImpulseFactorB) { // TODO: Vector allocations could be removed
        Vector3d effectiveMass = new Vector3d();
        Vector3d RColumn = new Vector3d();
        Vector3d factorColumn = new Vector3d();
        for (int i = 0; i < 3; i++) {
            Vector3dc RA_col = RA.getColumn(i, RColumn);
            Vector3dc factorA_col = angularImpulseFactorA.getColumn(i, factorColumn);
            double termA = factorA_col.dot(RA_col) + m.a.getInverseMass();

            Vector3dc RB_col = RB.getColumn(i, RColumn);
            Vector3dc factorB_col = angularImpulseFactorB.getColumn(i, factorColumn);
            double termB = factorB_col.dot(RB_col) + m.b.getInverseMass();

            effectiveMass.setComponent(i, 1.0 / (termA + termB));
        }
        return effectiveMass;
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
    private static void resolveVelocity(ResolvingContact contact) {
        updateContactVelocity(contact);
        updateClosingVelocity(contact);

        Vector3dc contactVelocity = contact.state().velocity;
        Matrix3dc orthonormalBasis = contact.manifoldContext().orthonormalBasis();

        Vector3d column = new Vector3d();
        double contactVelocityContactSpaceX = contact.state().closingVelocity; // I avoid 'orthonormalBasis.transformTranspose(contactVelocity)' because the x component (closingVelocity) is already calculated
        double contactVelocityContactSpaceY = orthonormalBasis.getColumn(1, column).dot(contactVelocity);
        double contactVelocityContactSpaceZ = orthonormalBasis.getColumn(2, column).dot(contactVelocity);
        Vector3d contactVelocityContactSpace = column.set(contactVelocityContactSpaceX, contactVelocityContactSpaceY, contactVelocityContactSpaceZ);

        // Early out if converged
        double deltaVelocity = calculateDeltaVelocity(contact);
        boolean normalConverged = Math.abs(deltaVelocity) < MIN_DELTA_VELOCITY;
        boolean tangentialConverged = (contactVelocityContactSpace.y()*contactVelocityContactSpace.y()
                + contactVelocityContactSpace.z()*contactVelocityContactSpace.z()) < MIN_TANGENTIAL_VELOCITY_SQUARED;
        if (normalConverged && tangentialConverged) return;

        // Impulse
        Vector3dc impulse = buildImpulse(contact, deltaVelocity, contactVelocityContactSpace); // Required impulse for velocity change

        /*if ((contactVelocityInContactSpace.y*contactVelocityInContactSpace.y + contactVelocityInContactSpace.z*contactVelocityInContactSpace.z) < 0.2) { // TODO: Temporarily disabled, fixes friction sliding issue?
            impulse.y = 0d;
            impulse.z = 0d;
        }*/

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

        Vector3d deltaImpulse = combinedImpulse.sub(accumulatedImpulse); // Re-assignment
        contact.state().accumulatedImpulseContactSpace.add(deltaImpulse);

        orthonormalBasis.transform(deltaImpulse); // To world coordinates

        // Apply impulse
        applyImpulse(contact, deltaImpulse);
    }

    private static void updateContactVelocity(ResolvingContact contact) {
        PhysicsObject a = contact.manifold().a;
        PhysicsObject b = contact.manifold().b;
        Vector3dc rA = contact.contactContext().relativeContactPosA();
        Vector3dc rB = contact.contactContext().relativeContactPosB();

        Vector3d velocityA = contact.state().velocity;
        Vector3d velocityB = new Vector3d();
        velocityA.set(a.getAngularVelocity()).cross(rA).add(a.getLinearVelocity());
        velocityB.set(b.getAngularVelocity()).cross(rB).add(b.getLinearVelocity());

        contact.state().velocity.sub(velocityB);
    }

    private static void updateClosingVelocity(ResolvingContact contact) {
        contact.state().closingVelocity = contact.state().velocity.dot(contact.point().getNormal());
    }

    private static double calculateDeltaVelocity(ResolvingContact contact) {
        return contact.state().closingVelocity - contact.contactContext().targetClosingVelocity();
    }

    private static Vector3dc buildImpulse(ResolvingContact contact, double deltaVelocity, Vector3dc contactVelocityContactSpace) {
        Vector3dc effectiveMass = contact.contactContext().effectiveMass();
        Vector3d impulse = new Vector3d();
        impulse.x = -deltaVelocity * effectiveMass.x();
        impulse.y = -contactVelocityContactSpace.y() * effectiveMass.y();
        impulse.z = -contactVelocityContactSpace.z() * effectiveMass.z();
        return impulse;
    }

    private static void applyImpulse(ResolvingContact contact, Vector3dc impulse) { // TODO: Get rid of vector allocation
        PhysicsObject a = contact.manifold().a;
        a.applyImpulse(impulse, contact.contactContext().relativeContactPosA());

        PhysicsObject b = contact.manifold().b;
        b.applyImpulse(new Vector3d(impulse).negate(), contact.contactContext().relativeContactPosB());
    }


    // Penetration resolution
    private static void resolvePenetration(ResolvingContact contact) {} // TODO


}

// TODO: Is my naming convention clean (build for creating new objects, calculate for returning value, update for in-place)? Not used consistently (i.e., calculateImpulseLinearVelocity() in PhysicsObject)
// TODO: Maybe change effective mass from Vector3d to Matrix3d, should make it more stable (but also a bit more expensive...)
