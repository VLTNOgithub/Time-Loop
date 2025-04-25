package com.vltno.timeloop.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.Commands;
import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class TogglesCommands {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> settingsCommandBuilder)
    {
        LiteralArgumentBuilder<ServerCommandSource> togglesNode = CommandManager.literal("toggles");
        
        togglesNode.then(CommandManager.literal("trackTimeOfDay").executes(context -> Commands.returnText(context, "Track time of day is set to: " + TimeLoop.trackTimeOfDay))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackTimeOfDay)));

        togglesNode.then(CommandManager.literal("trackItems").executes(context -> Commands.returnText(context, "Track items is set to: " + TimeLoop.trackItems))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackItems)));

        togglesNode.then(CommandManager.literal("displayTimeInTicks").executes(context -> Commands.returnText(context, "Display time in ticks is set to: " + TimeLoop.displayTimeInTicks))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::displayTimeInTicks)));

        togglesNode.then(CommandManager.literal("showLoopInfo").executes(context -> Commands.returnText(context, "Show loop info is set to: " + TimeLoop.showLoopInfo))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::showLoopInfo)));

        togglesNode.then(CommandManager.literal("trackChat").executes(context -> Commands.returnText(context, "Tracking chat is set to: " + TimeLoop.trackChat))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackChat)));

        togglesNode.then(CommandManager.literal("hurtLoopedPlayers").executes(context -> Commands.returnText(context, "Hurting looped players is set to: " + TimeLoop.hurtLoopedPlayers))
                .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::hurtLoopedPlayers)));
        
        settingsCommandBuilder.then(togglesNode);
    }

    private static int trackTimeOfDay(CommandContext<ServerCommandSource> context) {
        boolean newTrackTimeOfDay = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackTimeOfDay = newTrackTimeOfDay;
        TimeLoop.config.trackTimeOfDay = newTrackTimeOfDay;
        TimeLoop.config.save();

        context.getSource().sendMessage(Text.literal("Track time of day is set to: " + newTrackTimeOfDay));
        Commands.LOOP_COMMANDS_LOGGER.info("Track time of day set to {}", newTrackTimeOfDay);
        return 1;
    }

    private static int trackItems(CommandContext<ServerCommandSource> context) {
        boolean newTrackItems = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackItems = newTrackItems;
        TimeLoop.config.trackItems = newTrackItems;
        TimeLoop.config.save();

        TimeLoop.updateEntitiesToTrack(newTrackItems);

        context.getSource().sendMessage(Text.literal("Track items is set to: " + newTrackItems));
        Commands.LOOP_COMMANDS_LOGGER.info("Track items set to {}", newTrackItems);
        return 1;
    }

    private static int displayTimeInTicks(CommandContext<ServerCommandSource> context) {
        boolean newDisplayTimeInTicks = BoolArgumentType.getBool(context, "value");
        TimeLoop.displayTimeInTicks = newDisplayTimeInTicks;
        TimeLoop.config.displayTimeInTicks = newDisplayTimeInTicks;
        TimeLoop.config.save();

        context.getSource().sendMessage(Text.literal("Display time in ticks is set to: " + newDisplayTimeInTicks));
        Commands.LOOP_COMMANDS_LOGGER.info("Display time in ticks set to {}", newDisplayTimeInTicks);
        return 1;
    }
    
    private static int showLoopInfo(CommandContext<ServerCommandSource> context) {
        boolean newShowLoopInfo = BoolArgumentType.getBool(context, "value");
        TimeLoop.showLoopInfo = newShowLoopInfo;
        TimeLoop.config.showLoopInfo = newShowLoopInfo;
        TimeLoop.config.save();

        if (newShowLoopInfo) {
            TimeLoop.loopBossBar.visible(TimeLoop.loopType.equals(LoopTypes.TICKS) || TimeLoop.loopType.equals(LoopTypes.TIME_OF_DAY));
        }

        context.getSource().sendMessage(Text.literal("Showing loop info is set to: " + newShowLoopInfo));
        Commands.LOOP_COMMANDS_LOGGER.info("Show loop info set to {}", newShowLoopInfo);
        return 1;
    }
    
    private static int trackChat(CommandContext<ServerCommandSource> context) {
        boolean newTrackChat = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackChat = newTrackChat;
        TimeLoop.config.trackChat = newTrackChat;
        TimeLoop.config.save();

        TimeLoop.executeCommand("mocap settings recording chat_recording " + newTrackChat);

        context.getSource().sendMessage(Text.literal("Tracking chat is set to: " + newTrackChat));
        Commands.LOOP_COMMANDS_LOGGER.info("Tracking chat set to {}", newTrackChat);
        return 1;
    }
    
    private static int hurtLoopedPlayers(CommandContext<ServerCommandSource> context) {
        boolean newHurtLoopedPlayers = BoolArgumentType.getBool(context, "value");
        TimeLoop.hurtLoopedPlayers = newHurtLoopedPlayers;
        TimeLoop.config.hurtLoopedPlayers = newHurtLoopedPlayers;
        TimeLoop.config.save();

        TimeLoop.executeCommand("mocap settings playback invulnerable_playback " + !newHurtLoopedPlayers);

        context.getSource().sendMessage(Text.literal("Hurting looped players is set to: " + newHurtLoopedPlayers));
        Commands.LOOP_COMMANDS_LOGGER.info("Hurting looped players set to {}", newHurtLoopedPlayers);
        return 1;
    }
}
