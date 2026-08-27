package net.silicatyt.physicsengine.data;

import java.util.Optional;

public record SatResult(CandidateAxisData candidateAxisData,
                        Optional<PersistedAxisData> persistedAxisData,
                        Optional<Manifold> lastTickManifold,
                        double[][] axisDot,
                        double[] offsetInA,
                        double[] offsetInB
) {}
