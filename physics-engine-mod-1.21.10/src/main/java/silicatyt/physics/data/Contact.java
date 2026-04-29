package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;
import silicatyt.physics.versioning.VersionNode;

public abstract class Contact {
    public final PhysicsObject objectA;
    public final PhysicsObject objectB; // 'null' if it's a TerrainContact
    public final int featureA;
    public final int featureB;
    protected final Vector3d contactPos = new Vector3d();
    protected final Vector3d contactNormal = new Vector3d();
    protected final Vector3d contactVelocity = new Vector3d();
    protected double penetrationDepth;

    protected final VersionNode contactPosVersion = new VersionNode(this::updateContactPos);
    protected final VersionNode contactNormalVersion = new VersionNode(this::updateContactNormal);
    protected final VersionNode contactVelocityVersion = new VersionNode(this::updateContactVelocity);
    protected final VersionNode penetrationDepthVersion = new VersionNode(this::updatePenetrationDepth);

    public Contact(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.featureA = featureA;
        this.featureB = featureB;

        // Add variable dependencies
        contactVelocityVersion.addDependencies(
                objectA.getPosVersion(),  objectB.getPosVersion(),
                objectA.getLinearVelocityVersion(),  objectB.getLinearVelocityVersion(),
                objectA.getAngularVelocityVersion(),  objectB.getAngularVelocityVersion()
                );
    }

    public Vector3dc getContactPos() {
        contactPosVersion.updateIfNeeded();
        return contactPos;
    }

    public Vector3dc getContactNormal() {
        contactNormalVersion.updateIfNeeded();
        return contactNormal;
    }

    public double getPenetrationDepth() {
        penetrationDepthVersion.updateIfNeeded();
        return penetrationDepth;
    }

    protected abstract void updateContactNormal();
    protected abstract void updatePenetrationDepth();
    protected abstract void updateContactPos();
    protected abstract void updateContactVelocity();


    public Vector3dc getContactVelocity() {
        contactVelocityVersion.updateIfNeeded();
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
        Vector3d pointVelocityReference = new Vector3d(referenceObject.getAngularVelocity()).cross(relativeContactPos);
        pointVelocityReference.add(referenceObject.getLinearVelocity());

        // pointVelocityOther
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(otherObject.getInternalPos());
        Vector3d pointVelocityOther = new Vector3d(otherObject.getAngularVelocity()).cross(relativeContactPos);
        pointVelocityOther.add(otherObject.getLinearVelocity());

        return pointVelocityReference.sub(pointVelocityOther);
    }
}
