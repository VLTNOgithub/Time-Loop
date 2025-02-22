package com.vltno.timeloop;

import net.minecraft.util.StringIdentifiable;

public enum LoopTypes implements StringIdentifiable {
    TICKS,
    TIME_OF_DAY,
    SLEEP,
    DEATH;

    @Override
    public String asString() {
        return name();
    }
}
