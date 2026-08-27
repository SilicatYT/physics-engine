package net.silicatyt.physicsengine.data;

public record CandidateAxisData(int index, double overlapSquared, double lengthSquared, double signedDistance) implements AxisData {}
