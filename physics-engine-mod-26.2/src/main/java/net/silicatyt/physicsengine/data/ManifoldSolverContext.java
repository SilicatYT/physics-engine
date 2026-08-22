package net.silicatyt.physicsengine.data;

import org.joml.Matrix3dc;

public record ManifoldSolverContext(Matrix3dc orthonormalBasis, double frictionCoefficient, double restitutionCoefficient) {}
