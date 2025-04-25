package com.vltno.timeloop;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum LoopTypes implements StringRepresentable {
    TICKS,
    TIME_OF_DAY,
    SLEEP,
    DEATH;

    @Override
    public @NotNull String getSerializedName() {
        return name();
    }
}