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
        if (!Commands.mod.isLooping) {
            Commands.mod.startTimeOfDay = Commands.mod.serverWorld.getTimeOfDay();
            TimeLoop.config.startTimeOfDay = Commands.mod.startTimeOfDay;
            Commands.mod.startLoop();
            context.getSource().sendMessage(Text.literal("Loop started!"));
            Commands.LOOP_COMMANDS_LOGGER.info("loop started");
            return 1;
        }
        context.getSource().sendMessage(Text.literal("Loop already running!"));
        return 0;
    }
    
    private static int stop(CommandContext<ServerCommandSource> context) {
        if (Commands.mod.isLooping) {
            Commands.mod.stopLoop();
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
        String extras = " Looping on " + Commands.mod.loopType + "." + (Commands.mod.isLooping && Commands.mod.loopType == LoopTypes.TICKS ? " Ticks Left: " + Commands.mod.ticksLeft : "") + (Commands.mod.trackItems ? " Tracking items." : "");
        String status = Commands.mod.isLooping ?
                "Loop is active. Current iteration: " + Commands.mod.loopIteration + extras:
                "Loop is inactive. Last iteration: " + Commands.mod.loopIteration + extras;
        context.getSource().sendMessage(Text.literal(status));
        Commands.LOOP_COMMANDS_LOGGER.info("Status requested: {}", status);
        return 1;
    }
}
