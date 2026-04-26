package silicatyt.physics.data;

import org.joml.Vector3d;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.CollisionDetector.projectObjectOntoAxis;

public class ContactPointFace extends Contact {
    public ContactPointFace(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        super(objectA, objectB, featureA, featureB);
    }

    @Override
    public void updateContactNormal() {
        int faceObjectIndex = getFaceObjectIndex();

        PhysicsObject faceObject = objects[faceObjectIndex];
        Vector3d newContactNormal = new Vector3d(faceObject.getAxis((features[faceObjectIndex] - 11) / 2));
        if (features[faceObjectIndex] % 2 == 0) {
            newContactNormal.mul(-1d);
        }

        contactNormal.set(newContactNormal);
    }

    @Override
    public void updatePenetrationDepth() {
        PhysicsObject faceObject = objects[getFaceObjectIndex()];
        penetrationDepth = projectObjectOntoAxis(faceObject, contactNormal)[1] - getCorner().dot(contactNormal); // On new contacts, the selected corner's projection is the minProjection. That's not guaranteed afterwards (i.e., when updating the previous tick's contacts).
    }

    @Override
    public void updateContactPoint() {
        contactPos.set(getCorner().add(new Vector3d(contactNormal).mul(penetrationDepth)));
    }

    @Override
    public void updateContactVelocity() {
        contactVelocity.set(calculateContactVelocity(getFaceObjectIndex()));
    }

    // Helper methods
    private int getFaceObjectIndex() {
        return features[0] < 10 ? 1 : 0;
    }

    private int getCornerObjectIndex() {
        return 1 - getFaceObjectIndex();
    }

    private Vector3d getCorner() {
        int cornerObjectIndex = getCornerObjectIndex();
        return objects[cornerObjectIndex].getCornerPosAbsolute(features[cornerObjectIndex]);
    }

}
