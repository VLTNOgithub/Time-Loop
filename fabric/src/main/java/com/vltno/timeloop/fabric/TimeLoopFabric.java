package com.vltno.timeloop.fabric;

import com.vltno.timeloop.*;
import com.vltno.timeloop.fabric.events.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Optional;

public class TimeLoopFabric extends TimeLoop implements ModInitializer, TimeLoopLoaderInterface {
    public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TimeLoop");

    public static final boolean isDedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    
    @Override
    public void onInitialize() {
        LOOP_LOGGER.info("Initializing TimeLoop mod (Fabric)");
        
        TimeLoop.init(isDedicatedServer, this);
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Only register commands on the logical server
            if (environment.dedicated || environment.integrated) {
                TimeLoopCommand.register(dispatcher);
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register(EntitySleepFabricEvent::onStopSleeping);
        
        ServerLifecycleEvents.SERVER_STARTED.register(LifecycleFabricEvent::onServerStart);
        
        ServerLifecycleEvents.SERVER_STOPPING.register(LifecycleFabricEvent::onServerStopping);
        
        ServerPlayConnectionEvents.JOIN.register(PlayConnectionFabricEvent::onJoin);

        ServerPlayConnectionEvents.DISCONNECT.register(PlayConnectionFabricEvent::onDisconnect);

        ServerPlayerEvents.AFTER_RESPAWN.register(PlayerFabricEvent::afterRespawn);

        ServerTickEvents.END_SERVER_TICK.register(TickFabricEvent::onEndServerTick);

        LOOP_LOGGER.info("TimeLoop mod initialized successfully (Fabric)");
    }

    @Override public String getLoaderName()
    {
        return "Fabric";
    }

    @Override public String getModVersion()
    {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("time-loop");
        return modContainer.isPresent() ? modContainer.get().getMetadata().getVersion().getFriendlyString() : "[unknown]";
    }
}