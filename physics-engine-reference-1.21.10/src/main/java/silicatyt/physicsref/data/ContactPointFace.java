package silicatyt.physicsref.data;

import org.joml.Vector3d;
import silicatyt.physicsref.entity.PhysicsObject;

import static silicatyt.physicsref.simulation.CollisionDetection.projectObjectOntoAxis;

public class ContactPointFace extends Contact {
    public ContactPointFace(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        super(objectA, objectB, featureA, featureB);
    }

    @Override
    public void updateContactNormal() {
        PhysicsObject faceObject = this.objects[this.getFaceObjectIndex()];
        Vector3d newContactNormal = new Vector3d(faceObject.getAxis((this.features[this.getFaceObjectIndex()] - 11) / 2));
        if (this.getFaceObjectIndex() % 2 == 0) {
            newContactNormal.mul(-1d);
        }
        this.contactNormal.set(newContactNormal);
    }

    @Override
    public void updatePenetrationDepth() {
        PhysicsObject faceObject = this.objects[this.getFaceObjectIndex()];
        this.penetrationDepth = projectObjectOntoAxis(faceObject, this.contactNormal)[1] - this.getCorner().dot(this.contactNormal); // On new contacts, the selected corner's projection is the minProjection. That's not guaranteed during contact accumulation.
    }

    @Override
    public void updateContactPoint() {
        this.contactPoint.set(this.getCorner().add(new Vector3d(contactNormal).mul(penetrationDepth))); // New vector to avoid overwriting contactNormal
    }

    @Override
    public void updateContactVelocity() {
        this.contactVelocity.set(this.calculateContactVelocity(this.getFaceObjectIndex()));
    }

    // Helper methods
    private int getFaceObjectIndex() {
        return this.features[0] < 10 ? 1 : 0;
    }

    private int getCornerObjectIndex() {
        return 1 - this.getFaceObjectIndex();
    }

    private Vector3d getCorner() {
        return this.objects[this.getCornerObjectIndex()].getCornerPosAbsolute()[this.features[this.getCornerObjectIndex()]];
    }

}
