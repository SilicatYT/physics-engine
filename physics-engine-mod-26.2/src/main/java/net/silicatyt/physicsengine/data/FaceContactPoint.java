package net.silicatyt.physicsengine.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FaceContactPoint implements ContactPoint { // TODO: Currently the contact gets re-created every tick. It would be better if I could simply update the already allocated Contact objects
    private final int id;
    private final Vector3d position;
    private double penetrationDepth;
    private final Vector3d accumulatedImpulse;
    private final FaceContactState parent;

    public FaceContactPoint(int id, FaceContactState parent, Vector3d position, double penetrationDepth) {

    }

    @Override
    public Vector3d getNormal() { // TODO: Update, then return
    }
    @Override
    public Vector3dc getPosition() { // TODO: Update, then return
    }
    @Override
    public double getPenetrationDepth() { // TODO: Update, then return
    }
    @Override
    public Vector3d getAccumulatedImpulse() { return accumulatedImpulse; }
    @Override
    public void setAccumulatedImpulse(Vector3dc impulse) { accumulatedImpulse.set(impulse); }

}
