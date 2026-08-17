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
import static net.silicatyt.physicsengine.simulation.Integrator.EPSILON;

public final class CollisionDetector {
    private static Long2ObjectOpenHashMap<Manifold> previousManifolds = new Long2ObjectOpenHashMap<>();

    public static List<Island> findIslands(List<PhysicsObject> loadedObjects, ForkJoinPool pool) {
        List<PhysicsObjectPair> uniquePairs = PairFinder.findCandidatePairs(loadedObjects);
        List<Manifold> manifolds = generateManifolds(uniquePairs, pool);

        previousManifolds = buildManifoldCache(manifolds);
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

        if (!testAabb(a, b, dx, dy, dz)) return Optional.empty();
        Optional<SatResult> collision = testSat(a, b, dx, dy, dz, lastTick);
        if (collision.isEmpty()) return Optional.empty();
        //return ContactGenerator.generateManifold(a, b, collision.get(), dx, dy, dz);
        // TODO ^
        return Optional.empty();
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
        int persistedAxisIndex = lastTickManifold != null ? lastTickManifold.getPersistedAxisIndex() : -1;
        Vector3dc persistedAxis = null;
        double persistedAxisOverlap = Double.MAX_VALUE;

        int candidateAxisIndex = -1;
        Vector3d candidateAxis = new Vector3d(); // Can't re-assign vectors as I go because of the cross products (Mutable vector, can't use "=")
        double candidateAxisOverlap = Double.MAX_VALUE;

        Vector3dc axis;
        double axisOverlap;

        // a's axes
        Vector3dc scaleA = a.getScale();
        for (int i = 0; i < 3; i++) { // TODO: Put the contents of the for-loop into a helper method (or 3). But how to do it cleanly, I can't pass 'double' references in Java.
            axis = a.getAxis(i);
            axisOverlap = calculateAxisOverlap(scaleA.get(i) * 0.5, calculateRelativeMaxProjectionOntoAxis(b, axis), axis, dx, dy, dz);
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
        Vector3dc scaleB = b.getScale();
        for (int i = 3; i < 6; i++) {
            axis = b.getAxis(i - 3);
            axisOverlap = calculateAxisOverlap(calculateRelativeMaxProjectionOntoAxis(a, axis), scaleB.get(i - 3) * 0.5, axis, dx, dy, dz);
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
                if (lengthSquared < EPSILON) continue; // Degenerate axis, ignore it
                mutableAxis.normalize(Math.sqrt(lengthSquared)); // Doesn't calculate lengthSquared again

                axisOverlap = calculateAxisOverlap(calculateRelativeMaxProjectionOntoAxis(a, mutableAxis), calculateRelativeMaxProjectionOntoAxis(b, mutableAxis), mutableAxis, dx, dy, dz);
                if (axisOverlap <= 0.0) return Optional.empty();
                if (axisIndex == persistedAxisIndex) {
                    persistedAxis = new Vector3d(mutableAxis); // TODO: Maybe it's not clean to only assign a new object sometimes? I only need it for the current tick, so the re-assignment of getAxis() (Which only changes the next tick) is not a problem
                    persistedAxisOverlap = axisOverlap;
                }
                if (axisOverlap < candidateAxisOverlap) {
                    candidateAxisIndex = axisIndex;
                    candidateAxis.set(mutableAxis);
                    candidateAxisOverlap = axisOverlap;
                }
            }
        }

        Optional<PersistedAxisData> persisted = persistedAxisIndex == -1 ? Optional.empty() : Optional.of(new PersistedAxisData(
                persistedAxisIndex, persistedAxisOverlap, persistedAxis, lastTickManifold.isPersistedAxisFacingOutward(), lastTickManifold.isPersistedAxisFacingB()
        ));
        return Optional.of(
                new SatResult(
                        new AxisData(candidateAxisIndex, candidateAxisOverlap, candidateAxis),
                        persisted
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
        return relativeMaxProjectionA + relativeMaxProjectionB - abs(axis.dot(dx, dy, dz));
    }

}

// TODO: When I have to calculate relativeContactPos for ContactGenerator, do so with ex, ey and ez (fastest way), and only calculate the 4 points I have to calculate. Perhaps keep track of what I've calculated so far, in case a 2nd collision with a different object in the same tick happens. I wouldn't want to calculate the same corners multiple times: At that point, it would be faster to just calculate all 8 once, which I don't want to do.