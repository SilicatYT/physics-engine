package silicatyt.physicsref;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silicatyt.physicsref.entity.PhysicsObject;
import silicatyt.physicsref.entity.ModEntities;

public class PhysicsRef implements ModInitializer {
	public static final String MOD_ID = "physicsref";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
        ServerTickEvents.START_SERVER_TICK.register(this::physicsTick);
        ModEntities.registerModEntities();
        PolymerEntityUtils.registerType(ModEntities.PHYSICS_OBJECT); // Mark Physics Object Entity as server-side only


    }


    private void physicsTick(MinecraftServer server) {

        ServerWorld world = server.getOverworld();
        PhysicsObject obj = new PhysicsObject(ModEntities.PHYSICS_OBJECT, world); // NOTE:
        obj.updatePosition(0, 100, 0);
        world.spawnEntity(obj);

    }

}