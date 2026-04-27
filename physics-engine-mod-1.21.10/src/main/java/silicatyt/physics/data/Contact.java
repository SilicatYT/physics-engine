package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

public abstract class Contact {
    public final PhysicsObject objectA;
    public final PhysicsObject objectB; // 'null' if it's a TerrainContact
    public final int featureA;
    public final int featureB;
    protected final Vector3d contactPos = new Vector3d();
    protected final Vector3d contactNormal = new Vector3d();
    protected final Vector3d contactVelocity = new Vector3d();
    protected double penetrationDepth;

    protected boolean contactPosDirty = true;
    protected boolean contactNormalDirty = true;
    protected boolean contactVelocityDirty = true;
    protected boolean penetrationDepthDirty = true;

    public Contact(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.featureA = featureA;
        this.featureB = featureB;
    }

    public abstract Vector3dc getContactPos();
    public abstract Vector3dc getContactNormal();
    public abstract double getPenetrationDepth();
    protected abstract void updateContactNormal();
    protected abstract void updatePenetrationDepth();
    protected abstract void updateContactPos();
    protected abstract void updateContactVelocity();


    public Vector3dc getContactVelocity() {
        if (contactPosDirty) { updateContactPos(); }
        if (contactVelocityDirty) { updateContactVelocity(); }
        return contactVelocity;
    }

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
