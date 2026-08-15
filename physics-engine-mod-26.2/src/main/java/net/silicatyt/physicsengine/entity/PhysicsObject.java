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
import net.silicatyt.physicsengine.versioning.physicsobject.PhysicsObjectVersions;
import net.silicatyt.physicsengine.versioning.physicsobject.PhysicsObjectVersionsView;
import org.joml.*;
import org.jspecify.annotations.NonNull;

import static net.silicatyt.physicsengine.data.PhysicsObjectCodecs.*;
import static net.silicatyt.physicsengine.simulation.Integrator.EPSILON_SQUARED;
import static net.silicatyt.physicsengine.simulation.Main.DELTA_TIME;

public final class PhysicsObject extends Display.ItemDisplay implements PolymerEntity {
    private static final double DEFAULT_INVERSE_MASS = 0.001;
    private static final Vector3dc DEFAULT_SCALE = new Vector3d(1.0, 1.0, 1.0);
    private static final double DEFAULT_FRICTION_COEFFICIENT = 0.5;
    private static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3;
    private static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(Items.STONE);

    public static final double MIN_SCALE = 0.01;

    public PhysicsObject(EntityType<?> type, Level world) {
        super(type, world);
        setTransformationInterpolationDuration(1);
        setPosRotInterpolationDuration(1);
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
        setScale(valueInput.read("scale", SCALE_CODEC).orElse(new Vector3d(DEFAULT_SCALE)));
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
    private Vec3 lastEntityPos = new Vec3(0.0, 0.0, 0.0);
    private final Vector3d accumulatedForce = new Vector3d();
    private final Vector3d accumulatedTorque = new Vector3d();
    private final Vector3d linearVelocityFromAcceleration = new Vector3d();


    // Versioning
    private final PhysicsObjectVersions versions = new PhysicsObjectVersions(this::updateRotationMatrix, this::updateInverseInertiaTensorLocal, this::updateInverseInertiaTensorWorld);
    public PhysicsObjectVersionsView getVersions() { return versions; }


    // Getters
    public double getInverseMass() { return inverseMass; }
    public Vector3dc getLinearVelocity() { return linearVelocity; }
    public Vector3dc getAngularVelocity() { return angularVelocity; }
    public Quaterniondc getOrientation() { return orientation; }
    public Vector3dc getScale() { return scale; }
    public double getFrictionCoefficient() { return frictionCoefficient; }
    public double getRestitutionCoefficient() { return restitutionCoefficient; }

    public Matrix3dc getInverseInertiaTensorWorld() {
        versions.inverseInertiaTensorWorld.updateIfNeeded();
        return inverseInertiaTensorWorld;
    }

    public Vector3dc getInternalPos() { return internalPos; }
    public Vec3 getLastEntityPos() { return lastEntityPos; }
    public Vector3dc getAccumulatedForce() { return accumulatedForce; }
    public Vector3dc getAccumulatedTorque() { return accumulatedTorque; }
    public Vector3dc getLinearVelocityFromAcceleration() { return linearVelocityFromAcceleration; }


    // Setters
    public void setInverseMass(double d) throws IllegalArgumentException {
        if (d < 0.0) throw new IllegalArgumentException("Inverse mass must be >= 0");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Inverse mass must be finite");
        if (inverseMass == d) return;
        inverseMass = d;
        versions.inverseMass.increment();
    }

    public void setLinearVelocity(double x, double y, double z) throws IllegalArgumentException {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Linear velocity must be finite");
        if (linearVelocity.equals(x, y, z)) return;
        linearVelocity.set(x, y, z);
        versions.linearVelocity.increment();
    }

    public void setLinearVelocity(@NonNull Vector3dc v) throws IllegalArgumentException { setLinearVelocity(v.x(), v.y(), v.z()); }

    public void setAngularVelocity(double x, double y, double z) throws IllegalArgumentException {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Angular velocity must be finite");
        if (angularVelocity.equals(x, y, z)) return;
        angularVelocity.set(x, y, z);
        versions.angularVelocity.increment();
    }

    public void setAngularVelocity(@NonNull Vector3dc v) throws IllegalArgumentException { setAngularVelocity(v.x(), v.y(), v.z()); }


    public void setOrientation(@NonNull Quaterniondc q) throws IllegalArgumentException {
        if (!q.isFinite()) throw new IllegalArgumentException("Orientation must be finite");
        if (q.lengthSquared() < EPSILON_SQUARED) throw new IllegalArgumentException("Orientation must not be degenerate");
        if (orientation.equals(q)) return;
        orientation.set(q);
        orientation.normalize();
        versions.orientation.increment();
    }

    public void setScale(@NonNull Vector3dc v) throws IllegalArgumentException {
        if (v.x() < MIN_SCALE || v.y() < MIN_SCALE || v.z() < MIN_SCALE) throw new IllegalArgumentException("Scale must be >= " + MIN_SCALE);
        if (!v.isFinite()) throw new IllegalArgumentException("Scale must be finite");
        if (scale.equals(v)) return;
        scale.set(v);
        versions.scale.increment();
    }

    public void setFrictionCoefficient(double d) throws IllegalArgumentException {
        if (d < 0.0 || d > 1.0) throw new IllegalArgumentException("Friction coefficient must be between 0 and 1");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Friction coefficient must be finite");
        if (frictionCoefficient == d) return;
        frictionCoefficient = d;
        versions.frictionCoefficient.increment();
    }

    public void setRestitutionCoefficient(double d) throws IllegalArgumentException {
        if (d < 0.0 || d > 1.0) throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1");
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Restitution coefficient must be finite");
        if (restitutionCoefficient == d) return;
        restitutionCoefficient = d;
        versions.restitutionCoefficient.increment();
    }
    
    public void setInternalPos(double x, double y, double z) throws IllegalArgumentException { // TODO: Maybe check if the entity needs to be in-bounds (Might crash?)
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Internal pos must be finite");
        if (internalPos.equals(x, y, z)) return;
        versions.internalPos.increment();
        internalPos.set(x, y, z);
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
    public void updateTransformation() { // TODO: Remove "new ...()" calls
        setTransformationInterpolationDelay(0);
        setTransformation(new Transformation(new Vector3f(), new Quaternionf(orientation), new Vector3f(scale), new Quaternionf())); // Item display rendering only accepts floats
    }

    public void updateEntityPos() { // This isn't in updateVisuals because updating the entity pos in readCustomData (where entityPos is potentially still 0,0,0) would cause the object to teleport to 0,0,0 without additional code
        setPos(internalPos.x, internalPos.y, internalPos.z); // The vanilla method for entity position
        lastEntityPos = position();
    }

    public void addLinearVelocityAcceleration(double x, double y, double z) throws IllegalArgumentException { // TODO: Maybe put the whole formula here, and rename to "applyAcceleration" for consistency with "applyTorque"?
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Linear velocity acceleration must be finite");
        if (x == 0.0 && y == 0.0 && z == 0.0) return;
        linearVelocityFromAcceleration.set(x, y, z); // TODO: Should I rather set it to 0 each tick and use add() here, in case someone uses this method multiple times? They *could* set both velocity values independently by just using 2 method calls rn. But if I add a "clear()" method, it'll still be possible. I'd need to bundle it in a "startTick()" method, which would be annoying. I'll just keep it like this for now.
        linearVelocity.add(x, y, z);
        versions.linearVelocity.increment();
    }

    public void applyTorque(Vector3dc torque) throws IllegalArgumentException {
        if (!torque.isFinite()) throw new IllegalArgumentException("Torque must be finite");
        if (torque.equals(0.0, 0.0, 0.0)) return;
        double x = angularVelocity.x();
        double y = angularVelocity.y();
        double z = angularVelocity.z();
        getInverseInertiaTensorWorld().transform(torque, angularVelocity);
        angularVelocity.mul(DELTA_TIME);
        angularVelocity.add(x, y, z);
        versions.angularVelocity.increment(); // TODO: Should I use setAngularVelocity here? Same reason as for rotateOrientation()
    }

    public void rotateOrientation(double angle, Vector3dc axis) throws IllegalArgumentException {
        if (!axis.isFinite()) throw new IllegalArgumentException("Rotation axis must be finite");
        if (axis.lengthSquared() < EPSILON_SQUARED) throw new IllegalArgumentException("Rotation axis must not be degenerate");
        orientation.rotateAxis(angle, axis); // The axis is normalized automatically by rotateAxis
        orientation.normalize(); // Could suffice to do it once every few ticks
        versions.orientation.increment(); // TODO: Should I use a setter so I don't need this explicitly? Would add more overhead, but I wouldn't have to increment the version in several places?
    }

    //public void addLinearCorrection(@NonNull Vector3dc v) throws IllegalArgumentException {
    //    if (!v.isFinite()) throw new IllegalArgumentException("Linear correction must be finite");
        accumulatedTorque.zero();
    }
}
