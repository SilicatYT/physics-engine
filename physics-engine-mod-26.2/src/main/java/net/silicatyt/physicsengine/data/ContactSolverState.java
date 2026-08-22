package net.silicatyt.physicsengine.data;

import org.joml.Vector3d;

public final class ContactSolverState {
    public Vector3d accumulatedImpulseContactSpace = new Vector3d(); // Converted from/to world coordinates at the start/end of resolution
    public double accumulatedSplitImpulse = 0.0;
}
