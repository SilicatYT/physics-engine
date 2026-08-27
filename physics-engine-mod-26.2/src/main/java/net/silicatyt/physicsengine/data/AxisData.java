package net.silicatyt.physicsengine.data;

public interface AxisData {
    int index();
    double overlapSquared(); // So I don't have to get the actual overlap for cross-product axes
    double lengthSquared();
}
