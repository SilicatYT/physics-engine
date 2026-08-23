package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;

public class Manifold {
    private final static int MAX_INACTIVE_TIME = 10; // Can be kept for up to this number of ticks for warm-starting

    public final PhysicsObject a, b;
    public final ContactState state;
    public final int persistedAxisIndex;
    public final boolean persistedAxisFacingOutward;
    public final boolean persistedAxisFacingB;
    public final double offsetX, offsetY, offsetZ;
    private int inactiveTime = 0;

    public Manifold(PhysicsObject a, PhysicsObject b, ContactState state, int persistedAxisIndex, boolean persistedAxisFacingOutward, boolean persistedAxisFacingB, double offsetX, double offsetY, double offsetZ) {
        this.a = a;
        this.b = b;
        this.state = state;
        this.persistedAxisIndex = persistedAxisIndex;
        this.persistedAxisFacingOutward = persistedAxisFacingOutward;
        this.persistedAxisFacingB = persistedAxisFacingB;

        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public void incrementInactiveTime() { inactiveTime++; }
    public boolean isToBeDiscarded() { return inactiveTime > MAX_INACTIVE_TIME; }
    public boolean isOld() { return inactiveTime != 0; } // Old manifolds should be ignored in the hysteresis checks
}
