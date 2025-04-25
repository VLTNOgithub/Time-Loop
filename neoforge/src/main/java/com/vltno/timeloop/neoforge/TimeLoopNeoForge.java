package com.vltno.timeloop.neoforge; // Changed package

import com.vltno.timeloop.LoopCommands;
import com.vltno.timeloop.TimeLoop;
import com.vltno.timeloop.neoforge.events.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod("timeloop")
public class TimeLoopNeoForge {

    public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TimeLoop");
    
    public static final boolean isLogicalServer = !FMLEnvironment.dist.isClient();

    public TimeLoopNeoForge(IEventBus modEventBus) { // NeoForge uses constructor injection for mod bus
        LOOP_LOGGER.info("Initializing TimeLoop mod (NeoForge)");

        // Pass the logical server status to common init
        TimeLoop.init(isLogicalServer);

        // Register command registration listener to the MOD event bus
        modEventBus.addListener(this::registerCommands);

        // Register gameplay event listeners to the NeoForge EVENT bus
        NeoForge.EVENT_BUS.register(this);

        LOOP_LOGGER.info("TimeLoop mod initialized successfully (NeoForge)");
    }

    // Command registration handler
    public void registerCommands(RegisterCommandsEvent event) {
        LOOP_LOGGER.info("Registering TimeLoop commands (NeoForge)");
        LoopCommands.register(event.getDispatcher());
    }

    // Server Tick handler
    @SubscribeEvent
    public void onEndServerTick(ServerTickEvent.Post event) {
        TickNeoForgeEvent.onEndServerTick(event.getServer());
    }

    // Player Wake Up handler (approximation for Stop Sleeping)
    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntitySleepNeoForgeEvent.onStopSleeping(event.getEntity());
    }

    // Server Started handler
    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        LifecycleNeoForgeEvent.onServerStart(event.getServer());
    }

    // Server Stopping handler
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LifecycleNeoForgeEvent.onServerStopping(event.getServer());
    }

    // Player Join handler
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Cast the entity to ServerPlayer to access server-side fields/methods
        if (event.getEntity() instanceof ServerPlayer player) {
            // Get the network connection handler from the player
            ServerGamePacketListenerImpl connection = player.connection;
            MinecraftServer server = player.server; // Get server from player
            
            PlayConnectionNeoForgeEvent.onJoin(connection, server);
        } else {
            LOOP_LOGGER.warn("PlayerLoggedInEvent received a non-ServerPlayer entity?");
        }
    }

    // Player Disconnect handler
    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        // Cast the entity to ServerPlayer
        if (event.getEntity() instanceof ServerPlayer player) {
            // Get the network connection handler from the player
            ServerGamePacketListenerImpl connection = player.connection;
            
            PlayConnectionNeoForgeEvent.onDisconnect(connection);
        } else {
            LOOP_LOGGER.warn("PlayerLoggedOutEvent received a non-ServerPlayer entity?");
        }
    }

    // Player Respawn handler
    @SubscribeEvent
    public void afterRespawn(PlayerEvent.PlayerRespawnEvent event) {
        PlayerNeoForgeEvent.afterRespawn();
    }
}