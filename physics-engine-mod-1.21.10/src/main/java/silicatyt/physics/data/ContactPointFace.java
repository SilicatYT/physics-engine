package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.CollisionDetector.projectObjectOntoAxis;

public class ContactPointFace extends Contact {
    public ContactPointFace(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        super(objectA, objectB, featureA, featureB);
    }

    // Getters
    @Override
    public Vector3dc getContactPos() {
        if (contactPosDirty) { updateContactPos(); }
        return contactPos;
    }

    @Override
    public Vector3dc getContactNormal() {
        if (contactNormalDirty) { updateContactNormal(); }
        return contactNormal;
    }

    @Override
    public double getPenetrationDepth() {
        if (penetrationDepthDirty) { updatePenetrationDepth(); }
        return penetrationDepth;
    }





    // Updates
    @Override
    protected void updateContactNormal() {
        int faceIndex = getFaceIndex();

        PhysicsObject faceObject = getFaceObject();
        Vector3d newContactNormal = new Vector3d(faceObject.getAxis((faceIndex - 11) / 2));
        if (faceIndex % 2 == 0) {
            newContactNormal.mul(-1d);
        }

        contactNormal.set(newContactNormal);

        penetrationDepthDirty = true;
        contactPosDirty = true;
        contactNormalDirty = false;
    }

    @Override
    protected void updatePenetrationDepth() {
        penetrationDepth = projectObjectOntoAxis(getFaceObject(), contactNormal)[1] - getCornerPos().dot(contactNormal); // On new contacts, the selected corner's projection is the minProjection. That's not guaranteed afterwards (i.e., when updating the previous tick's contacts).

        contactPosDirty = true;
        penetrationDepthDirty = false;
    }

    @Override
    protected void updateContactPos() {
        contactPos.set(getCornerPos().add(new Vector3d(contactNormal).mul(penetrationDepth)));

        contactVelocityDirty = true;
        contactPosDirty = false;
    }

    @Override
    protected void updateContactVelocity() {
        contactVelocity.set(calculateContactVelocity(getFaceObject()));

        contactVelocityDirty = false;
    }





    // Helper methods
    private boolean isFeatureACorner() { return featureA < 10; }

    private PhysicsObject getFaceObject() { return isFeatureACorner() ? objectB : objectA; }

    private int getFaceIndex() { return isFeatureACorner() ? featureB : featureA; }

    private PhysicsObject getCornerObject() { return isFeatureACorner() ? objectA : objectB; }

    private int getCornerIndex() { return isFeatureACorner() ? featureA : featureB; }

    private Vector3d getCornerPos() { return getCornerObject().getCornerPosAbsolute(getCornerIndex()); }

}
