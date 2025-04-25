package com.vltno.timeloop.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.LoopCommands;
import com.vltno.timeloop.LoopTypes;
import com.vltno.timeloop.TimeLoop;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SettingsCommands {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentBuilder) {
        LiteralArgumentBuilder<CommandSourceStack> settingsNode = Commands.literal("settings")
                .requires(source -> source.hasPermission(2));

        settingsNode.then(Commands.literal("setLoopType")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Loop type is set to: " + TimeLoop.loopType), false);
                    return 1;
                })
                .then(Commands.argument("loopType", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"TICKS", "TIME_OF_DAY", "SLEEP", "DEATH"}, builder))
                        .executes(SettingsCommands::setLoopType)));

        settingsNode.then(Commands.literal("setLength")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Loop length is set to: " + TimeLoop.config.loopLengthTicks + " ticks"), false);
                    return 1;
                })
                .then(Commands.argument("ticks", IntegerArgumentType.integer(20))
                        .executes(SettingsCommands::setLoopLength)));

        settingsNode.then(Commands.literal("maxLoops")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Max loops is set to: " + TimeLoop.config.maxLoops), false);
                    return 1;
                })
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(SettingsCommands::maxLoops)));

        settingsNode.then(Commands.literal("setTimeOfDay")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Time of day is set to: " + TimeLoop.config.timeSetting), false);
                    return 1;
                })
                .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(SettingsCommands::setTimeOfDay)));

        settingsNode.then(Commands.literal("modifyPlayer")
                .then(Commands.argument("targetPlayer", StringArgumentType.string())
                .then(Commands.argument("newName", StringArgumentType.string())
                .then(Commands.argument("newSkin", StringArgumentType.string())
                        .executes(SettingsCommands::modifyPlayer)))));

        TogglesCommands.register(settingsNode);

        parentBuilder.then(settingsNode);
    }

    private static int setLoopType(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String loopTypeStr = StringArgumentType.getString(context, "loopType");
        try {
            LoopTypes newLoopType = LoopTypes.valueOf(loopTypeStr.toUpperCase());
            TimeLoop.loopType = newLoopType;
            TimeLoop.config.loopType = newLoopType;
            TimeLoop.config.save();

            if (TimeLoop.showLoopInfo && TimeLoop.loopBossBar != null) {
                TimeLoop.loopBossBar.visible(newLoopType.equals(LoopTypes.TICKS) || newLoopType.equals(LoopTypes.TIME_OF_DAY));
            }

            source.sendSuccess(() -> Component.literal("Looping type is set to: " + newLoopType), true);
            LoopCommands.LOOP_COMMANDS_LOGGER.info("Loop type set to {}", newLoopType);
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid loop type: " + loopTypeStr));
            return 0;
        }
    }

    private static int setLoopLength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int newTicks = IntegerArgumentType.getInteger(context, "ticks");
        TimeLoop.loopLengthTicks = newTicks;
        TimeLoop.config.loopLengthTicks = newTicks;

        TimeLoop.ticksLeft = newTicks;
        TimeLoop.config.ticksLeft = newTicks;

        TimeLoop.config.save();

        source.sendSuccess(() -> Component.literal("Loop length is set to: " + newTicks + " ticks"), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Loop length set to {} ticks", newTicks);
        return 1;
    }

    private static int maxLoops(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int maxLoops = IntegerArgumentType.getInteger(context, "value");
        TimeLoop.maxLoops = maxLoops;
        TimeLoop.config.maxLoops = maxLoops;
        TimeLoop.config.save();
        source.sendSuccess(() -> Component.literal("Max loops is set to: " + maxLoops), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Max loops set to {}", maxLoops);
        return 1;
    }

    private static int setTimeOfDay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int newTime = IntegerArgumentType.getInteger(context, "time");
        TimeLoop.timeSetting = newTime;
        TimeLoop.config.timeSetting = newTime;
        TimeLoop.config.save();
        source.sendSuccess(() -> Component.literal("Time of day is set to: " + newTime), true);
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Time of day set to {}", newTime);
        return 1;
    }

    private static int modifyPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String targetPlayer = StringArgumentType.getString(context, "targetPlayer");
        String newName = StringArgumentType.getString(context, "newName");
        String newSkin = StringArgumentType.getString(context, "newSkin");
        
        TimeLoop.modifyPlayerAttributes(targetPlayer, newName, newSkin);
        source.sendSuccess(() -> Component.literal("Attempted to modify player " + targetPlayer), true); // Generic success message
        LoopCommands.LOOP_COMMANDS_LOGGER.info("Attempted to modify player {} with name {} and skin {}", targetPlayer, newName, newSkin);
        return 1;
    }
}