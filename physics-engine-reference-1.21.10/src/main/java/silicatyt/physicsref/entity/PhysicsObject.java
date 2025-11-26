package silicatyt.physicsref.entity;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.*;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

// TODO: Important notes or things to rethink at some point
// Note: All doubles could be NaN, Infinity or -Infinity, but I don't check for that everywhere. I don't have a consequent rule for that. I could check for isFinite() on the vectors and quaternions (on quaternions I could additionally check if it's all 0; I can't normalize that), but there's not really a point because this isn't meant to be an official well-made mod. Just a reference.
// Note: addXYZ and setXYZ also update dependent data like the rotationMatrices or the inverseInertiaTensors automatically. Calling add and set repeatedly therefore causes unnecessary work for the CPU. But it's most readable like this, so I won't bother fixing it for now. This is just a reference mod after all, not meant to be optimized. BUT a potential idea would be: Mark rotationMatrix and stuff as "dirty", and in "getRotationMatrix" accesses I check if it's dirty. If yes, update.
// Note: In Integration, I have this: "obj.addLinearVelocity(obj.getAccumulatedForce().mul(obj.getInverseMass()));". The "getAccumulatedForce()" creates a new object. Is there a good way around that? Probably not without giving direct access, which would probably be bad.
// Note: I made the decision to only write a new getter or setter method if I need it. Not write all of them at once for "consistency". That would create bloat and waste too much time. I can't know when I need a Vec3d as an input, or when I need a Vector3d or even 3 individual doubles.
// Note: Currently, all getters return a new object in order to avoid giving access to the object directly (which would circumvent restrictions when setting the contents, like negative scale). And all setters keep the same object reference and do not re-assign. Additionally, that's the reason I use ".set()" instead of "=" for assignments.

public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    public static final double DEFAULT_INVERSE_MASS = 0.001d;
    public static final Vector3d DEFAULT_SCALE = new Vector3d(1d, 1d, 1d);
    public static final double DEFAULT_FRICTION_COEFFICIENT = 0.5d;
    public static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3d;
    public static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(Items.STONE);

    // Stored data
    private double inverseMass; // In kilograms
    private Vector3d linearVelocity;
    private Vector3d angularVelocity;
    private Quaterniond orientation;
    private Vector3d scale;
    private double frictionCoefficient;
    private double restitutionCoefficient;

    // Transient data
    private Vector3d pos; // Just for easy and consistent access
    private Vec3d lastEntityPos;
    private Matrix3d inverseInertiaTensorLocal;
    private Matrix3d inverseInertiaTensorWorld;
    private Matrix3d rotationMatrix;
    private Matrix3d rotationMatrixTranspose; // To avoid duplicate calculations when calling ".transpose()"
    private Vector3d accumulatedForce;
    private Vector3d accumulatedTorque;

    // Constructor
    // TODO: Add better constructors
    public PhysicsObject(EntityType<?> type, World world) {
        super(type, world);

        // Set default values
        this.inverseMass = DEFAULT_INVERSE_MASS;
        this.linearVelocity = new Vector3d();
        this.angularVelocity = new Vector3d();
        this.orientation = new Quaterniond();
        this.scale = DEFAULT_SCALE;
        this.frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
        this.restitutionCoefficient = DEFAULT_RESTITUTION_COEFFICIENT;

        this.initializeObjectData();
        this.updateTransientObjectData();
    }

    // Getters & Setters
    public Vec3d getLastEntityPos() {
        return this.lastEntityPos;
    }

    public void setInternalPos(Vec3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.pos.x = value.x;
        this.pos.y = value.y;
        this.pos.z = value.z;
    }

    public void addInternalPos(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.pos.add(value);
    }

    public double getInverseMass() {
        return this.inverseMass;
    }

    public void setInverseMass(double value) throws IllegalArgumentException {
        if (value < 0) {
            throw new IllegalArgumentException("Mass must not be negative");
        }
        this.inverseMass = value;
        this.updateInertiaTensors();
    }

    public Vector3d getLinearVelocity() {
        return new Vector3d(this.linearVelocity);
    }

    public void setLinearVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.linearVelocity.set(value);
    }

    public void addLinearVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.linearVelocity.add(value);
    }

    public void scaleLinearVelocity(double scale) {
        this.linearVelocity.mul(scale);
    }

    public Vector3d getAngularVelocity() {
        return new Vector3d(this.angularVelocity);
    }

    public void setAngularVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.angularVelocity.set(value);
    }

    public void addAngularVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.angularVelocity.add(value);
    }

    public void scaleAngularVelocity(double scale) {
        this.angularVelocity.mul(scale);
    }

    public void setOrientation(Quaterniond value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Quaternion must not be null");
        }
        this.orientation.set(value);
        this.orientation.normalize();

        this.updateRotationMatrix();
        this.updateInertiaTensorWorld(); // Requires the updated rotation matrix
    }

    public Quaterniond getOrientation() {
        return new Quaterniond(this.orientation);
    }

    public Vector3d getScale() {
        return new Vector3d(this.scale);
    }

    public void setScale(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        if (value.x < 0 || value.y < 0 || value.z < 0) {
            throw new IllegalArgumentException("Scale must not be negative");
        }
        this.scale.set(value);
        this.updateInertiaTensors();
    }

    public double getFrictionCoefficient() {
        return this.frictionCoefficient;
    }

    public void setFrictionCoefficient(double value) throws IllegalArgumentException {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Friction coefficient must be between 0 and 1");
        }
        this.frictionCoefficient = value;
    }

    public double getRestitutionCoefficient() {
        return this.restitutionCoefficient;
    }

    public void setRestitutionCoefficient(double value) throws IllegalArgumentException {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1");
        }
        this.restitutionCoefficient = value;
    }

    public Matrix3d getInverseInertiaTensorLocal() {
        return new Matrix3d(this.inverseInertiaTensorLocal); // TODO: REMOVE
    }

    public Matrix3d getInverseInertiaTensorWorld() {
        return new Matrix3d(this.inverseInertiaTensorWorld);
    }

    public void resetAccumulatedForce() {
        this.accumulatedForce.zero();
    }

    public Vector3d getAccumulatedForce() {
        return new Vector3d(this.accumulatedForce);
    }

    public void resetAccumulatedTorque() {
        this.accumulatedTorque.zero();
    }

    public Vector3d getAccumulatedTorque() {
        return new Vector3d(this.accumulatedTorque);
    }

    // NBT saving & loading
    @Override
    protected void writeCustomData(WriteView view) { // I can't store doubles properly, so I have to convert to float here
        // super.writeCustomData(view); // <- Completely ignore the item display's NBT: I'll take over from here, thanks. I can't read the orientation and stuff anyway (requiring me to keep track of them myself), so I might as well. Makes the data prettier if useless data isn't in there.
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        if (!this.getItemStack().isEmpty()) { // ItemStack.Codec doesn't work for empty items
            view.put("item", ItemStack.CODEC, this.getItemStack());
        }
        view.putDouble("inverse_mass", inverseMass);
        view.put("linear_velocity", doubleListCodec, List.of(linearVelocity.x, linearVelocity.y, linearVelocity.z));
        view.put("angular_velocity", doubleListCodec, List.of(angularVelocity.x, angularVelocity.y, angularVelocity.z));
        view.put("orientation", doubleListCodec, List.of(orientation.x, orientation.y, orientation.z, orientation.w));
        view.put("scale", doubleListCodec, List.of(scale.x, scale.y, scale.z));
        view.putDouble("friction_coefficient", frictionCoefficient);
        view.putDouble("restitution_coefficient", restitutionCoefficient);
    }

    @Override
    protected void readCustomData(ReadView view) { // ALL required instance variables need to be set after this, so I need to also calculate the rotationMatrix from the orientation for example. Otherwise it will be null after unloading and loading again, causing all sorts of calculation bugs.
        // super.readCustomData(view);
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        this.initializeObjectData();

        this.setItemStack(view.read("item", ItemStack.CODEC).orElse(DEFAULT_ITEM_STACK));
        this.setInverseMass(view.read("inverse_mass", Codec.DOUBLE).orElse(DEFAULT_INVERSE_MASS));
        List<Double> linearVelocity = view.read("linear_velocity", doubleListCodec).orElse(List.of(0d, 0d, 0d));
        this.setLinearVelocity(new Vector3d(linearVelocity.get(0), linearVelocity.get(1), linearVelocity.get(2)));
        List<Double> angularVelocity = view.read("angular_velocity", doubleListCodec).orElse(List.of(0d, 0d, 0d));
        this.setAngularVelocity(new Vector3d(angularVelocity.get(0), angularVelocity.get(1), angularVelocity.get(2)));
        List<Double> orientation = view.read("orientation", doubleListCodec).orElse(List.of(0d, 0d, 0d, 1d));
        this.setOrientation(new Quaterniond(orientation.get(0), orientation.get(1), orientation.get(2), orientation.get(3)));
        List<Double> scale = view.read("scale", doubleListCodec).orElse(List.of(DEFAULT_SCALE.x, DEFAULT_SCALE.y, DEFAULT_SCALE.z));
        this.setScale(new Vector3d(scale.get(0), scale.get(1), scale.get(2)));
        this.setFrictionCoefficient(view.read("friction_coefficient", Codec.DOUBLE).orElse(DEFAULT_FRICTION_COEFFICIENT));
        this.setRestitutionCoefficient(view.read("restitution_coefficient", Codec.DOUBLE).orElse(DEFAULT_RESTITUTION_COEFFICIENT));

        this.updateTransientObjectData();
        this.updateVisuals();
    }

    // Normal methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    private void initializeObjectData() { // Makes sure that all instance variables are initialized (Not NULL) and set other runtime data upon getting loaded or instantiated
        this.setInterpolationDuration(1);
        this.setTeleportDuration(1);

        this.lastEntityPos = this.getEntityPos();
        this.pos = new Vector3d(this.lastEntityPos.x, this.lastEntityPos.y, this.lastEntityPos.z);

        this.rotationMatrix = new Matrix3d();
        this.rotationMatrixTranspose = new Matrix3d();
        this.inverseInertiaTensorLocal = new Matrix3d();
        this.inverseInertiaTensorWorld = new Matrix3d();
        this.accumulatedForce = new Vector3d();
        this.accumulatedTorque = new Vector3d();
    }

    private void updateTransientObjectData() { // Requires rotationMatrix and inverseInertiaTensorLocal to be initialized. Was originally part of initializeObjectData()
        this.updateRotationMatrix();
        this.updateInertiaTensors();
        this.updateVisuals();
    }

    public void updateVisuals() {
        this.setStartInterpolation(0);
        this.setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(this.orientation), new Vector3f(this.scale), new Quaternionf())); // Although I calculate everything in doubles, the rendering uses floats because that's how item displays (and Minecraft's rendering in general) work
        this.setPos(this.pos.x, this.pos.y, this.pos.z); // The vanilla method for entity position
    }

    private void updateRotationMatrix() {
        this.orientation.get(this.rotationMatrix);
        this.rotationMatrix.transpose(this.rotationMatrixTranspose); // transpose() with no parameters modifies the matrix, which is why I could first copy the rotationMatrix and then transpose the copy, or specify the target in the transpose method directly as I do here
    }

    private void updateInertiaTensorWorld() {
        this.rotationMatrix.mul(this.inverseInertiaTensorLocal, this.inverseInertiaTensorWorld).mul(this.rotationMatrixTranspose, this.inverseInertiaTensorWorld); // Uses the return value of "mul". Very important that it doesn't overwrite inverseInertiaTensorLocal or the rotationMatrix! If I do "x.mul(y)" without specifying a destination, x is overwritten.
    }

    private void updateInertiaTensors() {
        this.inverseInertiaTensorLocal.m00 = (12 * this.inverseMass) / (this.scale.y * this.scale.y + this.scale.z * this.scale.z);
        this.inverseInertiaTensorLocal.m11 = (12 * this.inverseMass) / (this.scale.x * this.scale.x + this.scale.z * this.scale.z);
        this.inverseInertiaTensorLocal.m22 = (12 * this.inverseMass) / (this.scale.x * this.scale.x + this.scale.y * this.scale.y);

        this.updateInertiaTensorWorld();
    }

}
