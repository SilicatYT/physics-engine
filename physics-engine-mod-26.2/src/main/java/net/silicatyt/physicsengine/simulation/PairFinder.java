package net.silicatyt.physicsengine.simulation;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.silicatyt.physicsengine.data.PhysicsObjectPair;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.util.PairKey;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.floor;
import static java.lang.Math.floorDiv;

public class PairFinder { // AI-generated
    private static final long MAX_PRESIZE = 2_000_000;
    public static List<PhysicsObjectPair> findCandidatePairs(List<PhysicsObject> loadedObjects) {
        var buckets = buildBuckets(loadedObjects);
        return collectUniquePairs(buckets);
    }

    private static Long2ObjectOpenHashMap<List<PhysicsObject>> buildBuckets(List<PhysicsObject> objects) {  // Building buckets for each chunk, for fast object pair creation
        // TODO: Maybe use something octree-like with more buckets, or at least make the chunks cubic
        Long2ObjectOpenHashMap<List<PhysicsObject>> buckets = new Long2ObjectOpenHashMap<>();
        for (PhysicsObject obj : objects) {
            Vector3dc pos = obj.getInternalPos();
            Vector3dc min = obj.getAabbRelativeMin(), max = obj.getAabbRelativeMax(); // Relative to center of mass
            int chunkMinX = floorDiv((int) floor(min.x() + pos.x()), 16);
            int chunkMaxX = floorDiv((int) floor(max.x() + pos.x()), 16);
            int chunkMinZ = floorDiv((int) floor(min.z() + pos.z()), 16);
            int chunkMaxZ = floorDiv((int) floor(max.z() + pos.z()), 16);
            for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                    buckets.computeIfAbsent(
                            PairKey.pack(cx, cz),
                            k -> new ArrayList<>()
                    ).add(obj);
                }
            }
        }
        return buckets;
    }

    private static List<PhysicsObjectPair> collectUniquePairs(Long2ObjectOpenHashMap<List<PhysicsObject>> buckets) { // Sequential because "pairs" can't be safely written to in parallel
        long estimatedPairs = 0; // To prevent excessive doubling as new entries are added to 'tested' and 'pairs'
        for (List<PhysicsObject> bucket : buckets.values()) {
            long n = bucket.size();
            estimatedPairs += n * (n - 1) / 2;
        }

        LongOpenHashSet tested = new LongOpenHashSet((int) Math.min(estimatedPairs, MAX_PRESIZE));
        List<PhysicsObjectPair> pairs = new ArrayList<>((int) Math.min(estimatedPairs, MAX_PRESIZE));
        for (List<PhysicsObject> bucket : buckets.values()) {
            for (int i = 0; i < bucket.size(); i++) {
                for (int j = i + 1; j < bucket.size(); j++) {
                    PhysicsObject a = bucket.get(i), b = bucket.get(j);
                    if (!tested.add(PairKey.packUnordered(a.getId(), b.getId()))) continue;
                    boolean aStatic = a.getInverseMass() == 0.0;
                    boolean bStatic = b.getInverseMass() == 0.0;
                    if (aStatic && bStatic) continue; // Two static objects cannot collide with each other
                    if (aStatic) { PhysicsObject temp = a; a = b; b = temp; } // Static objects will always be objectB (Makes checks easier)
                    pairs.add(new PhysicsObjectPair(a, b));
                    }
                }
            }
        return pairs;
    }

}
