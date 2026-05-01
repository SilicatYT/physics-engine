package silicatyt.physics.data;

import net.minecraft.util.math.Vec3i;

import java.util.LinkedList;
import java.util.List;

public class TerrainHitboxContactManifold implements ContactManifold {
    public final int hitboxId;
    private final Vec3i blockPos;
    private final List<Contact> contacts = new LinkedList<>();

    public TerrainHitboxContactManifold(int hitboxId, Vec3i blockPos) {
        this.hitboxId = hitboxId;
        this.blockPos = blockPos;
    }

    @Override
    public void updateWithContact(Contact newContact) {

    }

    @Override
    public void updateWithoutContact() {

    }

    @Override
    public List<Contact> getContacts() {
        return null;
    }
}



// TODO