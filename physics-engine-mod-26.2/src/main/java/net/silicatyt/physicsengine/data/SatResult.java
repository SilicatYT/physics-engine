package net.silicatyt.physicsengine.data;

import java.util.Optional;

public record SatResult(AxisData candidateAxisData, Optional<PersistedAxisData> persistedAxisData) {}
