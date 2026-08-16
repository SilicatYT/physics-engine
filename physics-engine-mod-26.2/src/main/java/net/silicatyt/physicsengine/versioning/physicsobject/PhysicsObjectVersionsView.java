package net.silicatyt.physicsengine.versioning.physicsobject;

import net.silicatyt.physicsengine.versioning.VersionSource;

public interface PhysicsObjectVersionsView { // Necessary so classes that don't see the original 'PhysicsObjectVersions' object cannot call increment() on the versions.
    // A VersionSource for every versioned field that can be depended on from outside the class
    public VersionSource inverseMass();
    public VersionSource linearVelocity();
    public VersionSource angularVelocity();
    public VersionSource orientation();
    public VersionSource scale();
    public VersionSource frictionCoefficient();
    public VersionSource restitutionCoefficient();

    public VersionSource inverseInertiaTensorWorld();
    public VersionSource axes();

    public VersionSource internalPos();
}
