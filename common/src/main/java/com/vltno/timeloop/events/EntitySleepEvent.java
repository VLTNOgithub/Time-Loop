package com.vltno.timeloop.events;

import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.entity.LivingEntity;

public class EntitySleepEvent {
    public static void onStopSleeping(LivingEntity entity) {
        if (entity.isPlayer() && (TimeLoop.loopType == LoopTypes.SLEEP) ) {
            TimeLoop.LOOP_LOGGER.info("Player slept, looping.");
            TimeLoop.runLoopIteration();
        }
    }
}
