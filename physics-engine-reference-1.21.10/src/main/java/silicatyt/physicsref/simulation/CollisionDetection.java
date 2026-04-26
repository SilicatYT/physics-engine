package silicatyt.physicsref.simulation;

import org.joml.Vector3d;
import silicatyt.physicsref.data.Contact;
import silicatyt.physicsref.entity.PhysicsObject;

import java.util.ArrayList;
import java.util.HashMap;

import static silicatyt.physicsref.PhysicsRef.loadedPhysicsObjects;
import static silicatyt.physicsref.simulation.ContactGeneration.*;

public class CollisionDetection {
    public static void start() {
        for (PhysicsObject obj : loadedPhysicsObjects) {
            if (obj.getInverseMass() == 0d) { // Don't search for collisions if you're a static object. Those should only appear as ObjectB.
                continue;
            }

            // TODO: Terrain contacts (Maybe I won't do them in the mod, because I can test everything I want with object-object collisions?)

            // Collision detection with other objects
            HashMap<PhysicsObject, ArrayList<Contact>> previousObjectContacts = obj.clearObjectContacts(); // Clears the contacts and gives me access to the previous contacts without creating any deep copies
            obj.isChecked = true; // Makes it so other objects don't check for collisions with this object anymore (to avoid duplicate collision entries).

            for (PhysicsObject otherObj : loadedPhysicsObjects) { // TODO: Maybe optimize by only checking entities whose AABB intersects with chunks that my AABB intersects with. Could make a separate data structure where the entity is stored in each chunk it intersects with
                if (otherObj.isChecked) {
                    continue;
                }
                // Coarse collision check (See if AABBs intersect)
                if (isIntersectingAABB(obj, otherObj)) {
                    performSat(obj, otherObj); // Leads into contact generation if a collision is detected
                }
            }

            // Contact accumulation for object-object contacts
            accumulateContacts(obj, previousObjectContacts); // Update contacts from previous ticks
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
        if (minAxisIndex < 6) { // Point-face
            genContactPointFace(left, right, axes[minAxisIndex], minAxisIndex);
        } else { // Edge-edge
            genContactEdgeEdge(left, right, axes[minAxisIndex], minAxisIndex);
        }
    }

}
