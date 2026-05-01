package silicatyt.physics.simulation;

import net.minecraft.server.MinecraftServer;
import silicatyt.physics.data.ObjectCollision;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactManager;
import silicatyt.physics.entity.PhysicsObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static silicatyt.physics.Physics.LOADED_PHYSICS_OBJECTS;
import static silicatyt.physics.simulation.ContactGenerator.generateContact;

public class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;
    public static final ContactManager contactManager = new ContactManager();

    public static void physicsTick(MinecraftServer server) {
        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            Integrator.phaseOne(obj);
        }

        Set<PhysicsObject> checkedObjects = new HashSet<>();
        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            if (obj.getInverseMass() == 0d) { continue; } // Don't search for collisions if you're a static object. Static objects can only appear as ObjectB because they can't move.

            // Collision Detection
            // List<TerrainCollision> terrainCollisions = CollisionDetector.getTerrainCollisions(obj);
            List<ObjectCollision> objectCollisions = CollisionDetector.getObjectCollisions(obj, checkedObjects);

            // Contact Generation TODO: Maybe unify the two collisions into a single for-loop
            //for (TerrainCollision collision : terrainCollisions) {
            //    generateContact(obj, collision);
            //}
            for (ObjectCollision collision : objectCollisions) {
                Contact newContact = generateContact(obj, collision);
                if (newContact == null) { continue; }
                contactManager.accumulateContacts(newContact);
            }
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
