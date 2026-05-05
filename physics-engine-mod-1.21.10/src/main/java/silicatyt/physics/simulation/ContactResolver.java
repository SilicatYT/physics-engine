package silicatyt.physics.simulation;

// TODO: Also add a resolver that prioritizes contacts with higher penetrationDepth and closingVelocity

import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactManager;
import silicatyt.physics.entity.PhysicsObject;

import java.util.List;

public class ContactResolver {
    private static final int NOF_ITERATIONS = 10;
    private static final double MIN_CLOSING_VELOCITY = 0d;
    private static final double MIN_PENETRATION_DEPTH = 0d;
    private static final double RESTITUTION_ACTIVATION_SPEED_THRESHOLD = 0.1d; // If the closing velocity is smaller than this, the coefficient of restitution will be set to 0

    public static void resolve(ContactManager manager) {
        List<Contact> contacts = manager.getContacts();
        for (int i = 0; i < NOF_ITERATIONS; i++) {
            for (Contact contact : contacts) {
                if (contact.getClosingVelocity() < MIN_CLOSING_VELOCITY) { continue; }
                resolveVelocity(contact);
            }
        }

        for (int i = 0; i < NOF_ITERATIONS; i++) {
            for (Contact contact : contacts) {
                if (contact.getPenetrationDepth() < MIN_PENETRATION_DEPTH) { continue; }
                resolvePenetrationLinearProjection(contact);
            }
        }
    }

    public static void resolveALT(ContactManager manager) {} // TODO: Same as resolve, but prioritize contacts with higher penetrationDepth and closingVelocity

    private static void resolveVelocity(Contact contact) { // TODO: Clamp the normal impulse to positive values, and prepare warm starting by applying the accumulatedImpulse, not just the current one. Even if the normal impulse is negative, it should still account for the tangential impulses
        PhysicsObject objectA = contact.objectA;
        PhysicsObject objectB = contact.objectB;

        double deltaVelocity = calculateDesiredDeltaVelocity(contact);
        Matrix3dc orthonormalBasis = contact.getOrthonormalBasis();

        Vector3d contactVelocityInContactSpace = new Vector3d(contact.getContactVelocity());
        orthonormalBasis.transformTranspose(contactVelocityInContactSpace);

        // Required impulse for velocity change
        Vector3dc inverseEffectiveMass = contact.getInverseEffectiveMass();
        Vector3d impulse = new Vector3d();
        impulse.x = deltaVelocity * inverseEffectiveMass.x();
        if (impulse.x <= 0.0) { return; } // TODO: TEMPORARY. DO NOT KEEP THIS, USE A MORE STABLE FORM FOR WARM STARTING
        impulse.y = -contactVelocityInContactSpace.y * inverseEffectiveMass.y();
        impulse.z = -contactVelocityInContactSpace.z * inverseEffectiveMass.z();

        // Friction
        double planarImpulseMagnitudeSquared = impulse.y*impulse.y + impulse.z*impulse.z;
        double maxFriction = contact.getFrictionCoefficient() * impulse.x;

        if (planarImpulseMagnitudeSquared > maxFriction*maxFriction) {
            // Use dynamic friction
            double scalingFactor = maxFriction / Math.sqrt(planarImpulseMagnitudeSquared);
            impulse.y *= scalingFactor;
            impulse.z *= scalingFactor;
        }

        // Transform impulse to world coordinates
        orthonormalBasis.transform(impulse);

        // Apply impulse
        double inverseMassTotal = objectA.getInverseMass();
        if (objectB != null) { inverseMassTotal += objectB.getInverseMass(); }

        objectA.applyImpulse(impulse, inverseMassTotal, contact.getContactPos()); // Calculates some things twice in exchange for readability and code re-use
        if (objectB != null) { objectB.applyImpulse(impulse.negate(), inverseMassTotal, contact.getContactPos()); }
    }

    private static void resolvePenetration(Contact contact) {}

    private static void resolvePenetrationLinearProjection(Contact contact) { // Simple linear projection
        PhysicsObject objectA = contact.objectA;
        PhysicsObject objectB = null;
        if (contact.objectB != null) { objectB = contact.objectB; }

        double inverseMassTotal = objectB == null ? objectA.getInverseMass() : objectA.getInverseMass() + objectB.getInverseMass();
        Vector3d linearMovementPerInverseMass = new Vector3d(contact.getContactNormal()).mul(contact.getPenetrationDepth()).div(inverseMassTotal);

        Vector3d linearMovementA = new Vector3d(linearMovementPerInverseMass).mul(objectA.getInverseMass());
        objectA.setInternalPos(linearMovementA.add(objectA.getInternalPos()));
        if (objectB != null) {
            Vector3d linearMovementB = new Vector3d(linearMovementPerInverseMass).mul(-1 * objectB.getInverseMass()); // TODO: I could optimize a few calculations by re-using previous objects that I no longer need, and calling them "linearMovement" for example. Not sure if that would be clean though.
            objectB.setInternalPos(linearMovementB.add(objectB.getInternalPos()));
        }
    }





    // Helper methods (Velocity resolution)
    private static double calculateDesiredDeltaVelocity(Contact contact) {
        // desiredDeltaVelocity = closingVelocity + restitution * (closingVelocity + relativeVelocityFromAcceleration)
        Vector3d deltaVelocity = new Vector3d();
        if (contact.getClosingVelocity() >= RESTITUTION_ACTIVATION_SPEED_THRESHOLD) {
            Vector3d relativeLinearVelocityFromAcceleration = new Vector3d(contact.objectA.getLinearVelocityFromAcceleration());
            if (contact.objectB != null) { relativeLinearVelocityFromAcceleration.sub(contact.objectB.getLinearVelocityFromAcceleration()); }
            deltaVelocity.set(contact.getContactVelocity()).sub(relativeLinearVelocityFromAcceleration);
            deltaVelocity.mul(-contact.getRestitutionCoefficient());
        }

        deltaVelocity.sub(contact.getContactVelocity());
        return deltaVelocity.dot(contact.getContactNormal());
    }

    // Helper methods (Penetration resolution)
}
