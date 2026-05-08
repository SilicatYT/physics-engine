package silicatyt.physics.simulation;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import static java.lang.Math.pow;
import static silicatyt.physics.simulation.Main.DELTA_TIME;

public class Integrator {
    public static final Vector3d DEFAULT_GRAVITY = new Vector3d(0d, -9.81d, 0d);
    public static final double DEFAULT_LINEAR_DAMPING = pow(0.7d, DELTA_TIME); // "After 1 second, this much of its linear velocity should remain".
    public static final double DEFAULT_ANGULAR_DAMPING = pow(0.7d, DELTA_TIME);

    // Phases
    public static void phaseOne(PhysicsObject obj) { // Update internal state
        fixEntityPos(obj);
        updateLinearVelocity(obj);
        updatePos(obj);
        updateAngularVelocity(obj);
        updateOrientationExponentialMap(obj, obj.getAngularVelocity());
    }

    public static void phaseTwo(PhysicsObject obj) { // Update visual state & reset accumulators
        // Apply split-impulse
        obj.setInternalPos(new Vector3d(obj.getInternalPos()).add(new Vector3d(obj.getLinearCorrection()).mul(DELTA_TIME)));
        updateOrientationExponentialMap(obj, new Vector3d(obj.getAngularCorrection()).mul(DELTA_TIME)); // TODO: multiply by deltatime here? Add the regular linear velocity? I originally just had "add the correction"
        obj.clearCorrection();

        // Update visuals
        obj.updateVisuals();
        obj.updateEntityPos();

        // Clear accumulators
        obj.accumulatedForce.zero();
        obj.accumulatedTorque.zero(); // The accumulatedForce and accumulatedTorque still need to be stored until now so I can subtract them from contactVelocity during contact generation. I could also precalculate the stuff in integration, but then I'd need an additional instance variable. I could also store the velocityPrev (which just wouldn't include the acceleration induced velocity), which would be even faster. BUT: Damping should NOT be removed from the velocity, so that makes it a tiny bit more complicated.
    }





    // Helper methods
    private static void fixEntityPos(PhysicsObject obj) { // If the actual entity pos has changed (i.e. by teleportation, or when the entity is summoned for the 1st time), the internal "pos" is set to the entity pos to allow for teleportation and summoning at the correct locations. Without this, the entity teleports to 0,0,0 upon being summoned and ignores being teleported around.
        Vec3d entityPos = obj.getEntityPos();
        if (!obj.getLastEntityPos().equals(entityPos)) { obj.setInternalPos(entityPos); }
    }

    private static void updateLinearVelocity(PhysicsObject obj) {
        // Apply accumulated force & gravity
        Vector3d velocityFromAcceleration = new Vector3d(obj.accumulatedForce).mul(obj.getInverseMass());
        //velocityFromAcceleration.add(DEFAULT_GRAVITY);
        velocityFromAcceleration.mul(DELTA_TIME);
        obj.setLinearVelocityFromAcceleration(velocityFromAcceleration);

        // Apply linear damping
        obj.setLinearVelocity(velocityFromAcceleration.add(obj.getLinearVelocity()).mul(DEFAULT_LINEAR_DAMPING));
    }

    private static void updatePos(PhysicsObject obj) {
        Vector3d movement = new Vector3d(obj.getLinearVelocity()).mul(DELTA_TIME);
        obj.setInternalPos(movement.add(obj.getInternalPos()));
    }

    private static void updateAngularVelocity(PhysicsObject obj) {
        // Apply accumulated torque
        Vector3d scaledTorque = new Vector3d(obj.accumulatedTorque).mul(DELTA_TIME);
        Vector3d velocityFromAcceleration = obj.getInverseInertiaTensorWorld().transform(scaledTorque);

        // Apply angular damping
        obj.setAngularVelocity(velocityFromAcceleration.add(obj.getAngularVelocity()).mul(DEFAULT_ANGULAR_DAMPING));
    }

    private static void updateOrientationEuler(PhysicsObject obj, Vector3dc angularVelocity) { // Approach: Euler integration (TODO: Less accurate but faster. How about in a datapack, where I can use entity rotation tricks to compute sin and cos quickly? What to choose there?)
        Quaterniond orientation = new Quaterniond(obj.getOrientation());
        obj.setOrientation(
                orientation.add(new Quaterniond(angularVelocity.x(), angularVelocity.y(), angularVelocity.z(), 0) // angularVelocity is treated as a quaternion
                        .mul(orientation)
                        .scale(0.5d * DELTA_TIME)
                )
        ); // I don't normalize it here because setOrientation() already does that automatically
    }

    private static void updateOrientationExponentialMap(PhysicsObject obj, Vector3dc angularVelocity) { // Approach: Exponential map integration (TODO: Maybe move into an "applyAngularVelocity()" method inside PhysicsObject?)
        double angularVelocityLength = angularVelocity.length();
        if (angularVelocityLength < 1e-12) { // No orientation change. Continuing here (normalizing at some point) would produce NaN. 1e-12 is used because it's "pretty much 0" and makes it ignore unstable divisors.
            return;
        }

        // Compute rotation magnitude
        double angle = angularVelocityLength * DELTA_TIME;

        // Normalize direction
        Vector3d axis = new Vector3d(angularVelocity).normalize();

        // Create quaternion for the orientation change
        Quaterniond orientationChange = new Quaterniond().fromAxisAngleRad(axis, angle); // angularVelocity normalized is the axis

        // Apply orientation change
        obj.setOrientation(orientationChange.mul(obj.getOrientation())); // setOrientation() automatically normalizes, but for this method it's not required. Still good to normalize every couple hundred ticks or so to avoid accumulated precision errors.
    }

}
