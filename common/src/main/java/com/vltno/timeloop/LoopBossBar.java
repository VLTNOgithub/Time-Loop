package com.vltno.timeloop;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class LoopBossBar {
    private final ServerBossEvent bossBar;

    public LoopBossBar() {
        bossBar = new ServerBossEvent(Component.literal("TimeLoop"), BossBarColor.YELLOW, BossBarOverlay.PROGRESS);
    }

    public void visible(boolean bool) {
        bossBar.setVisible(bool);
    }

    public void setBossBarName(String bossBarName) {
        bossBar.setName(Component.literal(bossBarName));
    }

    public void setBossBarPercentage(int whole, int part) {
        if (whole <= 0) {
            bossBar.setProgress(1.0f);
            return;
        }
        // Calculate progress (fraction remaining)
        float progress = 1.0f - ((float) part / whole);
        // Clamp progress between 0.0 and 1.0
        bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    public void addPlayer(ServerPlayer player) {
        TimeLoop.LOOP_LOGGER.info("Adding player to boss bar: {}", player.getName().getString());
        bossBar.addPlayer(player);
    }

    public void removePlayer(ServerPlayer player) {
        TimeLoop.LOOP_LOGGER.info("Removing player from boss bar: {}", player.getName().getString());
        bossBar.removePlayer(player);
    }
}