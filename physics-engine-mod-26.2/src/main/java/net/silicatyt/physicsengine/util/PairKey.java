package net.silicatyt.physicsengine.util;

public class PairKey { // AI-generated (Pack 2 ints into a single long)
    public static long pack(int high, int low) { return ((long) high << 32) | (low & 0xFFFFFFFFL); }

    public static long packUnordered(int a, int b) { return pack(Math.max(a, b), Math.min(a, b)); }
}
