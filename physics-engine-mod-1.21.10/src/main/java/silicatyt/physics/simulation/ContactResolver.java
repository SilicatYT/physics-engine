package silicatyt.physics.simulation;

// TODO: Also add a resolver that prioritizes contacts with higher penetrationDepth and closingVelocity

import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactManager;

import java.util.List;

import static silicatyt.physics.simulation.Main.DELTA_TIME;

public class ContactResolver {
    private static final int NOF_VELOCITY_RESOLUTION_ITERATIONS = 10;
    private static final int NOF_PENETRATION_RESOLUTION_ITERATIONS = 10;
    private static final double MIN_DELTA_VELOCITY = 0.01d;
    private static final double MIN_PENETRATION_DEPTH = 0.01d;
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.3d; // If the closing velocity is smaller than this, the coefficient of restitution will be set to 0
    private static final double PENETRATION_RESOLUTION_STRENGTH = 0.25d; // How much of the penetration is resolved per iteration
    private static final double PENETRATION_RESOLUTION_SLOP = 0.01d;

    public static void resolve(ContactManager manager) {
        List<Contact> contacts = manager.getContacts();

        // Preparations
        for (Contact contact : contacts) {
            // Calculate target closing velocity (velocity resolution) that is used for the rest of the tick
            contact.setTargetClosingVelocity(calculateTargetClosingVelocity(contact));

            // Calculate the bias velocity (penetration resolution) that's used for the rest of the tick
            contact.setBiasVelocity(calculateBiasVelocity(contact));
            contact.clearAccumulatedSplitImpulse();

            // Warm-starting
            if (!contact.getAccumulatedImpulse().equals(0d, 0d, 0d)) {
                Vector3d impulseWorld = new Vector3d(contact.getAccumulatedImpulse());
                contact.getOrthonormalBasis().transform(impulseWorld); // TODO: Maybe I can optimize it so I don't need this extra transformation, for example if I store the accumulated impulse in world coordinates, and add some (cheaper) calculations in resolveVelocity()?
                applyImpulse(contact, impulseWorld);
            }
        }

        // Velocity resolution
        for (int i = 0; i < NOF_VELOCITY_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contacts) {
                resolveVelocity(contact);
            }
        }

        // Penetration resolution
        for (int i = 0; i < NOF_PENETRATION_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contacts) {
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

        if (planarImpulseMagnitudeSquared > maxFriction*maxFriction) { // Use dynamic friction
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
        double biasVelocity = contact.getBiasVelocity();
        //double deltaVelocity = biasVelocity - calculateSplitImpulseContactVelocity(contact).dot(contact.getContactNormal());
        double deltaVelocity = PENETRATION_RESOLUTION_STRENGTH * (biasVelocity - calculateSplitImpulseContactVelocity(contact).dot(contact.getContactNormal()));
        //if (Math.abs(deltaVelocity) < MIN_PENETRATION_DEPTH) { return; } TODO: IMPROVE THIS CHECK. RN it's bugged because it compares meters with meters/second.

        double impulse = deltaVelocity * contact.getInverseEffectiveMass().x(); // Only the component along the contact normal
        double accumulatedImpulse = contact.getAccumulatedSplitImpulse();

        double combinedImpulse = Math.max(0d, impulse + accumulatedImpulse);

        Vector3d deltaImpulse = new Vector3d(combinedImpulse - accumulatedImpulse, 0d, 0d);
        contact.addAccumulatedSplitImpulse(deltaImpulse.x);

        contact.getOrthonormalBasis().transform(deltaImpulse);

        // Apply impulse (Add it to linear & angular correction)
        applySplitImpulse(contact, deltaImpulse);
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
        // desiredDeltaVelocity = closingVelocity - targetClosingVelocity
        return contact.getClosingVelocity() - contact.getTargetClosingVelocity();
    }

    private static void applyImpulse(Contact contact, Vector3dc impulse) {
        contact.objectA.applyImpulse(impulse, contact.getContactPos());
        if (contact.objectB != null) { contact.objectB.applyImpulse(new Vector3d(impulse).negate(), contact.getContactPos()); } // Does a few unnecessary calculations because I could use the same intermediate results
    }





    // Helper methods (Penetration resolution)
    private static double calculateBiasVelocity(Contact contact) { // Basically the targetClosingVelocity for split-impulse penetration resolution
        //return PENETRATION_RESOLUTION_STRENGTH * Math.max(contact.getPenetrationDepth() - PENETRATION_RESOLUTION_SLOP, 0.0) / DELTA_TIME;
        return Math.max(contact.getPenetrationDepth() - PENETRATION_RESOLUTION_SLOP, 0.0) / DELTA_TIME;
    }

    private static void applySplitImpulse(Contact contact, Vector3d impulse) {
        // ObjectA
        Vector3dc linearDelta = contact.objectA.calculateImpulseLinearVelocity(impulse);
        contact.objectA.addLinearCorrection(linearDelta);

        Vector3dc angularDelta = contact.objectA.calculateImpulseAngularVelocity(impulse, contact.getContactPos()); // TODO: Maybe move these methods (calculateImpulseXYZVelocity) into a helper class?
        contact.objectA.addAngularCorrection(angularDelta);

        // ObjectB
        if (contact.objectB == null) { return; }
        impulse.negate();
        linearDelta = contact.objectB.calculateImpulseLinearVelocity(impulse);
        contact.objectB.addLinearCorrection(linearDelta);

        angularDelta = contact.objectB.calculateImpulseAngularVelocity(impulse, contact.getContactPos());
        contact.objectB.addAngularCorrection(angularDelta);
    }

    private static Vector3d calculateSplitImpulseContactVelocity(Contact contact) {
        return contact.calculateContactVelocity(
                new Vector3d(contact.objectA.getLinearCorrection()),
                new Vector3d(contact.objectA.getAngularCorrection()),
                new Vector3d(contact.objectB.getLinearCorrection()),
                new Vector3d(contact.objectB.getAngularCorrection())
        );
    }
}

// TODO: Do I really need slop?
// TODO: Should I really fix 100% of the penetration each tick, or multiply biasVelocity by some percentage?
// TODO: Should I really make it so each iteration only resolves a fraction of the current tick's penetration, so that multiple iterations are necessary?
// TODO: Why do objects visually clip into the ground for 1 tick after falling? Shouldn't all the correction be applied before the visual update? Or are corrections delayed by 1 tick?
// TODO: The sliding & rotating also happened with linear projection, and it's improved by fixing the restitution threshold (so it ignores gravity). But maybe the rotation is slightly stronger with split-impulse (while the general stability is higher)? Is it caused by velocity or penetration resolution?
// TODO: Objects are stable shortly after falling to the ground. Then, it takes a while until they start jittering/sliding. What's the problem here?
// TODO: Is it a problem that my orthonormal basis can completely change from one tick to the next, could this affect warm-starting? Is this the reason why my objects start jittering at some point?
// TODO: SETTING FRICTION TO 0 MAKES THE JITTER AND DRIFTING DISAPPEAR! INVESTIGATE!



// TODO: In warm-starting, I transform the impulse from contact coordinates to world coordinates before I apply it. I store it in contact coordinates, but in the current tick's. The next tick, transforming back will mean something else, because the normal has maybe changed.