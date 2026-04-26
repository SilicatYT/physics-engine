// TODO: REWORK

package silicatyt.physics.data;

import org.joml.Vector3d;
import silicatyt.physics.entity.PhysicsObject;

import static silicatyt.physics.simulation.ContactGenerator.getEdgeStartingPointIndices;

public class ContactEdgeEdge extends Contact {
    public ContactEdgeEdge(PhysicsObject objectA, PhysicsObject objectB, int featureA, int featureB) {
        super(objectA, objectB, featureA, featureB);
    }

    @Override
    public void updateContactNormal() {
        Vector3d axisA = objects[0].getAxis((features[0] - 20) / 4);
        Vector3d axisB = objects[1].getAxis((features[1] - 20) / 4);
        axisA.cross(axisB, this.contactNormal);
    }

    @Override
    public void updatePenetrationDepth() { // Very unoptimized, but I had to refactor and I want to keep the same formula as the one I'll use in the datapack. It keeps recalculating the same known values. If I were to rewrite this mod, I'd do many things much differently. I also use "xyzA / xyzB" and "xyz[0] / xyz[1]" inconsistently. I'd also make it so you can't re-assign the individual feature values, by making features into an enum or its own class.
        Vector3d[] edgeStartingPoints = this.getEdgeStartingPoints();

        this.penetrationDepth = edgeStartingPoints[0].dot(this.contactNormal) - edgeStartingPoints[1].dot(this.contactNormal);
    }

    @Override
    public void updateContactPoint() { // Center of the shortest connecting line between the two edges
        // Calculation: u (EdgeStartA), v (EdgeDirectionA = AxisA), m (EdgeStartB), n (EdgeDirectionB = AxisB)
        //              Point on EdgeA = u + s * v, Point on EdgeB = m + t * n
        //              A = v * v (Always 1 because v is normalized), B = n * n (Always 1 because n is normalized), C = v * n, D = v * (u - m), E = n * (u - m)
        //              s = (CE - BD) / (AB - CC), t = (AE - CD) / (AB - CC)
        Vector3d axisA = this.objects[0].getAxis((this.features[0] - 20) / 4);
        Vector3d axisB = this.objects[1].getAxis((this.features[1] - 20) / 4);
        Vector3d[] edgeStartingPoints = this.getEdgeStartingPoints();
        double c = axisA.dot(axisB); // Reminder: .dot() does NOT override the left input. It only returns the result.
        Vector3d startingPointDifference = new Vector3d();
        edgeStartingPoints[0].sub(edgeStartingPoints[1], startingPointDifference); // u - m. Reminder: .sub() would override the left element if I didn't specify an output variable
        double d = axisA.dot(startingPointDifference);
        double e = axisB.dot(startingPointDifference);
        double denominator = 1.0 - c*c; // AB - CC
        double s = (c*e - d) / denominator;
        double t = (e - c*d) / denominator;

        Vector3d pointEdgeA = new Vector3d(axisA);
        pointEdgeA.mul(s).add(edgeStartingPoints[0]);

        Vector3d pointEdgeB = new Vector3d(axisB);
        pointEdgeB.mul(t).add(edgeStartingPoints[1]);

        this.contactPoint.set(pointEdgeA).add(pointEdgeB).mul(0.5d);
    }

    @Override
    public void updateContactVelocity() {
        this.contactVelocity.set(this.calculateContactVelocity(0)); // TODO: Check if the referenceObject is ALWAYS objectA
    }

    // Helper methods
    private Vector3d[] getEdgeStartingPoints() {
        Vector3d[] cornersA = this.objects[0].getCornerPosAbsolute();
        int axisIndexA = (this.features[0] - 20) / 4;
        int[] edgeStartingPointIndicesA = getEdgeStartingPointIndices(axisIndexA);
        int edgeStartingPointIndexA = edgeStartingPointIndicesA[this.features[0] - 20 - 4 * axisIndexA];

        Vector3d[] cornersB = this.objects[1].getCornerPosAbsolute();
        int axisIndexB = (this.features[1] - 20) / 4;
        int[] edgeStartingPointIndicesB = getEdgeStartingPointIndices(axisIndexB);
        int edgeStartingPointIndexB = edgeStartingPointIndicesB[this.features[1] - 20 - 4 * axisIndexB];

        return new Vector3d[]{cornersA[edgeStartingPointIndexA], cornersB[edgeStartingPointIndexB]};
    }
}
