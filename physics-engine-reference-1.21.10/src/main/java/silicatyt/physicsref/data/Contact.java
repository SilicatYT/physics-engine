package silicatyt.physicsref.data;

import org.joml.Matrix3d;
import org.joml.Vector3d;

public class Contact {
    public final int[] features = new int[2]; // TODO: Check whether an array or 2 individual entries is faster in the datapack
    public final Vector3d contactPoint = new Vector3d();
    public final Vector3d contactNormal = new Vector3d();
    public final Vector3d contactVelocity = new Vector3d();
    public double closingVelocity; // Dot product of contactVelocity and contactNormal. TODO: Check if I need to store that data, or if I can calculate it at runtime.
    public double penetrationDepth;
}
