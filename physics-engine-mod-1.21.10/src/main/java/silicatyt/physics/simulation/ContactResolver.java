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
    }





    // Helper methods (Velocity resolution)
    private static double calculateDesiredDeltaVelocity(Contact contact) {
        // desiredDeltaVelocity = closingVelocity + restitution * (closingVelocity + relativeVelocityFromAcceleration)
        Vector3d relativeLinearVelocityFromAcceleration =  new Vector3d(contact.objectA.getLinearVelocityFromAcceleration());
        if (contact.objectB != null) { relativeLinearVelocityFromAcceleration.sub(contact.objectB.getLinearVelocityFromAcceleration()); }
        Vector3d deltaVelocity = new Vector3d(contact.getContactVelocity()).sub(relativeLinearVelocityFromAcceleration);
        deltaVelocity.mul(contact.getRestitutionCoefficient());
        deltaVelocity.add(contact.getContactVelocity());
        deltaVelocity.negate();

        return deltaVelocity.dot(contact.getContactNormal());
    }

    // Helper methods (Penetration resolution)
}
