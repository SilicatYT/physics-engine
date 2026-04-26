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
import silicatyt.physics.data.Contact;
import xyz.nucleoid.packettweaker.PacketContext;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO: Check if "velocityWithoutAcceleration" is the correct approach for physics stability

public class PhysicsObject extends ItemDisplayEntity implements PolymerEntity {
    public static final double DEFAULT_INVERSE_MASS = 0.001d;
    public static final Vector3d DEFAULT_SCALE = new Vector3d(1d, 1d, 1d);
    public static final double DEFAULT_FRICTION_COEFFICIENT = 0.5d;
    public static final double DEFAULT_RESTITUTION_COEFFICIENT = 0.3d;
    public static final ItemStack DEFAULT_ITEM_STACK = new ItemStack(Items.STONE);

    // Stored data
    private double inverseMass; // In 1/kg. If inverseMass is 0, mass is infinite. This is interpreted as "the object is static and will not actively look for collisions", meaning two static objects cannot collide with each other.
    public final Vector3d linearVelocity = new Vector3d();
    public final Vector3d angularVelocity = new Vector3d();
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
    private final Vector3d linearVelocityWithoutAcceleration = new Vector3d(); // Linear velocity from the start of integration, but only including the damping and not the velocity from accumulatedForce. Used during contactVelocity calculation for stability.
    private final Vector3d angularVelocityWithoutAcceleration = new Vector3d();

    // Other transient data
    private final Vector3d pos = new Vector3d(); // Just for easy and consistent access without risking unloading the entity mid-tick
    private Vec3d lastEntityPos = new Vec3d(0d, 0d, 0d);
    public final Vector3d accumulatedForce = new Vector3d();
    public final Vector3d accumulatedTorque = new Vector3d();

    public boolean rotationMatrixDirty = true;
    public boolean inverseInertiaTensorLocalDirty = true;
    public boolean inverseInertiaTensorWorldDirty = true;
    public boolean cornerPosLocalDirty = true;
    public boolean cornerPosRelativeDirty = true;
    public boolean cornerPosAbsoluteDirty = true;
    public boolean boundingBoxAbsoluteDirty = true;





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
    }





    // Getters
    public Vector3d getInternalPos() {
        return new Vector3d(this.pos);
    }

    public Vec3d getLastEntityPos() {
        return lastEntityPos;
    }

    public double getInverseMass() {
        return inverseMass;
    }

    public Vector3d getLinearVelocity() {
        return getLinearVelocity(new Vector3d());
    }

    public Vector3d getLinearVelocity(Vector3d dest) { return dest.set(linearVelocity); }

    public Vector3d getAngularVelocity() { return getAngularVelocity(new Vector3d()); }

    public Vector3d getAngularVelocity(Vector3d dest) { return dest.set(angularVelocity); }

    public Quaterniond getOrientation() { return getOrientation(new Quaterniond()); }

    public Quaterniond getOrientation(Quaterniond dest) { return dest.set(orientation); }

    public Vector3d getScale() { return getScale(new Vector3d()); }

    public Vector3d getScale(Vector3d dest) { return dest.set(scale); }

    public double getFrictionCoefficient() { return frictionCoefficient; }

    public double getRestitutionCoefficient() { return restitutionCoefficient; }

    public Matrix3d getInverseInertiaTensorWorld() { return getInverseInertiaTensorWorld(new Matrix3d()); }

    public Matrix3d getInverseInertiaTensorWorld(Matrix3d dest) {
        if (inverseInertiaTensorWorldDirty) { updateInverseInertiaTensorWorld(); }
        return dest.set(inverseInertiaTensorWorld);
    }

    public Vector3d[] getCornerPosRelative() { return getCornerPosRelative(new Vector3d[]{new Vector3d(), new Vector3d()}); }

    public Vector3d[] getCornerPosRelative(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 2) { throw new IllegalArgumentException("Expected array length of 2, got " +  dest.length); }
        if (cornerPosRelativeDirty) { updateCornerPosRelative(); }
        dest[0].set(cornerPosRelative[0]);
        dest[1].set(cornerPosRelative[1]);
        return dest;
    }

    public Vector3d getCornerPosRelative(int index) { return getCornerPosRelative(index, new Vector3d()); }

    public Vector3d getCornerPosRelative(int index, Vector3d dest) {
        if (cornerPosRelativeDirty) { updateCornerPosRelative(); }
        return dest.set(cornerPosRelative[index]);
    }

    public Vector3d[] getCornerPosAbsolute() { return getCornerPosAbsolute(new Vector3d[]{new Vector3d(), new Vector3d()}); }

    public Vector3d[] getCornerPosAbsolute(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 2) { throw new IllegalArgumentException("Expected array length of 2, got " +  dest.length); }
        if (cornerPosAbsoluteDirty) { updateCornerPosAbsolute(); }
        dest[0].set(cornerPosAbsolute[0]);
        dest[1].set(cornerPosAbsolute[1]);
        return dest;
    }

    public Vector3d getCornerPosAbsolute(int index) { return getCornerPosAbsolute(index, new Vector3d()); }

    public Vector3d getCornerPosAbsolute(int index, Vector3d dest) {
        if (cornerPosAbsoluteDirty) { updateCornerPosAbsolute(); }
        return dest.set(cornerPosAbsolute[index]);
    }

    public Vector3d[] getBoundingBoxAbsolute() { return getBoundingBoxAbsolute(new Vector3d[]{new Vector3d(), new Vector3d()}); }

    public Vector3d[] getBoundingBoxAbsolute(Vector3d[] dest) throws IllegalArgumentException {
        if (dest.length != 2) { throw new IllegalArgumentException("Expected array length of 2, got " +  dest.length); }
        if (boundingBoxAbsoluteDirty) { updateBoundingBoxAbsolute(); }
        dest[0].set(boundingBoxAbsolute[0]);
        dest[1].set(boundingBoxAbsolute[1]);
        return dest;
    }

    public Vector3d getAxis(int index) { return getAxis(index, new Vector3d()); }

    public Vector3d getAxis(int index, Vector3d dest) {
        if (rotationMatrixDirty) { updateRotationMatrix(); }
        return rotationMatrix.getColumn(index, dest);
    }





    // Setters
    public void setInternalPos(Vector3d position) {
        pos.set(position);

        cornerPosAbsoluteDirty = true;
        boundingBoxAbsoluteDirty = true;
    }

    public void setInternalPos(Vec3d position) { setInternalPos(new Vector3d(position.x, position.y, position.z)); }

    public void setInverseMass(double inverseMass) throws IllegalArgumentException {
        if (inverseMass < 0) { throw new IllegalArgumentException("Inverse mass must not be negative"); }
        if (Double.isInfinite(inverseMass) || Double.isNaN(inverseMass)) { throw new IllegalArgumentException("Inverse mass must not be infinite or NaN"); }
        this.inverseMass = inverseMass;

        inverseInertiaTensorLocalDirty = true;
        inverseInertiaTensorWorldDirty = true;
    }

    public void setLinearVelocity(Vector3d linearVelocity) { linearVelocity.set(linearVelocity); }

    public void setAngularVelocity(Vector3d angularVelocity) { angularVelocity.set(angularVelocity); }

    public void setOrientation(Quaterniond orientation) {
        this.orientation.set(orientation);
        this.orientation.normalize();

        rotationMatrixDirty = true;
        inverseInertiaTensorWorldDirty = true;
        cornerPosRelativeDirty = true;
        cornerPosAbsoluteDirty = true;
    }

    public void setScale(Vector3d scale) throws IllegalArgumentException {
        if (scale.x < 0 || scale.y < 0 || scale.z < 0) { throw new IllegalArgumentException("Scale must not be negative"); }
        this.scale.set(scale);

        inverseInertiaTensorLocalDirty = true;
        inverseInertiaTensorWorldDirty = true;
        cornerPosLocalDirty = true;
        cornerPosRelativeDirty = true;
        cornerPosAbsoluteDirty = true;
        boundingBoxAbsoluteDirty = true;
    }

    public void setFrictionCoefficient(double frictionCoefficient) throws IllegalArgumentException {
        if (frictionCoefficient < 0 || frictionCoefficient > 1) { throw new IllegalArgumentException("Friction coefficient must be between 0 and 1"); }
        this.frictionCoefficient = frictionCoefficient;
    }

    public void setRestitutionCoefficient(double restitutionCoefficient) throws IllegalArgumentException {
        if (restitutionCoefficient < 0 || restitutionCoefficient > 1) { throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1"); }
        this.restitutionCoefficient = restitutionCoefficient;
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
    protected void readCustomData(ReadView view) { // All required instance variables need to be set or marked as dirty after this. Otherwise, it will be null after unloading and loading again, causing all sorts of calculation bugs.
        // super.readCustomData(view); <- Ignore for the same reason as in "writeCustomData"
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

        markEverythingDirty();
        updateVisuals();
    }





    // Update derived data
    private void updateRotationMatrix() {
        orientation.get(rotationMatrix);
        rotationMatrix.transpose(rotationMatrixTranspose);

        rotationMatrixDirty = false;
    }

    private void updateInverseInertiaTensorLocal() {
        inverseInertiaTensorLocal.m00 = (12 * inverseMass) / (scale.y * scale.y + scale.z * scale.z);
        inverseInertiaTensorLocal.m11 = (12 * inverseMass) / (scale.x * scale.x + scale.z * scale.z);
        inverseInertiaTensorLocal.m22 = (12 * inverseMass) / (scale.x * scale.x + scale.y * scale.y);

        inverseInertiaTensorLocalDirty = false;
    }

    private void updateInverseInertiaTensorWorld() {
        if (inverseInertiaTensorLocalDirty) { updateInverseInertiaTensorLocal(); }
        rotationMatrix.mul(inverseInertiaTensorLocal, inverseInertiaTensorWorld).mul(rotationMatrixTranspose, inverseInertiaTensorWorld);

        inverseInertiaTensorWorldDirty = false;
    }

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

        cornerPosLocalDirty = false;
    }

    private void updateCornerPosRelative() {
        if (rotationMatrixDirty) { updateRotationMatrix(); }
        if (cornerPosLocalDirty) { updateCornerPosLocal(); }
        for (int i = 0; i < 8; i++) {
            rotationMatrix.transform(cornerPosLocal[i], cornerPosRelative[i]);
        }

        cornerPosRelativeDirty = false;
    }

    private void updateCornerPosAbsolute() {
        if (cornerPosRelativeDirty) { updateCornerPosRelative(); }
        for (int i = 0; i < 8; i++) {
            cornerPosRelative[i].add(pos, cornerPosAbsolute[i]);
        }

        cornerPosAbsoluteDirty = false;
    }

    private void updateBoundingBoxAbsolute() {
        if (cornerPosAbsoluteDirty) { updateCornerPosAbsolute(); }
        for (Vector3d corner : cornerPosAbsolute) {
            boundingBoxAbsolute[0].x = Math.min(boundingBoxAbsolute[0].x, corner.x);
            boundingBoxAbsolute[1].x = Math.max(boundingBoxAbsolute[1].x, corner.x);

            boundingBoxAbsolute[0].y = Math.min(boundingBoxAbsolute[0].y, corner.y);
            boundingBoxAbsolute[1].y = Math.max(boundingBoxAbsolute[1].y, corner.y);

            boundingBoxAbsolute[0].z = Math.min(boundingBoxAbsolute[0].z, corner.z);
            boundingBoxAbsolute[1].z = Math.max(boundingBoxAbsolute[1].z, corner.z);
        }

        boundingBoxAbsoluteDirty = false;
    }

    public void updateLinearVelocityWithoutAcceleration(Vector3d finalVelocity, Vector3d acceleration) { // It's important that it still includes the damping on the entire velocity (including on the acceleration)
        finalVelocity.sub(acceleration, linearVelocityWithoutAcceleration); // TODO: See note on updateAngularVelocity in integration
    }

    public void updateAngularVelocityWithoutAcceleration(Vector3d finalVelocity, Vector3d acceleration) {
        finalVelocity.sub(acceleration, angularVelocityWithoutAcceleration); // TODO: See note on updateAngularVelocity in integration
    }





    // Other methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    private void markEverythingDirty() {
        rotationMatrixDirty = true;
        inverseInertiaTensorLocalDirty = true;
        inverseInertiaTensorWorldDirty = true;
        cornerPosLocalDirty = true;
        cornerPosRelativeDirty = true;
        cornerPosAbsoluteDirty = true;
        boundingBoxAbsoluteDirty = true;
    }

    public void updateVisuals() {
        setStartInterpolation(0);
        setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(orientation), new Vector3f(scale), new Quaternionf())); // Although I calculate everything in doubles, the rendering uses floats because that's how item displays (and Minecraft's rendering in general) work
    }

    public void updateEntityPos() { // This isn't in updateVisuals because updating the entity pos in readCustomData (where entityPos is potentially still 0,0,0) would cause the object to teleport to 0,0,0 without additional code
        setPos(pos.x, pos.y, pos.z); // The vanilla method for entity position
        lastEntityPos = getEntityPos();
    }

    // Debugging
    //public HashMap<PhysicsObject, ArrayList<Contact>> getObjectContactsDebug() { // Currently only used for debugging because it would give direct access to the data instead of copying by value
    //    return objectContacts;
    //}

    // TODO (VERY IMPORTANT): Check whether it's more stable to calculate contactVelocity with "(velocityBeforeIntegration + acceleration) * dampingMultiplier) - acceleration" or "velocityBeforeIntegration * dampingMultiplier" (or maybe even "velocityBeforeIntegration"). Currently I use the former.
    // TODO: Maybe change the tick order (Apparently, modern engines use a different one? Could this improve stability?) => Experiment with different approaches in the mod to see what's best, then implement that in the datapack
    // TODO: Check if any other improvements can be made with modern approaches (post Ian Millington's book)

}
