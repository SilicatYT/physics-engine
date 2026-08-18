package net.silicatyt.physicsengine;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;

import net.silicatyt.physicsengine.entity.ModEntityTypes;
import net.silicatyt.physicsengine.entity.PhysicsObject;
import net.silicatyt.physicsengine.simulation.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;

import java.util.HashSet;
import java.util.Set;


public final class PhysicsEngine implements ModInitializer {
	public static final String MOD_ID = "physicsengine";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Set<PhysicsObject> LOADED_PHYSICS_OBJECTS = new HashSet<>();

	@Override
	public void onInitialize() {
		ModEntityTypes.registerModEntityTypes();
		PolymerEntityUtils.registerType(ModEntityTypes.PHYSICS_OBJECT); // Mark entities as server-side-only

		ServerTickEvents.START_SERVER_TICK.register(Main::tick);

		ServerEntityEvents.ENTITY_LOAD.register((entity, _) -> {
			if (entity instanceof PhysicsObject obj) {
				LOADED_PHYSICS_OBJECTS.add(obj);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, _) -> {
			if (entity instanceof PhysicsObject obj) {
				LOADED_PHYSICS_OBJECTS.remove(obj);
			}
		});

		ServerLifecycleEvents.SERVER_STARTING.register(_ -> Main.createPhysicsPool());

		ServerLifecycleEvents.SERVER_STOPPING.register(_ -> {
			LOADED_PHYSICS_OBJECTS.clear();
			Main.shutdownPhysicsPool();
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
