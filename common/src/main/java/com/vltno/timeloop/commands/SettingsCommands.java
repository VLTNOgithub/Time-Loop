package com.vltno.timeloop.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.Commands;
import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SettingsCommands {
    public static LiteralArgumentBuilder<ServerCommandSource> getArgumentBuilder()
    {
        LiteralArgumentBuilder<ServerCommandSource> commandBuilder = CommandManager.literal("settings");
        
        commandBuilder.then(CommandManager.literal("setLoopType")).executes(context -> Commands.returnText(context, "Loop type is set to: " + Commands.mod.loopType))
                .then(CommandManager.argument("loopType", StringArgumentType.word()).suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"TICKS", "TIME_OF_DAY", "SLEEP", "DEATH"}, builder))
                        .executes(SettingsCommands::setLoopType));
        
        commandBuilder.then(CommandManager.literal("setLength")).executes(context -> Commands.returnText(context, "Loop length is set to: " + Commands.mod.loopLengthTicks + " ticks"))
                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20))
                        .executes(SettingsCommands::setLoopLength));
        
        commandBuilder.then(CommandManager.literal("setTimeOfDay")).executes(context -> Commands.returnText(context, "Time of day is set to: " + Commands.mod.timeSetting))
                .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(SettingsCommands::setTimeOfDay));
        
        commandBuilder.then(CommandManager.literal("modifyPlayer"))
                .then(CommandManager.argument("targetPlayer", StringArgumentType.string())
                .then(CommandManager.argument("newName", StringArgumentType.string())
                .then(CommandManager.argument("newSkin", StringArgumentType.string())
                        .executes(SettingsCommands::modifyPlayer))));
        
        return commandBuilder;
    }
    
    private static int setLoopType(CommandContext<ServerCommandSource> context) {
        LoopTypes newLoopType = LoopTypes.valueOf(StringArgumentType.getString(context, "loopType"));
        Commands.mod.loopType = newLoopType;
        TimeLoop.config.loopType = newLoopType;
        TimeLoop.config.save();

        // Hide BossBar when loopType is not Ticks or TimeOfDay
        if (Commands.mod.showLoopInfo) {
            TimeLoop.loopBossBar.visible(newLoopType.equals(LoopTypes.TICKS) || newLoopType.equals(LoopTypes.TIME_OF_DAY));
        }

        context.getSource().sendMessage(Text.literal("Looping type is set to: " + newLoopType));
        Commands.LOOP_COMMANDS_LOGGER.info("Loop type set to {}", newLoopType);
        return 1;
    }
    
    private static int setLoopLength(CommandContext<ServerCommandSource> context) {
        int newTicks = IntegerArgumentType.getInteger(context, "ticks");
        Commands.mod.loopLengthTicks = newTicks;
        TimeLoop.config.loopLengthTicks = newTicks;

        Commands.mod.ticksLeft = newTicks;
        TimeLoop.config.ticksLeft = newTicks;

        TimeLoop.config.save();

        context.getSource().sendMessage(Text.literal("Loop length is set to: " + newTicks + " ticks"));
        Commands.LOOP_COMMANDS_LOGGER.info("Loop length set to {} ticks", newTicks);
        return 1;
    }
    
    private static int setTimeOfDay(CommandContext<ServerCommandSource> context) {
        int newTime = IntegerArgumentType.getInteger(context, "time");
        Commands.mod.timeSetting = newTime;
        TimeLoop.config.timeSetting = newTime;
        TimeLoop.config.save();
        context.getSource().sendMessage(Text.literal("Time of day is set to: " + newTime));
        Commands.LOOP_COMMANDS_LOGGER.info("Time of day set to {}", newTime);
        return 1;
    }
    
    private static int modifyPlayer(CommandContext<ServerCommandSource> context) {
        String targetPlayer = StringArgumentType.getString(context, "targetPlayer");
        String newName = StringArgumentType.getString(context, "newName");
        String newSkin = StringArgumentType.getString(context, "newSkin");

        Commands.mod.modifyPlayerAttributes(targetPlayer, newName, newSkin);
        return 1;
    }
}
