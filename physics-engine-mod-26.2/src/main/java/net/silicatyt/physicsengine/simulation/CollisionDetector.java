package net.silicatyt.physicsengine.simulation;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.silicatyt.physicsengine.data.*;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.util.PairKey;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static java.lang.Math.abs;
import static net.silicatyt.physicsengine.simulation.Integrator.EPSILON_SQUARED;

public final class CollisionDetector {
    private static final double FACE_AXIS_PREFERENCE_MULTIPLIER = 1.0 / 0.7;
    private static final double FACE_AXIS_PREFERENCE_MULTIPLIER_SQUARED = FACE_AXIS_PREFERENCE_MULTIPLIER * FACE_AXIS_PREFERENCE_MULTIPLIER;
    private static final double CROSS_PRODUCT_EPSILON = 1e-3;
    private static final double CROSS_PRODUCT_EPSILON_SQUARED = CROSS_PRODUCT_EPSILON * CROSS_PRODUCT_EPSILON;

    private static Long2ObjectOpenHashMap<Manifold> previousManifolds = new Long2ObjectOpenHashMap<>();

    public static List<Island> findIslands(List<PhysicsObject> loadedObjects, ForkJoinPool pool) {
        // Generate manifolds
        List<PhysicsObjectPair> uniquePairs = PairFinder.findCandidatePairs(loadedObjects);
        List<Manifold> manifolds = generateManifolds(uniquePairs, pool);

        // Carry over old manifolds for warm-starting purposes
        Long2ObjectOpenHashMap<Manifold> newManifolds = buildManifoldCache(manifolds);
        carryOverOldManifolds(uniquePairs, newManifolds, previousManifolds);
        previousManifolds = newManifolds;

        return IslandBuilder.buildIslands(manifolds);
    }

    private static Long2ObjectOpenHashMap<Manifold> buildManifoldCache(List<Manifold> manifolds) {
        Long2ObjectOpenHashMap<Manifold> cache = new Long2ObjectOpenHashMap<>(manifolds.size());
        for (Manifold m : manifolds) {
            cache.put(
                    PairKey.packUnordered(m.a.getId(), m.b.getId()),
                    m
            );
        }
        return cache;
    }

    private static void carryOverOldManifolds(List<PhysicsObjectPair> uniquePairs, Long2ObjectOpenHashMap<Manifold> current, Long2ObjectOpenHashMap<Manifold> previous) {
        for (PhysicsObjectPair pair : uniquePairs) {
            long key = PairKey.packUnordered(pair.a().getId(), pair.b().getId());
            if (current.containsKey(key)) continue; // Simply keep the newly generated manifold

            Manifold old = previous.get(key);
            if (old == null) continue; // No manifold to carry over

            if (old.isToBeDiscarded()) continue; // Too old, don't carry over
            old.incrementInactiveTime();

            current.put(key, old); // Carry over to the next tick despite there not being a contact, because maybe I can still warm-start in the future
        }
    }

    private static List<Manifold> generateManifolds(List<PhysicsObjectPair> uniquePairs, ForkJoinPool pool) {
        Long2ObjectOpenHashMap<Manifold> lastTick = previousManifolds; // For safety: To make sure the parallel stream always uses the correct manifolds, even if 'previousManifolds' gets re-assigned and the code order changes

        return pool.submit(() ->
                uniquePairs.parallelStream()
                        .map(pair -> processPair(pair, lastTick))
                        .flatMap(Optional::stream)
                        .toList()
        ).join();
    }

    private static Optional<Manifold> processPair(PhysicsObjectPair pair, Long2ObjectOpenHashMap<Manifold> lastTick) {
        PhysicsObject a = pair.a();
        PhysicsObject b = pair.b();
        Vector3dc posA = a.getInternalPos();
        Vector3dc posB = b.getInternalPos();
        double dx = posA.x() - posB.x();
        double dy = posA.y() - posB.y();
        double dz = posA.z() - posB.z();

        if (!testAabb(a, b, dx, dy, dz)) return Optional.empty(); // TODO: Maybe somehow get rid of the many parameters (dx, dy, dz). But how? I want to avoid allocating objects on the heap.
        Optional<SatResult> collision = testSat(a, b, dx, dy, dz, lastTick);
        return collision.flatMap(satResult -> ContactGenerator.generateManifold(a, b, satResult, dx, dy, dz));
    }

    private static boolean testAabb(PhysicsObject a, PhysicsObject b, double dx, double dy, double dz) {
        Vector3dc minA = a.getAabbRelativeMin();
        Vector3dc maxA = a.getAabbRelativeMax();
        Vector3dc minB = b.getAabbRelativeMin();
        Vector3dc maxB = b.getAabbRelativeMax();

        return minA.x() + dx <= maxB.x()
                && maxA.x() + dx >= minB.x()
                && minA.y() + dy <= maxB.y()
                && maxA.y() + dy >= minB.y()
                && minA.z() + dz <= maxB.z()
                && maxA.z() + dz >= minB.z();
    }

    private static Optional<SatResult> testSat(PhysicsObject a, PhysicsObject b, double dx, double dy, double dz, Long2ObjectOpenHashMap<Manifold> lastTick) { // Gottschalk-Erikson approach
        // TODO: Add helper methods, clean up
        // TODO: Maybe store the disconnecting axis for failed SATs and perform its check first in the next tick? Might not be easily possible with the new approach
        // Setup
        Manifold lastTickManifold = lastTick.get(PairKey.packUnordered(a.getId(), b.getId()));

        int persistedAxisIndex = lastTickManifold != null && !lastTickManifold.isOld() ? lastTickManifold.persistedAxisIndex : -1;
        double persistedAxisOverlapSquared = -1.0;

        int candidateAxisIndex = -1;
        double candidateAxisOverlap = Double.MAX_VALUE;

        // SAT pre-calculations
        double[][] axisDot = new double[3][3]; // axisDot[i][j] is the projection of a's axis i onto b's axis j
        double[][] absAxisDot = new double[3][3];
        double[] offsetInA = new double[3]; // offset (a-b) in a's local axis frame

        // objectA axes
        for (int i = 0; i < 3; i++) {
            // Setup
            for (int j = 0; j < 3; j++) {
                axisDot[i][j] = a.getAxis(i).dot(b.getAxis(j));
                absAxisDot[i][j] = Math.abs(axisDot[i][j]);
            }
            offsetInA[i] = a.getAxis(i).dot(dx, dy, dz);

            // Overlap calculation
            double radiusA = a.getHalfExtent(i);
            double radiusB = b.getHalfExtent(0) * absAxisDot[i][0]
                    + b.getHalfExtent(1) * absAxisDot[i][1]
                    + b.getHalfExtent(2) * absAxisDot[i][2];
            double distanceAlongAxis = Math.abs(offsetInA[i]);
            double overlap = radiusA + radiusB - distanceAlongAxis;

            // Check
            if (overlap <= 0.0) return Optional.empty();
            if (i == persistedAxisIndex) {
                persistedAxisOverlapSquared = overlap * overlap;
            }
            if (overlap < candidateAxisOverlap) {
                candidateAxisIndex = i;
                candidateAxisOverlap = overlap;
            }
        }

        // objectB axes
        for (int j = 0; j < 3; j++) {
            // Setup
            for (int i = 0; i < 3; i++) {
                axisDot[i][j] = a.getAxis(i).dot(b.getAxis(j));
                absAxisDot[i][j] = Math.abs(axisDot[i][j]);
            }

            // Overlap calculation
            double radiusA = a.getHalfExtent(0) * absAxisDot[0][j]
                    + a.getHalfExtent(1) * absAxisDot[1][j]
                    + a.getHalfExtent(2) * absAxisDot[2][j];
            double radiusB = b.getHalfExtent(j);
            double distanceAlongAxis = Math.abs(
                    offsetInA[0] * axisDot[0][j]
                    + offsetInA[1] * axisDot[1][j]
                    + offsetInA[2] * axisDot[2][j]
            );
            double overlap = radiusA + radiusB - distanceAlongAxis;

            // Check
            if (overlap <= 0.0) return Optional.empty();
            int collisionAxisIndex = j + 3;
            if (collisionAxisIndex == persistedAxisIndex) {
                persistedAxisOverlapSquared = overlap * overlap;
            }
            if (overlap < candidateAxisOverlap) {
                candidateAxisIndex = collisionAxisIndex;
                candidateAxisOverlap = overlap;
            }
        }

        double candidateAxisOverlapSquared = candidateAxisOverlap * candidateAxisOverlap;
        double candidateAxisLengthSquared = 1.0; // Face axes are unit length

        // Cross-product axes
        for (int i = 0; i < 3; i++) {
            int iNext = (i + 1) % 3, iPrev = (i + 2) % 3;
            for (int j = 0; j < 3; j++) {
                int jNext = (j + 1) % 3, jPrev = (j + 2) % 3;
                double axisLengthSquared = 1.0 - axisDot[i][j] * axisDot[i][j];
                if (axisLengthSquared < CROSS_PRODUCT_EPSILON_SQUARED) continue; // Axes nearly parallel, degenerate

                double radiusA = a.getHalfExtent(iNext) * absAxisDot[iPrev][j]
                        + a.getHalfExtent(iPrev) * absAxisDot[iNext][j];
                double radiusB = b.getHalfExtent(jNext) * absAxisDot[i][jPrev]
                        + b.getHalfExtent(jPrev) * absAxisDot[i][jNext];
                double distanceAlongAxis = Math.abs(
                        offsetInA[iPrev] * axisDot[iNext][j]
                        - offsetInA[iNext] * axisDot[iPrev][j]
                );
                double unnormalizedOverlap = radiusA + radiusB - distanceAlongAxis;

                // Check
                if (unnormalizedOverlap <= 0.0) return Optional.empty();
                double overlapSquared = unnormalizedOverlap * unnormalizedOverlap;
                double biasedOverlapSquared = candidateAxisIndex < 6 ? overlapSquared * FACE_AXIS_PREFERENCE_MULTIPLIER_SQUARED : overlapSquared;
                int collisionAxisIndex = 6 + 3*i + j;
                if (collisionAxisIndex == persistedAxisIndex) persistedAxisOverlapSquared = overlapSquared / axisLengthSquared;
                if (biasedOverlapSquared * candidateAxisLengthSquared < candidateAxisOverlapSquared * axisLengthSquared) {
                    candidateAxisIndex = collisionAxisIndex;
                    candidateAxisOverlapSquared = overlapSquared;
                    candidateAxisLengthSquared = axisLengthSquared;
                }
            }
        }

        // Process results
        candidateAxisOverlapSquared /= candidateAxisLengthSquared;
        Optional<PersistedAxisData> persisted = persistedAxisIndex == -1 || persistedAxisOverlapSquared < 0.0 ? Optional.empty() : Optional.of(new PersistedAxisData( // The overlapSquared check is necessary in case the axis was a degenerate cross-product axis that should be ignored
                persistedAxisIndex, persistedAxisOverlapSquared, lastTickManifold.persistedAxisFacingOutward, lastTickManifold.persistedAxisFacingB
        ));
        return Optional.of(
                new SatResult(
                        new CandidateAxisData(candidateAxisIndex, candidateAxisOverlapSquared),
                        persisted,
                        lastTickManifold == null ? Optional.empty() : Optional.of(lastTickManifold)
                )
        );
    }

}
