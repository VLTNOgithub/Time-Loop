package com.vltno.timeloop.neoforge;

import net.neoforged.fml.common.Mod;

import com.vltno.timeloop.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
