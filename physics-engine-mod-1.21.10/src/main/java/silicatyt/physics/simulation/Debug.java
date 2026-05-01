package silicatyt.physics.simulation;

import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.Physics.LOADED_PHYSICS_OBJECTS;

public class Debug {
    public static void showObjectAxes() {
        for (PhysicsObject obj : LOADED_PHYSICS_OBJECTS) {
            if (obj.getEntityWorld() instanceof ServerWorld world) { // Only run serverside
                Vector3dc[] cornerPos = obj.getCornerPosAbsolute();


                Vector3dc from = cornerPos[0];
                Vec3d to;
                int color;

                // x-axis
                to = new Vec3d(cornerPos[4].x(), cornerPos[4].y(), cornerPos[4].z());
                color = 0xFF0000;
                world.spawnParticles(new TrailParticleEffect(to, color, 10), from.x(), from.y(), from.z(),
                        1, 0, 0, 0, 0
                );

                // y-axis
                to = new Vec3d(cornerPos[2].x(), cornerPos[2].y(), cornerPos[2].z());
                color = 0x00FF00;
                world.spawnParticles(new TrailParticleEffect(to, color, 10), from.x(), from.y(), from.z(),
                        1, 0, 0, 0, 0
                );

                // z-axis
                to = new Vec3d(cornerPos[1].x(), cornerPos[1].y(), cornerPos[1].z());
                color = 0x0000FF;
                world.spawnParticles(new TrailParticleEffect(to, color, 10), from.x(), from.y(), from.z(),
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    public static void showContactPoint() {

    }

    public static void showContactNormal() {

    }

}
