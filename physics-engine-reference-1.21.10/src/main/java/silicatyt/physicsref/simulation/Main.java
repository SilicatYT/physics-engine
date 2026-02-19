package silicatyt.physicsref.simulation;

import net.minecraft.server.MinecraftServer;

public class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;

    public static void physicsTick(MinecraftServer server) {
        Integration.phaseOne();
        CollisionDetection.start();
        ContactResolution.resolve();
        Integration.phaseTwo();

        // DEBUG
        Debug.showObjectAxes();
        Debug.showContactPoint();
        Debug.showContactNormal();

    }
}
