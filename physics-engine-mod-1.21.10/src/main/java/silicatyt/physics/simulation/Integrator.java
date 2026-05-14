package silicatyt.physics.simulation;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.Physics;
import silicatyt.physics.entity.PhysicsObject;

import static java.lang.Math.pow;
import static silicatyt.physics.simulation.Main.DELTA_TIME;

public class Integrator {
    public static final Vector3d DEFAULT_GRAVITY = new Vector3d(0d, -9.81d, 0d);
    public static final double DEFAULT_LINEAR_DAMPING = pow(0.9d, DELTA_TIME); // "After 1 second, this much of its linear velocity should remain".
    public static final double DEFAULT_ANGULAR_DAMPING = pow(0.9d, DELTA_TIME);

    // Phases
    public static void phaseOne(PhysicsObject obj) { // Update internal state
        fixEntityPos(obj);
        updateLinearVelocity(obj);
        updatePos(obj, obj.getLinearVelocity());
        updateAngularVelocity(obj);
        updateOrientationExponentialMap(obj, obj.getAngularVelocity());
    }

    public static void phaseTwo(PhysicsObject obj) {
        // Apply split-impulse corrections
        updatePos(obj, obj.getLinearCorrection());
        updateOrientationExponentialMap(obj, obj.getAngularCorrection());
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
        // TEMPORARY (TODO: REMOVE)
        if (obj.getInverseMass() == 0d) { return; }

        // Apply linear damping
        Vector3d dampedVelocity = new Vector3d(obj.getLinearVelocity()).mul(DEFAULT_LINEAR_DAMPING);

        // Apply accumulated force & gravity
        Vector3d velocityFromAcceleration = new Vector3d(obj.accumulatedForce).mul(obj.getInverseMass());
        velocityFromAcceleration.add(DEFAULT_GRAVITY);
        velocityFromAcceleration.mul(DELTA_TIME);
        obj.setLinearVelocityFromAcceleration(velocityFromAcceleration);

        // Apply linear damping
        obj.setLinearVelocity(dampedVelocity.add(velocityFromAcceleration));
    }

    private static void updatePos(PhysicsObject obj, Vector3dc linearVelocity) {
        Vector3d movement = new Vector3d(linearVelocity).mul(DELTA_TIME);
        obj.setInternalPos(movement.add(obj.getInternalPos()));
    }

    private static void updateAngularVelocity(PhysicsObject obj) {
        // Apply angular damping
        Vector3d dampedVelocity = new Vector3d(obj.getAngularVelocity()).mul(DEFAULT_ANGULAR_DAMPING);

        // Apply accumulated torque
        Vector3d scaledTorque = new Vector3d(obj.accumulatedTorque).mul(DELTA_TIME);
        Vector3d velocityFromAcceleration = obj.getInverseInertiaTensorWorld().transform(scaledTorque);

        // Apply angular damping
        obj.setAngularVelocity(dampedVelocity.add(velocityFromAcceleration));
    }

   /* private static void updateOrientationEuler(PhysicsObject obj, Vector3dc angularVelocity) { // Approach: Euler integration (TODO: Less accurate but faster. How about in a datapack, where I can use entity rotation tricks to compute sin and cos quickly? What to choose there?)
        Quaterniond orientation = new Quaterniond(obj.getOrientation());
        obj.setOrientation(
                orientation.add(new Quaterniond(angularVelocity.x(), angularVelocity.y(), angularVelocity.z(), 0) // angularVelocity is treated as a quaternion
                        .mul(orientation)
                        .scale(0.5d * DELTA_TIME)
                )
        ); // I don't normalize it here because setOrientation() already does that automatically
    }*/

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

// TODO: Should I revert back to the original state as I do now, or just apply the corrections?
// TODO: Should I interleave velocity and penetration resolution, or do penetration afterwards?
// TODO: Why is it still so incredibly jittery, even with a lot of iterations and a small penetration resolution strength?
// TODO: Objects slide down a slope even with friction of 1. Check the book to see how to fix it
// TODO: Some spots of a giant rotated object seem to have no collision? -> Maybe caused by missing edge-edge, but not sure