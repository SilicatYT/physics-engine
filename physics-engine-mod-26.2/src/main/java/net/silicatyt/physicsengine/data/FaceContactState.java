package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public final class FaceContactState implements ContactState { // TODO: Maybe once I make FaceContactState persistent (instead of recreating it every tick), put the previous tick's contacts here?
    public final PhysicsObject a, b;
    public final boolean referenceObjectIsA;
    public final int incidentFaceIndex, referenceFaceIndex;
    private final Vector3d normal; // Facing outward
    private final List<FaceContactPoint> points;

    public FaceContactState(PhysicsObject a, PhysicsObject b, boolean referenceObjectIsA, int incidentFaceIndex, int referenceFaceIndex, Vector3dc normal, List<FaceContactPoint> points) {
        this.a = a;
        this.b = b;
        this.referenceObjectIsA = referenceObjectIsA;
        this.incidentFaceIndex = incidentFaceIndex;
        this.referenceFaceIndex = referenceFaceIndex;
        this.normal = new Vector3d(normal);
        this.points = points;
    }

    @Override public Vector3dc getNormal() { return normal; }
    @Override public List<FaceContactPoint> getPoints() { return points; }
}
