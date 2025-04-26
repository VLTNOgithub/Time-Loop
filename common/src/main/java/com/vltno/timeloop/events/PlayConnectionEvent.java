package com.vltno.timeloop.events;

import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.RewindTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public class PlayConnectionEvent {
    public static void onJoin(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();
        
        TimeLoop.loopSceneManager.addPlayer(Arrays.asList(playerName, null, null, player.position().toString()));
        
        if (TimeLoop.loopBossBar != null) {
            TimeLoop.loopBossBar.addPlayer(player);
        }

        TimeLoop.executeCommand(String.format("mocap scenes add %s", TimeLoop.loopSceneManager.getPlayerSceneName(playerName)));

        if (TimeLoop.config != null && TimeLoop.config.firstStart) {
            TimeLoop.config.firstStart = false;
            TimeLoop.config.save();

            TimeLoop.LOOP_LOGGER.info("First start detected, sending message to ops.");

            // Check if player is OP and send message directly
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(Component.literal("Use '/loop start' to start the time loop!"));
            }
        }

        if (TimeLoop.isLooping) {
            TimeLoop.LOOP_LOGGER.info("Starting recording for newly joined player: {}", playerName);
            TimeLoop.executeCommand(String.format("mocap recording start %s", playerName));
            
            if (TimeLoop.showLoopInfo && TimeLoop.loopBossBar != null) {
                
                boolean shouldBeVisible = TimeLoop.loopType != null && (TimeLoop.loopType.equals(LoopTypes.TICKS) || TimeLoop.loopType.equals(LoopTypes.TIME_OF_DAY));
                TimeLoop.loopBossBar.visible(shouldBeVisible);
            }
        }
    }

    public static void onDisconnect(ServerGamePacketListenerImpl handler) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();

        TimeLoop.loopSceneManager.removePlayer(playerName);
        if (TimeLoop.loopBossBar != null) {
            TimeLoop.loopBossBar.removePlayer(player);
        }
        if (TimeLoop.isLooping) {
            TimeLoop.saveRecordings();
            TimeLoop.loopSceneManager.saveRecordingPlayers();
        }
    }
}