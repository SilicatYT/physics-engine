package silicatyt.physicsref.simulation;

import silicatyt.physicsref.entity.PhysicsObject;

import static silicatyt.physicsref.PhysicsRef.loadedPhysicsObjects;
import static silicatyt.physicsref.settings.Integration.DEFAULT_GRAVITY;
import static silicatyt.physicsref.settings.Integration.DEFAULT_LINEAR_DAMPING;

public class Integration {
    // Phases
    public static void phaseOne() {
        for (PhysicsObject obj : loadedPhysicsObjects) {
            fixEntityPos(obj);
            updateLinearVelocity(obj);
            updatePos(obj);
            // TODO: Do the remaining stuff
        }
    }

    public static void phaseTwo() {
        for (PhysicsObject obj : loadedPhysicsObjects) {
            obj.updateVisuals();
            // TODO: Do the remaining stuff
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
        //obj.addLinearVelocity(DEFAULT_GRAVITY);

        // Apply accumulated force
        obj.addLinearVelocity(obj.getAccumulatedForce().mul(obj.getInverseMass()));

        // Apply linear damping
        obj.scaleLinearVelocity(DEFAULT_LINEAR_DAMPING);
    }

    private static void updatePos(PhysicsObject obj) {
        obj.addInternalPos(obj.getLinearVelocity());
    }

}
