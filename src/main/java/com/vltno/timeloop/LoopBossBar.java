package com.vltno.timeloop;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.boss.BossBar.Style;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class LoopBossBar {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoopCommands");
    private final ServerBossBar bossBar;


    public LoopBossBar() {
        bossBar = new ServerBossBar(Text.literal("TimeLoop"), Color.YELLOW, Style.PROGRESS);
    }

    public void visible(boolean bool) {
        bossBar.setVisible(bool);
    }

    public void setBossBarName(String bossBarName) {
        bossBar.setName(Text.literal(bossBarName));
    }

    public void setBossBarPercentage(int whole, int part) {
        bossBar.setPercent(1.0f - ((float) part / whole));
    }
    
    public float getBossBarPercentage() {
        return bossBar.getPercent();
    }

    public void addPlayer(ServerPlayerEntity player) {
        LOGGER.info("Adding player: {}", player.getName().getString());
        bossBar.addPlayer(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        LOGGER.info("Player removed");
        bossBar.removePlayer(player);
    }
}
