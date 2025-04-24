package com.vltno.timeloop.events;

import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;

public class PlayConnectionEvent {
    public static void onJoin(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        String playerName = player.getName().getString();

        TimeLoop.loopSceneManager.addPlayer(Arrays.asList(playerName));
        TimeLoop.loopBossBar.addPlayer(player);

        TimeLoop.executeCommand(String.format("mocap scenes add %s", TimeLoop.loopSceneManager.getPlayerSceneName(playerName)));

        if (TimeLoop.config.firstStart) {
            TimeLoop.config.firstStart = false;
            TimeLoop.config.save();

            TimeLoop.LOOP_LOGGER.info("First start detected, sending message to ops.");

            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                player.sendMessage(Text.literal(("Use '/loop start' to start the time loop!")));
            }
        }

        if (TimeLoop.isLooping) {
            TimeLoop.LOOP_LOGGER.info("Starting recording for newly joined player: {}", playerName);
            TimeLoop.executeCommand(String.format("mocap recording start %s", playerName));
            if (TimeLoop.showLoopInfo) {
                TimeLoop.loopBossBar.visible(TimeLoop.loopType.equals(LoopTypes.TICKS) || TimeLoop.loopType.equals(LoopTypes.TIME_OF_DAY));
            }
        }
    }
    
    public static void onDisconnect(ServerPlayNetworkHandler handler) {
        ServerPlayerEntity player = handler.getPlayer();
        String playerName = player.getName().getString();

        TimeLoop.loopSceneManager.removePlayer(playerName);
        TimeLoop.loopBossBar.removePlayer(player);
        if (TimeLoop.isLooping) {
            TimeLoop.saveRecordings();
            TimeLoop.loopSceneManager.saveRecordingPlayers();
        }
    }
}
