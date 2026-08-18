package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public record CandidateAxisData(int index, double overlap, Vector3dc axis) implements AxisData {}
