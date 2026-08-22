package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.*;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollisionResolver {
    private static final double MIN_DELTA_VELOCITY = 0.0; // TODO: Define a number
    private static final double MIN_DELTA_VELOCITY_SQUARED = MIN_DELTA_VELOCITY * MIN_DELTA_VELOCITY;

    // Setup
    public static void resolve(Island island) {
        for (PhysicsObject obj : island.objects()) obj.snapshotPreSolveVelocity(); // So targetVelocity can use the velocity from before warm-starting was applied. Additional loop required because applying warm-starting affects two objects, not just one.

        // Merge manifold contact points together & pre-calculate values
        List<ResolvingContact> allContacts = new ArrayList<>();
        for (Manifold m : island.manifolds()) {
            ManifoldSolverContext manifoldContext = buildManifoldContext(m);

            for (ContactPoint p : m.state.getPoints()) {
                ContactSolverContext contactContext = buildContactContext(p, m);
                ContactSolverState state = new ContactSolverState();
                ResolvingContact c = new ResolvingContact(p, m, manifoldContext, contactContext, state);
                allContacts.add(c);

                state.accumulatedImpulseContactSpace.set(p.getAccumulatedImpulse());
                convertToContactSpace(state.accumulatedImpulseContactSpace, manifoldContext);

                // Warm-starting
                if (state.accumulatedImpulseContactSpace.lengthSquared() >= MIN_DELTA_VELOCITY_SQUARED) {
                    applyImpulse(c, p.getAccumulatedImpulse());
                }
            }
        }

        // Velocity resolution
        allContacts.sort(Comparator.comparingDouble((ResolvingContact c) -> c.state().accumulatedImpulseContactSpace.x()).reversed()); // Sorted by warm-start impulse along contact normal (desc) for stack stability
        for (ResolvingContact c : allContacts) {
             // TODO
        }

        // Penetration resolution
        allContacts.sort(Comparator.comparingDouble((ResolvingContact c) -> c.point().getPenetrationDepth()).reversed()); // TODO: Check whether sorting by the previous tick's applied splitImpulse would be more stable. I'd need to move accumulatedSplitImpulse from ContactSolverState to ContactPoint in that case.
        for (ResolvingContact c : allContacts) {
            // TODO
        }
    }

    private static ManifoldSolverContext buildManifoldContext(Manifold m) {
        Matrix3dc orthonormalBasis = calculateOrthonormalBasis(m);
        double frictionCoefficient = calculateFrictionCoefficient(m);
        double restitutionCoefficient = calculateRestitutionCoefficient(m);
        return new ManifoldSolverContext(orthonormalBasis, frictionCoefficient, restitutionCoefficient);
    }

    private static ContactSolverContext buildContactContext(ContactPoint p, Manifold m) {
        double targetClosingVelocity = calculateTargetClosingVelocity(p);
        double biasVelocity = calculateBiasVelocity(p);
        Vector3dc effectiveMass = calculateEffectiveMass(p);
        return new ContactSolverContext(targetClosingVelocity, biasVelocity, effectiveMass);
    }

    private static void convertToContactSpace(Vector3d v, ManifoldSolverContext ctx) {
        ctx.orthonormalBasis().transformTranspose(v);
    }

    private static Matrix3dc calculateOrthonormalBasis(Manifold m) { return null; } // TODO
    private static double calculateFrictionCoefficient(Manifold m) { return 0; } // TODO
    private static double calculateRestitutionCoefficient(Manifold m) { return 0; } // TODO

    private static double calculateTargetClosingVelocity(ContactPoint p) { return 0; } // TODO
    private static double calculateBiasVelocity(ContactPoint p) { return 0; } // TODO
    private static Vector3dc calculateEffectiveMass(ContactPoint p) { return null; } // TODO


    // Velocity resolution
    private static void applyImpulse(ResolvingContact contact, Vector3dc impulse) {
        // TODO
    }


    // Penetration resolution


}
