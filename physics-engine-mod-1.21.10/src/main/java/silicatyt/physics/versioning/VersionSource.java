package silicatyt.physics.versioning;

public interface VersionSource {
    long getVersion();
    void updateIfNeeded();
}
