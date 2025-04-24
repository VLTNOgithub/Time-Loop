package com.vltno.timeloop.fabric.events;

import net.minecraft.entity.LivingEntity;
import com.vltno.timeloop.events.EntitySleepEvent;
import net.minecraft.util.math.BlockPos;

public class EntitySleepFabricEvent {
    public static void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
        EntitySleepEvent.onStopSleeping(entity);
    }
}
