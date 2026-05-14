package silicatyt.physics.data;

import net.minecraft.util.math.Vec3i;
import silicatyt.physics.entity.PhysicsObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TerrainContactManifold implements ContactManifold {
    private final PhysicsObject object;
    private final Vec3i blockPos;
    private final Map<Integer, TerrainHitboxContactManifold> contacts = new HashMap<>();

    public TerrainContactManifold(PhysicsObject object, Vec3i blockPos) {
        this.object = object;
        this.blockPos = blockPos;
    }

    @Override
    public void updateWithContacts(Set<Contact> newContact) {

    }

    @Override
    public boolean updateWithoutContacts() {
        return false;
    }

    @Override
    public List<Contact> getContacts() {
        return null;
    }
}



// TODO (Maybe also store hitbox size information here)