package net.silicatyt.physicsengine.versioning;

import java.util.LinkedList;
import java.util.List;

public final class VersionNode implements VersionSource {
    private long version = 0;
    private final Runnable update;
    private final List<DependencyNode> dependencies = new LinkedList<>();

    public VersionNode(Runnable update) { this.update = update; }

    @Override
    public long getVersion() { return version; }

    @Override
    public void updateIfNeeded() {
        boolean isDirty = false;

        for (DependencyNode dependency : dependencies) {
            dependency.source.updateIfNeeded();
            if (dependency.hasChanged()) isDirty = true;
        }

        if (isDirty) {
            update.run();
            increment();
        }

        for (DependencyNode dependency : dependencies) dependency.markSeen(); // I could merge hasChanged and markSeen to get rid of this loop, but it might be good to have separation there
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