package silicatyt.physicsref.simulation;

import net.minecraft.server.MinecraftServer;

public class Main {
    public static final double DELTA_TIME = 1.0 / 20.0;

    public static void physicsTick(MinecraftServer server) {
        Integration.phaseOne();
        CollisionDetection.start(server); // TODO: Runs ContactGeneration later. Or should I split it up?
        ContactResolution.resolve();
        Integration.phaseTwo();
    }
}
