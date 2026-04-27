package silicatyt.physics.data;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.ContactGenerator.correctContactNormalDirectionEdgeEdge;
import static silicatyt.physics.simulation.ContactGenerator.getEdgeStartingPointIndices;

public class ContactEdgeEdge extends Contact {
    public ContactEdgeEdge(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
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
    public void updateContactNormal() {
        getAxis(objectA).cross(getAxis(objectB)).normalize(contactNormal);
        correctContactNormalDirectionEdgeEdge(contactNormal, objectA, objectB);

        penetrationDepthDirty = true;
        contactNormalDirty = false;
    }

    @Override
    public void updatePenetrationDepth() { // In the datapack, I will directly work with the projections and subtract those (mathematically equivalent) because I already calculate them earlier.
        penetrationDepth = getEdgeStartingPoint(objectA).sub(getEdgeStartingPoint(objectB)).dot(contactNormal);
        penetrationDepthDirty = false;
    }

    @Override
    public void updateContactPos() { // Center of the shortest connecting line between the two edges
        // Calculation: u (EdgeStartA), v (EdgeDirectionA = AxisA), m (EdgeStartB), n (EdgeDirectionB = AxisB)
        //              Point on EdgeA = u + s * v, Point on EdgeB = m + t * n
        //              A = v * v (Always 1 because v is normalized), B = n * n (Always 1 because n is normalized), C = v * n, D = v * (u - m), E = n * (u - m)
        //              s = (CE - BD) / (AB - CC), t = (AE - CD) / (AB - CC)
        Vector3d axisA = getAxis(objectA);
        Vector3d axisB = getAxis(objectB);

        Vector3d edgeStartingPointA = getEdgeStartingPoint(objectA);
        Vector3d edgeStartingPointB = getEdgeStartingPoint(objectB);

        double c = axisA.dot(axisB);
        Vector3d startingPointDifference = new Vector3d();
        edgeStartingPointA.sub(edgeStartingPointB, startingPointDifference);
        double d = axisA.dot(startingPointDifference);
        double e = axisB.dot(startingPointDifference);
        double denominator = 1.0 - c*c; // AB - CC
        double s = (c*e - d) / denominator;
        double t = (e - c*d) / denominator;

        Vector3d pointEdgeA = new Vector3d(axisA);
        pointEdgeA.mul(s).add(edgeStartingPointA);

        Vector3d pointEdgeB = new Vector3d(axisB);
        pointEdgeB.mul(t).add(edgeStartingPointB);

        contactPos.set(pointEdgeA).add(pointEdgeB).mul(0.5d);

        contactVelocityDirty = true;
        contactPosDirty = false;
    }

    @Override
    public void updateContactVelocity() { // TODO: Check if the referenceObject is ALWAYS objectA
        contactVelocity.set(calculateContactVelocity(objectA));
        contactVelocityDirty = false;
    }





    // Helper methods
    private Vector3d getEdgeStartingPoint(PhysicsObject object) {
        int feature = getFeature(object);

        int axisIndex = getAxisIndex(feature);
        int[] edgeStartingPointIndices = getEdgeStartingPointIndices(axisIndex);
        int edgeStartingPointIndex = edgeStartingPointIndices[feature - 20 - 4 * axisIndex];

        return object.getCornerPosAbsolute(edgeStartingPointIndex);
    }

    private int getAxisIndex(int feature) { return (feature - 20) / 4; }

    private Vector3d getAxis(PhysicsObject object) { return object.getAxis(getAxisIndex(getFeature(object))); };
}
