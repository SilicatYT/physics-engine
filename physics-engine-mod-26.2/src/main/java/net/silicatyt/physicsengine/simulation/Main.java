package net.silicatyt.physicsengine.simulation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.silicatyt.physicsengine.PhysicsEngine;
import net.silicatyt.physicsengine.data.Island;
import net.silicatyt.physicsengine.entity.PhysicsObject;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static net.silicatyt.physicsengine.PhysicsEngine.LOADED_PHYSICS_OBJECTS;

public final class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;
    private static ForkJoinPool PHYSICS_POOL;
    public static boolean DISABLE_LAZY_UPDATES = false; // Lazy updates for PhysicsObject getters should not occur during asynchronous code that runs code for the same object (data races)

    public static void tick(MinecraftServer server) {
        ServerTickRateManager tickRateManager = server.tickRateManager();
        if (tickRateManager.isFrozen() && !tickRateManager.isSteppingForward()) return;

        List<PhysicsObject> loadedObjects = List.copyOf(LOADED_PHYSICS_OBJECTS); // So I don't modify LOADED_PHYSICS_OBJECTS in integration (entities could unload) while I iterate over it

        PHYSICS_POOL.submit(
                () -> loadedObjects.parallelStream().forEach(obj -> {
                    Integrator.phaseOne(obj);
                    obj.updateDerivedValues(); // Necessary before the asynchronous part in the collision detection runs. No values change at that point, but several of the same type of getter run in parallel, potentially for the same object. The updates would cause data races
                })
        ).join();

        DISABLE_LAZY_UPDATES = true;
        List<Island> islands = CollisionDetector.findIslands(loadedObjects, PHYSICS_POOL);
        DISABLE_LAZY_UPDATES = false;

        PHYSICS_POOL.submit(
                () -> islands.parallelStream().forEach(CollisionResolver::resolve)
        ).join();

        PHYSICS_POOL.submit(
                () -> loadedObjects.parallelStream().forEach(Integrator::phaseTwo)
        ).join();
        for (PhysicsObject obj : loadedObjects) {
            obj.updateTransformation(); // Assumes it's run on the server thread, so I took it out of phaseTwo to be safe
            obj.updateEntityPos(); // Can't be part of phaseTwo because it can't run parallel. Crossing chunk borders affects a data structure that contains other entities.
        }
    }

    public static void createPhysicsPool() {
        if (PHYSICS_POOL == null || PHYSICS_POOL.isShutdown()) {
            PHYSICS_POOL = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)); // TODO: Double-check this value
        }
    }
    public static void shutdownPhysicsPool() { // AI-generated
        if (PHYSICS_POOL == null) return;
        PHYSICS_POOL.shutdown();
        try {
            if (!PHYSICS_POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                PhysicsEngine.LOGGER.warn("Physics pool didn't terminate in time, forcing shutdown");
                PHYSICS_POOL.shutdownNow();
                if (!PHYSICS_POOL.awaitTermination(2, TimeUnit.SECONDS)) {
                    PhysicsEngine.LOGGER.error("Physics pool still hasn't terminated after shutdownNow()");
                }
            }
        } catch (InterruptedException e) {
            PHYSICS_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
