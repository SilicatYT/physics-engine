package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public record PersistedAxisData(int index, double overlap, Vector3dc axis, boolean isFacingOutward, boolean isFacingB) {} // Doesn't contain AxisData because it would be annoyingly nested. Probably not worth it for just 3 shared elements.
