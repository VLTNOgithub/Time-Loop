package com.vltno.timeloop.neoforge;

import com.vltno.timeloop.TimeLoop;
import com.vltno.timeloop.TimeLoopLoaderInterface;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

@Mod("timeloop")
public class TimeLoopNeoForge extends TimeLoop implements TimeLoopLoaderInterface {
    public static final boolean isDedicatedServer = FMLEnvironment.dist.isDedicatedServer();
    private final @Nullable ModContainer modContainer;
    
    public TimeLoopNeoForge(IEventBus eventBus) {
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        this.modContainer = modContainer.getModId().equals("minecraft") ? null : modContainer;
        TimeLoop.init(isDedicatedServer, this);
    }
    
    @Override public String getLoaderName()
    {
        return "NeoForge";
    }

    @Override public String getModVersion()
    {
        return modContainer != null ? modContainer.getModInfo().getVersion().toString() : "[unknown]";
    }
}
