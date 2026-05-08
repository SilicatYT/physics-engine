package silicatyt.physics.data;

import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;
import silicatyt.physics.versioning.VersionNode;

// TODO: You can currently access protected variables everywhere in the same package. Make it so those are exclusive to contact and its subclasses.
// TODO: frictionCoefficient and restitutionCoefficient are not persisted, so their getters do not need dependencies, at least for now. But I do run getXYZ() on them more than once per tick...
// TODO: Should I move the "calculateTargetVelocity()" logic here, so it's in one place? Or would that make it look like it needs dependencies and an automatically updated getter? It's technically resolution logic, but so are orthonormalBasis and inverseEffectiveMass, no?
// TODO: Add setters and getters for targetClosingVelocity. Don't keep it public. Also verify if it's finite. How to make it clear that those getters do not automatically update?
// TODO: Clean up method order

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
    private final Vector3d accumulatedImpulse = new Vector3d();
    public double targetClosingVelocity; // It should not be updated except manually. Its whole point is being cached, so it has no dependencies.
    private final Vector3d accumulatedSplitImpulse = new Vector3d(); // TODO: Clean up
    public double biasVelocity; // TODO: Same notes as for targetClosingVelocity

    private boolean isActive = true;

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

    protected void updateContactVelocity() { // TODO: Rework so it works for terrain contacts too
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

        Vector3d relativeContactPosA = new Vector3d();
        Vector3d crossProductA = new Vector3d();
        Vector3d angularA = new Vector3d();
        double termA;

        Vector3d relativeContactPosB = null;
        Vector3d crossProductB = null;
        Vector3d angularB = null;
        double termB = 0.0;
        if (objectB != null) {
            relativeContactPosB = new Vector3d();
            crossProductB = new Vector3d();
            angularB = new Vector3d();
        }

        for (int i = 0; i < 3; i++) {
            orthonormalBasis.getColumn(i, axis);

            // For objectA
            relativeContactPosA.set(contactPos).sub(objectA.getInternalPos());
            crossProductA.set(relativeContactPosA.cross(axis));
            objectA.getInverseInertiaTensorWorld().transform(crossProductA, angularA);
            termA = angularA.dot(crossProductA) + objectA.getInverseMass();

            // For objectB
            if (objectB != null) {
                relativeContactPosB.set(contactPos).sub(objectB.getInternalPos());
                crossProductB.set(relativeContactPosB.cross(axis));
                objectB.getInverseInertiaTensorWorld().transform(crossProductB, angularB);
                termB = angularB.dot(crossProductB) + objectB.getInverseMass();
            }

            // Total
            combinedEffectiveMass = 1.0 / (termA + termB);
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

    public Vector3dc getAccumulatedImpulse() { return accumulatedImpulse; }

    public void addAccumulatedImpulse(Vector3dc impulseDelta) {
        if (!impulseDelta.isFinite()) { throw new IllegalArgumentException("Impulse delta must be finite"); }
        accumulatedImpulse.add(impulseDelta);
    }

    public Vector3dc getAccumulatedSplitImpulse() { return accumulatedSplitImpulse; }

    public void addAccumulatedSplitImpulse(Vector3dc impulseDelta) {
        if (!impulseDelta.isFinite()) { throw new IllegalArgumentException("Impulse delta must be finite"); }
        accumulatedSplitImpulse.add(impulseDelta);
    }

    public void clearAccumulatedSplitImpulse() { accumulatedSplitImpulse.zero(); }

    public void setActivity(boolean isActive) {
        if (!isActive) { accumulatedImpulse.zero(); }
        this.isActive = isActive;
    }

    public boolean isActive() { return isActive; }
}
