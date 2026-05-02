package silicatyt.physics.versioning;

public final class DependencyNode {
    public final VersionSource source;
    private long lastSeenVersion;

    public DependencyNode(VersionSource source, boolean isUpToDate) {
        this.source = source;
        this.lastSeenVersion = isUpToDate ? source.getVersion() : -1;
    }

    public DependencyNode(VersionSource source) { this(source, false); }

    public boolean hasChanged() { return source.getVersion() != lastSeenVersion; }

    public void markSeen() { lastSeenVersion = source.getVersion(); }
}
