package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public interface ContactPoint {
    Vector3dc getNormal();
    Vector3dc getPosition();
    double getPenetrationDepth();
    Vector3dc getAccumulatedImpulse();
    void setAccumulatedImpulse(Vector3dc impulse);
}
