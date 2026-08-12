package net.silicatyt.physicsengine.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

public class PhysicsObject extends Display.ItemDisplay implements PolymerEntity {
    public PhysicsObject(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // Make PhysicsObject appear as an item display entity
        return EntityTypes.ITEM_DISPLAY;
    }
}
