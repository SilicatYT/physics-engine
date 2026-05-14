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
import static silicatyt.physics.simulation.ContactGenerator.generateContacts;

public class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;
    public static final ContactManager CONTACT_MANAGER = new ContactManager();

    public static void physicsTick(MinecraftServer server) {
        if (!server.getTickManager().shouldTick()) { return; }

        List<PhysicsObject> loadedObjects = List.copyOf(LOADED_PHYSICS_OBJECTS); // So I don't modify LOADED_PHYSICS_OBJECTS in integration (entities could unload) while I iterate over it

        for (PhysicsObject obj : loadedObjects) {
            Integrator.phaseOne(obj);
        }

        Set<PhysicsObject> checkedObjects = new HashSet<>();
        for (PhysicsObject obj : loadedObjects) {
            if (obj.getInverseMass() == 0d) { continue; } // Don't search for collisions if you're a static object. Static objects can only appear as ObjectB because they can't move.

            // Collision Detection
            List<ObjectCollision> objectCollisions = CollisionDetector.getObjectCollisions(obj, checkedObjects);
            for (ObjectCollision collision : objectCollisions) {
                Set<Contact> newContacts = generateContacts(obj, collision);
                if (newContacts == null) { continue; }
                CONTACT_MANAGER.accumulateContacts(newContacts);
            }
        }

        CONTACT_MANAGER.prepareResolution();
        ContactResolver.resolve(CONTACT_MANAGER);
        CONTACT_MANAGER.finishTick();

        for (PhysicsObject obj : loadedObjects) {
            Integrator.phaseTwo(obj);
        }

        checkedObjects.clear();
    }
}
