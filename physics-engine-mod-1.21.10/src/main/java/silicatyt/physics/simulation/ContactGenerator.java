package silicatyt.physics.simulation;

import org.joml.Vector3d;
import silicatyt.physics.data.ColliderCollision;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactEdgeEdge;
import silicatyt.physics.data.ContactPointFace;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.CollisionDetector.projectObjectOntoAxis;

public class ContactGenerator {
    public static Contact generateContact(PhysicsObject objectA, ColliderCollision collision) {
        if (collision.axisOfMinOverlapIndex() < 6) { return generateContactPointFace(objectA, collision); }
        return generateContactEdgeEdge(objectA, collision);
    }

    private static ContactPointFace generateContactPointFace(PhysicsObject objectA, ColliderCollision collision) {
        PhysicsObject objectB = collision.objectB();

        // Select faceObject and cornerObject
        PhysicsObject faceObject;
        PhysicsObject cornerObject;
        if (collision.axisOfMinOverlapIndex() < 3) {
            faceObject = objectA;
            cornerObject = objectB;
        } else {
            faceObject = objectB;
            cornerObject = objectA;
        }

        // Select the correct face and invert the contact normal if necessary
        Vector3d contactNormal = collision.axisOfMinOverlap();
        int chosenFace = 11 + (collision.axisOfMinOverlapIndex() % 3) * 2;
        if (projectObjectOntoAxis(cornerObject, contactNormal)[0] < projectObjectOntoAxis(faceObject, contactNormal)[0]) {
            contactNormal.mul(-1d);
            chosenFace--; // The chosenFace is 10 for normal=-X, 11 for normal=+X, 12 for normal=-Y etc
        }

        // Select the correct corner (The one with the most negative projection onto the normal)
        Vector3d[] corners = cornerObject.getCornerPosAbsolute();
        double projection;
        double minProjection = Double.MAX_VALUE;
        int chosenCorner = -1;
        for (int i = 0; i < 8; i++) {
            if (isPointInsideObject(corners[i], faceObject)) {
                projection = corners[i].dot(contactNormal);
                if (projection < minProjection) {
                    minProjection = projection;
                    chosenCorner = i;
                }
            }
        }
        if (chosenCorner == -1) { return null; } // No corner found (TODO: Check whether I should make a fallback here)

        // Create contact
        if (faceObject == objectA) {
            return new ContactPointFace(objectA, objectB, chosenFace, chosenCorner);
        }
        return new ContactPointFace(objectA, objectB, chosenCorner, chosenFace);
    }

    private static ContactEdgeEdge generateContactEdgeEdge(PhysicsObject objectA, ColliderCollision collision) {
        PhysicsObject objectB = collision.objectB();

        // Invert the contact normal if necessary
        Vector3d contactNormal = collision.axisOfMinOverlap();
        double[] projectionObjectA = projectObjectOntoAxis(objectA, contactNormal);
        double[] projectionObjectB = projectObjectOntoAxis(objectB, contactNormal);
        if (projectionObjectB[0] < projectionObjectA[0]) {
            contactNormal.mul(-1d);
        }

        // Get objectA's edge (The one that's closest to objectB)
        int featureA = getObjectEdgeIndex(objectA, collision.axisOfMinOverlapIndex(), contactNormal, true);

        // Get objectB's edge (The one that's closest to objectA)
        int featureB = getObjectEdgeIndex(objectB, collision.axisOfMinOverlapIndex(), contactNormal, false);

        // Create contact
        return new ContactEdgeEdge(objectA, objectB, featureA, featureB);
    }





    // Helper Methods (Point-Face)
    private static boolean isPointInsideObject(Vector3d point, PhysicsObject obj) {
        double pointProjection;
        double[] objProjection;

        for (int i = 0; i < 3; i++) {
            pointProjection = point.dot(obj.getAxis(i));
            objProjection = projectObjectOntoAxis(obj, obj.getAxis(i));
            if (objProjection[0] > pointProjection || pointProjection > objProjection[1]) {
                return false;
            }
        }
        return true;
    }





    // Helper Methods (Edge-Edge)
    private static int getObjectEdgeIndex(PhysicsObject object, int axisOfMinOverlapIndex, Vector3d contactNormal, boolean isObjectA) {
        double projection;
        double maxProjection = -Double.MAX_VALUE;

        Vector3d[] corners = object.getCornerPosAbsolute();
        int axisIndex = getObjectAxisIndex(axisOfMinOverlapIndex, isObjectA); // x (0), y (1) or z (2)
        int[] edgeStartingPointIndices = getEdgeStartingPointIndices(axisIndex);
        int feature = -1;

        for (int i = 0; i < 4; i++) { // Which edge has the deepest projection (most positive) onto the contact normal? Basically "Which one is equal to projectionObjectA[1]", but I must consider floating point errors.
            projection = corners[edgeStartingPointIndices[i]].dot(contactNormal);
            if (!isObjectA) { projection *= -1; } // Deepest projection for objectB is the most negative
            if (projection > maxProjection) {
                maxProjection = projection;
                feature = 20 + i + 4 * axisIndex; // Edges have IDs 20 - 31 (4 edges for x, 4 edges for y, and 4 edges for z)
            }
        }

        return feature;
    }

    private static int getObjectAxisIndex(int crossProductAxisIndex, boolean isObjectA) { // Takes axisIndex as used in the SAT (0-14). Returns 0, 1 or 2 for x, y or z.
        if (crossProductAxisIndex < 6 || crossProductAxisIndex > 14) { throw new IllegalArgumentException("Index does not match any cross product index (6-14)"); }
        if (isObjectA) {
            return (crossProductAxisIndex - 6) / 3;
        }
        return (crossProductAxisIndex - 6) % 3;
    }

    private static int[] getEdgeStartingPointIndices(int axisIndex) {
        if (axisIndex == 0) { // x
            return new int[]{0,1,2,3};
        }
        else if (axisIndex == 1) { // y
            return new int[]{0,1,4,5};
        }
        else if (axisIndex == 2) { // z
            return new int[]{0,2,4,6};
        }
        throw new IllegalArgumentException("axisIndex must be between 0 and 2.");
    }

}
