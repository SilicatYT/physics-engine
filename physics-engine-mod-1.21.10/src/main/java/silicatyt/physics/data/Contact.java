package silicatyt.physics.data;

import org.joml.Matrix3d;
import org.joml.Matrix3dc;
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

    private final Matrix3d orthonormalBasis = new Matrix3d();
    private final Vector3d inverseEffectiveMass = new Vector3d(); // effective mass along each axis of the orthonormal basis

    public boolean isActive = true;

    protected final VersionNode contactPosVersion = new VersionNode(this::updateContactPos);
    protected final VersionNode contactNormalVersion = new VersionNode(this::updateContactNormal);
    protected final VersionNode contactVelocityVersion = new VersionNode(this::updateContactVelocity);
    protected final VersionNode penetrationDepthVersion = new VersionNode(this::updatePenetrationDepth);
    private final VersionNode orthonormalBasisVersion = new VersionNode(this::updateOrthonormalBasis);
    private final VersionNode inverseEffectiveMassVersion = new VersionNode(this::updateInverseEffectiveMass);

    public Contact(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.featureA = featureA;
        this.featureB = featureB;

        // Add variable dependencies
        contactVelocityVersion.addDependencies(contactPosVersion, objectA.getPosVersion(), objectA.getLinearVelocityVersion(), objectA.getAngularVelocityVersion());
        orthonormalBasisVersion.addDependencies(contactNormalVersion);
        inverseEffectiveMassVersion.addDependencies(contactPosVersion, orthonormalBasisVersion, objectA.getInverseMassVersion(), objectA.getPosVersion(), objectA.getInverseInertiaTensorWorldVersion());

        if (objectB != null) {
            contactVelocityVersion.addDependencies(objectB.getPosVersion(), objectB.getLinearVelocityVersion(), objectB.getAngularVelocityVersion());
            inverseEffectiveMassVersion.addDependencies(objectB.getInverseMassVersion(), objectB.getPosVersion(), objectB.getInverseInertiaTensorWorldVersion());
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

    public double getClosingVelocity() { return -getContactVelocity().dot(getContactNormal()); }

    public double getRestitutionCoefficient() { return objectB == null ? objectA.getRestitutionCoefficient() : Math.min(objectA.getRestitutionCoefficient(), objectB.getRestitutionCoefficient()); } // TODO: Maybe change to avg or something else

    public double getFrictionCoefficient() { return objectB == null ? objectA.getFrictionCoefficient() : Math.min(objectA.getFrictionCoefficient(), objectB.getFrictionCoefficient()); } // TODO: Maybe change to avg or something else

    private void updateOrthonormalBasis() { // TODO: Optimize if it's a terrain contact (Or in general: contactNormal is one of the world axes)
        Vector3d tangent1 = new Vector3d();
        if (Math.abs(contactNormal.x) > Math.abs(contactNormal.y)) {
            // Use (z, 0, -x)
            tangent1.set(contactNormal.z, 0.0, -contactNormal.x);
            tangent1.normalize();
        } else {
            // Use (0, -z, y)
            tangent1.set(0.0, -contactNormal.z, contactNormal.y);
            tangent1.normalize();
        }
        Vector3d tangent2 = new Vector3d(contactNormal).cross(tangent1);
        orthonormalBasis.set(contactNormal, tangent1, tangent2);
    }

    public Matrix3dc getOrthonormalBasis() {
        orthonormalBasisVersion.updateIfNeeded();
        return orthonormalBasis;
    }

    private void updateInverseEffectiveMass() {
        Vector3d axis = new Vector3d();
        double combinedEffectiveMass;

        Vector3d relativeContactPos0 = new Vector3d();
        Vector3d crossProduct0 = new Vector3d();
        Vector3d angular0 = new Vector3d();
        double term0;

        Vector3d relativeContactPos1 = null;
        Vector3d crossProduct1 = null;
        Vector3d angular1 = null;
        double term1 = 0.0;
        if (objectB != null) {
            relativeContactPos1 = new Vector3d();
            crossProduct1 = new Vector3d();
            angular1 = new Vector3d();
        }

        for (int i = 0; i < 3; i++) {
            orthonormalBasis.getColumn(i, axis);

            // For objectA
            relativeContactPos0.set(contactPos).sub(objectA.getInternalPos());
            crossProduct0.set(relativeContactPos0.cross(axis));
            objectA.getInverseInertiaTensorWorld().transform(crossProduct0, angular0);
            term0 = angular0.dot(crossProduct0) + objectA.getInverseMass();

            // For objectB
            if (objectB != null) {
                relativeContactPos1.set(contactPos).sub(objectB.getInternalPos());
                crossProduct1.set(relativeContactPos1.cross(axis));
                objectB.getInverseInertiaTensorWorld().transform(crossProduct1, angular1);
                term1 = angular1.dot(crossProduct1) + objectB.getInverseMass();
            }

            // Total
            combinedEffectiveMass = 1.0 / (term0 + term1);
            switch (i) {
                case 0 -> inverseEffectiveMass.x = combinedEffectiveMass;
                case 1 -> inverseEffectiveMass.y = combinedEffectiveMass;
                case 2 -> inverseEffectiveMass.z = combinedEffectiveMass;
            }
        }
    }

    public Vector3dc getInverseEffectiveMass() {
        inverseEffectiveMassVersion.updateIfNeeded();
        return inverseEffectiveMass;
    }
}
