// TODO: REWORK

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
    public final int[] features = new int[2]; // TODO: Check whether an array or 2 individual entries is faster in the datapack
    public final Vector3d contactPoint = new Vector3d();
    public final Vector3d contactNormal = new Vector3d();
    public final Vector3d contactVelocity = new Vector3d();
    public double closingVelocity; // Dot product of contactVelocity and contactNormal. TODO: Check if I need to store that data, or if I can calculate it at runtime.
    public double penetrationDepth;

    public void updateAllData() {
        this.updateContactNormal();
        this.updatePenetrationDepth();
        this.updateContactPoint();
        this.updateContactVelocity();
    }

    public abstract void updateContactNormal();
    public abstract void updatePenetrationDepth();
    public abstract void updateContactPoint();
    public abstract void updateContactVelocity(); // Also updates closing velocity

    protected Vector3d calculateContactVelocity(int referenceObjectIndex) { // referenceObject is the face object for point-face
        PhysicsObject left = this.objects[referenceObjectIndex];
        PhysicsObject right = this.objects[1 - referenceObjectIndex];

        // pointVelocityLeft
        Vector3d relativeContactPoint = new Vector3d(contactPoint);
        relativeContactPoint.sub(left.getInternalPos());
        Vector3d pointVelocityLeft = left.getAngularVelocityWithoutAcceleration().cross(relativeContactPoint);
        pointVelocityLeft.add(left.getLinearVelocityWithoutAcceleration());

        // pointVelocityRight
        relativeContactPoint.set(contactPoint);
        relativeContactPoint.sub(right.getInternalPos());
        Vector3d pointVelocityRight = right.getAngularVelocityWithoutAcceleration().cross(relativeContactPoint);
        pointVelocityRight.add(right.getLinearVelocityWithoutAcceleration());

        return pointVelocityLeft.sub(pointVelocityRight);
    }
}
