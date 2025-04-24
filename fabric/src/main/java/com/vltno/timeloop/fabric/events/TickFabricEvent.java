package com.vltno.timeloop.fabric.events;

import com.vltno.timeloop.events.TickEvent;
import net.minecraft.server.MinecraftServer;

public class TickFabricEvent {
    public static void onEndServerTick(MinecraftServer server) {
        TickEvent.onEndServerTick();
    }
}
