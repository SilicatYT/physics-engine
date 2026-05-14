package silicatyt.physics.simulation;

// TODO: I could optimize a few calculations by re-using previous objects that I no longer need, and calling them "linearMovement" for example. It would avoid some "new Vector3d(...)" calls, but I'm not sure if that would be clean.

import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactManager;
import silicatyt.physics.entity.PhysicsObject;

import java.util.LinkedList;
import java.util.List;

import static silicatyt.physics.simulation.Main.DELTA_TIME;

public class ContactResolver {
    private static final int NOF_VELOCITY_RESOLUTION_ITERATIONS = 10;
    private static final int NOF_PENETRATION_RESOLUTION_ITERATIONS = 10;
    private static final double MIN_DELTA_VELOCITY = 0.01d;
    private static final double MIN_PENETRATION_DEPTH = 0.001d;
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.3d; // If the closing velocity is smaller than this, the coefficient of restitution will be set to 0

    public static void resolve(ContactManager manager) {
        List<Contact> contacts = manager.getContacts();

        List<Contact> contactsSortedByVelocity = new LinkedList<>(contacts); // Sorted descending
        contactsSortedByVelocity.sort((a, b) -> Double.compare(
                b.getAccumulatedImpulseWorld().dot(b.getContactNormal()),
                a.getAccumulatedImpulseWorld().dot(a.getContactNormal())));

        List<Contact> contactsSortedByPenetration = new LinkedList<>(contacts); // Sorted descending
        contactsSortedByPenetration.sort((a, b) -> Double.compare(
                b.getAccumulatedSplitImpulse(),
                a.getAccumulatedSplitImpulse()));

        // Preparations
        for (Contact contact : contacts) {
            // Calculate target closing velocity (velocity resolution) that is used for the rest of the tick
            contact.setTargetClosingVelocity(calculateTargetClosingVelocity(contact));

            // Calculate the bias velocity (penetration resolution) that's used for the rest of the tick
            contact.setBiasVelocity(calculateBiasVelocity(contact));
            contact.clearAccumulatedSplitImpulse();

            // Warm-starting
            if (contact.getAccumulatedImpulseWorld().lengthSquared() >= MIN_DELTA_VELOCITY*MIN_DELTA_VELOCITY) {
                Vector3d impulseWorld = new Vector3d(contact.getAccumulatedImpulseWorld());
                applyImpulse(contact, impulseWorld);
            }
        }

        // Velocity resolution
        for (int i = 0; i < NOF_VELOCITY_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contactsSortedByVelocity) {
                resolveVelocity(contact);
            }
        }

        // Penetration resolution
        for (int i = 0; i < NOF_PENETRATION_RESOLUTION_ITERATIONS; i++) {
            for (Contact contact : contactsSortedByPenetration) {
                resolvePenetration(contact);
            }
        }
    }

    private static void resolveVelocity(Contact contact) {
        double deltaVelocity = calculateDeltaVelocity(contact);
        //if (Math.abs(deltaVelocity) < MIN_DELTA_VELOCITY) { return; } // TODO: Can't return early, messes with friction. Need to separate the friction early-return (coulomb) with this one (for tangential) so I can still benefit of a performance boost

        Matrix3dc orthonormalBasis = contact.getOrthonormalBasis();

        Vector3d contactVelocityInContactSpace = new Vector3d(contact.getContactVelocity());
        orthonormalBasis.transformTranspose(contactVelocityInContactSpace);

        // Required impulse for velocity change
        Vector3dc inverseEffectiveMass = contact.getEffectiveMass();
        Vector3d impulse = new Vector3d();
        impulse.x = deltaVelocity * inverseEffectiveMass.x();
        impulse.y = -contactVelocityInContactSpace.y * inverseEffectiveMass.y();
        impulse.z = -contactVelocityInContactSpace.z * inverseEffectiveMass.z();

        Vector3d accumulatedImpulse = new Vector3d(contact.getAccumulatedImpulseWorld());
        contact.getOrthonormalBasis().transformTranspose(accumulatedImpulse); // TODO: Can I remove some transformations by calculating the impulse with the world space contactVelocity first, so I don't have to transform the accumulatedImpulse?

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

        // Transform impulse to world coordinates
        Vector3d deltaImpulse = combinedImpulse.sub(accumulatedImpulse); // Same reference, just a different name for readability
        orthonormalBasis.transform(deltaImpulse);

        // Update the accumulatedImpulse
        contact.addAccumulatedImpulseWorld(deltaImpulse);

        // Apply impulse
        applyImpulse(contact, deltaImpulse);
    }

    private static void resolvePenetration(Contact contact) { // Split-impulse
        double biasVelocity = contact.getBiasVelocity();
        double deltaVelocity = biasVelocity - calculateSplitImpulseContactVelocity(contact).dot(contact.getContactNormal());
        //if (Math.abs(deltaVelocity) < MIN_PENETRATION_DEPTH / DELTA_TIME) { return; } TODO: IMPROVE THIS CHECK. RN it's bugged because it compares meters with meters/second.

        double impulse = deltaVelocity * contact.getEffectiveMass().x(); // Only the component along the contact normal
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
        //if (contact.getClosingVelocity() < RESTITUTION_ACTIVATION_SPEED_THRESHOLD) { return 0d; } // Ignore restitution if the speed is small, for stability
        // TODO: ^ vielleicht (closingVelocity + velocityFromAcceleration) < ... ? Dann wäre gravity egal für restitution und würde ignoriert werden, oder?

        Vector3d relativeLinearVelocityFromAcceleration = new Vector3d(contact.objectA.getLinearVelocityFromAcceleration());
        if (contact.objectB != null) { relativeLinearVelocityFromAcceleration.sub(contact.objectB.getLinearVelocityFromAcceleration()); }

        if (contact.getClosingVelocity() + relativeLinearVelocityFromAcceleration.dot(contact.getContactNormal()) < RESTITUTION_ACTIVATION_SPEED_THRESHOLD) { return 0d; } // Ignore restitution if the speed is small, for stability

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
        return Math.max(contact.getPenetrationDepth(), 0.0) / DELTA_TIME;
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

// TODO: Objects are stable shortly after falling to the ground. Then, it takes a while until they start jittering/sliding. What's the problem here?
// TODO: Is it a problem that my orthonormal basis can completely change from one tick to the next, could this affect warm-starting? Is this the reason why my objects start jittering at some point?
// TODO: SETTING FRICTION TO 0 MAKES THE JITTER AND DRIFTING DISAPPEAR! INVESTIGATE!



// TODO: In warm-starting, I transform the impulse from contact coordinates to world coordinates before I apply it. I store it in contact coordinates, but in the current tick's. The next tick, transforming back will mean something else, because the normal has maybe changed.// TODO: When I disable warm-starting (while the temporary fix of "if tangential movement is small, set the impulse to 0" is active), the random sliding stops, but friction seems to be strongly reduced, so objects slide as if on ice.
// TODO: => It looks like warm-starting messes with things in that the previously accumulated impulse is applied while the object is rotated slightly differently from before, causing the impulse that's being applied to be incorrect? But warm-starting is only a "first draft" for the resolution, so it should get overwritten by future resolutions, no?
// TODO: => Also, disabling both warm-starting AND the temporary fix still has the objects slide around randomly.
// TODO: => Overall. I'm very unsure as to what the problem is...



// TODO: Stacks of 3 or more are not stable (They slide around)



// TODO: => The sliding bug was mainly caused by the early return in velocity resolution, BUT warm-starting still makes objects slide *once* after some time and then stop?? If the problem is outdated contact bases, why would it stop? Warm-starting also breaks stability for stacks (objects slide very fast)
// TODO: => And do I need penetration slop or not?





// TODO: --------------------------

// TODO: I just changed accumulatedImpulse to be in world coordinates, but it's mathmatically the same as before. The bug (objects slide a little bit after a few seconds before stabilizing) is LIKELY in the orthonormalBasis calculation, I need to change it so its "seam" isn't as obvious on a flat floor.
// TODO: Not sure why stacked objects are so unstable, even though I've sorted my contacts. Even if I decrease deltatime and increase the number of iterations. Does it properly create 4 contacts on the ground, or is there an issue with that?
// TODO: Maybe carry over accumulatedImpulse both in world and contact space, so I don't need any transformations? Or maybe keep the basis from the previous tick if it's similar enough, so I don't have drift?
// TODO: Add early outs for velocity and penetration resolution (velocity resolution's early exit needs to be separate for tangential and normal)
// TODO: Why do objects visually clip into the ground for 1 tick after falling? Shouldn't all the correction be applied before the visual update?
// TODO: Clean up everything, optimize some operations (to remove new Vector3d(...)), add more helper methods so each method only does one thing, replace setters with "add..." where appropriate to remove a new() call
// TODO: Why do objects slide down slopes with frictionCoefficient 1?