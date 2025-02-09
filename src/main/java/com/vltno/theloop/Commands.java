package com.vltno.theloop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger("theloop");
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
                                    "Loop is active. Current iteration: " + mod.loopIteration :
                                    "Loop is inactive. Last iteration: " + mod.loopIteration;;
                            context.getSource().sendMessage(Text.literal(status));
                            LOGGER.debug("Status requested: {}", status);
                            return 1;
                        }))

                .then(CommandManager.literal("setlength")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(10000))
                                .executes(context -> {
                                    int newLength = IntegerArgumentType.getInteger(context, "value");
                                    mod.looplength = newLength;
                                    mod.config.loopLength = newLength;
                                    mod.config.save();
                                    context.getSource().sendMessage(Text.literal("Loop length set to " + newLength + " ms"));
                                    LOGGER.info("Loop length set to {} ms", newLength);
                                    return 1;
                                })))

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.stopLoop();
                            mod.timeOfDay = 0;
                            mod.config.timeOfDay = 0;
                            mod.executeCommand("mocap playback stop_all");
                            mod.executeCommand("mocap scenes remove main_scene");
                            mod.executeCommand("mocap scenes add main_scene");
                            mod.loopIteration = 0;
                            mod.config.loopIteration = 0;
                            mod.config.save();
                            context.getSource().sendMessage(Text.literal("Loop reset!"));
                            return 1;
                        })));
    }
}