package com.vltno.timeloop.fabric.events;

import net.minecraft.server.level.ServerPlayer;
import com.vltno.timeloop.events.PlayerEvent;

public class PlayerFabricEvent {
    public static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        PlayerEvent.afterRespawn();
    }
}
