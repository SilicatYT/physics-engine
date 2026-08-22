package net.silicatyt.physicsengine.data;

public record ResolvingContact(ContactPoint point, Manifold manifold, ManifoldSolverContext manifoldContext, ContactSolverContext contactContext, ContactSolverState state) {}
