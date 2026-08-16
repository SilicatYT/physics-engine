package net.silicatyt.physicsengine.simulation;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.silicatyt.physicsengine.data.Island;
import net.silicatyt.physicsengine.data.Manifold;
import net.silicatyt.physicsengine.data.PhysicsObjectPair;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.util.PairKey;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public final class CollisionDetector {
    private static Long2ObjectOpenHashMap<Manifold> previousManifolds = new Long2ObjectOpenHashMap<>();

    public static List<Island> findIslands(List<PhysicsObject> loadedObjects, ForkJoinPool pool) {
        List<PhysicsObjectPair> uniquePairs = PairFinder.findCandidatePairs(loadedObjects);
        List<Manifold> manifolds = generateContacts(uniquePairs, pool);

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

    private static List<Manifold> generateContacts(List<PhysicsObjectPair> uniquePairs, ForkJoinPool pool) {
        Long2ObjectOpenHashMap<Manifold> lastTick = previousManifolds; // For safety: To make sure the parallel stream always uses the correct manifolds, even if 'previousManifolds' gets re-assigned and the code order changes
        return pool.submit(() ->
                uniquePairs.parallelStream()
                        .map(pair -> processPair(pair, lastTick))
                        .flatMap(Optional::stream)
                        .toList()
        ).join();
    }

    private static Optional<Manifold> processPair(PhysicsObjectPair pair, Long2ObjectOpenHashMap<Manifold> lastTick) {
        // TODO: AABB, then SAT
        return Optional.empty();
    }
}
