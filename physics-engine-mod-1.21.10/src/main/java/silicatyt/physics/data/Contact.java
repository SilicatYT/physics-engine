package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;
import silicatyt.physics.versioning.VersionNode;

// TODO: You can currently access protected variables everywhere in the same package. Make it so those are exclusive to contact and its subclasses.
// TODO: Maybe isActive should not be public? But I need to be able to set it in accumulation...

public abstract class Contact {
    public final PhysicsObject objectA;
    public final PhysicsObject objectB; // 'null' if it's a TerrainContact
    public final int featureA;
    public final int featureB;
    protected final Vector3d contactPos = new Vector3d();
    protected final Vector3d contactNormal = new Vector3d();
    protected final Vector3d contactVelocity = new Vector3d();
    protected double penetrationDepth;

    public boolean isActive = true;

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
        contactVelocityVersion.addDependencies(contactPosVersion, objectA.getPosVersion(), objectA.getLinearVelocityVersion(), objectA.getAngularVelocityVersion());
        if (objectB != null) {
            contactVelocityVersion.addDependencies(objectB.getPosVersion(), objectB.getLinearVelocityVersion(), objectB.getAngularVelocityVersion());
        }
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

    protected void updateContactVelocity() {
        Vector3d relativeContactPos = new Vector3d();

        // pointVelocityA
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(objectA.getInternalPos());
        Vector3d pointVelocityA = new Vector3d(objectA.getAngularVelocity()).cross(relativeContactPos);
        pointVelocityA.add(objectA.getLinearVelocity());

        // pointVelocityB
        relativeContactPos.set(contactPos);
        relativeContactPos.sub(objectB.getInternalPos());
        Vector3d pointVelocityB = new Vector3d(objectB.getAngularVelocity()).cross(relativeContactPos);
        pointVelocityB.add(objectB.getLinearVelocity());

        contactVelocity.set(pointVelocityA.sub(pointVelocityB));
    }

    public Vector3dc getContactVelocity() {
        contactVelocityVersion.updateIfNeeded();
        return contactVelocity;
    }

    protected int getFeature(PhysicsObject object) { return object == objectA ? featureA : featureB; }




    }
}
