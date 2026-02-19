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
import silicatyt.physicsref.data.Contact;
import xyz.nucleoid.packettweaker.PacketContext;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO: Important notes or things to rethink at some point
// Note: All doubles could be NaN, Infinity or -Infinity, but I don't check for that everywhere. I don't have a consequent rule for that. I could check for isFinite() on the vectors and quaternions (on quaternions I could additionally check if it's all 0; I can't normalize that), but there's not really a point because this isn't meant to be an official well-made mod. Just a reference.
// Note: addXYZ and setXYZ also update dependent data like the rotationMatrices or the inverseInertiaTensors automatically. Calling add and set repeatedly therefore causes unnecessary work for the CPU. But it's most readable like this, so I won't bother fixing it for now. This is just a reference mod after all, not meant to be optimized. BUT a potential idea would be: Mark rotationMatrix and stuff as "dirty", and in "getRotationMatrix" accesses I check if it's dirty. If yes, update.
// Note: In Integration, I have this: "obj.addLinearVelocity(obj.getAccumulatedForce().mul(obj.getInverseMass()));". The "getAccumulatedForce()" creates a new object. Is there a good way around that? Probably not without giving direct access, which would probably be bad.
// Note: I made the decision to only write a new getter or setter method if I need it. Not write all of them at once for "consistency". That would create bloat and waste too much time. I can't know when I need a Vec3d as an input, or when I need a Vector3d or even 3 individual doubles.
// Note: Currently, all getters return a new object in order to avoid giving access to the object directly (which would circumvent restrictions when setting the contents, like negative scale). And all setters keep the same object reference and do not re-assign. Additionally, that's the reason I use ".set()" instead of "=" for assignments.
// Note: All derived data like cornerPosRelative always update when the data they depend on (pos, scale, orientation) changes, even if the data isn't going to be used after that point. This adds unnecessary overhead (which I will avoid in the datapack), but updating everything automatically makes it more readable and safer. So for a non-commercial reference mod, that's fine.
// Note: When an entity unloads and loads back in, a new instance is created, which is why the instance variables are never null if I initialize them in the constructor or directly in the class. No helper method needed that's run on readCustomData().
// Note: An "updateXYZ()" method should NOT implicitly update another variable to avoid hiding operations and having something "accidentally work". Always explicitly declare what variables depend on the current variable.
// Note: Not everything is perfectly optimized (at all). It just needs to work and be readable. This isn't an engine that's heavily optimized; I just need it for reference and to learn some Java. See the old physics engine datapack for the optimizations I can make.

// TODO: Make the current contact generation and collision detection code cleaner (incl. maybe making contacts public, so that I don't have to have a dedicated method for getting previous contacts and clearing the current contacts?)

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
    private final Matrix3d inverseInertiaTensorLocal = new Matrix3d();
    private final Matrix3d inverseInertiaTensorWorld = new Matrix3d();
    private final Matrix3d rotationMatrix = new Matrix3d();
    private final Matrix3d rotationMatrixTranspose = new Matrix3d(); // To avoid duplicate calculations when calling ".transpose()"
    private final Vector3d[] cornerPosLocal = new Vector3d[8]; // Only used to avoid unnecessary calculations every time cornerPosRelative updates due to orientation changes
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
    private HashMap<PhysicsObject, ArrayList<Contact>> objectContacts = new HashMap<>();
    public boolean isChecked = false;
    public Contact lastContact; // TODO: There may be a better way to achieve the "Don't accumulate contacts with the same feature pair" thing than this. It feels wrong, but I've made decisions like these several times here. I just need to get it working, I don't care about performance or clean code as long as I can finish this mod as a reference for the eventual datapack. It's also public, but honestly this code is already pretty messy.





    // Constructor
    public PhysicsObject(EntityType<?> type, World world) {
        super(type, world);

        // Set default values (pos is taken care of in integrationPhaseOne)
        this.setInterpolationDuration(1);
        this.setTeleportDuration(1);

        this.inverseMass = DEFAULT_INVERSE_MASS;
        this.scale.set(DEFAULT_SCALE);
        this.frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
        this.restitutionCoefficient = DEFAULT_RESTITUTION_COEFFICIENT;

        for (int i = 0; i < 8; i++) {
            this.cornerPosLocal[i] = new Vector3d();
            this.cornerPosRelative[i] = new Vector3d();
            this.cornerPosAbsolute[i] = new Vector3d();
        }

        this.updateDerivedObjectData();
    }





    // Getters
    public Vector3d getInternalPos() {
        return new Vector3d(this.pos);
    }

    public Vec3d getLastEntityPos() {
        return this.lastEntityPos;
    }

    public double getInverseMass() {
        return this.inverseMass;
    }

    public Vector3d getLinearVelocity() {
        return new Vector3d(this.linearVelocity);
    }

    public Vector3d getAngularVelocity() {
        return new Vector3d(this.angularVelocity);
    }

    public Quaterniond getOrientation() {
        return new Quaterniond(this.orientation);
    }

    public Vector3d getScale() {
        return new Vector3d(this.scale);
    }

    public double getFrictionCoefficient() {
        return this.frictionCoefficient;
    }

    public double getRestitutionCoefficient() {
        return this.restitutionCoefficient;
    }

    public Matrix3d getInverseInertiaTensorWorld() {
        return new Matrix3d(this.inverseInertiaTensorWorld);
    }

    public Vector3d[] getCornerPosRelative() {
        return this.cornerPosRelative.clone();
    }

    public Vector3d[] getCornerPosAbsolute() {
        return this.cornerPosAbsolute.clone();
    }

    public Vector3d[] getBoundingBoxAbsolute() {
        return new Vector3d[]{new Vector3d(this.boundingBoxAbsolute[0]), new Vector3d(this.boundingBoxAbsolute[1])};
    }

    public Vector3d getAxis(int axisIndex) { // axisIndex: 0 - 2
        Vector3d out = new Vector3d();
        this.rotationMatrix.getColumn(axisIndex, out);
        return out;
    }

    public Vector3d getLinearVelocityWithoutAcceleration() {
        return new Vector3d(this.linearVelocityWithoutAcceleration);
    }

    public Vector3d getAngularVelocityWithoutAcceleration() {
        return new Vector3d(this.angularVelocityWithoutAcceleration);
    }





    // Setters
    public void setInternalPos(Vec3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.pos.x = value.x;
        this.pos.y = value.y;
        this.pos.z = value.z;

        this.updateCornerPosAbsolute();
        this.updateBoundingBoxAbsolute();
    }

    public void setInverseMass(double value) throws IllegalArgumentException {
        if (value < 0) {
            throw new IllegalArgumentException("Mass must not be negative");
        }
        this.inverseMass = value;
        this.updateInertiaTensorLocal();
        this.updateInertiaTensorWorld();
    }

    public void setLinearVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.linearVelocity.set(value);
    }

    public void setAngularVelocity(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.angularVelocity.set(value);
    }

    public void setOrientation(Quaterniond value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Quaternion must not be null");
        }
        this.orientation.set(value);
        this.orientation.normalize();

        this.updateRotationMatrix();
        this.updateInertiaTensorWorld(); // Requires the updated rotation matrix
        this.updateCornerPosRelative();
        this.updateCornerPosAbsolute();
    }

    public void setScale(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        if (value.x < 0 || value.y < 0 || value.z < 0) {
            throw new IllegalArgumentException("Scale must not be negative");
        }
        this.scale.set(value);
        this.updateInertiaTensorLocal();
        this.updateInertiaTensorWorld();
        this.updateCornerPosLocal();
        this.updateCornerPosRelative();
        this.updateCornerPosAbsolute();
        this.updateBoundingBoxAbsolute();
    }

    public void setFrictionCoefficient(double value) throws IllegalArgumentException {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Friction coefficient must be between 0 and 1");
        }
        this.frictionCoefficient = value;
    }

    public void setRestitutionCoefficient(double value) throws IllegalArgumentException {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Restitution coefficient must be between 0 and 1");
        }
        this.restitutionCoefficient = value;
    }





    // Other operations (Similar to setters)
    public void addInternalPos(Vector3d value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        this.pos.add(value);
        this.updateCornerPosAbsolute();
        this.updateBoundingBoxAbsolute();
    }

    public void addObjectContact(PhysicsObject otherObject, Contact contact) { // TODO: Right now, I have to specify the object every time I want to add a contact. Maybe make it so I can get the list myself and then add/remove freely (without getters or setters). Maybe do it like "getObjectContacts(otherObject)" and then I can add/remove manually. It would return null if none exists. So I'd need addObjectContactsList or something like that.
        ArrayList<Contact> contacts = this.objectContacts.computeIfAbsent(otherObject, k -> new ArrayList<>());
        contacts.add(contact);
    }

    public HashMap<PhysicsObject, ArrayList<Contact>> clearObjectContacts() { // Returns the current contacts list as the "previous" and creates a new object for the current tick. It's on purpose that "previous" is returned directly without copying it.
        HashMap<PhysicsObject, ArrayList<Contact>> prev = this.objectContacts; // TODO: Split it up so that I explicitly "getPreviousContacts" and "clearObjectContacts". Maybe I should really give direct access to the contacts list
        this.objectContacts = new HashMap<>();
        return prev;
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
    protected void readCustomData(ReadView view) { // All required instance variables need to be set after this, so I need to also calculate the rotationMatrix from the orientation for example. Otherwise, it will be null after unloading and loading again, causing all sorts of calculation bugs.
        // super.readCustomData(view);
        Codec<List<Double>> doubleListCodec = Codec.DOUBLE.listOf();

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

        this.updateDerivedObjectData();
        this.updateVisuals();
    }





    // Update derived data
    private void updateRotationMatrix() {
        this.orientation.get(this.rotationMatrix);
        this.rotationMatrix.transpose(this.rotationMatrixTranspose);
    }

    private void updateInertiaTensorLocal() {
        this.inverseInertiaTensorLocal.m00 = (12 * this.inverseMass) / (this.scale.y * this.scale.y + this.scale.z * this.scale.z);
        this.inverseInertiaTensorLocal.m11 = (12 * this.inverseMass) / (this.scale.x * this.scale.x + this.scale.z * this.scale.z);
        this.inverseInertiaTensorLocal.m22 = (12 * this.inverseMass) / (this.scale.x * this.scale.x + this.scale.y * this.scale.y);
    }

    private void updateInertiaTensorWorld() {
        this.rotationMatrix.mul(this.inverseInertiaTensorLocal, this.inverseInertiaTensorWorld).mul(this.rotationMatrixTranspose, this.inverseInertiaTensorWorld); // Uses the return value of "mul". Very important that it doesn't overwrite inverseInertiaTensorLocal or the rotationMatrix! If I do "x.mul(y)" without specifying a destination, x is overwritten.
    }

    private void updateCornerPosLocal() {
        int i = 0;
        for (int x = -1; x <= 1; x += 2) { // x, y and z are either -1 or 1
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    this.cornerPosLocal[i++].set(
                            0.5 * x * this.scale.x,
                            0.5 * y * this.scale.y,
                            0.5 * z * this.scale.z
                    );
                }
            }
        }
    }

    private void updateCornerPosRelative() {
        for (int i = 0; i < 8; i++) {
            this.rotationMatrix.transform(this.cornerPosLocal[i], this.cornerPosRelative[i]);
        }
    }

    private void updateCornerPosAbsolute() {
        for (int i = 0; i < 8; i++) {
            this.cornerPosRelative[i].add(this.pos, this.cornerPosAbsolute[i]);
        }
    }

    private void updateBoundingBoxAbsolute() {
        for (Vector3d corner : this.cornerPosAbsolute) {
            this.boundingBoxAbsolute[0].x = Math.min(this.boundingBoxAbsolute[0].x, corner.x);
            this.boundingBoxAbsolute[1].x = Math.max(this.boundingBoxAbsolute[1].x, corner.x);

            this.boundingBoxAbsolute[0].y = Math.min(this.boundingBoxAbsolute[0].y, corner.y);
            this.boundingBoxAbsolute[1].y = Math.max(this.boundingBoxAbsolute[1].y, corner.y);

            this.boundingBoxAbsolute[0].z = Math.min(this.boundingBoxAbsolute[0].z, corner.z);
            this.boundingBoxAbsolute[1].z = Math.max(this.boundingBoxAbsolute[1].z, corner.z);
        }
    }

    public void updateLinearVelocityWithoutAcceleration(Vector3d finalVelocity, Vector3d acceleration) { // It's important that it still includes the damping on the entire velocity (including the acceleration)
        finalVelocity.sub(acceleration, this.linearVelocityWithoutAcceleration); // TODO: See note on updateAngularVelocity in integration
    }

    public void updateAngularVelocityWithoutAcceleration(Vector3d finalVelocity, Vector3d acceleration) {
        finalVelocity.sub(acceleration, this.angularVelocityWithoutAcceleration); // TODO: See note on updateAngularVelocity in integration
    }





    // Other methods
    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) { // To make the PhysicsObject render as a regular item display on the client
        return EntityType.ITEM_DISPLAY;
    }

    private void updateDerivedObjectData() {
        this.updateRotationMatrix();
        this.updateInertiaTensorLocal();
        this.updateInertiaTensorWorld();
        this.updateCornerPosLocal();
        this.updateCornerPosRelative();
        this.updateCornerPosAbsolute();
        this.updateBoundingBoxAbsolute();
    }

    public void updateVisuals() {
        this.setStartInterpolation(0);
        this.setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(this.orientation), new Vector3f(this.scale), new Quaternionf())); // Although I calculate everything in doubles, the rendering uses floats because that's how item displays (and Minecraft's rendering in general) work
    }

    public void updateEntityPos() { // This isn't in updateVisuals because updating the entity pos in readCustomData (where entityPos is potentially still 0,0,0) would cause the object to teleport to 0,0,0 without additional code
        this.setPos(this.pos.x, this.pos.y, this.pos.z); // The vanilla method for entity position
        this.lastEntityPos = this.getEntityPos();
    }

    // Debugging
    public HashMap<PhysicsObject, ArrayList<Contact>> getObjectContactsDebug() { // Currently only used for debugging because it would give direct access to the data instead of copying by value
        return this.objectContacts;
    }

    // TODO (VERY IMPORTANT): Check whether it's more stable to calculate contactVelocity with "(velocityBeforeIntegration + acceleration) * dampingMultiplier) - acceleration" or "velocityBeforeIntegration * dampingMultiplier" (or maybe even "velocityBeforeIntegration"). Currently I use the former.
    // TODO: Maybe change the tick order (Apparently, modern engines use a different one? Could this improve stability?) => Experiment with different approaches in the mod to see what's best, then implement that in the datapack
    // TODO: Check if any other improvements can be made with modern approaches (post Ian Millington's book)

}
