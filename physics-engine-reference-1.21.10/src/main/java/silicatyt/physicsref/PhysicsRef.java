package silicatyt.physicsref;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silicatyt.physicsref.entity.ModEntities;
import silicatyt.physicsref.entity.PhysicsObject;
import silicatyt.physicsref.simulation.CollisionDetection;
import silicatyt.physicsref.simulation.ContactResolution;
import silicatyt.physicsref.simulation.Integration;

import java.util.HashSet;

public class PhysicsRef implements ModInitializer {
	public static final String MOD_ID = "physicsref";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final HashSet<PhysicsObject> loadedPhysicsObjects = new HashSet<>();


	@Override
	public void onInitialize() {
        // Register things
        ServerTickEvents.START_SERVER_TICK.register(this::physicsTick);
        ModEntities.registerModEntities();
        PolymerEntityUtils.registerType(ModEntities.PHYSICS_OBJECT); // Mark Physics Object Entity as server-side only

        // Keep track of loaded physics objects
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PhysicsObject physicsObject) {
                loadedPhysicsObjects.add(physicsObject);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PhysicsObject physicsObject) {
                loadedPhysicsObjects.remove(physicsObject);
            }
        });
    }


    private void physicsTick(MinecraftServer server) {
        Integration.phaseOne();
        CollisionDetection.start(); // TODO: Runs ContactGeneration later. Or should I split it up?
        ContactResolution.resolve();
        Integration.phaseTwo();
    }

}