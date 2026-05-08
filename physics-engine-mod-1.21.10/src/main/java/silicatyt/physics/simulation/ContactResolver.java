package silicatyt.physics.simulation;

// TODO: Also add a resolver that prioritizes contacts with higher penetrationDepth and closingVelocity
// TODO: I could optimize a few calculations by re-using previous objects that I no longer need, and calling them "linearMovement" for example. It would avoid some "new Vector3d(...)" calls, but I'm not sure if that would be clean.

import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.Physics;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactManager;
import silicatyt.physics.entity.PhysicsObject;

import java.util.List;

import static silicatyt.physics.simulation.Main.DELTA_TIME;

public class ContactResolver {
    private static final int NOF_VELOCITY_RESOLUTION_ITERATIONS = 10;
    private static final int NOF_PENETRATION_RESOLUTION_ITERATIONS = 10;
    private static final double MIN_DELTA_VELOCITY = 0.01d;
    private static final double MIN_PENETRATION_DEPTH = 0.001d;
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.3d; // If the closing velocity is smaller than this, the coefficient of restitution will be set to 0
    private static final double PENETRATION_RESOLUTION_STRENGTH = 0.2d; // How much of the penetration is resolved per iteration (in the split-impulse method)
    private static final double PENETRATION_RESOLUTION_SLOP = 0.001d;

    public static void resolve(ContactManager manager) {
        List<Contact> contacts = manager.getContacts();

        // Preparations
        for (Contact contact : contacts) {
            // Calculate target closing velocity (velocity resolution) that is used for the rest of the tick
            contact.targetClosingVelocity = calculateTargetClosingVelocity(contact);

            // Calculate the bias velocity (penetration resolution) that's used for the rest of the tick
            contact.biasVelocity = calculateBiasVelocity(contact);
            contact.clearAccumulatedSplitImpulse();

            // Warm-starting
            if (!contact.getAccumulatedImpulse().equals(0d, 0d, 0d)) {
                Vector3d impulseWorld = new Vector3d(contact.getAccumulatedImpulse());
                contact.getOrthonormalBasis().transform(impulseWorld); // TODO: Maybe I can optimize it so I don't need this extra transformation, for example if I store the accumulated impulse in world coordinates, and add some (cheaper) calculations in resolveVelocity()?
                applyImpulse(contact, impulseWorld);
            }
        }

        // Resolution
        for (int i = 0; i < NOF_VELOCITY_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contacts) {
                resolveVelocity(contact);
            }
        }

        for (int i = 0; i < NOF_PENETRATION_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contacts) {
                if (contact.getPenetrationDepth() < MIN_PENETRATION_DEPTH) { continue; }
                resolvePenetration(contact);
            }
        }
    }

    public static void resolveALT(ContactManager manager) {} // TODO: Same as resolve, but prioritize contacts with higher penetrationDepth and closingVelocity

    private static void resolveVelocity(Contact contact) {
        double deltaVelocity = calculateDeltaVelocity(contact);
        if (Math.abs(deltaVelocity) < MIN_DELTA_VELOCITY) { return; }

        Matrix3dc orthonormalBasis = contact.getOrthonormalBasis();

        Vector3d contactVelocityInContactSpace = new Vector3d(contact.getContactVelocity());
        orthonormalBasis.transformTranspose(contactVelocityInContactSpace);

        // Required impulse for velocity change
        Vector3dc accumulatedImpulse =  contact.getAccumulatedImpulse();

        Vector3dc inverseEffectiveMass = contact.getInverseEffectiveMass();
        Vector3d impulse = new Vector3d();
        impulse.x = deltaVelocity * inverseEffectiveMass.x();
        impulse.y = -contactVelocityInContactSpace.y * inverseEffectiveMass.y();
        impulse.z = -contactVelocityInContactSpace.z * inverseEffectiveMass.z();

        Vector3d combinedImpulse = new Vector3d(impulse).add(accumulatedImpulse);
        combinedImpulse.x = Math.max(0.0, combinedImpulse.x); // Clamp the combined impulse (incl. the warm-starting impulse), so the total impulse for this tick does not go in the negatives. I only apply combinedImpulse - accumulatedImpulse each iteration.

        // Friction
        double planarImpulseMagnitudeSquared = combinedImpulse.y*combinedImpulse.y + combinedImpulse.z*combinedImpulse.z;
        double maxFriction = contact.getFrictionCoefficient() * combinedImpulse.x;

        if (planarImpulseMagnitudeSquared > maxFriction*maxFriction) {
            // Use dynamic friction
            double scalingFactor = maxFriction / Math.sqrt(planarImpulseMagnitudeSquared);
            combinedImpulse.y *= scalingFactor;
            combinedImpulse.z *= scalingFactor;
        }

        // Update accumulated impulse
        Vector3d deltaImpulse = combinedImpulse.sub(accumulatedImpulse); // Same reference, just a different name for readability
        contact.addAccumulatedImpulse(deltaImpulse);

        // Transform impulse to world coordinates
        orthonormalBasis.transform(deltaImpulse);

        // Apply impulse
        applyImpulse(contact, deltaImpulse);
    }

    private static void resolvePenetration(Contact contact) { // Split-impulse
        double biasVelocity = contact.biasVelocity;
        double deltaVelocity = biasVelocity - calculateSplitImpulseContactVelocity(contact).dot(contact.getContactNormal()); // TODO: Check the signs
        //if (Math.abs(deltaVelocity) < MIN_PENETRATION_DEPTH) { return; } TODO: IMPROVE THIS CHECK. RN it's bugged because it compares meters with meters/second.

        Physics.LOGGER.info("Resolving penetration. BiasVelocity: {}, DeltaVelocity: {}", biasVelocity, deltaVelocity);

        Vector3d impulse = new Vector3d(); // TODO: Optimize (Using a whole Vector3d even though I only have a single component is maybe a little weird)
        impulse.x = deltaVelocity * contact.getInverseEffectiveMass().x();

        Vector3dc accumulatedImpulse = contact.getAccumulatedSplitImpulse();
        Vector3d combinedImpulse = new Vector3d(impulse).add(accumulatedImpulse);
        combinedImpulse.x = Math.max(0.0, combinedImpulse.x);

        Vector3d deltaImpulse = combinedImpulse.sub(accumulatedImpulse); // Same reference
        contact.addAccumulatedSplitImpulse(deltaImpulse);

        contact.getOrthonormalBasis().transform(deltaImpulse);

        // Apply impulse (Add it to linear & angular correction)
        applySplitImpulse(contact, deltaImpulse);
    }

    private static void resolvePenetrationLinearProjection(Contact contact) { // Simple linear projection
        PhysicsObject objectA = contact.objectA;
        PhysicsObject objectB = null;
        if (contact.objectB != null) { objectB = contact.objectB; }

        double inverseMassTotal = objectB == null ? objectA.getInverseMass() : objectA.getInverseMass() + objectB.getInverseMass();
        Vector3d linearMovementPerInverseMass = new Vector3d(contact.getContactNormal()).mul(contact.getPenetrationDepth()).div(inverseMassTotal);

        Vector3d linearMovementA = new Vector3d(linearMovementPerInverseMass).mul(objectA.getInverseMass());
        objectA.setInternalPos(linearMovementA.add(objectA.getInternalPos()));
        if (objectB != null) {
            Vector3d linearMovementB = new Vector3d(linearMovementPerInverseMass).mul(-1 * objectB.getInverseMass());
            objectB.setInternalPos(linearMovementB.add(objectB.getInternalPos()));
        }
    }





    // Helper methods (Velocity resolution)
    private static double calculateTargetClosingVelocity(Contact contact) {
        // targetClosingVelocity = -restitution * (closingVelocity + relativeVelocityFromAcceleration.dot(contactNormal))
        if (contact.getClosingVelocity() < RESTITUTION_ACTIVATION_SPEED_THRESHOLD) { return 0d; } // Ignore restitution if the speed is small, for stability

        Vector3d relativeLinearVelocityFromAcceleration = new Vector3d(contact.objectA.getLinearVelocityFromAcceleration());
        if (contact.objectB != null) { relativeLinearVelocityFromAcceleration.sub(contact.objectB.getLinearVelocityFromAcceleration()); }

        return -contact.getRestitutionCoefficient() * (contact.getClosingVelocity() + relativeLinearVelocityFromAcceleration.dot(contact.getContactNormal()));
    }

    private static double calculateDeltaVelocity(Contact contact) {
        // desiredDeltaVelocity = targetClosingVelocity - closingVelocity
        return contact.getClosingVelocity() - contact.targetClosingVelocity;
    }

    private static void applyImpulse(Contact contact, Vector3dc impulse) {
        contact.objectA.applyImpulse(impulse, contact.getContactPos());
        if (contact.objectB != null) { contact.objectB.applyImpulse(new Vector3d(impulse).negate(), contact.getContactPos()); } // Does a few unnecessary calculations because I could use the same intermediate results
    }

    // Helper methods (Penetration resolution)
    private static double calculateBiasVelocity(Contact contact) { // Basically the targetClosingVelocity for split-impulse penetration resolution
        return PENETRATION_RESOLUTION_STRENGTH * Math.max(contact.getPenetrationDepth() - PENETRATION_RESOLUTION_SLOP, 0.0) / DELTA_TIME;
    }

    private static void applySplitImpulse(Contact contact, Vector3d impulse) {
        // ObjectA
        Vector3dc linearDelta = contact.objectA.calculateImpulseLinearVelocity(impulse);
        contact.objectA.addLinearCorrection(linearDelta);

        Vector3dc angularDelta = contact.objectA.calculateImpulseAngularVelocity(impulse, contact.getContactPos()); // TODO: Maybe move these methods (calculateImpulseXYZVelocity) into a helper class?
        Physics.LOGGER.info("Linear correction: {}", linearDelta.toString());
        Physics.LOGGER.info("Angular correction: {}", angularDelta.toString());
        contact.objectA.addAngularCorrection(angularDelta);

        // ObjectB
        if (contact.objectB == null) { return; }
        linearDelta = contact.objectB.calculateImpulseLinearVelocity(impulse);
        contact.objectB.addLinearCorrection(linearDelta);

        angularDelta = contact.objectB.calculateImpulseAngularVelocity(impulse, contact.getContactPos());
        contact.objectB.addAngularCorrection(angularDelta);
    }

    private static Vector3d calculateSplitImpulseContactVelocity(Contact contact) { // TODO: Clean up to remove duplicated code. Maybe make a util class, or move this to the Contact class?
        PhysicsObject objectA = contact.objectA;
        PhysicsObject objectB = contact.objectB;
        Vector3dc contactPos = contact.getContactPos();

        Vector3d relativeContactPos = new Vector3d();

        // pointVelocityA
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(objectA.getInternalPos());
        Vector3d pointVelocityA = new Vector3d(objectA.getAngularVelocity()).add(objectA.getAngularCorrection()).cross(relativeContactPos);
        pointVelocityA.add(objectA.getLinearVelocity()).add(objectA.getLinearCorrection());

        // pointVelocityB
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(objectB.getInternalPos());
        Vector3d pointVelocityB = new Vector3d(objectB.getAngularVelocity()).add(objectB.getAngularCorrection()).cross(relativeContactPos);
        pointVelocityB.add(objectB.getLinearVelocity()).add(objectA.getLinearCorrection());

        return pointVelocityA.sub(pointVelocityB);
    }
}