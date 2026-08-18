package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;

public record Manifold(PhysicsObject a, PhysicsObject b, ContactState state, int persistedAxisIndex, boolean persistedAxisFacingOutward, boolean persistedAxisFacingB) {}
