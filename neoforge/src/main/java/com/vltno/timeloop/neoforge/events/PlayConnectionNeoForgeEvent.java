package com.vltno.timeloop.neoforge.events;

import com.vltno.timeloop.events.PlayConnectionEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class PlayConnectionNeoForgeEvent {
    public static void onJoin(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        PlayConnectionEvent.onJoin(handler, server);
    }

    public static void onDisconnect(ServerGamePacketListenerImpl handler) {
        PlayConnectionEvent.onDisconnect(handler);
    }
}
