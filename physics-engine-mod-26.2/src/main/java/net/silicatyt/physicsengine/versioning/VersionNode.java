package net.silicatyt.physicsengine.versioning;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static net.silicatyt.physicsengine.simulation.Main.DISABLE_LAZY_UPDATES;

public final class VersionNode implements VersionSource {
    private long version = 0;
    private final Runnable update;
    private final List<DependencyNode> dependencies = new ArrayList<>(); // TODO: There are many dependency checks throughout the logic, each one allocating an iterator object. Reduce the number of checks where possible (for example, getting a different single element of an array 3x, such as getHalfExtentAxisProjections(i), runs the dependency checks 3x as well). Each one uses 2 enhanced for-loops, which creates 2 iterator objects. I could change to an indexed loop, but the checks themselves are more meaningful.

    public VersionNode(Runnable update) { this.update = update; }

    @Override
    public long getVersion() { return version; }

    @Override
    public void updateIfNeeded() {
        if (DISABLE_LAZY_UPDATES) return;
        boolean isDirty = false;

        for (DependencyNode dependency : dependencies) {
            dependency.source.updateIfNeeded();
            if (dependency.hasChanged()) {
                isDirty = true;
                dependency.markSeen();
            }
        }

        if (isDirty) {
            update.run();
            increment();
        }
    }

    public void addDependencies(VersionSource source, VersionSource... sources) throws IllegalArgumentException {
        if (source == null) throw new IllegalArgumentException("Dependency sources cannot be null.");
        for (VersionSource s : sources) {
            if (s == null) throw new IllegalArgumentException("Dependency sources cannot be null.");
        }

        dependencies.add(new DependencyNode(source));
        for (VersionSource s : sources) { dependencies.add(new DependencyNode(s)); }
    }

    public void increment() { version++; }
}