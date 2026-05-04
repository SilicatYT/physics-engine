package silicatyt.physics.entity;

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
import silicatyt.physics.versioning.VersionNode;
import silicatyt.physics.versioning.VersionSource;
import xyz.nucleoid.packettweaker.PacketContext;

import java.lang.Math;
import java.util.List;

public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    public static final double DEFAULT_INVERSE_MASS = 0.001d;
    public static final Vector3d DEFAULT_SCALE = new Vector3d(1d, 1d, 1d);
    public static final double DEFAULT_FRICTION_COEFFICIENT = 0.5d;
    public static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3d;
    public static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(Items.STONE);

    // Stored data
    private double inverseMass; // In 1/kg. If inverseMass is 0, mass is infinite. This is interpreted as "the object is static and will not actively look for collisions", meaning two static objects cannot collide with each other.
    private final Vector3d linearVelocity = new Vector3d();
    private final Vector3d angularVelocity = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private final Vector3d scale = new Vector3d();
    private double frictionCoefficient;
    private double restitutionCoefficient;

    // Derived data
    private final Matrix3d rotationMatrix = new Matrix3d();
    private final Matrix3d rotationMatrixTranspose = new Matrix3d();
    private final Matrix3d inverseInertiaTensorLocal = new Matrix3d();
    private final Matrix3d inverseInertiaTensorWorld = new Matrix3d();
    private final Vector3d[] cornerPosLocal = new Vector3d[8];
    private final Vector3d[] cornerPosRelative = new Vector3d[8];
    private final Vector3d[] cornerPosAbsolute = new Vector3d[8]; // Corners are ordered like this: [[-,-,-,], [-,-,+], [-,+,-], [-,+,+], [+,-,-,], [+,-,+], [+,+,-], [+,+,+]]
    private final Vector3d[] boundingBoxAbsolute = {new Vector3d(), new Vector3d()}; // 1st element is the "[-,-,-]" corner of the bounding box, 2nd element is the [+,+,+] corner. I don't use the "Box" class Minecraft provides because that is immutable, and it's not compatible with JOML maths.

    // Other transient data
    private final Vector3d pos = new Vector3d(); // Just for easy and consistent access without risking unloading the entity mid-tick
    private Vec3d lastEntityPos = new Vec3d(0d, 0d, 0d);
    public final Vector3d accumulatedForce = new Vector3d();
    public final Vector3d accumulatedTorque = new Vector3d();
    private final Vector3d linearVelocityFromAcceleration = new Vector3d();

    // Variable Versioning (Transient)
    private final VersionNode inverseMassVersion = new VersionNode(() -> {}); // inverseMass is a directly settable field, so it has no update method. Instead, it directly bumps its version in the public setter method.
    private final VersionNode orientationVersion = new VersionNode(() -> {});
    private final VersionNode linearVelocityVersion = new VersionNode(() -> {});
    private final VersionNode angularVelocityVersion = new VersionNode(() -> {});
    private final VersionNode scaleVersion = new VersionNode(() -> {});
    private final VersionNode frictionCoefficientVersion = new VersionNode(() -> {});
    private final VersionNode restitutionCoefficientVersion = new VersionNode(() -> {});

    private final VersionNode rotationMatrixVersion = new VersionNode(this::updateRotationMatrix);
    private final VersionNode inverseInertiaTensorLocalVersion = new VersionNode(this::updateInverseInertiaTensorLocal);
    private final VersionNode inverseInertiaTensorWorldVersion = new VersionNode(this::updateInverseInertiaTensorWorld);
    private final VersionNode cornerPosLocalVersion = new VersionNode(this::updateCornerPosLocal);
    private final VersionNode cornerPosRelativeVersion = new VersionNode(this::updateCornerPosRelative);
    private final VersionNode cornerPosAbsoluteVersion = new VersionNode(this::updateCornerPosAbsolute);
    private final VersionNode boundingBoxAbsoluteVersion = new VersionNode(this::updateBoundingBoxAbsolute);

    private final VersionNode posVersion = new VersionNode(() -> {});


    public VersionSource getInverseMassVersion() { return inverseMassVersion; }
    public VersionSource getLinearVelocityVersion() { return linearVelocityVersion; }
    public VersionSource getAngularVelocityVersion() { return angularVelocityVersion; }
    public VersionSource getOrientationVersion() { return orientationVersion; }
    public VersionSource getScaleVersion() { return scaleVersion; }
    public VersionSource getFrictionCoefficientVersion() { return frictionCoefficientVersion; }
    public VersionSource getRestitutionCoefficientVersion() { return restitutionCoefficientVersion; }

    //public VersionSource getRotationMatrixVersion() { return rotationMatrixVersion; } // There is no getter, so there's no point
    //public VersionSource getInverseInertiaTensorLocalVersion() { return inverseInertiaTensorLocalVersion; }
    public VersionSource getInverseInertiaTensorWorldVersion() { return inverseInertiaTensorWorldVersion; }
    //public VersionSource getCornerPosLocalVersion() { return cornerPosLocalVersion; }
    public VersionSource getCornerPosRelativeVersion() { return cornerPosRelativeVersion; }
    public VersionSource getCornerPosAbsoluteVersion() { return cornerPosAbsoluteVersion; }
    public VersionSource getBoundingBoxAbsoluteVersion() { return boundingBoxAbsoluteVersion; }

    public VersionSource getPosVersion() { return posVersion; }



    // Constructor
    public PhysicsObject(EntityType<?> type, World world) {
        super(type, world);

        // Set default values (pos is taken care of in phaseOne of Integration)
        setInterpolationDuration(1);
        setTeleportDuration(1);

        inverseMass = DEFAULT_INVERSE_MASS;
        scale.set(DEFAULT_SCALE);
        frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
        restitutionCoefficient = DEFAULT_RESTITUTION_COEFFICIENT;

        for (int i = 0; i < 8; i++) {
            cornerPosLocal[i] = new Vector3d();
            cornerPosRelative[i] = new Vector3d();
            cornerPosAbsolute[i] = new Vector3d();
        }

        // Add variable dependencies
        rotationMatrixVersion.addDependencies(orientationVersion);
        inverseInertiaTensorLocalVersion.addDependencies(inverseMassVersion, scaleVersion);
        inverseInertiaTensorWorldVersion.addDependencies(rotationMatrixVersion, inverseInertiaTensorLocalVersion); // inverseInertiaTensorLocal already has rotationMatrix as a dependency, but I add both as dependencies for extra safety & clarity
        cornerPosLocalVersion.addDependencies(scaleVersion);
        cornerPosRelativeVersion.addDependencies(cornerPosLocalVersion, rotationMatrixVersion);
        cornerPosAbsoluteVersion.addDependencies(cornerPosRelativeVersion, posVersion);
        boundingBoxAbsoluteVersion.addDependencies(cornerPosAbsoluteVersion);
    }





    // Getters
    public Vector3dc getInternalPos() { return pos; }

    public Vec3d getLastEntityPos() { return lastEntityPos; }

    public double getInverseMass() { return inverseMass; }

    public Vector3dc getLinearVelocity() { return linearVelocity; }

    public Vector3d getLinearVelocity(Vector3d dest) { return dest.set(linearVelocity); }

    public Vector3dc getAngularVelocity() { return angularVelocity; }

    public Vector3d getAngularVelocity(Vector3d dest) { return dest.set(angularVelocity); }

    public Quaterniondc getOrientation() { return orientation; }

    public Quaterniond getOrientation(Quaterniond dest) { return dest.set(orientation); }

    public Vector3dc getScale() { return scale; }

    public Vector3d getScale(Vector3d dest) { return dest.set(scale); }

    public double getFrictionCoefficient() { return frictionCoefficient; }

    public double getRestitutionCoefficient() { return restitutionCoefficient; }

    public Matrix3dc getInverseInertiaTensorWorld() {
        inverseInertiaTensorWorldVersion.updateIfNeeded();
        return inverseInertiaTensorWorld;
    }

    public Matrix3d getInverseInertiaTensorWorld(Matrix3d dest) { return dest.set(getInverseInertiaTensorWorld()); }

    public Vector3dc[] getCornerPosRelative() {
        cornerPosRelativeVersion.updateIfNeeded();
        return cornerPosRelative;
    }

    public Vector3d[] getCornerPosRelative(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 8) { throw new IllegalArgumentException("Expected array length of 8, got " +  dest.length); }
        Vector3dc[] cornerPos = getCornerPosRelative();
        for (int i = 0; i < 8; i++) {
            dest[i].set(cornerPos[i]);
        }
        return dest;
    }

    public Vector3dc getCornerPosRelative(int index) {
        cornerPosRelativeVersion.updateIfNeeded();
        return cornerPosRelative[index];
    }

    public Vector3d getCornerPosRelative(int index, Vector3d dest) { return dest.set(getCornerPosRelative(index)); }

    public Vector3dc[] getCornerPosAbsolute() {
        cornerPosAbsoluteVersion.updateIfNeeded();
        return cornerPosAbsolute;
    }

    public Vector3d[] getCornerPosAbsolute(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 8) { throw new IllegalArgumentException("Expected array length of 8, got " +  dest.length); }
        Vector3dc[] cornerPos = getCornerPosAbsolute();
        for (int i = 0; i < 8; i++) {
            dest[i].set(cornerPos[i]);
        }
        return dest;
    }

    public Vector3dc getCornerPosAbsolute(int index) {
        cornerPosAbsoluteVersion.updateIfNeeded();
        return cornerPosAbsolute[index];
    }

    public Vector3d getCornerPosAbsolute(int index, Vector3d dest) { return dest.set(getCornerPosAbsolute(index)); }

    public Vector3dc[] getBoundingBoxAbsolute() {
        boundingBoxAbsoluteVersion.updateIfNeeded();
        return boundingBoxAbsolute;
    }

    public Vector3d[] getBoundingBoxAbsolute(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 2) { throw new IllegalArgumentException("Expected array length of 2, got " +  dest.length); }
        Vector3dc[] boundingBox = getBoundingBoxAbsolute();
        dest[0].set(boundingBox[0]);
        dest[1].set(boundingBox[1]);
        return dest;
    }

    public Vector3d getAxis(int index) { return getAxis(index, new Vector3d()); } // TODO: Maybe store this as its own instance variable for consistency, so I don't need to make a new object every time I call this.

    public Vector3d getAxis(int index, Vector3d dest) {
        rotationMatrixVersion.updateIfNeeded();
        return rotationMatrix.getColumn(index, dest);
    }

    public Vector3dc getLinearVelocityFromAcceleration() { return linearVelocityFromAcceleration; }

    public Vector3d getLinearVelocityFromAcceleration(Vector3d dest) { return dest.set(linearVelocityFromAcceleration); }





    // Setters
    public void setInternalPos(Vector3dc position) {
        if (!position.isFinite()) { throw new IllegalArgumentException("Position must be finite"); }
        posVersion.increment();
        pos.set(position);
    }

    public void setInternalPos(Vec3d position) { setInternalPos(new Vector3d(position.x, position.y, position.z)); }

    public void setInverseMass(double inverseMass) throws IllegalArgumentException {
        if (inverseMass < 0) { throw new IllegalArgumentException("Inverse mass must not be negative"); }
        if (!Double.isFinite(inverseMass)) { throw new IllegalArgumentException("Inverse mass must be finite"); }

        inverseMassVersion.increment();
        this.inverseMass = inverseMass;
    }

    public void setLinearVelocity(Vector3dc linearVelocity) {
        if (!linearVelocity.isFinite()) { throw new IllegalArgumentException("Linear velocity must be finite"); }

        linearVelocityVersion.increment();
        this.linearVelocity.set(linearVelocity);
    }

    public void setAngularVelocity(Vector3dc angularVelocity) {
        if (!angularVelocity.isFinite()) { throw new IllegalArgumentException("Angular velocity must be finite"); }

        angularVelocityVersion.increment();
        this.angularVelocity.set(angularVelocity);
    }

    public void setOrientation(Quaterniond orientation) {
        if (!isValidQuaternion(orientation)) { throw new  IllegalArgumentException("Orientation must have a finite, non-zero length"); }
        orientationVersion.increment();
        this.orientation.set(orientation);
        this.orientation.normalize();
    }

    public void setScale(Vector3dc scale) throws IllegalArgumentException {
        if (scale.x() < 0 || scale.y() < 0 || scale.z() < 0) { throw new IllegalArgumentException("Scale must not be negative"); }
        if (!scale.isFinite()) { throw new IllegalArgumentException("Scale must be finite"); }

        scaleVersion.increment();
        this.scale.set(scale);
    }

    public void setFrictionCoefficient(double frictionCoefficient) throws IllegalArgumentException {
        if (frictionCoefficient < 0 || frictionCoefficient > 1) { throw new IllegalArgumentException("Friction coefficient must be between 0 and 1"); }
        if (!Double.isFinite(frictionCoefficient)) { throw new IllegalArgumentException("Friction coefficient must be finite"); }

        frictionCoefficientVersion.increment();
        this.frictionCoefficient = frictionCoefficient;
    }

    public void setRestitutionCoefficient(double restitutionCoefficient) throws IllegalArgumentException {
        if (restitutionCoefficient < 0 || restitutionCoefficient > 1) { throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1"); }
        if (!Double.isFinite(restitutionCoefficient)) { throw new IllegalArgumentException("Restitution coefficient must be finite"); }

        restitutionCoefficientVersion.increment();
        this.restitutionCoefficient = restitutionCoefficient;
    }

    public void setLinearVelocityFromAcceleration(Vector3dc linearVelocity) { // TODO: Maybe make "addAcceleration" its own method, so that linearVelocityFromAcceleration and linearVelocity are always consistent
        if (!linearVelocity.isFinite()) { throw new IllegalArgumentException("linear velocity must be finite"); }
        linearVelocityFromAcceleration.set(linearVelocity);
    }





    // NBT saving & loading
    @Override
    protected void writeCustomData(WriteView view) {
        // super.writeCustomData(view); <- Completely ignore the item display's NBT: I'll take over from here, thanks. I can't read the orientation and stuff anyway (requiring me to keep track of them myself), so I might as well. Makes the data prettier if useless data isn't in there.
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        if (!getItemStack().isEmpty()) { view.put("item", ItemStack.CODEC, getItemStack()); } // ItemStack.Codec doesn't work for empty items
        view.putDouble("inverse_mass", inverseMass);
        view.put("linear_velocity", doubleListCodec, List.of(linearVelocity.x, linearVelocity.y, linearVelocity.z));
        view.put("angular_velocity", doubleListCodec, List.of(angularVelocity.x, angularVelocity.y, angularVelocity.z));
        view.put("orientation", doubleListCodec, List.of(orientation.x, orientation.y, orientation.z, orientation.w));
        view.put("scale", doubleListCodec, List.of(scale.x, scale.y, scale.z));
        view.putDouble("friction_coefficient", frictionCoefficient);
        view.putDouble("restitution_coefficient", restitutionCoefficient);
    }

    @Override
    protected void readCustomData(ReadView view) { // All required instance variables need to not be null after this. Otherwise, it will cause all sorts of calculation bugs after unloading and loading again.
        // super.readCustomData(view); <- Ignore for the same reason as in "writeCustomData"
        // TODO: Add list size validation & "isFinite" checks, as well as "fail if scale is not bigger than 0". Also add a check for "0,0,0,0" quaternions
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

        setItemStack(view.read("item", ItemStack.CODEC).orElse(DEFAULT_ITEM_STACK));
        setInverseMass(view.read("inverse_mass", Codec.DOUBLE).orElse(DEFAULT_INVERSE_MASS));
        List<Double> linearVelocity = view.read("linear_velocity", doubleListCodec).orElse(List.of(0d, 0d, 0d));
        setLinearVelocity(new Vector3d(linearVelocity.get(0), linearVelocity.get(1), linearVelocity.get(2)));
        List<Double> angularVelocity = view.read("angular_velocity", doubleListCodec).orElse(List.of(0d, 0d, 0d));
        setAngularVelocity(new Vector3d(angularVelocity.get(0), angularVelocity.get(1), angularVelocity.get(2)));
        List<Double> orientation = view.read("orientation", doubleListCodec).orElse(List.of(0d, 0d, 0d, 1d));
        setOrientation(new Quaterniond(orientation.get(0), orientation.get(1), orientation.get(2), orientation.get(3)));
        List<Double> scale = view.read("scale", doubleListCodec).orElse(List.of(DEFAULT_SCALE.x, DEFAULT_SCALE.y, DEFAULT_SCALE.z));
        setScale(new Vector3d(scale.get(0), scale.get(1), scale.get(2)));
        setFrictionCoefficient(view.read("friction_coefficient", Codec.DOUBLE).orElse(DEFAULT_FRICTION_COEFFICIENT));
        setRestitutionCoefficient(view.read("restitution_coefficient", Codec.DOUBLE).orElse(DEFAULT_RESTITUTION_COEFFICIENT));

        updateVisuals();
    }





    // Update derived data
    private void updateRotationMatrix() {
        orientation.get(rotationMatrix);
        rotationMatrix.transpose(rotationMatrixTranspose);
    }

    private void updateInverseInertiaTensorLocal() {
        inverseInertiaTensorLocal.m00 = (12 * inverseMass) / (scale.y * scale.y + scale.z * scale.z);
        inverseInertiaTensorLocal.m11 = (12 * inverseMass) / (scale.x * scale.x + scale.z * scale.z);
        inverseInertiaTensorLocal.m22 = (12 * inverseMass) / (scale.x * scale.x + scale.y * scale.y);
    }

    private void updateInverseInertiaTensorWorld() { rotationMatrix.mul(inverseInertiaTensorLocal, inverseInertiaTensorWorld).mul(rotationMatrixTranspose, inverseInertiaTensorWorld); }

    private void updateCornerPosLocal() {
        int i = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    cornerPosLocal[i++].set(
                            0.5 * x * scale.x,
                            0.5 * y * scale.y,
                            0.5 * z * scale.z
                    );
                }
            }
        }
    }

    private void updateCornerPosRelative() {
        for (int i = 0; i < 8; i++) { rotationMatrix.transform(cornerPosLocal[i], cornerPosRelative[i]); }
    }

    private void updateCornerPosAbsolute() {
        for (int i = 0; i < 8; i++) { cornerPosRelative[i].add(pos, cornerPosAbsolute[i]); }
    }

    private void updateBoundingBoxAbsolute() {
        boundingBoxAbsolute[0].set(cornerPosAbsolute[0]);
        boundingBoxAbsolute[1].set(cornerPosAbsolute[0]);

        for (int i = 1; i < 8; i++) {
            boundingBoxAbsolute[0].x = Math.min(boundingBoxAbsolute[0].x, cornerPosAbsolute[i].x);
            boundingBoxAbsolute[1].x = Math.max(boundingBoxAbsolute[1].x, cornerPosAbsolute[i].x);

            boundingBoxAbsolute[0].y = Math.min(boundingBoxAbsolute[0].y, cornerPosAbsolute[i].y);
            boundingBoxAbsolute[1].y = Math.max(boundingBoxAbsolute[1].y, cornerPosAbsolute[i].y);

            boundingBoxAbsolute[0].z = Math.min(boundingBoxAbsolute[0].z, cornerPosAbsolute[i].z);
            boundingBoxAbsolute[1].z = Math.max(boundingBoxAbsolute[1].z, cornerPosAbsolute[i].z);
        }
    }





    // Other methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    public void updateVisuals() {
        setStartInterpolation(0);
        setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(orientation), new Vector3f(scale), new Quaternionf())); // Although I calculate everything in doubles, the rendering uses floats because that's how item displays (and Minecraft's rendering in general) work
    }

    public void updateEntityPos() { // This isn't in updateVisuals because updating the entity pos in readCustomData (where entityPos is potentially still 0,0,0) would cause the object to teleport to 0,0,0 without additional code
        setPos(pos.x, pos.y, pos.z); // The vanilla method for entity position
        lastEntityPos = getEntityPos();
    }

    public void applyImpulse(Vector3dc impulse, double inverseMassTotal, Vector3dc contactPos) { // TODO: Maybe move this to a helper method in ContactResolver
        if (inverseMass == 0.0) { return; }

        // Linear part
        Vector3d relativeContactPos =  new Vector3d(contactPos).sub(pos);
        Vector3d linearVelocityChange = new Vector3d(impulse).mul(inverseMass);
        setLinearVelocity(linearVelocityChange.add(getLinearVelocity()));

        // Angular part
        Vector3d torque = new Vector3d(relativeContactPos).cross(impulse);
        Vector3d angularVelocityChange = getInverseInertiaTensorWorld().transform(torque);
        setAngularVelocity(angularVelocityChange.add(getAngularVelocity()));
    }

    private static boolean isValidQuaternion(Quaterniondc quaternion) {
        if (!quaternion.isFinite()) { return false; }
        double lengthSquared = quaternion.x()*quaternion.x() + quaternion.y()*quaternion.y() + quaternion.z()*quaternion.z() + quaternion.w()*quaternion.w();
        return lengthSquared > 1e-24;
    }

    // TODO (VERY IMPORTANT): Check whether it's more stable to calculate contactVelocity with "(velocityBeforeIntegration + acceleration) * dampingMultiplier) - acceleration" or "velocityBeforeIntegration * dampingMultiplier" (or maybe even "velocityBeforeIntegration"). Currently I use the former.

}


// TODO: Fix a bug where scale flickers (interpolates every tick after setting)
