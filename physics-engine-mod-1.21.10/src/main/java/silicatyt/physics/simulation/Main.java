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
                Contact newContact = generateContact(obj, collision);
                if (newContact == null) { continue; }
                CONTACT_MANAGER.accumulateContacts(newContact);
            }
        }

        CONTACT_MANAGER.prepareResolution();
        ContactResolver.resolve(CONTACT_MANAGER);
        CONTACT_MANAGER.finishTick();

        for (PhysicsObject obj : loadedObjects) {
            Integrator.phaseTwo(obj);
        }

        checkedObjects.clear();

        // DEBUG
        //Debug.showObjectAxes();
        //Debug.showContactPoint();
        //Debug.showContactNormal();
    }
}
