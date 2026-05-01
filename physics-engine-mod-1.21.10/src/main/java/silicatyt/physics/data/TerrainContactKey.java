package silicatyt.physics.data;

import net.minecraft.util.math.Vec3i;
import silicatyt.physics.entity.PhysicsObject;

public record TerrainContactKey(PhysicsObject objectA, Vec3i blockPos) {}
