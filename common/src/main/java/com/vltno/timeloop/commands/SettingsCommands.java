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

    public static void register(LiteralArgumentBuilder<ServerCommandSource> parentBuilder) {
        LiteralArgumentBuilder<ServerCommandSource> settingsNode = CommandManager.literal("settings")
                .requires(source -> source.hasPermissionLevel(2)); // Permission for all settings

        settingsNode.then(CommandManager.literal("setLoopType").executes(context -> Commands.returnText(context, "Loop type is set to: " + TimeLoop.loopType))
                .then(CommandManager.argument("loopType", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"TICKS", "TIME_OF_DAY", "SLEEP", "DEATH"}, builder))
                        .executes(SettingsCommands::setLoopType)));

        settingsNode.then(CommandManager.literal("setLength")
                .executes(context -> Commands.returnText(context, "Loop length is set to: " + TimeLoop.config.loopLengthTicks + " ticks"))
                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20))
                        .executes(SettingsCommands::setLoopLength)));

        settingsNode.then(CommandManager.literal("maxLoops")
                .executes(context -> Commands.returnText(context, "Max loops is set to: " + TimeLoop.config.maxLoops))
                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0))
                        .executes(SettingsCommands::maxLoops)));

        settingsNode.then(CommandManager.literal("setTimeOfDay")
                .executes(context -> Commands.returnText(context, "Time of day is set to: " + TimeLoop.config.timeSetting))
                .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(SettingsCommands::setTimeOfDay)));

        settingsNode.then(CommandManager.literal("modifyPlayer")
                .then(CommandManager.argument("targetPlayer", StringArgumentType.string())
                        .then(CommandManager.argument("nickname", StringArgumentType.string()) // Updated name
                                .then(CommandManager.argument("skin", StringArgumentType.string()) // Updated skin
                                        .executes(SettingsCommands::modifyPlayer))))); // Chained arguments


        // --- Crucial Step: Call TogglesCommands to register its subcommands onto 'settingsNode' ---
        TogglesCommands.register(settingsNode);
        // ---------------------------------------------------------------------------------------


        // Add the fully built 'settings' node (including its 'toggles' child) to the main 'loop' builder
        parentBuilder.then(settingsNode);
    }

    private static int setLoopType(CommandContext<ServerCommandSource> context) {
        LoopTypes newLoopType = LoopTypes.valueOf(StringArgumentType.getString(context, "loopType"));
        TimeLoop.loopType = newLoopType;
        TimeLoop.config.loopType = newLoopType;
        TimeLoop.config.save();

        // Hide BossBar when loopType is not Ticks or TimeOfDay
        if (TimeLoop.showLoopInfo) {
            TimeLoop.loopBossBar.visible(newLoopType.equals(LoopTypes.TICKS) || newLoopType.equals(LoopTypes.TIME_OF_DAY));
        }

        context.getSource().sendMessage(Text.literal("Looping type is set to: " + newLoopType));
        Commands.LOOP_COMMANDS_LOGGER.info("Loop type set to {}", newLoopType);
        return 1;
    }

    private static int setLoopLength(CommandContext<ServerCommandSource> context) {
        int newTicks = IntegerArgumentType.getInteger(context, "ticks");
        TimeLoop.loopLengthTicks = newTicks;
        TimeLoop.config.loopLengthTicks = newTicks;

        TimeLoop.ticksLeft = newTicks;
        TimeLoop.config.ticksLeft = newTicks;

        TimeLoop.config.save();

        context.getSource().sendMessage(Text.literal("Loop length is set to: " + newTicks + " ticks"));
        Commands.LOOP_COMMANDS_LOGGER.info("Loop length set to {} ticks", newTicks);
        return 1;
    }

    private static int maxLoops(CommandContext<ServerCommandSource> context) {
        int maxLoops = IntegerArgumentType.getInteger(context, "value");
        TimeLoop.maxLoops = maxLoops;
        TimeLoop.config.maxLoops = maxLoops;
        TimeLoop.config.save();
        context.getSource().sendMessage(Text.literal("Max loops is set to: " + maxLoops));
        Commands.LOOP_COMMANDS_LOGGER.info("Max loops set to {}", maxLoops);
        return 1;
    }

    private static int setTimeOfDay(CommandContext<ServerCommandSource> context) {
        int newTime = IntegerArgumentType.getInteger(context, "time");
        TimeLoop.timeSetting = newTime;
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

        TimeLoop.modifyPlayerAttributes(targetPlayer, newName, newSkin);
        return 1;
    }
}