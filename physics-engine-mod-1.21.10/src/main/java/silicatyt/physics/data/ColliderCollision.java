package silicatyt.physics.data;

import org.joml.Vector3d;
import silicatyt.physics.entity.PhysicsObject;

public record ColliderCollision(PhysicsObject objectB, Vector3d axisOfMinOverlap, int axisOfMinOverlapIndex) {
}
