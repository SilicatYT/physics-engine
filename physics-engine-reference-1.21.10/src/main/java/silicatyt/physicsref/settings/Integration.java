package silicatyt.physicsref.settings;

import org.joml.Vector3d;

import static java.lang.Math.pow;
import static silicatyt.physicsref.simulation.Main.DELTA_TIME;

public class Integration {
    public static final Vector3d DEFAULT_GRAVITY = new Vector3d(0d, -9.81d, 0d).mul(DELTA_TIME); // Velocity difference per tick. So don't apply DELTA_TIME in tick again.
    public static final double DEFAULT_LINEAR_DAMPING = pow(0.7d, DELTA_TIME); // "After 1 second, this much of its speed should remain". This is necessary so less DELTA_TIME doesn't make damping stronger. It should stay identical.
    public static final double DEFAULT_ANGULAR_DAMPING = pow(0.7d, DELTA_TIME);
}
