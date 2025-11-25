package silicatyt.physicsref.entity;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.*;
import xyz.nucleoid.packettweaker.PacketContext;


// Note: All doubles could be NaN, Infinity or -Infinity, but I don't check for that everywhere. I don't have a consequent rule for that. I could check for isFinite() on the vectors and quaternions (on quaternions I could additionally check if it's all 0; I can't normalize that), but there's not really a point because this isn't meant to be an official well-made mod. Just a reference.

public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    public static final double DEFAULT_INVERSE_MASS = 0.001d;
    public static final Vector3d DEFAULT_SIZE = new Vector3d(1d, 1d, 1d);
    public static final double DEFAULT_FRICTION_COEFFICIENT = 0.5d;
    public static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3d;

    // Stored data
    private double inverseMass; // In kilograms
    private Vector3d linearVelocity;
    private Vector3d angularVelocity;
    private Quaterniond orientation;
    private Vector3d size;
    private double frictionCoefficient;
    private double restitutionCoefficient;

    // Transient data
    private Vector3d pos; // Just for easy and consistent access
    private Matrix3d inverseInertiaTensorLocal;
    private Matrix3d inverseInertiaTensorGlobal;
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
        this.size = DEFAULT_SIZE;
        this.frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
        this.restitutionCoefficient = DEFAULT_RESTITUTION_COEFFICIENT;

        this.updateAllTransientData();
    }

    // Getters & Setters
    public double getInverseMass() {
        return this.inverseMass;
    }

    public double getMass() {
        return 1 / this.inverseMass;
    }

    public void setInverseMass(double value) throws IllegalArgumentException {
        if (value < 0) {
            throw new IllegalArgumentException("Mass must not be negative");
        }
        this.inverseMass = value;
        this.updateInverseInertiaTensors();
    }

    public void setMass(double value) throws IllegalArgumentException {
        this.setInverseMass(1 / value);
    }

    public Vector3d getLinearVelocity() {
        return this.linearVelocity;
    }

    public void setLinearVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.linearVelocity = value;
    }

    public Vector3d getAngularVelocity() {
        return this.angularVelocity;
    }

    public void setAngularVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.angularVelocity = value;
    }

    public Quaterniond getOrientation() {
        return this.orientation;
    }

    public void setOrientation(Quaterniond value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Quaternion must not be null");
        }
        this.orientation = value.normalize();

        this.updateRotationMatrix();
        this.updateInverseInertiaTensorWorld(); // Requires the updated rotation matrix
        this.updateVisualTransformation();
    }

    public Vector3d getSize() {
        return this.size;
    }

    public void setSize(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        if (value.x < 0 || value.y < 0 || value.z < 0) {
            throw new IllegalArgumentException("Size must not be negative");
        }
        this.size = value;

        this.updateInverseInertiaTensors();
        this.updateVisualTransformation();
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

    // NBT saving & loading
    @Override
    protected void writeCustomData(WriteView view) { // I can't store doubles properly, so I have to convert to float here
        // super.writeCustomData(view); // <- Completely ignore the item display's NBT: I'll take over from here, thanks. I can't read the orientation and stuff anyway (requiring me to keep track of them myself), so I might as well. Makes the data prettier if useless data isn't in there.

        if (!this.getItemStack().isEmpty()) { // ItemStack.Codec doesn't work for empty items
            view.put("item", ItemStack.CODEC, this.getItemStack());
        }
        view.putDouble("inverse_mass", inverseMass);
        view.put("linear_velocity", Codecs.VECTOR_3F, new Vector3f(linearVelocity));
        view.put("angular_velocity", Codecs.VECTOR_3F, new Vector3f(angularVelocity));
        view.put("orientation", Codecs.QUATERNION_F, new Quaternionf(orientation));
        view.put("size", Codecs.VECTOR_3F, new Vector3f(size)); // size and the velocities are stored in floats (because there is no VECTOR_3D codec), but is a double at runtime to avoid excessive casting
        view.putDouble("friction_coefficient", frictionCoefficient);
        view.putDouble("restitution_coefficient", restitutionCoefficient);
    }

    @Override
    protected void readCustomData(ReadView view) { // ALL required instance variables need to be set after this, so I need to also calculate the rotationMatrix from the orientation for example. Otherwise it will be null after unloading and loading again, causing all sorts of calculation bugs.
        // super.readCustomData(view);

        this.setItemStack(view.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.inverseMass = view.read("inverse_mass", Codec.DOUBLE).orElse(DEFAULT_INVERSE_MASS);
        this.linearVelocity = new Vector3d(view.read("linear_velocity", Codecs.VECTOR_3F).orElse(new Vector3f()));
        this.angularVelocity = new Vector3d(view.read("angular_velocity", Codecs.VECTOR_3F).orElse(new Vector3f()));
        this.orientation = new Quaterniond(view.read("orientation", Codecs.QUATERNION_F).orElse(new Quaternionf()));
        this.size = new Vector3d(view.read("size", Codecs.VECTOR_3F).orElse(new Vector3f()));
        this.frictionCoefficient = view.read("friction_coefficient", Codec.DOUBLE).orElse(DEFAULT_FRICTION_COEFFICIENT);
        this.restitutionCoefficient = view.read("restitution_coefficient", Codec.DOUBLE).orElse(DEFAULT_RESTITUTION_COEFFICIENT);

        this.updateAllTransientData();
        this.updateVisualTransformation();
    }

    // Normal methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    private void updateAllTransientData() { // Makes sure that no instance variables are unset after instantiation or getting loaded from disk. Some data is updated twice unfortunately.
        Vec3d entityPos = this.getEntityPos();
        this.pos = new Vector3d(entityPos.x, entityPos.y, entityPos.z);

        if (this.accumulatedForce == null) {
            this.accumulatedForce = new Vector3d();
        }
        if (this.accumulatedTorque == null) {
            this.accumulatedTorque = new Vector3d();
        }

        this.updateRotationMatrix();
        this.updateInverseInertiaTensors();
    }

    private void updateVisualTransformation() {
        this.setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(this.orientation), new Vector3f(this.size), new Quaternionf()));
    }

    private void updateRotationMatrix() {
        if (this.rotationMatrix == null) { // I don't want to have a dedicated updateRotationMatrix method and then still have to initialize it in the "object got loaded" or "new object got created" methods. Maybe it's unoptimized, but it's more readable imo
            this.rotationMatrix = new Matrix3d();
        }
        this.orientation.get(this.rotationMatrix);
        this.rotationMatrixTranspose = this.rotationMatrix.transpose();
    }

    private void updateInverseInertiaTensorWorld() {
        this.inverseInertiaTensorGlobal = this.inverseInertiaTensorLocal.mul(this.rotationMatrix);
    }

    private void updateInverseInertiaTensors() {
        if (this.inverseInertiaTensorLocal == null) { // I don't want to have a dedicated updateInverseInertiaTensors method and then still have to initialize it in the "object got loaded" or "new object got created" methods. Maybe it's unoptimized, but it's more readable imo
            this.inverseInertiaTensorLocal = new Matrix3d();
        }
        this.inverseInertiaTensorLocal.m00 = (12 * this.inverseMass) / (this.size.y * this.size.y + this.size.z * this.size.z);
        this.inverseInertiaTensorLocal.m11 = (12 * this.inverseMass) / (this.size.x * this.size.x + this.size.z * this.size.z);
        this.inverseInertiaTensorLocal.m22 = (12 * this.inverseMass) / (this.size.x * this.size.x + this.size.y * this.size.y);

        this.updateInverseInertiaTensorWorld();
    }

    // TODO: Add more public methods for manipulating the physics object
}
