package com.vltno.timeloop;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Optional;

public class TimeLoopFabric implements ModInitializer, TimeLoopLoaderInterface
{
    private static final FabricLoader FABRIC_LOADER = FabricLoader.getInstance();
    public static final boolean isDedicatedServer = FABRIC_LOADER.getEnvironmentType() == EnvType.SERVER;

    @Override public void onInitialize()
    {
        TimeLoop.init(isDedicatedServer, this);
    }

    @Override public String getLoaderName()
    {
        return "Fabric";
    }

    @Override public String getModVersion()
    {
        Optional<ModContainer> modContainer = FABRIC_LOADER.getModContainer("time-loop");
        return modContainer.isPresent() ? modContainer.get().getMetadata().getVersion().getFriendlyString() : "[unknown]";
    }
}
