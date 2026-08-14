package net.silicatyt.physicsengine.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.silicatyt.physicsengine.PhysicsEngine;

import static net.silicatyt.physicsengine.PhysicsEngine.id;

public final class ModEntityTypes {
    public static final EntityType<PhysicsObject> PHYSICS_OBJECT = register(
            "physics_object",
            EntityType.Builder.of(PhysicsObject::new, MobCategory.MISC)
                    .sized(0f, 0f)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntityTypes() {
        PhysicsEngine.LOGGER.info("Registering EntityTypes for " + PhysicsEngine.MOD_ID);
    }
}