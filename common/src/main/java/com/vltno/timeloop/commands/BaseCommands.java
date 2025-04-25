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
    public static LiteralArgumentBuilder<ServerCommandSource> getArgumentBuilder(LiteralArgumentBuilder<ServerCommandSource> baseCommandBuilder)
    {
        baseCommandBuilder.then(CommandManager.literal("start").executes(BaseCommands::start)).requires(source -> source.hasPermissionLevel(2));
        baseCommandBuilder.then(CommandManager.literal("stop").executes(BaseCommands::stop)).requires(source -> source.hasPermissionLevel(2));
        baseCommandBuilder.then(CommandManager.literal("status").executes(BaseCommands::status));
        
        return baseCommandBuilder;
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
}
