package com.vltno.timeloop.neoforge.events;

import com.vltno.timeloop.events.LifecycleEvent;
import net.minecraft.server.MinecraftServer;

public class LifecycleNeoForgeEvent {
    public static void onServerStart(MinecraftServer server) {
        LifecycleEvent.onServerStart(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        LifecycleEvent.onServerStopping();
    }
}
