package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public record ContactSolverContext(double targetClosingVelocity, double biasVelocity, Vector3dc effectiveMass) {} // TODO: Add new values like a pre-calculated value for contactVelocity (r x axis)
