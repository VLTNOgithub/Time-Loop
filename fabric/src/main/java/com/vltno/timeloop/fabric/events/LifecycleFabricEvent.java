package com.vltno.timeloop.fabric.events;

import com.vltno.timeloop.events.LifecycleEvent;
import net.minecraft.server.MinecraftServer;

public class LifecycleFabricEvent {
    public static void onServerStart(MinecraftServer server) {
        LifecycleEvent.onServerStart(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        LifecycleEvent.onServerStopping();
    }
}
