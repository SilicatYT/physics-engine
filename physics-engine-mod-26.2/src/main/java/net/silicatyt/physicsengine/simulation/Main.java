package net.silicatyt.physicsengine.simulation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.silicatyt.physicsengine.PhysicsEngine;
import net.silicatyt.physicsengine.entity.PhysicsObject;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static net.silicatyt.physicsengine.PhysicsEngine.LOADED_PHYSICS_OBJECTS;

public final class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;

    public static void tick(MinecraftServer server) {
        ServerTickRateManager tickRateManager = server.tickRateManager();
        if (tickRateManager.isFrozen() && !tickRateManager.isSteppingForward()) return;

        List<PhysicsObject> loadedObjects = List.copyOf(LOADED_PHYSICS_OBJECTS); // So I don't modify LOADED_PHYSICS_OBJECTS in integration (entities could unload) while I iterate over it

        loadedObjects.forEach(Integrator::phaseOne);
        loadedObjects.forEach(Integrator::phaseTwo);
    }
}
