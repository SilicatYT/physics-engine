package net.silicatyt.physicsengine.versioning.physicsobject;

import net.silicatyt.physicsengine.versioning.VersionNode;
import net.silicatyt.physicsengine.versioning.VersionSource;

public final class PhysicsObjectVersions implements PhysicsObjectVersionsView {
    // VersionNodes that are directly accessible via setters don't need an updater method. Only fields accessible with a getter need a VersionSource.
    public final VersionNode inverseMass = new VersionNode(() -> {});
    public final VersionNode linearVelocity = new VersionNode(() -> {});
    public final VersionNode angularVelocity = new VersionNode(() -> {});
    public final VersionNode orientation = new VersionNode(() -> {});
    public final VersionNode scale = new VersionNode(() -> {});
    public final VersionNode frictionCoefficient = new VersionNode(() -> {});
    public final VersionNode restitutionCoefficient = new VersionNode(() -> {});

    public final VersionNode rotationMatrix;
    public final VersionNode inverseInertiaTensorLocal;
    public final VersionNode inverseInertiaTensorWorld;

    public final VersionNode internalPos = new VersionNode(() -> {});


    public PhysicsObjectVersions(
            Runnable updateRotationMatrix,
            Runnable updateInverseInertiaTensorLocal,
            Runnable updateInverseInertiaTensorWorld
    ) {
        rotationMatrix = new VersionNode(updateRotationMatrix);
        inverseInertiaTensorLocal = new VersionNode(updateInverseInertiaTensorLocal);
        inverseInertiaTensorWorld = new VersionNode(updateInverseInertiaTensorWorld);

        // Dependencies
        rotationMatrix.addDependencies(orientation);
        inverseInertiaTensorLocal.addDependencies(inverseMass, scale);
        inverseInertiaTensorWorld.addDependencies(rotationMatrix, inverseInertiaTensorLocal);
    }


    @Override public VersionSource inverseMass() { return inverseMass; }
    @Override public VersionSource linearVelocity() { return linearVelocity; }
    @Override public VersionSource angularVelocity() { return angularVelocity; }
    @Override public VersionSource orientation() { return orientation; }
    @Override public VersionSource scale() { return scale; }
    @Override public VersionSource frictionCoefficient() { return frictionCoefficient; }
    @Override public VersionSource restitutionCoefficient() { return restitutionCoefficient; }

    @Override public VersionSource inverseInertiaTensorWorld() { return inverseInertiaTensorWorld; }

    @Override public VersionSource internalPos() { return internalPos; }
}
