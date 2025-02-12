package com.vltno.theloop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoopCommands");
    private final TheLoop mod;

    public Commands(TheLoop mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("loop")
                .then(CommandManager.literal("start")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            if (!mod.isLooping) {
                                mod.startLoop();
                                context.getSource().sendMessage(Text.literal("Loop started!"));
                                LOGGER.info("loop started");
                                return 1;
                            }
                            context.getSource().sendMessage(Text.literal("Loop already running!"));
                            return 0;
                        }))

                .then(CommandManager.literal("stop")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            if (mod.isLooping) {
                                mod.stopLoop();
                                context.getSource().sendMessage(Text.literal("Loop stopped"));
                                LOGGER.info("loop stopped");
                                return 1;
                            }
                            else {
                                context.getSource().sendMessage(Text.literal("Loop not running"));
                            }
                            return 0;
                        }))

                .then(CommandManager.literal("status")
                        .executes(context -> {
                            String status = mod.isLooping ?
                                    "Loop is active. Current iteration: " + mod.loopIteration + " Time of day: " + mod.timeOfDay:
                                    "Loop is inactive. Last iteration: " + mod.loopIteration + " Time of day: " + mod.timeOfDay;
                            context.getSource().sendMessage(Text.literal(status));
                            LOGGER.debug("Status requested: {}", status);
                            return 1;
                        }))

                .then(CommandManager.literal("setLength")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("Loop length is set to: " + mod.loopLength + " ticks"));
                            return 1;
                        })
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20))
                                .executes(context -> {
                                    int newLength = IntegerArgumentType.getInteger(context, "ticks");
                                    mod.loopLength = newLength;
                                    mod.config.loopLength = newLength;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("Loop length is set to: " + newLength + " ticks"));
                                    LOGGER.info("Loop length set to {} ticks", newLength);
                                    return 1;
                                })))

                .then(CommandManager.literal("loopBasedOnTime")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("Looping based on time is set to: " + mod.loopBasedOnTime));
                            return 1;
                        })
                        .then(CommandManager.argument("bool", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean newLoopBasedOnTime = BoolArgumentType.getBool(context, "bool");
                                    mod.loopBasedOnTime = newLoopBasedOnTime;
                                    mod.config.loopBasedOnTime = newLoopBasedOnTime;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("Looping based on time is set to: " + newLoopBasedOnTime));
                                    LOGGER.info("Looping based on time set to {}", newLoopBasedOnTime);
                                    return 1;
                                })))
                
                .then(CommandManager.literal("loopOnSleep")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("Looping on sleep is set to: " + mod.loopOnSleep));
                            return 1;
                        })
                        .then(CommandManager.argument("bool", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean newLoopOnSleep = BoolArgumentType.getBool(context, "bool");
                                    mod.loopOnSleep = newLoopOnSleep;
                                    mod.config.loopOnSleep = newLoopOnSleep;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("Looping on sleep is set to: " + newLoopOnSleep));
                                    LOGGER.info("Looping on sleep set to {}", newLoopOnSleep);
                                    return 1;
                                })))
                
                .then(CommandManager.literal("setTime")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("Time is set to: " + mod.timeSetting));
                            return 1;
                        })
                        .then(CommandManager.argument("time", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int newTime = IntegerArgumentType.getInteger(context, "time");
                                    mod.timeSetting = newTime;
                                    mod.config.timeSetting = newTime;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("Time is set to: " + newTime));
                                    LOGGER.info("Time set to {}", newTime);
                                    return 1;
                                })))

                .then(CommandManager.literal("maxLoops")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("maxLoops is currently set to: " + mod.maxLoops));
                            return 1;
                        })
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int maxLoops = IntegerArgumentType.getInteger(context, "value");
                                    mod.maxLoops = maxLoops;
                                    mod.config.maxLoops = maxLoops;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("maxLoops is currently set to: " + maxLoops));
                                    LOGGER.info("Max Loops set to {}", maxLoops);
                                    return 1;
                                })))

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.stopLoop();
                            mod.timeOfDay = 0;
                            mod.config.timeOfDay = 0;
                            mod.timeSetting = 0;
                            mod.config.timeSetting = 0;
                            mod.loopBasedOnTime = false;
                            mod.config.loopBasedOnTime = false;
                            mod.loopOnSleep = false;
                            mod.config.loopOnSleep = false;
                            mod.executeCommand("mocap playback stop_all");
                            mod.executeCommand(String.format("mocap scenes remove %s", mod.sceneName));
                            mod.executeCommand(String.format("mocap scenes add %s", mod.sceneName));
                            mod.loopIteration = 0;
                            mod.config.loopIteration = 0;
                            mod.config.save();
                            context.getSource().sendMessage(Text.literal("Loop reset!"));
                            return 1;
                        })));
    }
}