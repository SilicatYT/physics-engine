package silicatyt.physics.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactManager {
    private final Map<TerrainContactKey, TerrainContactManifold> previousTerrainManifolds = new HashMap<>();
    private final Map<TerrainContactKey, TerrainContactManifold> currentTerrainManifolds = new HashMap<>();

    private final Map<ObjectContactKey, ObjectContactManifold> previousObjectManifolds = new HashMap<>();
    private final Map<ObjectContactKey, ObjectContactManifold> currentObjectManifolds = new HashMap<>();

    public void accumulateContacts(Contact newContact) { // Take the previous tick's contacts and update and carry them over, or discard them

    }

    public void beginTick() {

    }

    public void finishTick() {

    }

    public List<Contact> getContacts() {
        return null;
    }

    public void clear() {
        previousTerrainManifolds.clear();
        currentTerrainManifolds.clear();
        previousObjectManifolds.clear();
        currentObjectManifolds.clear();
    }
}
