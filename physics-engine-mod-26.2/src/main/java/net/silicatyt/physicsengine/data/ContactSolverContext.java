package net.silicatyt.physicsengine.data;

import org.joml.Matrix3dc;
import org.joml.Vector3dc;

public record ContactSolverContext(
        double targetClosingVelocity,
        double biasVelocity,
        Vector3dc relativeContactPosA,
        Vector3dc relativeContactPosB,
        Matrix3dc RA, // relativeContactPosA x orthonormalBasis
        Matrix3dc RB,
        Matrix3dc angularImpulseFactorA, // inverseInertiaTensorWorld_A * RA
        Matrix3dc angularImpulseFactorB,
        EffectiveMass effectiveMass
){}
