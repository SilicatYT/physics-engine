package silicatyt.physics;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silicatyt.physics.entity.ModEntities;
import silicatyt.physics.entity.PhysicsObject;
import silicatyt.physics.simulation.Main;


import java.util.HashSet;
import java.util.Set;

public class Physics implements ModInitializer {
	public static final String MOD_ID = "physics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Set<PhysicsObject> LOADED_PHYSICS_OBJECTS = new HashSet<>();


	@Override
	public void onInitialize() {
        // Register things
        ServerTickEvents.START_SERVER_TICK.register(Main::physicsTick);
        ModEntities.registerModEntities();
        PolymerEntityUtils.registerType(ModEntities.PHYSICS_OBJECT); // Mark entity as server-side only

        // Keep track of loaded physics objects
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PhysicsObject obj) {
                LOADED_PHYSICS_OBJECTS.add(obj);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PhysicsObject obj) {
                LOADED_PHYSICS_OBJECTS.remove(obj);
            }
        });
    }

}