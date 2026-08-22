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
    public final VersionNode axes;
    public final VersionNode halfExtentAxisProjections;
    public final VersionNode aabbRelative;
    public final VersionNode cornerPosRelative;

    public final VersionNode internalPos = new VersionNode(() -> {});


    public PhysicsObjectVersions(
            Runnable updateRotationMatrix,
            Runnable updateInverseInertiaTensorLocal,
            Runnable updateInverseInertiaTensorWorld,
            Runnable updateAxes,
            Runnable updateHalfExtentAxisProjections,
            Runnable updateAabbRelative,
            Runnable updateCornerPosRelative
    ) {
        rotationMatrix = new VersionNode(updateRotationMatrix);
        inverseInertiaTensorLocal = new VersionNode(updateInverseInertiaTensorLocal);
        inverseInertiaTensorWorld = new VersionNode(updateInverseInertiaTensorWorld);
        axes = new VersionNode(updateAxes);
        halfExtentAxisProjections = new VersionNode(updateHalfExtentAxisProjections);
        aabbRelative = new VersionNode(updateAabbRelative); // aabbRelativeMin & aabbRelativeMax
        cornerPosRelative = new VersionNode(updateCornerPosRelative);

        // Dependencies
        rotationMatrix.addDependencies(orientation);
        inverseInertiaTensorLocal.addDependencies(inverseMass, scale);
        inverseInertiaTensorWorld.addDependencies(rotationMatrix, inverseInertiaTensorLocal);
        axes.addDependencies(rotationMatrix);
        halfExtentAxisProjections.addDependencies(axes, scale);
        aabbRelative.addDependencies(halfExtentAxisProjections);
        cornerPosRelative.addDependencies(halfExtentAxisProjections);
    }


    @Override public VersionSource inverseMass() { return inverseMass; }
    @Override public VersionSource linearVelocity() { return linearVelocity; }
    @Override public VersionSource angularVelocity() { return angularVelocity; }
    @Override public VersionSource orientation() { return orientation; }
    @Override public VersionSource scale() { return scale; }
    @Override public VersionSource frictionCoefficient() { return frictionCoefficient; }
    @Override public VersionSource restitutionCoefficient() { return restitutionCoefficient; }

    @Override public VersionSource inverseInertiaTensorWorld() { return inverseInertiaTensorWorld; }
    @Override public VersionSource axes() { return axes; }

    @Override public VersionSource internalPos() { return internalPos; }
}
