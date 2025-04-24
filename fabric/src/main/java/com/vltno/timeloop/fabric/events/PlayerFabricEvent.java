package com.vltno.timeloop.fabric.events;

import net.minecraft.server.network.ServerPlayerEntity;
import com.vltno.timeloop.events.PlayerEvent;

public class PlayerFabricEvent {
    public static void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        PlayerEvent.afterRespawn();
    }
}
