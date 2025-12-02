package silicatyt.physicsref.simulation;

import org.joml.Vector3d;
import silicatyt.physicsref.data.Contact;
import silicatyt.physicsref.entity.PhysicsObject;

import static silicatyt.physicsref.simulation.CollisionDetection.projectObjectOntoAxis;

public class ContactGeneration {
    public static void genContactPointFace(PhysicsObject faceObject, PhysicsObject cornerObject, Vector3d contactNormal, int axisIndex) {
        int chosenFace = 11 + (axisIndex % 3) * 2;
        if (projectObjectOntoAxis(cornerObject, contactNormal)[0] < projectObjectOntoAxis(faceObject, contactNormal)[0]) { // Invert the contact normal
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
        Contact contact = new Contact();
        if (axisIndex < 3) { // Face belongs to objectA
            faceObject.addObjectContact(cornerObject, contact);
            contact.features[0] = chosenFace;
            contact.features[1] = chosenCorner;
        } else {
            cornerObject.addObjectContact(faceObject, contact);
            contact.features[0] = chosenCorner;
            contact.features[1] = chosenFace;
        }

        // Calculate contact data
        double penetrationDepth = projectObjectOntoAxis(faceObject, contactNormal)[1] - minProjection;
        Vector3d contactPoint = corners[chosenCorner].add(new Vector3d(contactNormal).mul(penetrationDepth)); // (new vector to avoid overwriting contactNormal) Corner projected back onto the surface
        Vector3d contactVelocity = calculateContactVelocity(faceObject, cornerObject, contactPoint);

        // Store contact data
        contact.contactNormal.set(contactNormal);
        contact.contactPoint.set(contactPoint);
        contact.penetrationDepth = penetrationDepth;
        contact.contactVelocity.set(contactVelocity);
        contact.closingVelocity = contactVelocity.dot(contactNormal);
    }

    private static Vector3d calculateContactVelocity(PhysicsObject left, PhysicsObject right, Vector3d contactPoint) { // Important: The order of the objects matters. For point-face, the "left" object is the one with the face, so that the dot product with the contact normal is the closingVelocity (without any sign changes necessary).
        // pointVelocityA
        Vector3d relativeContactPoint = new Vector3d(contactPoint);
        relativeContactPoint.sub(left.getInternalPos());
        Vector3d pointVelocityA = left.getAngularVelocityWithoutAcceleration().cross(relativeContactPoint);
        pointVelocityA.add(left.getLinearVelocityWithoutAcceleration());

        // pointVelocityB
        relativeContactPoint.set(contactPoint);
        relativeContactPoint.sub(right.getInternalPos());
        Vector3d pointVelocityB = right.getAngularVelocityWithoutAcceleration().cross(relativeContactPoint);
        pointVelocityB.add(right.getLinearVelocityWithoutAcceleration());

        return pointVelocityA.sub(pointVelocityB);
    }

    public static void genContactEdgeEdge(PhysicsObject objectA, PhysicsObject objectB, Vector3d contactNormal) {

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

    public static void accumulateContacts() { // TODO: Run once after every new contact has been added. Iterate over every object pair

    }
}
