package silicatyt.physics.simulation;

import org.joml.Vector3d;
import silicatyt.physics.data.Contact;
import silicatyt.physics.data.ContactEdgeEdge;
import silicatyt.physics.data.ContactPointFace;
import silicatyt.physics.entity.PhysicsObject;

import java.util.ArrayList;
import java.util.HashMap;

import static silicatyt.physics.simulation.CollisionDetection.projectObjectOntoAxis;

// TODO: REWORK

public class ContactGeneration {

    public static void genContactPointFace(PhysicsObject objectA, PhysicsObject objectB, Vector3d contactNormal, int axisIndex) {
        // Select faceObject and cornerObject
        PhysicsObject faceObject;
        PhysicsObject cornerObject;
        boolean objectAIsFaceObject = axisIndex < 3;
        if (objectAIsFaceObject) {
            faceObject = objectA;
            cornerObject = objectB;
        } else {
            faceObject = objectB;
            cornerObject = objectA;
        }

        // Select the correct face and invert the contact normal if necessary
        int chosenFace = 11 + (axisIndex % 3) * 2;
        if (projectObjectOntoAxis(cornerObject, contactNormal)[0] < projectObjectOntoAxis(faceObject, contactNormal)[0]) {
            contactNormal.mul(-1d);
            chosenFace--; // The chosenFace is 10 for normal=-X, 11 for normal = +X, 12 for normal=-Y etc
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
        if (chosenCorner == -1) { // No corner found (TODO: Check whether I should make a fallback here)
            return;
        }

        // Create contact
        Contact contact;
        if (objectAIsFaceObject) { // Ugly duplicate check, but it doesn't matter
            contact = new ContactPointFace(objectA, objectB, chosenFace, chosenCorner);
        } else {
            contact = new ContactPointFace(objectA, objectB, chosenCorner, chosenFace);
        }
        contact.updateAllData();
        objectA.addObjectContact(objectB, contact);
    }

    public static void genContactEdgeEdge(PhysicsObject objectA, PhysicsObject objectB, Vector3d contactNormal, int axisIndex) {
        // Invert the contact normal if necessary
        double[] projectionObjectA = projectObjectOntoAxis(objectA, contactNormal);
        double[] projectionObjectB = projectObjectOntoAxis(objectB, contactNormal);
        if (projectionObjectB[0] < projectionObjectA[0]) { // Invert the contact normal
            contactNormal.mul(-1d);
        }

        // Get objectA's edge (The one that's closest to objectB)
        double maxProjection = -Double.MAX_VALUE;
        double projection;

        Vector3d[] cornersA = objectA.getCornerPosAbsolute();
        int axisIndexA = getAxisIndex(axisIndex, true); // x (0), y (1) or z (2)
        int[] edgeStartingPointIndicesA = getEdgeStartingPointIndices(axisIndexA);
        int featureA = 0;
        for (int i = 0; i < 4; i++) { // Which edge has the deepest projection (most positive) onto the contact normal? Basically "Which one is equal to projectionObjectA[1]", but I must consider floating point errors. TODO: Maybe change it to a loop that just checks for the highest value. Not sure which approach is better for the datapack.
            projection = cornersA[edgeStartingPointIndicesA[i]].dot(contactNormal);
            if (projection > maxProjection) {
                maxProjection = projection;
                featureA = 20 + i + 4 * axisIndexA; // Edges have IDs 20 - 31 (4 edges for x, 4 edges for y, and 4 edges for z)
            }
        }

        // Get objectB's edge (The one that's closest to objectA)
        double minProjection = Double.MAX_VALUE;

        Vector3d[] cornersB = objectB.getCornerPosAbsolute();
        int axisIndexB = getAxisIndex(axisIndex, false);
        int[] edgeStartingPointIndicesB = getEdgeStartingPointIndices(axisIndexB);
        int featureB = 0;
        for (int i = 0; i < 4; i++) {
            projection = cornersB[edgeStartingPointIndicesB[i]].dot(contactNormal);
            if (projection < minProjection) {
                minProjection = projection;
                featureB = 20 + i + 4 * axisIndexB;
            }
        }

        // Create contact
        Contact contact = new ContactEdgeEdge(objectA, objectB, featureA, featureB);
        contact.updateAllData();
        objectA.addObjectContact(objectB, contact);
    }

    public static int getAxisIndex(int crossProductAxisIndex, boolean isObjectA) { // Takes axisIndex as used in the SAT (0-14), but it assumes it's a cross product axis (so 6-14). Returns 0, 1 or 2 for x, y or z.
        if (isObjectA) {
            return (crossProductAxisIndex - 6) / 3;
        }
        return (crossProductAxisIndex - 6) % 3;
    }

    public static int[] getEdgeStartingPointIndices(int axisIndex) { // axisIndex is 0,1,2 for x,y,z. NOT 0-14 like in the SAT (for object pairs). Only for a single object.
        if (axisIndex == 0) { // x
            return new int[]{0,1,2,3};
        }
        else if (axisIndex == 1) { // y
            return new int[]{0,1,4,5};
        }
        else { // z
            return new int[]{0,2,4,6};
        }
    }

    public static boolean isPointInsideObject(Vector3d corner, PhysicsObject obj) {
        double pointProjection;
        double[] objProjection;

        for (int i = 0; i < 3; i++) {
            pointProjection = corner.dot(obj.getAxis(i));
            objProjection = projectObjectOntoAxis(obj, obj.getAxis(i));
            if (objProjection[0] > pointProjection || pointProjection > objProjection[1]) {
                return false;
            }
        }
        return true;
    }

    public static void accumulateContacts(Object object, HashMap<PhysicsObject, ArrayList<Contact>> allContactsMap) { // TODO: Run once after every new contact has been added. Iterate over every object pair
        for (HashMap.Entry<PhysicsObject, ArrayList<Contact>> entry : allContactsMap.entrySet()) {
            PhysicsObject objectB = entry.getKey();
            ArrayList<Contact> contacts = entry.getValue();
            // TODO
        }
    }
}
