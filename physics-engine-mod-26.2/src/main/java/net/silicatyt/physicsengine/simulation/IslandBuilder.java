package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.Island;
import net.silicatyt.physicsengine.data.Manifold;
import net.silicatyt.physicsengine.util.UnionFind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IslandBuilder { // AI-generated
    public static List<Island> buildIslands(List<Manifold> manifolds) {
        UnionFind uf = new UnionFind();
        for (Manifold m : manifolds) uf.union(m.a.getId(), m.b.getId()); // int IDs instead of object references because Int2IntOpenHashMap in the UnionFind is faster than using object references

        Map<Integer, List<Manifold>> manifoldsByRoot = new HashMap<>();
        for (Manifold m : manifolds) {
            manifoldsByRoot.computeIfAbsent(
                uf.find(m.a.getId()),
                k -> new ArrayList<>()
            ).add(m);
        }

        List<Island> islands = new ArrayList<>();
        for (List<Manifold> islandManifolds : manifoldsByRoot.values()) islands.add(new Island(islandManifolds));
        return islands;
    }
}
