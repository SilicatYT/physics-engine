package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.Island;
import net.silicatyt.physicsengine.data.Manifold;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.util.UnionFind;

import java.util.*;

public final class IslandBuilder { // AI-generated
    public static List<Island> buildIslands(List<Manifold> manifolds) {
        UnionFind uf = new UnionFind();
        for (Manifold m : manifolds) uf.union(m.a.getId(), m.b.getId()); // int IDs instead of object references because Int2IntOpenHashMap in the UnionFind is faster than using object references

        Map<Integer, List<Manifold>> manifoldsByRoot = new HashMap<>();
        Map<Integer, Set<PhysicsObject>> objectsByRoot = new HashMap<>();
        for (Manifold m : manifolds) {
            int root = uf.find(m.a.getId());
            manifoldsByRoot.computeIfAbsent(
                    root, k -> new ArrayList<>()
            ).add(m);

            Set<PhysicsObject> objects = objectsByRoot.computeIfAbsent(root, k -> new HashSet<>());
            objects.add(m.a);
            if (m.b != null) objects.add(m.b);
        }

        List<Island> islands = new ArrayList<>();
        for (int root : manifoldsByRoot.keySet()) {
            islands.add(new Island(manifoldsByRoot.get(root), new ArrayList<>(objectsByRoot.get(root))));
        }
        return islands;
    }
}
