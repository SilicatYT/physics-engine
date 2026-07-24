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

        /*if ((contactVelocityInContactSpace.y*contactVelocityInContactSpace.y + contactVelocityInContactSpace.z*contactVelocityInContactSpace.z) < 0.2) { // TODO: TEMPORARY "FIX" FOR FRICTION CAUSING SLIDING
            impulse.y = 0d;
            impulse.z = 0d;
        }*/

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
        //if (contact.getClosingVelocity() < RESTITUTION_ACTIVATION_SPEED_THRESHOLD) { return 0d; } // Ignore restitution if the speed is small, for stability
        // TODO: ^ vielleicht (closingVelocity + velocityFromAcceleration) < ... ? Dann wäre gravity egal für restitution und würde ignoriert werden, oder?

        Vector3d relativeLinearVelocityFromAcceleration = new Vector3d(contact.objectA.getLinearVelocityFromAcceleration());
        if (contact.objectB != null) { relativeLinearVelocityFromAcceleration.sub(contact.objectB.getLinearVelocityFromAcceleration()); }

        // TODO: Entfernen (oder behalten? Überprüfen! Müsste dann natürlich optimieren, so dass ich nicht 2x das ".dot()" habe)
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
        return Math.max(contact.getPenetrationDepth() - 0.01, 0.0) / DELTA_TIME; // TODO: Add a slop constant. Slop is necessary so that all contacts are penetrating at all times, otherwise stacks become unstable.
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



// TODO: I just changed accumulatedImpulse to be in world coordinates, but it's mathematically the same as before. The bug (objects slide a little bit after a few seconds before stabilizing) is LIKELY in the orthonormalBasis calculation, I need to change it so its "seam" isn't as obvious on a flat floor.
// TODO: Not sure why stacked objects are so unstable, even though I've sorted my contacts. Even if I decrease deltaTime and increase the number of iterations. Does it properly create 4 contacts on the ground, or is there an issue with that?
// TODO: Maybe carry over accumulatedImpulse both in world and contact space, so I don't need any transformations? Or maybe keep the basis from the previous tick if it's similar enough, so I don't have drift?
// TODO: Add early outs for velocity and penetration resolution (velocity resolution's early exit needs to be separate for tangential and normal)
// TODO: Why do objects visually clip into the ground for 1 tick after falling? Shouldn't all the correction be applied before the visual update?
// TODO: Clean up everything, optimize some operations (to remove new Vector3d(...)), add more helper methods so each method only does one thing, replace setters with "add..." where appropriate to remove a new() call
// TODO: Why do objects slide down slopes with frictionCoefficient 1?




// TODO: After adding multiple contacts per tick, it seems like the old contacts don't get deactivated properly? If I create a stack and make one object slide off the other, the object doesn't fall off until the last corner is no longer penetrating
// TODO: Stacks are incredibly unstable, and the number of active contacts fluctuates insanely. If I spawn a 20x2x20 object, with a 2x2x2 ontop, it alternates between 4 and 8 contacts, even though it should always have 8. So the top object doesn't detect any contacts with the 20x2x20 one every other tick.
// TODO: ^ The issue is mostly accumulation (not checking for tangential bounds). Now that I generate every contact every tick, I don't need to accumulate pointFace, so it's much more stable. BUT: The more objects are ontop of another, regardless of stack height, the more unstable it gets. Can this be fixed?
// TODO: ^ if I remove accumulation for pointFace, will I suffer a performance loss?


// TODO: Idea: If I can't fix warm-starting, only warm-start the normal component
// TODO: Should I clear splitImpulse between every tick, or keep it? It's just a position correction, so I think keeping it would be suboptimal

// TODO: WAIT, shouldn't I check whether the contact is in-bounds (or at least for the tangential axes) during accumulation? Did I forget to do that...?
// TODO: Check if the tangential bounds check in ContactGenerator (pointFace) actually checks for the OBB, or just the AABB. It *MIGHT* generate contacts for points that don't actually penetrate, leaving objects floating.
// TODO: ^ for edge-edge, maybe no such equivalent check is needed? Either that, or a full "is inside OBB" check.

// TODO: Potential bug: It could potentially generate AND KEEP both sets of corner contacts (the original, and the fallback) if it changes without removing the old contacts. The problem is that it will also reset the accumulatedImpulse
// TODO: ^ maybe that actually happens. It might alternate because of small angle differences? Though it shouldn't, because only one object's corners are actually in-bounds... Still, I need to check this. It's definitely a bug anyway


// TODO: I think it's relatively stable if I sort ascending and disable warm-starting and make 40 iterations.