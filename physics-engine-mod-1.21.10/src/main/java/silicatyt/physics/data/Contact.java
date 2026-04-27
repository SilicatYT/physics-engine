package silicatyt.physics.data;

import org.joml.Vector3d;
import silicatyt.physics.entity.PhysicsObject;

public abstract class Contact {
    public final PhysicsObject objectA;
    public final PhysicsObject objectB;
    public final int featureA;
    public final int featureB;
    public final Vector3d contactPos = new Vector3d();
    public final Vector3d contactNormal = new Vector3d();
    public final Vector3d contactVelocity = new Vector3d();
    // public double closingVelocity; // Dot product of contactVelocity and contactNormal. TODO: Check if I need to store that data, or if I should calculate it at runtime (Probably depends on which resolver algorithm I'll use).
    public double penetrationDepth;

    public Contact(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.featureA = featureA;
        this.featureB = featureB;
    }

    public void updateAllData() {
        updateContactNormal();
        updatePenetrationDepth();
        updateContactPoint();
        updateContactVelocity();
    }

    public abstract void updateContactNormal();
    public abstract void updatePenetrationDepth();
    public abstract void updateContactPoint();
    public abstract void updateContactVelocity(); // Also updates closing velocity (IF I store it in the contact directly)

    protected int getFeature(PhysicsObject object) { return object == objectA ? featureA : featureB; }

    protected Vector3d calculateContactVelocity(PhysicsObject referenceObject) {
        if (referenceObject != objectA && referenceObject != objectB) { throw new IllegalArgumentException("referenceObject must be the contact's objectA or objectB."); }
        PhysicsObject otherObject = objectA == referenceObject ? objectB : objectA;
        Vector3d relativeContactPos = new Vector3d();

        // pointVelocityReference
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(referenceObject.getInternalPos());
        Vector3d pointVelocityReference = referenceObject.getAngularVelocity().cross(relativeContactPos);
        pointVelocityReference.add(referenceObject.getLinearVelocity());

        // pointVelocityOther
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(otherObject.getInternalPos());
        Vector3d pointVelocityOther = otherObject.getAngularVelocity().cross(relativeContactPos);
        pointVelocityOther.add(otherObject.getLinearVelocity());

        return pointVelocityReference.sub(pointVelocityOther);
    }
}
