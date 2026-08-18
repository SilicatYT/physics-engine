package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.*;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Vector3dc;

import java.util.Optional;

public final class ContactGenerator {
    private static final double PREVIOUS_AXIS_PREFERENCE_MULTIPLIER = 0.9; // The currently chosen axis has to clearly win against the previous one, otherwise prefer the old one
    private static final double FACE_AXIS_PREFERENCE_MULTIPLIER = 0.7; // Edge-edge axes need to clearly win against face-face, otherwise prefer face-face
    private static final double PREVIOUS_EDGE_AXIS_PREFERENCE_MULTIPLIER = 0.95; // Prefer the previous axis, but not as strongly because it's edge-edge compared to face-face

    public static Optional<Manifold> generateManifold(PhysicsObject a, PhysicsObject b, SatResult collision, double dx, double dy, double dz) {
        boolean persistedAxisPreferred = isPersistedAxisPreferred(collision);
        AxisData chosenAxisData = persistedAxisPreferred ? collision.persistedAxisData().get() : collision.candidateAxisData(); // TODO: Get rid of the "might be empty" warning (It can't be empty because of the boolean) and the repeated "collision.persistedAxisData().get()"
        boolean contactNormalFacingB = persistedAxisPreferred ? collision.persistedAxisData().get().isFacingB() : isAxisFacingB(chosenAxisData.axis(), dx, dy, dz);

        Optional<ContactState> contact;
        boolean contactNormalFacingOutward = false;
        int chosenAxisIndex = chosenAxisData.index();

        if (chosenAxisIndex < 6) {
            contactNormalFacingOutward = persistedAxisPreferred ? collision.persistedAxisData().get().isFacingOutward() : (chosenAxisIndex < 3) == contactNormalFacingB;
            contact = generateContactPointFace(a, b, chosenAxisData, contactNormalFacingB, contactNormalFacingOutward);
        } else {
            contact = generateContactEdgeEdge(a, b, chosenAxisData, contactNormalFacingB);
        }
        return contact.isEmpty() ? Optional.empty() : Optional.of(new Manifold(
                a, b, contact.get(), chosenAxisIndex, contactNormalFacingB, contactNormalFacingOutward
        ));
    }

    private static boolean isPersistedAxisPreferred(SatResult collision) {
        if (collision.persistedAxisData().isEmpty()) return false;

        PersistedAxisData persistedAxis = collision.persistedAxisData().get();
        AxisData candidateAxis = collision.candidateAxisData();

        boolean persistedAxisIsPointFace = persistedAxis.index() < 6;
        boolean candidateAxisIsPointFace = candidateAxis.index() < 6;
        double persistedAxisMultiplier =
                persistedAxisIsPointFace == candidateAxisIsPointFace
                        ? PREVIOUS_AXIS_PREFERENCE_MULTIPLIER
                        : persistedAxisIsPointFace
                        ? FACE_AXIS_PREFERENCE_MULTIPLIER
                        : PREVIOUS_EDGE_AXIS_PREFERENCE_MULTIPLIER;
        return persistedAxis.overlap() * persistedAxisMultiplier < candidateAxis.overlap();
    }

    public static boolean isAxisFacingB(Vector3dc axis, double dx, double dy, double dz) {
        return axis.dot(dx, dy, dz) < 0;
    }

    // Face
    private static Optional<ContactState> generateContactPointFace(PhysicsObject a, PhysicsObject b, AxisData axisData, boolean contactNormalFacingB, boolean contactNormalFacingOutward) {
        return Optional.empty();
    }

    // Edge-edge
    private static Optional<ContactState> generateContactEdgeEdge(PhysicsObject a, PhysicsObject b, AxisData axisData, boolean contactNormalFacingB) {
        return Optional.empty();
    }

}

// TODO: Group the classes in "data" better
// TODO: Maybe split the ContactGenerator into 2 classes (or 3, because of isPersistedAxisPreferred and stuff). Package "simulation", or a sub-package?
// TODO: Maybe it's not clean to have "FaceContactState" and "EdgeContactPoint" both implement ContactState? Maybe I should add an explicit "EdgeContactState" for readability, even if it's not needed?
