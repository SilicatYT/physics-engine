package net.silicatyt.physicsengine.simulation;

import net.silicatyt.physicsengine.data.Island;
import net.silicatyt.physicsengine.data.Manifold;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.util.UnionFind;

import java.util.*;

public final class IslandBuilder { // AI-generated
    public static List<Island> buildIslands(List<Manifold> manifolds) {
        UnionFind uf = new UnionFind();
        for (Manifold m : manifolds) {
            boolean aDynamic = m.a.getInverseMass() != 0.0;
            boolean bDynamic = m.b != null && m.b.getInverseMass() != 0.0;
            if (aDynamic && bDynamic) uf.union(m.a.getId(), m.b.getId()); // Never union through a static object
        }

        Map<Integer, List<Manifold>> manifoldsByRoot = new HashMap<>();
        Map<Integer, Set<PhysicsObject>> objectsByRoot = new HashMap<>();
        for (Manifold m : manifolds) {
            boolean aDynamic = m.a.getInverseMass() != 0.0;
            boolean bDynamic = m.b != null && m.b.getInverseMass() != 0.0;
            if (!aDynamic && !bDynamic) continue; // Static-static: nothing to resolve. This shouldn't be possible due to how PairFinder works, but just as an extra safety net

            PhysicsObject dynamicRepresentative = aDynamic ? m.a : m.b;
            int root = uf.find(dynamicRepresentative.getId());

            manifoldsByRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(m);
            Set<PhysicsObject> objects = objectsByRoot.computeIfAbsent(root, k -> new HashSet<>());
            if (aDynamic) objects.add(m.a);
            if (bDynamic) objects.add(m.b);
        }

        List<Island> islands = new ArrayList<>();
        for (int root : manifoldsByRoot.keySet()) {
            islands.add(new Island(manifoldsByRoot.get(root), new ArrayList<>(objectsByRoot.get(root))));
        }
        return islands;
    }
}
