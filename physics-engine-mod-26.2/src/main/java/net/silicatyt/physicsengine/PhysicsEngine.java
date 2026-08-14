package net.silicatyt.physicsengine;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;

import net.silicatyt.physicsengine.entity.ModEntityTypes;
import net.silicatyt.physicsengine.simulation.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;


public final class PhysicsEngine implements ModInitializer {
	public static final String MOD_ID = "physicsengine";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerTickEvents.START_SERVER_TICK.register(Main::tick);
		ModEntityTypes.registerModEntityTypes();
		PolymerEntityUtils.registerType(ModEntityTypes.PHYSICS_OBJECT); // Mark entities as server-side-only
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
