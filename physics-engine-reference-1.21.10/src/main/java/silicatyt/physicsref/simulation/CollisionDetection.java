package silicatyt.physicsref.simulation;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector3d;
import silicatyt.physicsref.data.Contact;
import silicatyt.physicsref.entity.PhysicsObject;

import java.util.ArrayList;
import java.util.HashMap;

import static silicatyt.physicsref.PhysicsRef.LOGGER;
import static silicatyt.physicsref.PhysicsRef.loadedPhysicsObjects;
import static silicatyt.physicsref.simulation.ContactGeneration.genContactEdgeEdge;
import static silicatyt.physicsref.simulation.ContactGeneration.genContactPointFace;

public class CollisionDetection {
    public static void start(MinecraftServer server) { // TODO: Remove MinecraftServer server
        for (PhysicsObject obj : loadedPhysicsObjects) {
            if (obj.getInverseMass() == 0d) { // Don't search for collisions if you're a static object. Those should only appear as ObjectB.
                continue;
            }

            // TODO: Terrain contacts (Maybe I won't do them in the mod, because I can test everything I want with object-object collisions too)

            // Collision detection with other objects
            HashMap<PhysicsObject, ArrayList<Contact>> previousObjectContacts = obj.clearObjectContacts(); // Clears the contacts and gives me access to the previous contacts without creating any deep copies

            // TEST PARTICLES AT CONTACT POINT
            for (ArrayList<Contact> contacts : previousObjectContacts.values()) {
                for (Contact contact : contacts) {
                    Vector3d contactPoint = contact.contactPoint;
                    ServerWorld world = (ServerWorld) obj.getEntityWorld();
                    world.spawnParticles(ParticleTypes.CRIT, contactPoint.x, contactPoint.y, contactPoint.z, 1, 0d, 0d, 0d, 0d);
                    LOGGER.info(Double.toString(contact.closingVelocity));
                    LOGGER.info(contact.contactNormal.toString());
                }
            }




            obj.isChecked = true; // Make it so other objects don't check for collisions with this object anymore (to avoid duplicate collision entries).
            for (PhysicsObject otherObj : loadedPhysicsObjects) {
                if (otherObj.isChecked) {
                    continue;
                }
                // Coarse collision check (See if AABBs intersect)
                if (isIntersectingAABB(obj, otherObj)) {
                    performSat(obj, otherObj); // Leads into contact generation if a collision is detected
                }
            }

        }
    }

    // General helper methods

    // Helper methods for object-terrain collisions

    // Helper methods for object-object collisions
    private static boolean isIntersectingAABB(PhysicsObject left, PhysicsObject right) {
        Vector3d[] leftAABB = left.getBoundingBoxAbsolute();
        Vector3d[] rightAABB = right.getBoundingBoxAbsolute();
        return leftAABB[0].x <= rightAABB[1].x && rightAABB[0].x <= leftAABB[1].x && leftAABB[0].y <= rightAABB[1].y && rightAABB[0].y <= leftAABB[1].y && leftAABB[0].z <= rightAABB[1].z && rightAABB[0].z <= leftAABB[1].z;
    }

    private static double getAxisOverlap(Vector3d axis, PhysicsObject left, PhysicsObject right) {
        double[] leftProjection;
        double[] rightProjection;
        leftProjection = projectObjectOntoAxis(left, axis);
        rightProjection = projectObjectOntoAxis(right, axis);
        return Double.min(leftProjection[1] - rightProjection[0], rightProjection[1] - leftProjection[0]);
    }

    public static double[] projectObjectOntoAxis(PhysicsObject obj, Vector3d axis) { // Returns min and max projection for all corners
        Vector3d[] corners = obj.getCornerPosAbsolute();

        // Corner 0
        double projection = corners[0].dot(axis);
        double minProjection = projection;
        double maxProjection = projection;

        // Corners 1-7
        for (int i = 0; i < 8; i++) {
            projection = corners[i].dot(axis);
            minProjection = Double.min(minProjection, projection);
            maxProjection = Double.max(maxProjection, projection);
        }

        return new double[]{minProjection, maxProjection};
    }

    private static void performSat(PhysicsObject left, PhysicsObject right) {
        Vector3d[] axes = new Vector3d[]{
                left.getAxis(0), left.getAxis(1), left.getAxis(2), // Left object's axes
                right.getAxis(0), right.getAxis(1), right.getAxis(2), // Right object's axes
                left.getAxis(0).cross(right.getAxis(0)).normalize(), left.getAxis(0).cross(right.getAxis(1)).normalize(), left.getAxis(0).cross(right.getAxis(2)).normalize(), // Cross product axes
                left.getAxis(1).cross(right.getAxis(0)).normalize(), left.getAxis(1).cross(right.getAxis(1)).normalize(), left.getAxis(1).cross(right.getAxis(2)).normalize(),
                left.getAxis(2).cross(right.getAxis(0)).normalize(), left.getAxis(2).cross(right.getAxis(1)).normalize(), left.getAxis(2).cross(right.getAxis(2)).normalize()
        };

        // Check if any axis is a separating axis (I combined it with the "get overlap" method to avoid doing the literal same calculations twice)
        double overlap;
        double minOverlap = Double.MAX_VALUE;
        int minAxisIndex = -1;
        for (int i = 0; i < axes.length; i++) {
            overlap = getAxisOverlap(axes[i], left, right);
            if (overlap < 0d) { // Separating axis found: No collision
                return;
            }
            if (overlap < minOverlap) {
                minOverlap = overlap;
                minAxisIndex = i;
            }
        }

        // Generate contact with the minAxis as the normal
        if (minAxisIndex < 3) {
            genContactPointFace(left, right, axes[minAxisIndex], minAxisIndex);
        } else if (minAxisIndex < 6) {
            genContactPointFace(right, left, axes[minAxisIndex], minAxisIndex);
        } else {
            genContactEdgeEdge(left, right, axes[minAxisIndex]);
        }
    }

}
