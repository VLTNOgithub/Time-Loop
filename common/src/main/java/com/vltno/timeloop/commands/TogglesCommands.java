package com.vltno.timeloop.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.LoopCommands;
import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class TogglesCommands {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> settingsCommandBuilder)
    {
        LiteralArgumentBuilder<CommandSourceStack> togglesNode = Commands.literal("toggles");

        togglesNode.then(Commands.literal("trackTimeOfDay")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Track time of day is set to: " + TimeLoop.trackTimeOfDay), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackTimeOfDay)));

        togglesNode.then(Commands.literal("trackItems")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Track items is set to: " + TimeLoop.trackItems), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackItems)));

        togglesNode.then(Commands.literal("displayTimeInTicks")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Display time in ticks is set to: " + TimeLoop.displayTimeInTicks), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::displayTimeInTicks)));

        togglesNode.then(Commands.literal("showLoopInfo")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Show loop info is set to: " + TimeLoop.showLoopInfo), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::showLoopInfo)));

        togglesNode.then(Commands.literal("trackChat")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Tracking chat is set to: " + TimeLoop.trackChat), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::trackChat)));

        togglesNode.then(Commands.literal("hurtLoopedPlayers")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Hurting looped players is set to: " + TimeLoop.hurtLoopedPlayers), false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(TogglesCommands::hurtLoopedPlayers)));

        settingsCommandBuilder.then(togglesNode);
    }

    private static int trackTimeOfDay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newTrackTimeOfDay = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackTimeOfDay = newTrackTimeOfDay;
        TimeLoop.config.trackTimeOfDay = newTrackTimeOfDay;
        TimeLoop.config.save();

        source.sendSuccess(() -> Component.literal("Track time of day is set to: " + newTrackTimeOfDay), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Track time of day set to {}", newTrackTimeOfDay);
        return 1;
    }

    private static int trackItems(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newTrackItems = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackItems = newTrackItems;
        TimeLoop.config.trackItems = newTrackItems;
        TimeLoop.config.save();

        TimeLoop.updateEntitiesToTrack(newTrackItems);

        source.sendSuccess(() -> Component.literal("Track items is set to: " + newTrackItems), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Track items set to {}", newTrackItems);
        return 1;
    }

    private static int displayTimeInTicks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newDisplayTimeInTicks = BoolArgumentType.getBool(context, "value");
        TimeLoop.displayTimeInTicks = newDisplayTimeInTicks;
        TimeLoop.config.displayTimeInTicks = newDisplayTimeInTicks;
        TimeLoop.config.save();

        source.sendSuccess(() -> Component.literal("Display time in ticks is set to: " + newDisplayTimeInTicks), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Display time in ticks set to {}", newDisplayTimeInTicks);
        return 1;
    }

    private static int showLoopInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newShowLoopInfo = BoolArgumentType.getBool(context, "value");
        TimeLoop.showLoopInfo = newShowLoopInfo;
        TimeLoop.config.showLoopInfo = newShowLoopInfo;
        TimeLoop.config.save();

        if (TimeLoop.loopBossBar != null) {
            if (newShowLoopInfo) {
                TimeLoop.loopBossBar.visible(TimeLoop.loopType != null && (TimeLoop.loopType.equals(LoopTypes.TICKS) || TimeLoop.loopType.equals(LoopTypes.TIME_OF_DAY)));
            } else {
                TimeLoop.loopBossBar.visible(false);
            }
        }

        source.sendSuccess(() -> Component.literal("Showing loop info is set to: " + newShowLoopInfo), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Show loop info set to {}", newShowLoopInfo);
        return 1;
    }

    private static int trackChat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newTrackChat = BoolArgumentType.getBool(context, "value");
        TimeLoop.trackChat = newTrackChat;
        TimeLoop.config.trackChat = newTrackChat;
        TimeLoop.config.save();

        TimeLoop.executeCommand("mocap settings recording chat_recording " + newTrackChat);

        source.sendSuccess(() -> Component.literal("Tracking chat is set to: " + newTrackChat), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Tracking chat set to {}", newTrackChat);
        return 1;
    }

    private static int hurtLoopedPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean newHurtLoopedPlayers = BoolArgumentType.getBool(context, "value");
        TimeLoop.hurtLoopedPlayers = newHurtLoopedPlayers;
        TimeLoop.config.hurtLoopedPlayers = newHurtLoopedPlayers;
        TimeLoop.config.save();

        TimeLoop.executeCommand("mocap settings playback invulnerable_playback " + !newHurtLoopedPlayers);

        source.sendSuccess(() -> Component.literal("Hurting looped players is set to: " + newHurtLoopedPlayers), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Hurting looped players set to {}", newHurtLoopedPlayers);
        return 1;
    }
}