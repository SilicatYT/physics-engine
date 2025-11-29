package silicatyt.physicsref.simulation;

import org.joml.Vector3d;
import silicatyt.physicsref.entity.PhysicsObject;

import static silicatyt.physicsref.PhysicsRef.LOGGER;
import static silicatyt.physicsref.PhysicsRef.loadedPhysicsObjects;

public class CollisionDetection {
    public static void start() {
        for (PhysicsObject obj : loadedPhysicsObjects) {
            if (obj.getInverseMass() == 0d) { // Don't search for collisions if you're a static object. Those should only appear as ObjectB.
                continue;
            }

            // TODO: Terrain contacts (Maybe I won't do them in the mod, because I can test everything I want with object-object collisions too)

            // Collision detection with other objects
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

    private static boolean isIntersectingAABB(PhysicsObject left, PhysicsObject right) {
        Vector3d[] leftAABB = left.getBoundingBoxAbsolute();
        Vector3d[] rightAABB = right.getBoundingBoxAbsolute();
        return leftAABB[0].x <= rightAABB[1].x && rightAABB[0].x <= leftAABB[1].x && leftAABB[0].y <= rightAABB[1].y && rightAABB[0].y <= leftAABB[1].y && leftAABB[0].z <= rightAABB[1].z && rightAABB[0].z <= leftAABB[1].z;
    }

    private static boolean isSeparatingAxis(Vector3d axis, PhysicsObject left, PhysicsObject right) {
        double[] leftProjection;
        double[] rightProjection;
        leftProjection = projectOntoAxis(left, axis);
        rightProjection = projectOntoAxis(right, axis);
        return leftProjection[0] > rightProjection[1] || rightProjection[0] > leftProjection[1];
    }

    private static double[] projectOntoAxis(PhysicsObject obj, Vector3d axis) { // Returns min and max projection for all corners
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
                left.getAxis(0).cross(right.getAxis(0)), left.getAxis(0).cross(right.getAxis(1)), left.getAxis(0).cross(right.getAxis(2)), // Cross product axes
                left.getAxis(1).cross(right.getAxis(0)), left.getAxis(1).cross(right.getAxis(1)), left.getAxis(1).cross(right.getAxis(2)),
                left.getAxis(2).cross(right.getAxis(0)), left.getAxis(2).cross(right.getAxis(1)), left.getAxis(2).cross(right.getAxis(2))
        };

        // Check if any axis is a separating axis
        for (Vector3d axis : axes) {
            if (isSeparatingAxis(axis, left, right)) {
                return;
            }
        }

        // TODO: The rest
        LOGGER.info("Two objects are colliding");
    }

}
