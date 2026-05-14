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
        Vector3d newContactNormal = new Vector3d(faceObject.getAxis((faceIndex - 10) / 2));
        if (faceIndex % 2 == 0) { newContactNormal.negate(); }
        if (faceObject == objectA) { newContactNormal.negate(); } // contactNormal always points from B -> A

        contactNormal.set(newContactNormal);
    }

    @Override
    protected void updatePenetrationDepth() { // (TODO: Verify if this comment is still correct, now that the contactNormal points from B -> A): On new contacts, the selected corner's projection is the minProjection. That's not guaranteed afterwards (i.e., when updating the previous tick's contacts).
        double[] faceObjectProjection = projectObjectOntoAxis(getFaceObject(), contactNormal); // TODO: Optimize. It currently projects all 8 corners instead of the 2 needed ones
        double cornerProjection = getCornerPos().dot(contactNormal);
        if (getFaceObject() == objectA) {
            penetrationDepth = cornerProjection - faceObjectProjection[0];
        } else {
            penetrationDepth = faceObjectProjection[1] - cornerProjection;
        }
    }

    @Override
    protected void updateContactPos() {
        if (getFaceObject() == objectA) {
            contactPos.set(getCornerPos()).sub(new Vector3d(contactNormal).mul(penetrationDepth));
        } else {
            contactPos.set(getCornerPos()).add(new Vector3d(contactNormal).mul(penetrationDepth));
        }
    }





    // Helper methods
    private boolean isFeatureACorner() { return featureA < 10; }

    private PhysicsObject getFaceObject() { return isFeatureACorner() ? objectB : objectA; }

    private int getFaceIndex() { return isFeatureACorner() ? featureB : featureA; }

    private PhysicsObject getCornerObject() { return isFeatureACorner() ? objectA : objectB; }

    private int getCornerIndex() { return isFeatureACorner() ? featureA : featureB; }

    private Vector3dc getCornerPos() { return getCornerObject().getCornerPosAbsolute(getCornerIndex()); }

}
