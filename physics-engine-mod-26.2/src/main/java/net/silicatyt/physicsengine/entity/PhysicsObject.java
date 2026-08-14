package net.silicatyt.physicsengine.entity;

import com.mojang.math.Transformation;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.silicatyt.physicsengine.versioning.VersionNode;
import net.silicatyt.physicsengine.versioning.VersionSource;
import org.joml.*;
import org.jspecify.annotations.NonNull;

import static net.silicatyt.physicsengine.data.PhysicsObjectCodecs.*;

public class PhysicsObject extends Display.ItemDisplay implements PolymerEntity {
    private static final double DEFAULT_INVERSE_MASS = 0.001;
    private static final Vector3d DEFAULT_SCALE = new Vector3d(1.0, 1.0, 1.0);
    private static final double DEFAULT_FRICTION_COEFFICIENT = 0.5;
    private static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3;
    private static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(Items.STONE);

    public static final double MIN_SCALE = 0.01;

    public PhysicsObject(EntityType<?> type, Level world) {
        super(type, world);

        // Default values ('internalPos' is set in Integration.phaseOne)
        setTransformationInterpolationDuration(1);
        setPosRotInterpolationDuration(1);

        // Variable dependencies
        rotationMatrixVersion.addDependencies(orientationVersion);
        inverseInertiaTensorLocalVersion.addDependencies(inverseMassVersion, scaleVersion);
        inverseInertiaTensorWorldVersion.addDependencies(rotationMatrixVersion, inverseInertiaTensorLocalVersion);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // Make PhysicsObject appear as an item display entity
        return EntityTypes.ITEM_DISPLAY;
    }

    @Override
    protected void addAdditionalSaveData(@NonNull ValueOutput valueOutput) {
        if (!getItemStack().isEmpty()) valueOutput.store("item", ItemStack.CODEC, getItemStack());
        valueOutput.putDouble("inverse_mass", inverseMass);
        valueOutput.store("linear_velocity", VECTOR3D_CODEC, linearVelocity);
        valueOutput.store("angular_velocity", VECTOR3D_CODEC, angularVelocity);
        valueOutput.store("orientation", QUATERNIOND_CODEC, orientation);
        valueOutput.store("scale", SCALE_CODEC, scale);
        valueOutput.putDouble("friction_coefficient", frictionCoefficient);
        valueOutput.putDouble("restitution_coefficient", restitutionCoefficient);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        setItemStack(valueInput.read("item", ItemStack.CODEC).orElse(DEFAULT_ITEM_STACK));
        setInverseMass(valueInput.getDoubleOr("inverse_mass", DEFAULT_INVERSE_MASS));
        setLinearVelocity(valueInput.read("linear_velocity", VECTOR3D_CODEC).orElse(new Vector3d(0.0, 0.0, 0.0)));
        setAngularVelocity(valueInput.read("angular_velocity", VECTOR3D_CODEC).orElse(new Vector3d(0.0, 0.0, 0.0)));
        setOrientation(valueInput.read("orientation", QUATERNIOND_CODEC).orElse(new Quaterniond(0.0, 0.0, 0.0, 1.0)));
        setScale(valueInput.read("scale", SCALE_CODEC).orElse(DEFAULT_SCALE));
        setFrictionCoefficient(valueInput.getDoubleOr("friction_coefficient", DEFAULT_FRICTION_COEFFICIENT));
        setRestitutionCoefficient(valueInput.getDoubleOr("restitution_coefficient", DEFAULT_RESTITUTION_COEFFICIENT));

        updateTransformation();
    }

    // Persistent data
    private double inverseMass = DEFAULT_INVERSE_MASS; // In 1/kg. Objects with 0 inverseMass are interpreted as static and will not actively search for collisions.
    private final Vector3d linearVelocity = new Vector3d();
    private final Vector3d angularVelocity = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private final Vector3d scale = new Vector3d(DEFAULT_SCALE);
    private double frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
    private double restitutionCoefficient = DEFAULT_RESTITUTION_COEFFICIENT;

    // Derived data
    private final Matrix3d rotationMatrix = new Matrix3d();
    private final Matrix3d rotationMatrixTranspose = new Matrix3d();
    private final Matrix3d inverseInertiaTensorLocal = new Matrix3d();
    private final Matrix3d inverseInertiaTensorWorld = new Matrix3d();

    // Other transient data
    private final Vector3d internalPos = new Vector3d(); // Easy and consistent access without needing to move (and risk unloading) the entity mid-tick

    // Versioning (VersionNodes that are directly accessible via setters don't need an updater method. Only fields accessible with a getter need a VersionSource.)
    private final VersionNode inverseMassVersion = new VersionNode(() -> {});
    private final VersionNode linearVelocityVersion = new VersionNode(() -> {});
    private final VersionNode angularVelocityVersion = new VersionNode(() -> {});
    private final VersionNode orientationVersion = new VersionNode(() -> {});
    private final VersionNode scaleVersion = new VersionNode(() -> {});
    private final VersionNode frictionCoefficientVersion = new VersionNode(() -> {});
    private final VersionNode restitutionCoefficientVersion = new VersionNode(() -> {});

    public final VersionNode rotationMatrixVersion = new VersionNode(this::updateRotationMatrix);
    public final VersionNode inverseInertiaTensorLocalVersion = new VersionNode(this::updateInverseInertiaTensorLocal);
    public final VersionNode inverseInertiaTensorWorldVersion = new VersionNode(this::updateInverseInertiaTensorWorld);

    // Getters
    public double getInverseMass() { return inverseMass; }
    public Vector3dc getLinearVelocity() { return linearVelocity; }
    public Vector3dc getAngularVelocity() { return angularVelocity; }
    public Quaterniondc getOrientation() { return orientation; }
    public Vector3dc getScale() { return scale; }
    public double getFrictionCoefficient() { return frictionCoefficient; }
    public double getRestitutionCoefficient() { return restitutionCoefficient; }

    public Matrix3dc getInverseInertiaTensorWorld() { return inverseInertiaTensorWorld; }

    // Setters
    public void setInverseMass(double d) throws IllegalArgumentException {
        if (d < 0.0) throw new IllegalArgumentException("Inverse mass must be >= 0");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Inverse mass must be finite");
        if (inverseMass == d) return;
        inverseMassVersion.increment();
        inverseMass = d;
    }

    public void setLinearVelocity(@NonNull Vector3d v) throws IllegalArgumentException {
        if (!v.isFinite()) throw new IllegalArgumentException("Linear velocity must be finite");
        if (linearVelocity.equals(v)) return;
        linearVelocityVersion.increment();
        linearVelocity.set(v);
    }

    public void setAngularVelocity(@NonNull Vector3d v) throws IllegalArgumentException {
        if (!v.isFinite()) throw new IllegalArgumentException("Angular velocity must be finite");
        if (angularVelocity.equals(v)) return;
        angularVelocityVersion.increment();
        angularVelocity.set(v);
    }

    public void setOrientation(@NonNull Quaterniond q) throws IllegalArgumentException {
        if (!q.isFinite()) throw new IllegalArgumentException("Orientation must be finite");
        if (q.lengthSquared() < 1e-24) throw new IllegalArgumentException("Orientation must not be degenerate");
        if (orientation.equals(q)) return;
        orientationVersion.increment();
        orientation.set(q);
        orientation.normalize();
    }

    public void setScale(@NonNull Vector3d v) throws IllegalArgumentException {
        if (v.x < MIN_SCALE || v.y < MIN_SCALE || v.z < MIN_SCALE) throw new IllegalArgumentException("Scale must be >= " + MIN_SCALE);
        if (!v.isFinite()) throw new IllegalArgumentException("Scale must be finite");
        if (scale.equals(v)) return;
        scaleVersion.increment();
        scale.set(v);
    }

    public void setFrictionCoefficient(double d) throws IllegalArgumentException {
        if (d < 0.0 || d > 1.0) throw new IllegalArgumentException("Friction coefficient must be between 0 and 1");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Friction coefficient must be finite");
        if (frictionCoefficient == d) return;
        frictionCoefficientVersion.increment();
        frictionCoefficient = d;
    }

    public void setRestitutionCoefficient(double d) throws IllegalArgumentException {
        if (d < 0.0 || d > 1.0) throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Restitution coefficient must be finite");
        if (restitutionCoefficient == d) return;
        restitutionCoefficientVersion.increment();
        restitutionCoefficient = d;
    }

    // Update derived data
    private void updateRotationMatrix() {
        orientation.get(rotationMatrix);
        rotationMatrix.transpose(rotationMatrixTranspose);
    }

    private void updateInverseInertiaTensorLocal() {
        double numerator = 12 * inverseMass;
        double scaleSquaredX = scale.x * scale.x;
        double scaleSquaredY = scale.y * scale.y;
        double scaleSquaredZ = scale.z * scale.z;
        inverseInertiaTensorLocal.m00 = numerator / (scaleSquaredY + scaleSquaredZ);
        inverseInertiaTensorLocal.m11 = numerator / (scaleSquaredX + scaleSquaredZ);
        inverseInertiaTensorLocal.m22 = numerator / (scaleSquaredX + scaleSquaredY);
    }

    private void updateInverseInertiaTensorWorld() { rotationMatrix.mul(inverseInertiaTensorLocal, inverseInertiaTensorWorld).mul(rotationMatrixTranspose, inverseInertiaTensorWorld); }

    // Other
    public void updateTransformation() {
        setTransformationInterpolationDelay(0);
        setTransformation(new Transformation(new Vector3f(), new Quaternionf(orientation), new Vector3f(scale), new Quaternionf())); // Item display rendering only accepts floats
    }

}
