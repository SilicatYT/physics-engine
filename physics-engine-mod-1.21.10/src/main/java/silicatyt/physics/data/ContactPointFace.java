package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.CollisionDetector.projectObjectOntoAxis;

public class ContactPointFace extends Contact {
    public ContactPointFace(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        super(objectA, objectB, featureA, featureB);

        // Add variable dependencies
        PhysicsObject cornerObject = getCornerObject();

        contactPosVersion.addDependencies(
                cornerObject.getPosVersion(),
                cornerObject.getOrientationVersion(),
                contactNormalVersion, penetrationDepthVersion
        );
        contactNormalVersion.addDependencies(getFaceObject().getOrientationVersion());
        penetrationDepthVersion.addDependencies(
                objectA.getPosVersion(), objectB.getPosVersion(),
                objectA.getOrientationVersion(), objectB.getOrientationVersion(),
                contactNormalVersion
        );
    }





    // Updates
    @Override
    protected void updateContactNormal() {
        int faceIndex = getFaceIndex();

        PhysicsObject faceObject = getFaceObject();
        Vector3d newContactNormal = new Vector3d(faceObject.getAxis((faceIndex - 11) / 2));
        if (faceIndex % 2 == 0) { newContactNormal.mul(-1d); }

        contactNormal.set(newContactNormal);
    }

    @Override
    protected void updatePenetrationDepth() {
        penetrationDepth = projectObjectOntoAxis(getFaceObject(), contactNormal)[1] - getCornerPos().dot(contactNormal); // On new contacts, the selected corner's projection is the minProjection. That's not guaranteed afterwards (i.e., when updating the previous tick's contacts).
    }

    @Override
    protected void updateContactPos() {
        contactPos.set(new Vector3d(contactNormal).mul(penetrationDepth).add(getCornerPos()));
    }

    @Override
    protected void updateContactVelocity() {
        contactVelocity.set(calculateContactVelocity(getFaceObject()));
    }





    // Helper methods
    private boolean isFeatureACorner() { return featureA < 10; }

    private PhysicsObject getFaceObject() { return isFeatureACorner() ? objectB : objectA; }

    private int getFaceIndex() { return isFeatureACorner() ? featureB : featureA; }

    private PhysicsObject getCornerObject() { return isFeatureACorner() ? objectA : objectB; }

    private int getCornerIndex() { return isFeatureACorner() ? featureA : featureB; }

    private Vector3dc getCornerPos() { return getCornerObject().getCornerPosAbsolute(getCornerIndex()); }

}
