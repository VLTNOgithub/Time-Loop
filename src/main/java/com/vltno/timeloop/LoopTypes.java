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

    public static LoopTypes fromString(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Input for LoopType cannot be null or empty");
        }
        try {
            return LoopTypes.valueOf(name.toUpperCase()); // Case-insensitive matching
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid LoopType: " + name); // Clear error handling
        }
    }

}
