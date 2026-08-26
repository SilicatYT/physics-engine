package net.silicatyt.physicsengine.data;

public record EffectiveMass(double normal, double inverseK11, double inverseK12, double inverseK22) {} // 2x2 matrix for tangents, plus single normal component. Matrix is symmetrical, so no K21 necessary.
