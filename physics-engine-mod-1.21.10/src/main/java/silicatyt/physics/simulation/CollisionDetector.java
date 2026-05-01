package silicatyt.physics.simulation;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.data.ObjectCollision;
import silicatyt.physics.entity.PhysicsObject;

import java.util.*;

public class CollisionDetector {
    public static LinkedList<ObjectCollision> getObjectCollisions(PhysicsObject obj, Set<PhysicsObject> checkedObjects) {
        LinkedList<ObjectCollision> collisions = new LinkedList<>();
        for (PhysicsObject otherObj : checkedObjects) {
            if (checkedObjects.contains(otherObj)) { continue; }

            // Coarse collision check (AABB)
            if (!isIntersectingAABB(obj, otherObj)) { continue; }

            // Fine collision check (SAT)
            ObjectCollision collision = performSat(obj, otherObj);
            if (collision != null) { collisions.add(collision); }
        }
        checkedObjects.add(obj);
        return collisions;
    }





    // General helper methods

    // Helper methods for terrain collisions

    // Helper methods for object collisions
    private static boolean isIntersectingAABB(PhysicsObject left, PhysicsObject right) {
        Vector3dc[] leftAABB = left.getBoundingBoxAbsolute();
        Vector3dc[] rightAABB = right.getBoundingBoxAbsolute();
        return leftAABB[0].x() <= rightAABB[1].x() && rightAABB[0].x() <= leftAABB[1].x() && leftAABB[0].y() <= rightAABB[1].y() && rightAABB[0].y() <= leftAABB[1].y() && leftAABB[0].z() <= rightAABB[1].z() && rightAABB[0].z() <= leftAABB[1].z();
    }

    private static double getAxisOverlap(PhysicsObject left, PhysicsObject right, Vector3dc axis) {
        double[] leftProjection = projectObjectOntoAxis(left, axis);
        double[] rightProjection = projectObjectOntoAxis(right, axis);
        return Double.min(leftProjection[1] - rightProjection[0], rightProjection[1] - leftProjection[0]);
    }

    public static double[] projectObjectOntoAxis(PhysicsObject obj, Vector3dc axis) { // Returns min and max projection of any corner
        Vector3dc[] corners = obj.getCornerPosAbsolute();

        double projection;
        double minProjection = Double.MAX_VALUE;
        double maxProjection = -Double.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            projection = corners[i].dot(axis);
            minProjection = Double.min(minProjection, projection);
            maxProjection = Double.max(maxProjection, projection);
        }

        return new double[]{minProjection, maxProjection};
    }

    private static ObjectCollision performSat(PhysicsObject objectA, PhysicsObject objectB) {
        Vector3d[] axes = new Vector3d[]{
                objectA.getAxis(0), objectA.getAxis(1), objectA.getAxis(2), // objectA's axes
                objectB.getAxis(0), objectB.getAxis(1), objectB.getAxis(2), // objectB's axes
                objectA.getAxis(0).cross(objectB.getAxis(0)).normalize(), objectA.getAxis(0).cross(objectB.getAxis(1)).normalize(), objectA.getAxis(0).cross(objectB.getAxis(2)).normalize(), // Cross product axes with objectA's x-axis
                objectA.getAxis(1).cross(objectB.getAxis(0)).normalize(), objectA.getAxis(1).cross(objectB.getAxis(1)).normalize(), objectA.getAxis(1).cross(objectB.getAxis(2)).normalize(), // Cross product axes with objectA's y-axis
                objectA.getAxis(2).cross(objectB.getAxis(0)).normalize(), objectA.getAxis(2).cross(objectB.getAxis(1)).normalize(), objectA.getAxis(2).cross(objectB.getAxis(2)).normalize() // Cross product axes with objectA's z-axis
        };

        // Check if any axis is separating
        double overlap;
        double minOverlap = Double.MAX_VALUE;
        int minAxisIndex = -1;

        for (int i = 0; i < axes.length; i++) {
            if (axes[i].lengthSquared() < 1e-12) { continue; } // Skip (cross-product) axes that are unstable because the base axes are parallel
            overlap = getAxisOverlap(objectA, objectB, axes[i]);
            if (overlap < 0d) { return null; } // Separating axis found: No collision
            if (overlap < minOverlap) {
                minOverlap = overlap;
                minAxisIndex = i;
            }
        }

        return new ObjectCollision(objectB, axes[minAxisIndex], minAxisIndex);
    }

}
