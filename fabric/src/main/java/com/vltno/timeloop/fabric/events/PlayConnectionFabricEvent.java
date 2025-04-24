package com.vltno.timeloop.fabric.events;

import com.vltno.timeloop.events.LifecycleEvent;
import com.vltno.timeloop.events.PlayConnectionEvent;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class PlayConnectionFabricEvent {
    public static void onJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        PlayConnectionEvent.onJoin(handler, server);
    }

    public static void onDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        PlayConnectionEvent.onDisconnect(handler);
    }
}
