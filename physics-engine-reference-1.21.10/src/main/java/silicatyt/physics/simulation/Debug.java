package silicatyt.physics.simulation;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import silicatyt.physics.data.Contact;
import silicatyt.physics.entity.PhysicsObject;

import java.util.ArrayList;
import java.util.HashMap;

import static silicatyt.physics.Physics.loadedPhysicsObjects;

// TODO: REWORK

public class Debug {
    public static void showObjectAxes() {
        for (PhysicsObject object : loadedPhysicsObjects) {
            if (object.getEntityWorld() instanceof ServerWorld world) {
                Vector3d[] cornerPositions = object.getCornerPosAbsolute();

                world.spawnParticles(new TrailParticleEffect(new Vec3d(cornerPositions[4].x, cornerPositions[4].y, cornerPositions[4].z), 0xFF0000, 10), cornerPositions[0].x, cornerPositions[0].y, cornerPositions[0].z,
                        1, 0, 0, 0, 0
                );

                world.spawnParticles(new TrailParticleEffect(new Vec3d(cornerPositions[2].x, cornerPositions[2].y, cornerPositions[2].z), 0x00FF00, 10), cornerPositions[0].x, cornerPositions[0].y, cornerPositions[0].z,
                        1, 0, 0, 0, 0
                );

                world.spawnParticles(new TrailParticleEffect(new Vec3d(cornerPositions[1].x, cornerPositions[1].y, cornerPositions[1].z), 0x0000FF, 10), cornerPositions[0].x, cornerPositions[0].y, cornerPositions[0].z,
                        1, 0, 0, 0, 0
                );
            }

        }
    }

    public static void showContactPoint() {
        for (PhysicsObject object : loadedPhysicsObjects) {
            if (object.getEntityWorld() instanceof ServerWorld world) {
                for (HashMap.Entry<PhysicsObject, ArrayList<Contact>> contactEntry : object.getObjectContactsDebug().entrySet()) {
                    ArrayList<Contact> contacts = contactEntry.getValue(); // All contacts with a specific objectB
                    for (Contact contact : contacts) {
                        if (contact.features[0] <= 20) { // If point-face
                            world.spawnParticles(ParticleTypes.CRIT, contact.contactPoint.x, contact.contactPoint.y, contact.contactPoint.z, 1, 0, 0, 0, 0);
                        } else { // If edge-edge
                            world.spawnParticles(ParticleTypes.FLAME, contact.contactPoint.x, contact.contactPoint.y, contact.contactPoint.z, 1, 0, 0, 0, 0);
                        }

                    }
                }

            }
        }
    }

    public static void showContactNormal() {
        for (PhysicsObject object : loadedPhysicsObjects) {
            if (object.getEntityWorld() instanceof ServerWorld world) {
                for (HashMap.Entry<PhysicsObject, ArrayList<Contact>> contactEntry : object.getObjectContactsDebug().entrySet()) {
                    ArrayList<Contact> contacts = contactEntry.getValue(); // All contacts with a specific objectB
                    for (Contact contact : contacts) {
                        world.spawnParticles(new TrailParticleEffect(new Vec3d(contact.contactPoint.x + contact.contactNormal.x * 3, contact.contactPoint.y + contact.contactNormal.y * 3, contact.contactPoint.z + contact.contactNormal.z * 3), 0x00FFFF, 10), contact.contactPoint.x, contact.contactPoint.y, contact.contactPoint.z,
                                1, 0, 0, 0, 0
                        );
                    }
                }

            }
        }
    }

}
