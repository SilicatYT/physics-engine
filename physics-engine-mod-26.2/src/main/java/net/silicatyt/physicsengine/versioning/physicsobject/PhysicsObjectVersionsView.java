package net.silicatyt.physicsengine.versioning.physicsobject;

import net.silicatyt.physicsengine.versioning.VersionSource;

public interface PhysicsObjectVersionsView { // Necessary so classes that don't see the original 'PhysicsObjectVersions' object cannot call increment() on the versions.
    public VersionSource inverseMass();
    public VersionSource linearVelocity();
    public VersionSource angularVelocity();
    public VersionSource orientation();
    public VersionSource scale();
    public VersionSource frictionCoefficient();
    public VersionSource restitutionCoefficient();

    public VersionSource inverseInertiaTensorWorld();

    public VersionSource internalPos();
}
