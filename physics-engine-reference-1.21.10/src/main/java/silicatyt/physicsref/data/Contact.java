package silicatyt.physicsref.data;

import org.joml.Matrix3d;
import org.joml.Vector3d;

public class Contact {
    public final Feature[] features = new Feature[2];
    public final Vector3d effectiveMass = new Vector3d();
    public final Vector3d contactPoint = new Vector3d();
    public final Vector3d contactNormal = new Vector3d();
    public final Vector3d contactVelocity = new Vector3d();
    public final double penetrationVelocity = -Double.MAX_VALUE;
    public final double penetrationDepth = -Double.MAX_VALUE;
    public final Matrix3d orthonormalBasis = new Matrix3d();
}
