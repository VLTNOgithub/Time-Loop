package com.vltno.timeloop.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.Commands;
import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class BaseCommands {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> parentBuilder) {
        parentBuilder.then(CommandManager.literal("start")
                .executes(BaseCommands::start)
                .requires(source -> source.hasPermissionLevel(2)));

        parentBuilder.then(CommandManager.literal("stop")
                .executes(BaseCommands::stop)
                .requires(source -> source.hasPermissionLevel(2)));

        parentBuilder.then(CommandManager.literal("reset")
                .executes(BaseCommands::reset)
                .requires(source -> source.hasPermissionLevel(2)));

        parentBuilder.then(CommandManager.literal("status")
                .executes(BaseCommands::status)); // No permission needed
    }

    private static int start(CommandContext<ServerCommandSource> context) {
        if (!TimeLoop.isLooping) {
            TimeLoop.startTimeOfDay = TimeLoop.serverWorld.getTimeOfDay();
            TimeLoop.config.startTimeOfDay = TimeLoop.startTimeOfDay;
            TimeLoop.startLoop();
            context.getSource().sendMessage(Text.literal("Loop started!"));
            Commands.LOOP_COMMANDS_LOGGER.info("loop started");
            return 1;
        }
        context.getSource().sendMessage(Text.literal("Loop already running!"));
        return 0;
    }

    private static int stop(CommandContext<ServerCommandSource> context) {
        if (TimeLoop.isLooping) {
            TimeLoop.stopLoop();
            context.getSource().sendMessage(Text.literal("Loop stopped"));
            Commands.LOOP_COMMANDS_LOGGER.info("Loop stopped");
            return 1;
        }
        else {
            context.getSource().sendMessage(Text.literal("Loop not running"));
        }
        return 0;
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        String extras = " Looping on " + TimeLoop.loopType + "." + (TimeLoop.isLooping && TimeLoop.loopType == LoopTypes.TICKS ? " Ticks Left: " + TimeLoop.ticksLeft : "") + (TimeLoop.trackItems ? " Tracking items." : "");
        String status = TimeLoop.isLooping ?
                "Loop is active. Current iteration: " + TimeLoop.loopIteration + extras:
                "Loop is inactive. Last iteration: " + TimeLoop.loopIteration + extras;
        context.getSource().sendMessage(Text.literal(status));
        Commands.LOOP_COMMANDS_LOGGER.info("Status requested: {}", status);
        return 1;
    }

    private static int reset(CommandContext<ServerCommandSource> context) {
        TimeLoop.stopLoop();

        TimeLoop.startTimeOfDay = 0;
        TimeLoop.config.startTimeOfDay = 0;

        TimeLoop.timeSetting = 13000;
        TimeLoop.config.timeSetting = 0;

        TimeLoop.ticksLeft = TimeLoop.loopLengthTicks;
        TimeLoop.config.ticksLeft = TimeLoop.config.loopLengthTicks;

        TimeLoop.trackItems = false;
        TimeLoop.config.trackItems = false;

        TimeLoop.loopType = LoopTypes.TICKS;
        TimeLoop.config.loopType = LoopTypes.TICKS;

        TimeLoop.displayTimeInTicks = false;
        TimeLoop.config.displayTimeInTicks = false;

        TimeLoop.executeCommand("mocap playback stop_all");
        TimeLoop.loopSceneManager.forEachPlayerSceneName(playerSceneName -> {
            TimeLoop.executeCommand(String.format("mocap scenes remove %s", playerSceneName));
            TimeLoop.executeCommand(String.format("mocap scenes add %s", playerSceneName));
        });

        TimeLoop.loopIteration = 0;
        TimeLoop.config.loopIteration = 0;
        TimeLoop.config.save();
        context.getSource().sendMessage(Text.literal("Loop reset!"));
        return 1;
    }
}