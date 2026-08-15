package net.silicatyt.physicsengine.simulation;

import net.minecraft.world.phys.Vec3;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import static java.lang.Math.pow;
import static net.silicatyt.physicsengine.simulation.Main.DELTA_TIME;

public class Integrator {
    //public static final Vector3dc DEFAULT_GRAVITY = new Vector3d(0.0, -9.81, 0.0);
    public static final Vector3dc DEFAULT_GRAVITY = new Vector3d(0.0, 0.0, 0.0);
    public static final double DEFAULT_LINEAR_DAMPING = 0.95; // "After 1 second, this much of its linear velocity should remain".
    public static final double DEFAULT_ANGULAR_DAMPING = 0.95;

    private static final double LINEAR_DAMPING_MULTIPLIER = pow(DEFAULT_LINEAR_DAMPING, DELTA_TIME); // TODO: If I make damping configurable per-object, this will need to be stored in the PhysicsObject class as an instance variable, or I just calculate it on the fly
    private static final double ANGULAR_DAMPING_MULTIPLIER = pow(DEFAULT_ANGULAR_DAMPING, DELTA_TIME);

    public static final double EPSILON = 1e-12;
    public static final double EPSILON_SQUARED = EPSILON * EPSILON;

    // Phases
    public static void phaseOne(PhysicsObject obj) { // Update internal state
        fixEntityPos(obj);
        updateLinearVelocity(obj);
        updatePos(obj, obj.getLinearVelocity());
        updateAngularVelocity(obj);
        updateOrientation(obj, obj.getAngularVelocity());
    }

    public static void phaseTwo(PhysicsObject obj) {
        obj.updateTransformation();
        obj.updateEntityPos();
        obj.clearAccumulators();
    }


    // Helper methods
    private static void fixEntityPos(PhysicsObject obj) { // If the actual entity pos has changed (i.e. by teleportation, or when the entity is summoned for the 1st time), 'internalPos' is then set to the entity pos to allow for teleportation and summoning at the correct locations. Without this, the entity teleports to 0,0,0 upon being summoned and ignores being teleported around.
        Vec3 entityPos = obj.position();
        if (!obj.getLastEntityPos().equals(entityPos)) obj.setInternalPos(entityPos.x, entityPos.y, entityPos.z);
    }

    private static void updateLinearVelocity(PhysicsObject obj) {
        // Apply linear damping
        Vector3dc linearVelocity = obj.getLinearVelocity();
        double dampedVelocityX = linearVelocity.x() * LINEAR_DAMPING_MULTIPLIER;
        double dampedVelocityY = linearVelocity.y() * LINEAR_DAMPING_MULTIPLIER;
        double dampedVelocityZ = linearVelocity.z() * LINEAR_DAMPING_MULTIPLIER;
        obj.setLinearVelocity(dampedVelocityX, dampedVelocityY, dampedVelocityZ); // TODO: Should I somehow merge this call with addLinearVelocityAcceleration to reduce the input validation overhead?

        // Apply accumulated force & gravity
        double inverseMass = obj.getInverseMass();
        if (inverseMass != 0.0) { // Static objects (Like floating moving platforms) should not be affected, but velocity should still be settable manually
            Vector3dc force = obj.getAccumulatedForce();
            double velocityFromAccelerationX = (force.x() * inverseMass + DEFAULT_GRAVITY.x()) * DELTA_TIME;
            double velocityFromAccelerationY = (force.y() * inverseMass + DEFAULT_GRAVITY.y()) * DELTA_TIME;
            double velocityFromAccelerationZ = (force.z() * inverseMass + DEFAULT_GRAVITY.z()) * DELTA_TIME;
            obj.addLinearVelocityAcceleration(velocityFromAccelerationX, velocityFromAccelerationY, velocityFromAccelerationZ);
        }
    }

    private static void updatePos(PhysicsObject obj, Vector3dc movement) {
        Vector3dc pos = obj.getInternalPos();
        obj.setInternalPos(
                pos.x() + movement.x() * DELTA_TIME,
                pos.y() + movement.y() * DELTA_TIME,
                pos.z() + movement.z() * DELTA_TIME
        );
    }

    private static void updateAngularVelocity(PhysicsObject obj) {
        // Apply angular damping
        Vector3dc angularVelocity = obj.getAngularVelocity();
        double dampedVelocityX = angularVelocity.x() * ANGULAR_DAMPING_MULTIPLIER;
        double dampedVelocityY = angularVelocity.y() * ANGULAR_DAMPING_MULTIPLIER;
        double dampedVelocityZ = angularVelocity.z() * ANGULAR_DAMPING_MULTIPLIER;
        obj.setAngularVelocity(dampedVelocityX, dampedVelocityY, dampedVelocityZ); // TODO: Should I somehow merge this call with applyTorque to reduce the input validation overhead?

        // Apply accumulated torque
        Vector3dc torque = obj.getAccumulatedTorque();
        obj.applyTorque(torque);
    }

    private static void updateOrientation(PhysicsObject obj, Vector3dc angularVelocity) { // Approach: Exponential map integration
        double angularVelocityLengthSquared = angularVelocity.lengthSquared();
        if (angularVelocityLengthSquared < EPSILON_SQUARED) return; // No orientation change. Continuing here (normalizing at some point) would produce NaN. 1e-12 is used because it's "pretty much 0" and makes it ignore unstable divisors.
        double angle = Math.sqrt(angularVelocityLengthSquared) * DELTA_TIME;
        obj.rotateOrientation(angle, angularVelocity);
    }
}
