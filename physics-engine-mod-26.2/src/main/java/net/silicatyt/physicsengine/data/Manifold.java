package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;

public class Manifold { // TODO
    public final PhysicsObject a;
    public final PhysicsObject b;

    private int persistedAxisIndex;
    private boolean persistedAxisFacingOutward;
    private boolean persistedAxisFacingB;

    public Manifold(PhysicsObject a, PhysicsObject b) {
        this.a = a;
        this.b = b;
    }

    public int getPersistedAxisIndex() { return persistedAxisIndex; }
    public boolean isPersistedAxisFacingOutward() { return persistedAxisFacingOutward; }
    public boolean isPersistedAxisFacingB() { return persistedAxisFacingB; }
}
