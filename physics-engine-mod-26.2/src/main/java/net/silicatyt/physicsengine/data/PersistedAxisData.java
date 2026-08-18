package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public record PersistedAxisData(int index, double overlap, Vector3dc axis, boolean isFacingOutward, boolean isFacingB) implements AxisData {} // Didn't make it so "AxisData" (now CandidateAxisData) is contained by PersistedAxisData because it would be annoyingly nested (and require 1 new heap allocation). Probably not worth it for just 3 shared elements.
