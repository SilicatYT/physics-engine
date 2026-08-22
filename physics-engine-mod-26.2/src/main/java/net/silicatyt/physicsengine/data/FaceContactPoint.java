package net.silicatyt.physicsengine.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FaceContactPoint implements ContactPoint { // TODO: Currently the contact gets re-created every tick. It would be better if I could simply update the already allocated Contact objects
    public final int id;
    private final Vector3d position;
    private final double penetrationDepth;
    private final Vector3d accumulatedImpulse = new Vector3d();
    private final FaceContactState parent;

    public FaceContactPoint(int id, FaceContactState parent, Vector3d position, double penetrationDepth) {
        this.id = id;
        this.position = position;
        this.penetrationDepth = penetrationDepth;
        this.parent = parent;
    }

    @Override public Vector3dc getNormal() { return parent.getNormal(); }
    @Override public Vector3dc getPosition() { return position; }
    @Override public double getPenetrationDepth() { return penetrationDepth; }
    @Override public Vector3d getAccumulatedImpulse() { return accumulatedImpulse; }
    @Override public void setAccumulatedImpulse(Vector3dc impulse) { accumulatedImpulse.set(impulse); }

}
