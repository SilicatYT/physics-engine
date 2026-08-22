package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

import java.util.List;

public interface ContactState {
    public Vector3dc getNormal();
    public List<? extends ContactPoint> getPoints();
}
