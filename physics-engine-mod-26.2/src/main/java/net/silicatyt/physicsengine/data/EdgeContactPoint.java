package net.silicatyt.physicsengine.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public final class EdgeContactPoint implements ContactState, ContactPoint {
    public final int id;
    private final Vector3dc normal;
    private final Vector3dc position;
    private final double penetrationDepth;
    private final Vector3d accumulatedImpulse = new Vector3d();
    private final List<EdgeContactPoint> asList;

    public EdgeContactPoint(int id, Vector3dc normal, Vector3dc position, double penetrationDepth) {
        this.id = id;
        this.normal = normal;
        this.position = position;
        this.penetrationDepth = penetrationDepth;
        asList = List.of(this);
    }

    @Override public Vector3dc getNormal() { return normal; }
    @Override public Vector3dc getPosition() { return position; }
    @Override public double getPenetrationDepth() { return penetrationDepth; }
    @Override public Vector3d getAccumulatedImpulse() { return accumulatedImpulse; }
    @Override public void setAccumulatedImpulse(Vector3dc impulse) { accumulatedImpulse.set(impulse); }
    @Override public boolean isPositionRelativeToA() { return false; } // It's offset by (a - b)

    @Override public List<EdgeContactPoint> getPoints() { return asList; }

}
