package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;

public class Manifold { // TODO
    public final PhysicsObject a;
    public final PhysicsObject b;

    public Manifold(PhysicsObject a, PhysicsObject b) {
        this.a = a;
        this.b = b;
    }
}
