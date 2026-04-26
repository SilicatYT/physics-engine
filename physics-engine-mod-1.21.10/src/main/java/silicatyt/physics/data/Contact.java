package silicatyt.physics.data;

import org.joml.Vector3d;
import silicatyt.physics.entity.PhysicsObject;

public abstract class Contact {
    public Contact(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        this.objects[0] = objectA;
        this.objects[1] = objectB;
        this.features[0] = featureA;
        this.features[1] = featureB;
    }

    public final PhysicsObject[] objects = new PhysicsObject[2];
    public final int[] features = new int[2];
    public final Vector3d contactPos = new Vector3d();
    public final Vector3d contactNormal = new Vector3d();
    public final Vector3d contactVelocity = new Vector3d();
    // public double closingVelocity; // Dot product of contactVelocity and contactNormal. TODO: Check if I need to store that data, or if I can calculate it at runtime.
    public double penetrationDepth;

    public void updateAllData() {
        updateContactNormal();
        updatePenetrationDepth();
        updateContactPoint();
        updateContactVelocity();
    }

    public abstract void updateContactNormal();
    public abstract void updatePenetrationDepth();
    public abstract void updateContactPoint();
    public abstract void updateContactVelocity(); // Also updates closing velocity

    protected Vector3d calculateContactVelocity(int referenceObjectIndex) { // referenceObject is the face object for point-face
        PhysicsObject left = objects[referenceObjectIndex];
        PhysicsObject right = objects[1 - referenceObjectIndex];

        Vector3d relativeContactPos = new Vector3d();

        // pointVelocityLeft
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(left.getInternalPos());
        Vector3d pointVelocityLeft = left.getAngularVelocity().cross(relativeContactPos);
        pointVelocityLeft.add(left.getLinearVelocity());

        // pointVelocityRight
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(right.getInternalPos());
        Vector3d pointVelocityRight = right.getAngularVelocity().cross(relativeContactPos);
        pointVelocityRight.add(right.getLinearVelocity());

        return pointVelocityLeft.sub(pointVelocityRight);
    }
}
