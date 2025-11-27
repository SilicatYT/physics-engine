package silicatyt.physicsref.simulation;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import silicatyt.physicsref.entity.PhysicsObject;

import static java.lang.Math.pow;
import static silicatyt.physicsref.PhysicsRef.loadedPhysicsObjects;
import static silicatyt.physicsref.simulation.Main.DELTA_TIME;

public class Integration {
    public static final Vector3d DEFAULT_GRAVITY = new Vector3d(0d, -9.81d, 0d).mul(DELTA_TIME); // Velocity difference per tick. So don't apply DELTA_TIME in tick again.
    public static final double DEFAULT_LINEAR_DAMPING = pow(0.7d, DELTA_TIME); // "After 1 second, this much of its speed should remain". This is necessary so less DELTA_TIME doesn't make damping stronger. It should stay identical.
    public static final double DEFAULT_ANGULAR_DAMPING = pow(0.7d, DELTA_TIME);

    // Phases
    public static void phaseOne() {
        for (PhysicsObject obj : loadedPhysicsObjects) {
            fixEntityPos(obj);
            updateLinearVelocity(obj);
            updatePos(obj);
            updateAngularVelocity(obj);
            updateOrientation(obj);
            // TODO: Do the remaining stuff

            // Clear accumulators
            obj.clearAccumulators();
        }
    }

    public static void phaseTwo() { // Normally, I'd update cornerPosAbsolute here so the line of sight checks for hitting the object can use the updated data. But because the mod always automatically updates derived data (at the cost of unnecessary computation), this isn't necessary here. In the datapack, I would manually update the cornerPosAbsolute here, however.
        for (PhysicsObject obj : loadedPhysicsObjects) {
            obj.updateVisuals();
            obj.updateEntityPos();
        }
    }

    // Helper methods
    private static void fixEntityPos(PhysicsObject obj) { // If the actual entity pos has changed (e.g. through teleportation or when the entity is summoned for the 1st time), the internal "pos" is set to the entity pos to allow for teleportation and summoning at the correct locations. Without this, the entity teleports to 0,0,0 upon being summoned and ignores being teleported around.
        if (!obj.getLastEntityPos().equals(obj.getEntityPos())) {
            obj.setInternalPos(obj.getEntityPos());
        }
    }

    private static void updateLinearVelocity(PhysicsObject obj) {
        // Apply gravity
        //obj.addLinearVelocity(DEFAULT_GRAVITY); // DELTA_TIME is already accounted for in the DEFAULT_GRAVITY constant. Otherwise, I'd multiply it here.

        // Apply accumulated force
        obj.addLinearVelocity(obj.getAccumulatedForce().mul(obj.getInverseMass()).mul(DELTA_TIME)); // Scale by DELTA_TIME because force's unit uses seconds. See Euler Integration.

        // Apply linear damping
        obj.scaleLinearVelocity(DEFAULT_LINEAR_DAMPING);
    }

    private static void updatePos(PhysicsObject obj) {
        obj.addInternalPos(obj.getLinearVelocity().mul(DELTA_TIME)); // Velocity is m/s, so I need to scale by DELTA_TIME
    }

    private static void updateAngularVelocity(PhysicsObject obj) {
        // Apply accumulated torque
        obj.addAngularVelocity(obj.getInverseInertiaTensorWorld().transform(obj.getAccumulatedTorque()).mul(DELTA_TIME));

        // Apply angular damping
        obj.scaleAngularVelocity(DEFAULT_ANGULAR_DAMPING);
    }

    /*private static void updateOrientation(PhysicsObject obj) { // Method: Euler integration (TODO: Less accurate but faster. How about in a datapack, where I can use entity rotation tricks to compute sin and cos quickly? What to choose there?)
        Vector3d angularVelocity = obj.getAngularVelocity(); // TODO: Maybe I didn't account for the DELTA_TIME properly in the datapack?
        Quaterniond orientation = obj.getOrientation();
        obj.setOrientation(
                orientation.add(new Quaterniond(angularVelocity.x, angularVelocity.y, angularVelocity.z, 0) // angularVelocity is treated as a quaternion
                        .mul(orientation)
                        .scale(0.5d * DELTA_TIME)
                )
        ); // I don't normalize it here because setOrientation() already does that automatically
    }*/

    private static void updateOrientation(PhysicsObject obj) { // Method: Exponential map integration
        Vector3d angularVelocity = obj.getAngularVelocity();
        double angularVelocityLength = angularVelocity.length();
        if (angularVelocityLength < 1e-12) { // No orientation change. Continuing here (normalizing at some point) would produce NaN. 1e-12 is used because it's "pretty much 0" and makes it ignore unstable divisors.
            return;
        }

        // Compute rotation magnitude
        double angle = angularVelocityLength * DELTA_TIME;

        // Normalize direction
        angularVelocity.normalize();

        // Create quaternion for the orientation change
        Quaterniond orientationChange = new Quaterniond().fromAxisAngleRad(angularVelocity, angle); // angularVelocity normalized is the axis

        // Apply orientation change
        obj.setOrientation(orientationChange.mul(obj.getOrientation())); // setOrientation() automatically normalizes, but for this method it's not required. Still good to normalize every couple hundred ticks or so to avoid accumulated precision errors.
    }

}
