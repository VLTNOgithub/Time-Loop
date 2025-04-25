package com.vltno.timeloop.neoforge.events;

import com.vltno.timeloop.events.PlayerEvent;

public class PlayerNeoForgeEvent {
    public static void afterRespawn() {
        PlayerEvent.afterRespawn();
    }
}
