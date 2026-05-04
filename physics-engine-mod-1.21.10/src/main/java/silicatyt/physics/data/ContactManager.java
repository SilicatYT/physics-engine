package silicatyt.physics.data;

import java.util.*;

public class ContactManager {
    private final Map<TerrainContactKey, TerrainContactManifold> previousTerrainManifolds = new HashMap<>();
    private final Map<TerrainContactKey, TerrainContactManifold> currentTerrainManifolds = new HashMap<>();

    private final Map<ObjectContactKey, ObjectContactManifold> previousObjectManifolds = new HashMap<>();
    private final Map<ObjectContactKey, ObjectContactManifold> currentObjectManifolds = new HashMap<>();

    public void accumulateContacts(Contact newContact) { // Take the previous tick's contacts and update and carry them over, or discard them
        if (newContact.objectB == null) { // Terrain contact
            // TODO: For terrain contacts, create new manifolds each tick to keep the old ones, because I have to carry over the hitboxContactManifolds individually like objectContactManifolds. If I kept the same terrainContactManifolds, every hitboxContactManifold would automatically get carried over too, even if those hitboxes no longer overlap.
            // ...
        } else { // Object contact
            // Carry over contacts for the affected manifold (or create a new one), and remove it from the previous tick
            ObjectContactKey key = new ObjectContactKey(newContact.objectA, newContact.objectB);
            ObjectContactManifold manifold = previousObjectManifolds.getOrDefault(key, new ObjectContactManifold(newContact.objectA, newContact.objectB));
            previousObjectManifolds.remove(key);
            currentObjectManifolds.put(key, manifold);

            // Update the carried over manifold & add the new contact to it
            manifold.updateWithContact(newContact);
        }
    }

    public void prepareResolution() {
        // Carry over the remaining non-touching manifolds from the previous tick as inactive, or discard
        for (Map.Entry<ObjectContactKey, ObjectContactManifold> entry : previousObjectManifolds.entrySet()) {
            ObjectContactManifold manifold = entry.getValue();
            if (manifold.updateWithoutContact()) { currentObjectManifolds.put(entry.getKey(), manifold); }
        }
    }

    public void finishTick() {
        previousTerrainManifolds.clear();
        previousTerrainManifolds.putAll(currentTerrainManifolds);

        previousObjectManifolds.clear();
        previousObjectManifolds.putAll(currentObjectManifolds);

        currentTerrainManifolds.clear();
        currentObjectManifolds.clear();
    }

    public List<Contact> getContacts() {
        List<Contact> contacts = new LinkedList<>();
        for (ObjectContactManifold manifold : currentObjectManifolds.values()) {
            contacts.addAll(manifold.getContacts());
        }
        return contacts;
    }

    public void clear() {
        previousTerrainManifolds.clear();
        currentTerrainManifolds.clear();
        previousObjectManifolds.clear();
        currentObjectManifolds.clear();
    }
}
