package silicatyt.physics.simulation;

import net.minecraft.server.MinecraftServer;
import silicatyt.physics.data.ColliderCollision;
import silicatyt.physics.entity.PhysicsObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static silicatyt.physics.Physics.LOADED_PHYSICS_OBJECTS;

public class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;

    public static void physicsTick(MinecraftServer server) {
        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            Integrator.phaseOne(obj);
        }

        Set<PhysicsObject> checkedObjects = new HashSet<PhysicsObject>();
        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            if (obj.getInverseMass() == 0d) { continue; } // Don't search for collisions if you're a static object. Static objects can only appear as ObjectB because they can't move.

            // Collision Detection
            // List<TerrainCollision> terrainCollisions = CollisionDetector.getTerrainCollisions(obj);
            List<ColliderCollision> colliderCollisions = CollisionDetector.getColliderCollisions(obj, checkedObjects);

            // Contact Generation
            // TODO: Where to store contacts? Do I need a "final ... PREVIOUS_CONTACTS"? That should ideally be a map with objectA:<pair of terrainContacts and colliderContacts>. But the resolver should get an array or a list, right? Unless I need to search for contacts of a specific objectA efficiently (for the "update separatingVelocities" method)...
        }

        // ContactResolver.resolve(allContacts);

        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            Integrator.phaseTwo(obj);
        }

        // DEBUG
        Debug.showObjectAxes();
        Debug.showContactPoint();
        Debug.showContactNormal();

    }
}
