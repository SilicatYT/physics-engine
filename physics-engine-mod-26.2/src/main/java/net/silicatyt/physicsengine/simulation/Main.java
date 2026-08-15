package net.silicatyt.physicsengine.simulation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.silicatyt.physicsengine.entity.PhysicsObject;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static net.silicatyt.physicsengine.PhysicsEngine.LOADED_PHYSICS_OBJECTS;

public final class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;
    private static final ForkJoinPool PHYSICS_POOL = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)); // TODO: Double-check this value

    public static void tick(MinecraftServer server) {
        ServerTickRateManager tickRateManager = server.tickRateManager();
        if (tickRateManager.isFrozen() && !tickRateManager.isSteppingForward()) return;

        List<PhysicsObject> loadedObjects = List.copyOf(LOADED_PHYSICS_OBJECTS); // So I don't modify LOADED_PHYSICS_OBJECTS in integration (entities could unload) while I iterate over it

        PHYSICS_POOL.submit(
                () -> loadedObjects.parallelStream().forEach(Integrator::phaseOne)
        ).join();

        PHYSICS_POOL.submit(
                () -> loadedObjects.parallelStream().forEach(Integrator::phaseTwo)
        ).join();
        for (PhysicsObject obj : loadedObjects) obj.updateEntityPos(); // Can't be part of phaseTwo because it can't run parallel. Crossing chunk borders affects a data structure that contains other entities.
    }
}
