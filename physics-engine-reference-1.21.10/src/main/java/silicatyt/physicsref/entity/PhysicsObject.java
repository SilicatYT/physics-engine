package silicatyt.physicsref.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;


public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    private float objectAge;

    // Constructor
    public PhysicsObject(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    public void test() { // NOTE: I can't get the item display's transformation data for some reason, so maybe I'll have to keep track of it myself using instance variables. Setting the data works fine though
        Vector3f translation = new Vector3f(0f, 0f, 0f);
        Quaternionf leftRotation = new Quaternionf();   // identity rotation
        Quaternionf rightRotation = new Quaternionf();  // identity rotation
        Vector3f scale = new Vector3f(1f, 2f, 1f);      // your desired scale

        // Apply the new transformation
        this.setTransformation(new AffineTransformation(translation, leftRotation, scale, rightRotation));

        this.setItemStack(new ItemStack(Items.OAK_LOG));
    }

}
