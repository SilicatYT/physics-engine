package net.silicatyt.physicsengine.data;

import net.silicatyt.physicsengine.entity.PhysicsObject;

import java.util.List;

public record Island(List<Manifold> manifolds, List<PhysicsObject> dynamicObjects, List<PhysicsObject> staticObjects) {}
