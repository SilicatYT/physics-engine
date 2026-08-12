package net.silicatyt.physicsengine.versioning;

public interface VersionSource {
    long getVersion();
    void updateIfNeeded();
}
