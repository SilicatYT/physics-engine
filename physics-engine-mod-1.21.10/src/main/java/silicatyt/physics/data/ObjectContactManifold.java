package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    public void updateWithContact(Contact newContact) {
        Iterator<Contact> it = contacts.iterator();
        separatedForTicks = 0;
        Vector3dc oldContactAccumulatedImpulse = null;

        // Update previous contacts
        while (it.hasNext()) {
            Contact contact = it.next();
            contact.setActivity(true);

            if (contact.featureA == newContact.featureA && contact.featureB == newContact.featureB) { // Remove the old "newContact" if it already existed
                oldContactAccumulatedImpulse = contact.getAccumulatedImpulse();
                it.remove();
                continue;
            }

            double projection = new Vector3d(contact.getContactNormal()).dot(newContact.getContactNormal());

            // Discard contacts where necessary
            if (projection < ACCUMULATION_PROJECTION_DISCARD_THRESHOLD || contact.getPenetrationDepth() < ACCUMULATION_MIN_PENETRATION_DEPTH_THRESHOLD) { // No longer relevant, or too far away
                it.remove();
                continue;
            }

            // Set the "isActive" status: Deactivate contacts where necessary (Ignored during resolution, but kept in case they become valid again)
            if (projection < ACCUMULATION_PROJECTION_DEACTIVATION_THRESHOLD || contact.getPenetrationDepth() < 0d) { contact.setActivity(false); }
        }

        // Add new contact
        if (oldContactAccumulatedImpulse != null) { newContact.addAccumulatedImpulse(oldContactAccumulatedImpulse); } // Carry over the old contact's data
        contacts.add(newContact);
    }

    @Override
    public boolean updateWithoutContact() { // Returns true if the manifold should be kept for the current tick (But with its contacts marked as inactive)
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
