package com.vltno.timeloop.neoforge.events;

import com.vltno.timeloop.events.EntitySleepEvent;
import net.minecraft.world.entity.LivingEntity;

public class EntitySleepNeoForgeEvent {
    public static void onStopSleeping(LivingEntity entity) {
        EntitySleepEvent.onStopSleeping(entity);
    }
}
