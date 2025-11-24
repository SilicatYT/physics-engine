package silicatyt.physicsref.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import silicatyt.physicsref.PhysicsRef;

public class ModEntities {

    // Add Physics Object Entity
    public static final EntityType<PhysicsObject> PHYSICS_OBJECT = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(PhysicsRef.MOD_ID, "physics_object"),
            EntityType.Builder.create(PhysicsObject::new, SpawnGroup.MISC)
                    .dimensions(0f, 0f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(PhysicsRef.MOD_ID, "physics_object"))));

    public static void registerModEntities() {
        PhysicsRef.LOGGER.info("Registering mod entities for " + PhysicsRef.MOD_ID);
    }
}
