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

    private static Optional<SatResult> testSat(PhysicsObject a, PhysicsObject b, double dx, double dy, double dz, Long2ObjectOpenHashMap<Manifold> lastTick) { // Half extents could also be pre-calculated in PhysicsObject, but with my versioning system, that's absolutely not worth it
        Manifold lastTickManifold = lastTick.get(PairKey.packUnordered(a.getId(), b.getId()));

        int persistedAxisIndex = lastTickManifold != null && !lastTickManifold.isOld() ? lastTickManifold.persistedAxisIndex : -1;
        Vector3dc persistedAxis = null;
        double persistedAxisOverlap = Double.MAX_VALUE;

        int candidateAxisIndex = -1;
        Vector3d candidateAxis = new Vector3d(); // Can't re-assign vectors as I go because of the cross products (Mutable vector, can't use "=")
        double candidateAxisOverlap = Double.MAX_VALUE;

        Vector3dc axis;
        double axisOverlap;

        // a's axes
        for (int i = 0; i < 3; i++) { // TODO: Put the contents of the for-loop into a helper method (or 3). But how to do it cleanly, I can't pass 'double' references in Java.
            axis = a.getAxis(i);
            axisOverlap = calculateAxisOverlap(a.getHalfExtent(i), calculateRelativeMaxProjectionOntoAxis(b, axis), axis, dx, dy, dz);
            if (axisOverlap <= 0.0) return Optional.empty();
            if (i == persistedAxisIndex) {
                persistedAxis = axis;
                persistedAxisOverlap = axisOverlap;
            }
            if (axisOverlap < candidateAxisOverlap) {
                candidateAxisIndex = i;
                candidateAxis.set(axis);
                candidateAxisOverlap = axisOverlap;
            }
        }

        // b's axes
        for (int i = 3; i < 6; i++) {
            int axisIndex = i - 3;
            axis = b.getAxis(axisIndex);
            axisOverlap = calculateAxisOverlap(calculateRelativeMaxProjectionOntoAxis(a, axis), b.getHalfExtent(axisIndex), axis, dx, dy, dz);
            if (axisOverlap <= 0.0) return Optional.empty();
            if (i == persistedAxisIndex) {
                persistedAxis = axis;
                persistedAxisOverlap = axisOverlap;
            }
            if (axisOverlap < candidateAxisOverlap) {
                candidateAxisIndex = i;
                candidateAxis.set(axis);
                candidateAxisOverlap = axisOverlap;
            }
        }

        // Cross product axes
        Vector3d mutableAxis = new Vector3d();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int axisIndex = 6 + 3*i + j;
                a.getAxis(i).cross(b.getAxis(j), mutableAxis);
                double lengthSquared = mutableAxis.lengthSquared();
                if (lengthSquared < EPSILON_SQUARED) continue; // Degenerate axis, ignore it

                double inverseLength = 1.0 / Math.sqrt(lengthSquared); // TODO: Optimize it by not normalizing here (only at the end when the axis was chosen). I have to modify the axisOverlap calculation for this.
                mutableAxis.x *= inverseLength;
                mutableAxis.y *= inverseLength;
                mutableAxis.z *= inverseLength;

                axisOverlap = calculateAxisOverlap(calculateRelativeMaxProjectionOntoAxis(a, mutableAxis), calculateRelativeMaxProjectionOntoAxis(b, mutableAxis), mutableAxis, dx, dy, dz);
                if (axisOverlap <= 0.0) return Optional.empty();
                if (axisIndex == persistedAxisIndex) {
                    persistedAxis = new Vector3d(mutableAxis); // TODO: Maybe it's not clean to only assign a new object sometimes? I only need it for the current tick, so the re-assignment of getAxis() (Which only changes the next tick) is not a problem
                    persistedAxisOverlap = axisOverlap;
                }
                double effectiveOverlap = (candidateAxisIndex < 6 && axisIndex >= 6) ? axisOverlap * FACE_AXIS_PREFERENCE_MULTIPLIER : axisOverlap;
                if (effectiveOverlap < candidateAxisOverlap) {
                    candidateAxisIndex = axisIndex;
                    candidateAxis.set(mutableAxis);
                    candidateAxisOverlap = axisOverlap;
                }
            }
        }

        Optional<PersistedAxisData> persisted = persistedAxisIndex == -1 || persistedAxis == null ? Optional.empty() : Optional.of(new PersistedAxisData( // The null check is necessary in case the axis was a degenerate cross product axis and was never assigned
                persistedAxisIndex, persistedAxisOverlap, persistedAxis, lastTickManifold.persistedAxisFacingOutward, lastTickManifold.persistedAxisFacingB
        ));
        return Optional.of(
                new SatResult(
                        new CandidateAxisData(candidateAxisIndex, candidateAxisOverlap, candidateAxis),
                        persisted,
                        lastTickManifold == null ? Optional.empty() : Optional.of(lastTickManifold)
                )
        );
    }

    private static double calculateRelativeMaxProjectionOntoAxis(PhysicsObject obj, Vector3dc axis) {
        double x = obj.getHalfExtentAxisProjection(0).dot(axis);
        double y = obj.getHalfExtentAxisProjection(1).dot(axis);
        double z = obj.getHalfExtentAxisProjection(2).dot(axis);
        return abs(x) + abs(y) + abs(z);
    }

    private static double calculateAxisOverlap(double relativeMaxProjectionA, double relativeMaxProjectionB, Vector3dc axis, double dx, double dy, double dz) {
        return relativeMaxProjectionA + relativeMaxProjectionB - abs(axis.dot(dx, dy, dz)); // TODO: Pass this dot product to ContactGenerator, can be re-used in PointFace (projection onto tangent axes, constA & constB calculation) & EdgeEdge
    }

}

// TODO: When I have to calculate relativeContactPos for ContactGenerator, do so with ex, ey and ez (fastest way), and only calculate the 4 points I have to calculate. Perhaps keep track of what I've calculated so far, in case a 2nd collision with a different object in the same tick happens. I wouldn't want to calculate the same corners multiple times: At that point, it would be faster to just calculate all 8 once, which I don't want to do.