package net.silicatyt.physicsengine.data;

import java.util.List;

public record Island(List<Manifold> manifolds) {}
// TODO: For sleeping, add a list of all physicsObjects of that island? Or keep a global list of all physicsObjects total? What's better for parallelization?