package silicatyt.physicsref.entity;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import org.joml.*;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;



public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    private double inverseMass;
    private Vector3d pos;
    private Vector3f linearVelocity;
    private Vector3f angularVelocity;
    private Quaternionf orientation; // Only quaternionf because that's what Minecraft uses
    private Matrix3f inverseInertiaTensorLocal;
    private Matrix3f inverseInertiaTensorGlobal;
    private Matrix3f rotationMatrix;
    private Matrix3f rotationMatrixTranspose;
    private Vector3f accumulatedForce;
    private Vector3f accumulatedTorque;
    private Vector3f size;
    private float frictionCoefficient;
    private float restitutionCoefficient;

    // Constructor
    public PhysicsObject(EntityType<?> type, World world) {
        super(type, world);
        // TEMPORARY (Need to initialize the values properly). This just needs to exist for now to prevent a crash upon saving (NullPointerException)
        this.pos = new Vector3d();
        this.inverseMass = 0d;
        this.linearVelocity = new Vector3f();
        this.angularVelocity = new Vector3f();
        this.orientation = new Quaternionf();
        this.size = new Vector3f();
        this.frictionCoefficient = 0f;
        this.restitutionCoefficient = 0f;

    }
    // TODO: Add better constructors

    // NBT saving & loading
    @Override
    protected void writeCustomData(WriteView view) {
        // super.writeCustomData(view); // <- Completely ignore the item display's NBT: I'll take over from here, thanks. I can't read the orientation and stuff anyway (requiring me to keep track of them myself), so I might as well. Makes the data prettier if useless data isn't in there.
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        if (!this.getItemStack().isEmpty()) { // ItemStack.Codec doesn't work for empty items
            view.put("item", ItemStack.CODEC, this.getItemStack());
        }
        view.putDouble("inverse_mass", inverseMass);
        view.put("pos", doubleListCodec, List.of(pos.x, pos.y, pos.z));
        view.put("linear_velocity", Codecs.VECTOR_3F, linearVelocity);
        view.put("angular_velocity", Codecs.VECTOR_3F, angularVelocity);
        view.put("orientation", Codecs.QUATERNION_F, orientation);
        view.put("size", Codecs.VECTOR_3F, size);
        view.putFloat("friction_coefficient", frictionCoefficient);
        view.putFloat("restitution_coefficient", restitutionCoefficient);
    }

    @Override
    protected void readCustomData(ReadView view) { // ALL required instance variables need to be set after this, so I need to also calculate the rotationMatrix from the orientation for example. Otherwise it will be null after unloading and loading again, causing all sorts of calculation bugs.
        // super.readCustomData(view);
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        this.setItemStack(view.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        List<Double> posList = view.read("pos", doubleListCodec).orElse(List.of(0d, 0d, 0d));
        this.pos = new Vector3d(posList.get(0), posList.get(1), posList.get(2));
        this.linearVelocity = view.read("linear_velocity", Codecs.VECTOR_3F).orElse(new Vector3f());
        this.angularVelocity = view.read("angular_velocity", Codecs.VECTOR_3F).orElse(new Vector3f());
        this.orientation = view.read("orientation", Codecs.QUATERNION_F).orElse(new Quaternionf());
        this.size = view.read("size", Codecs.VECTOR_3F).orElse(new Vector3f());
        this.frictionCoefficient = view.read("friction_coefficient", Codec.FLOAT).orElse(0f);
        this.restitutionCoefficient = view.read("restitution_coefficient", Codec.FLOAT).orElse(0f);

        // TODO: Set the item display's transformation
        // TODO: Maybe I also need to teleport the entity to "pos" because it never got read? Although... how can the entity get loaded if its position is unknown lol. Not sure
        // TODO: Run "updateOrientation" or whatever to set the inverseInertiaTensorLocal/Global and the rotationMatrix/Transpose variables. updateOrientation() would calculate these values and also apply the left_rotation transformation
        // TODO: Add updateSize() that calculates the inertia tensors and applies the scale transformation
        // TODO: Pos, UUID, PortalCooldown and some other NBT fields are automatically saved and read. So *maybe* don't store "pos" separately? Gets confusing otherwise
    }

    // Normal methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    // TODO: Add more public methods for manipulating the physics object
}
