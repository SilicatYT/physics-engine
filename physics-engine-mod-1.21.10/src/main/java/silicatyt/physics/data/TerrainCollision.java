package silicatyt.physics.data;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

public record TerrainCollision(Vec3i blockPos, Vector3d axisOfMinOverlap, int axisOfMinOverlapIndex) {}
