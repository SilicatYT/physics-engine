package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public interface AxisData {
    int index();
    double overlap();
    Vector3dc axis();
}
