package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static silicatyt.physics.simulation.CollisionDetector.isIntersectingAABB;
import static silicatyt.physics.simulation.ContactGenerator.*;

public class ObjectContactManifold implements ContactManifold {
    public final PhysicsObject objectA;
    public final PhysicsObject objectB;
    private final List<Contact> contacts = new LinkedList<>();
    private int separatedForTicks;

    public ObjectContactManifold(PhysicsObject objectA, PhysicsObject objectB) {
        this.objectA = objectA;
        this.objectB = objectB;
    }

    @Override
    public void updateWithContacts(Set<Contact> newContacts) { // TODO: Make it so the new contact's data is kept (except accumulatedImpulse), small optimization
        Iterator<Contact> it = contacts.iterator();
        Vector3dc newContactNormal = newContacts.iterator().next().getContactNormal(); // Assumption: Every new contact has the same normal
        separatedForTicks = 0;

        // Update previous contacts
        while (it.hasNext()) {
            Contact contact = it.next();
            contact.setActivity(true);

            if (newContacts.remove(contact)) { continue; } // Keep any old versions of the new contacts

            Vector3dc contactNormal = contact.getContactNormal();
            if (!contact.isActive()) { continue; } // getContactNormal() can set activity to false for edge-edge

            // Discard contacts where necessary
            double normalSimilarity = contactNormal.dot(newContactNormal);
            double penetrationDepth = contact.getPenetrationDepth();
            if (normalSimilarity < ACCUMULATION_PROJECTION_DISCARD_THRESHOLD || penetrationDepth < ACCUMULATION_MIN_PENETRATION_DEPTH_THRESHOLD) { // No longer relevant, or too far away
                it.remove();
                continue;
            }

            // Set the "isActive" status: Deactivate contacts where necessary (Ignored during resolution, but kept in case they become valid again)
            if (normalSimilarity < ACCUMULATION_PROJECTION_DEACTIVATION_THRESHOLD || penetrationDepth < 0d) { contact.setActivity(false); }
        }

        // Add new contacts
        contacts.addAll(newContacts);
    }

    @Override
    public boolean updateWithoutContacts() { // Returns true if the manifold should be kept for the current tick (But with its contacts marked as inactive)
        if (objectA.isRemoved() || objectB.isRemoved()) { return false; }

        // Check if the manifold has been out of touch for too long
        if (separatedForTicks > ACCUMULATION_MAX_SEPARATION_TIME_THRESHOLD) { return false; }
        separatedForTicks++;

        // Check if the objects' AABBs are further away than the max configurable amount allows
        Vector3dc[] leftAABB = objectA.getBoundingBoxAbsolute();
        Vector3dc[] rightAABB = objectB.getBoundingBoxAbsolute();
        Vector3d[] expandedLeftAABB = new Vector3d[]{new Vector3d(leftAABB[0]), new Vector3d(leftAABB[1])};
        expandedLeftAABB[0].sub(ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD, ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD, ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD);
        expandedLeftAABB[1].add(ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD, ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD, ACCUMULATION_MAX_AABB_SEPARATION_THRESHOLD);
        if (isIntersectingAABB(expandedLeftAABB, rightAABB)) {
            for (Contact contact : contacts) { contact.setActivity(false); }
            return true;
        }
        return false;
    }

    @Override
    public List<Contact> getContacts() {
        List<Contact> result = new LinkedList<>();
        for (Contact contact : contacts) {
            if (contact.isActive()) { result.add(contact); }
        }
        return result;
    }
}
