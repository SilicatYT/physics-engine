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
        Map<Integer, Set<PhysicsObject>> dynamicObjectsByRoot = new HashMap<>();
        Map<Integer, Set<PhysicsObject>> staticObjectsByRoot = new HashMap<>();
        for (Manifold m : manifolds) {
            boolean aDynamic = m.a.getInverseMass() != 0.0;
            boolean bDynamic = m.b != null && m.b.getInverseMass() != 0.0;
            if (!aDynamic && !bDynamic) continue; // Static-static: nothing to resolve. This shouldn't be possible due to how PairFinder works, but just as an extra safety net

            PhysicsObject dynamicRepresentative = aDynamic ? m.a : m.b;
            int root = uf.find(dynamicRepresentative.getId());

            manifoldsByRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(m);
            Set<PhysicsObject> dynamicObjects = dynamicObjectsByRoot.computeIfAbsent(root, _ -> new HashSet<>());
            Set<PhysicsObject> staticObjects = staticObjectsByRoot.computeIfAbsent(root, _ -> new HashSet<>());
            if (aDynamic) dynamicObjects.add(m.a);
            else staticObjects.add(m.a);
            if (bDynamic) dynamicObjects.add(m.b);
            else staticObjects.add(m.b);
        }

        List<Island> islands = new ArrayList<>();
        for (int root : manifoldsByRoot.keySet()) {
            islands.add(new Island(manifoldsByRoot.get(root), new ArrayList<>(dynamicObjectsByRoot.get(root)), new ArrayList<>(staticObjectsByRoot.get(root))));
        }
        return islands;
    }
}
