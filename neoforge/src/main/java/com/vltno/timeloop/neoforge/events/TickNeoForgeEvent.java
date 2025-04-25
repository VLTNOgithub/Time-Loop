package com.vltno.timeloop.neoforge.events;

import com.vltno.timeloop.events.TickEvent;
import net.minecraft.server.MinecraftServer;

public class TickNeoForgeEvent {
    public static void onEndServerTick(MinecraftServer server) {
        TickEvent.onEndServerTick();
    }
}
