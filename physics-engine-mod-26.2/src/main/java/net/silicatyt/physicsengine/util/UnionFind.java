package net.silicatyt.physicsengine.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class UnionFind {  // AI-generated
    private final Int2IntOpenHashMap parent = new Int2IntOpenHashMap();

    public int find(int x) {
        if (!parent.containsKey(x)) {
            parent.put(x, x);
            return x;
        }
        int root = x;
        while (parent.get(root) != root) root = parent.get(root);
        while (parent.get(x) != root) {
            int next = parent.get(x);
            parent.put(x, root);
            x = next;
        }
        return root;
    }

    public void union(int a, int b) {
        int rootA = find(a), rootB = find(b);
        if (rootA != rootB) parent.put(rootA, rootB);
    }
}
