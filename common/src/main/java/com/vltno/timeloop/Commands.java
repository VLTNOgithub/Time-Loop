package com.vltno.timeloop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Commands {
    public static final Logger LOOP_COMMANDS_LOGGER = LoggerFactory.getLogger("LoopCommands");
    public static TimeLoop mod = null;

    public Commands(TimeLoop mod) {
        Commands.mod = mod;
    }

    /**
     * Creates a new argument. Intended to be imported statically. The benefit of this over the brigadier {@link RequiredArgumentBuilder#argument} method is that it is typed to {@link CommandSource}.
     */
    public static <T> RequiredArgumentBuilder<ServerCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
    
    public static int returnText(CommandContext<ServerCommandSource> context, String text) {
        context.getSource().sendMessage(Text.literal(text));
        return 1;
    }
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loop")
                .then(CommandManager.literal("start")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            if (!mod.isLooping) {
                                mod.startTimeOfDay = mod.serverWorld.getTimeOfDay();
                                TimeLoop.config.startTimeOfDay = mod.startTimeOfDay;
                                mod.startLoop();
                                context.getSource().sendMessage(Text.literal("Loop started!"));
                                LOOP_COMMANDS_LOGGER.info("loop started");
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
                                LOOP_COMMANDS_LOGGER.info("Loop stopped");
                                return 1;
                            }
                            else {
                                context.getSource().sendMessage(Text.literal("Loop not running"));
                            }
                            return 0;
                        }))

                .then(CommandManager.literal("status")
                        .executes(context -> {
                            String extras = " Looping on " + mod.loopType + "." + (mod.isLooping && mod.loopType == LoopTypes.TICKS ? " Ticks Left: " + mod.ticksLeft : "") + (mod.trackItems ? " Tracking items." : "");
                            String status = mod.isLooping ?
                                    "Loop is active. Current iteration: " + mod.loopIteration + extras:
                                    "Loop is inactive. Last iteration: " + mod.loopIteration + extras;
                            context.getSource().sendMessage(Text.literal(status));
                            LOOP_COMMANDS_LOGGER.info("Status requested: {}", status);
                            return 1;
                        }))

                // SETTINGS
                .then(CommandManager.literal("settings")

                        .then(CommandManager.literal("setLoopType")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Loop type is set to: " + mod.loopType));
                                    return 1;
                                })
                                .then(CommandManager.argument("loopType", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                CommandSource.suggestMatching(new String[]{"TICKS", "TIME_OF_DAY", "SLEEP", "DEATH"}, builder)
                                        )
                                        .executes(context -> {
                                            LoopTypes newLoopType = LoopTypes.valueOf(StringArgumentType.getString(context, "loopType"));
                                            mod.loopType = newLoopType;
                                            TimeLoop.config.loopType = newLoopType;
                                            TimeLoop.config.save();

                                            // Hide BossBar when loopType is not Ticks or TimeOfDay
                                            if (mod.showLoopInfo) {
                                                TimeLoop.loopBossBar.visible(newLoopType.equals(LoopTypes.TICKS) || newLoopType.equals(LoopTypes.TIME_OF_DAY));
                                            }

                                            context.getSource().sendMessage(Text.literal("Looping type is set to: " + newLoopType));
                                            LOOP_COMMANDS_LOGGER.info("Loop type set to {}", newLoopType);
                                            return 1;
                                        })))


                        .then(CommandManager.literal("setLength")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Loop length is set to: " + mod.loopLengthTicks + " ticks"));
                                    return 1;
                                })
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20))
                                        .executes(context -> {
                                            int newTicks = IntegerArgumentType.getInteger(context, "ticks");
                                            mod.loopLengthTicks = newTicks;
                                            TimeLoop.config.loopLengthTicks = newTicks;

                                            mod.ticksLeft = newTicks;
                                            TimeLoop.config.ticksLeft = newTicks;

                                            TimeLoop.config.save();
                                            
                                            context.getSource().sendMessage(Text.literal("Loop length is set to: " + newTicks + " ticks"));
                                            LOOP_COMMANDS_LOGGER.info("Loop length set to {} ticks", newTicks);
                                            return 1;
                                        })))

                        .then(CommandManager.literal("maxLoops")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Max loops is set to: " + mod.maxLoops));
                                    return 1;
                                })
                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int maxLoops = IntegerArgumentType.getInteger(context, "value");
                                            mod.maxLoops = maxLoops;
                                            TimeLoop.config.maxLoops = maxLoops;
                                            TimeLoop.config.save();
                                            context.getSource().sendMessage(Text.literal("Max loops is set to: " + maxLoops));
                                            LOOP_COMMANDS_LOGGER.info("Max loops set to {}", maxLoops);
                                            return 1;
                                        })))

                        .then(CommandManager.literal("setTimeOfDay")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Time of day is set to: " + mod.timeSetting));
                                    return 1;
                                })
                                .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                                        .executes(context -> {
                                            int newTime = IntegerArgumentType.getInteger(context, "time");
                                            mod.timeSetting = newTime;
                                            TimeLoop.config.timeSetting = newTime;
                                            TimeLoop.config.save();
                                            context.getSource().sendMessage(Text.literal("Time of day is set to: " + newTime));
                                            LOOP_COMMANDS_LOGGER.info("Time of day set to {}", newTime);
                                            return 1;
                                        })))
                        
                        .then(CommandManager.literal("modifyPlayer")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.argument("targetPlayer", StringArgumentType.string())
                                .then(CommandManager.argument("newName", StringArgumentType.string())
                                .then(CommandManager.argument("newSkin", StringArgumentType.string())
                                        .executes(context -> {
                                            String targetPlayer = StringArgumentType.getString(context, "targetPlayer");
                                            String newName = StringArgumentType.getString(context, "newName");
                                            String newSkin = StringArgumentType.getString(context, "newSkin");
                                            
                                            mod.modifyPlayerAttributes(targetPlayer, newName, newSkin);
                                            return 1;
                                        })))))

                        // TOGGLES
                        .then(CommandManager.literal("toggles")
                                .then(CommandManager.literal("trackTimeOfDay")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Track time of day is set to: " + mod.trackTimeOfDay));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newTrackTimeOfDay = BoolArgumentType.getBool(context, "value");
                                                    mod.trackTimeOfDay = newTrackTimeOfDay;
                                                    TimeLoop.config.trackTimeOfDay = newTrackTimeOfDay;
                                                    TimeLoop.config.save();

                                                    context.getSource().sendMessage(Text.literal("Track time of day is set to: " + newTrackTimeOfDay));
                                                    LOOP_COMMANDS_LOGGER.info("Track time of day set to {}", newTrackTimeOfDay);
                                                    return 1;
                                                })))

                                .then(CommandManager.literal("trackItems")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Track items is set to: " + mod.trackItems));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newTrackItems = BoolArgumentType.getBool(context, "value");
                                                    mod.trackItems = newTrackItems;
                                                    TimeLoop.config.trackItems = newTrackItems;
                                                    TimeLoop.config.save();

                                                    mod.updateEntitiesToTrack(newTrackItems);

                                                    context.getSource().sendMessage(Text.literal("Track items is set to: " + newTrackItems));
                                                    LOOP_COMMANDS_LOGGER.info("Track items set to {}", newTrackItems);
                                                    return 1;
                                                })))

                                .then(CommandManager.literal("displayTimeInTicks")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Display time in ticks is set to: " + mod.displayTimeInTicks));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newDisplayTimeInTicks = BoolArgumentType.getBool(context, "value");
                                                    mod.displayTimeInTicks = newDisplayTimeInTicks;
                                                    TimeLoop.config.displayTimeInTicks = newDisplayTimeInTicks;
                                                    TimeLoop.config.save();

                                                    context.getSource().sendMessage(Text.literal("Display time in ticks is set to: " + newDisplayTimeInTicks));
                                                    LOOP_COMMANDS_LOGGER.info("Display time in ticks set to {}", newDisplayTimeInTicks);
                                                    return 1;
                                                })))

                                .then(CommandManager.literal("showLoopInfo")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Show loop info is set to: " + mod.showLoopInfo));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newShowLoopInfo = BoolArgumentType.getBool(context, "value");
                                                    mod.showLoopInfo = newShowLoopInfo;
                                                    TimeLoop.config.showLoopInfo = newShowLoopInfo;
                                                    TimeLoop.config.save();

                                                    if (newShowLoopInfo) {
                                                        TimeLoop.loopBossBar.visible(mod.loopType.equals(LoopTypes.TICKS) || mod.loopType.equals(LoopTypes.TIME_OF_DAY));
                                                    }

                                                    context.getSource().sendMessage(Text.literal("Showing loop info is set to: " + newShowLoopInfo));
                                                    LOOP_COMMANDS_LOGGER.info("Show loop info set to {}", newShowLoopInfo);
                                                    return 1;
                                                })))
                                
                                .then(CommandManager.literal("trackChat")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Tracking chat is set to: " + mod.trackChat));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newTrackChat = BoolArgumentType.getBool(context, "value");
                                                    mod.trackChat = newTrackChat;
                                                    TimeLoop.config.trackChat = newTrackChat;
                                                    TimeLoop.config.save();
                                                    
                                                    TimeLoop.executeCommand("mocap settings recording chat_recording " + newTrackChat);
                                                    
                                                    context.getSource().sendMessage(Text.literal("Tracking chat is set to: " + newTrackChat));
                                                    LOOP_COMMANDS_LOGGER.info("Tracking chat set to {}", newTrackChat);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("hurtLoopedPlayers")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            context.getSource().sendMessage(Text.literal("Hurting looped players is set to: " + mod.hurtLoopedPlayers));
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    boolean newHurtLoopedPlayers = BoolArgumentType.getBool(context, "value");
                                                    mod.hurtLoopedPlayers = newHurtLoopedPlayers;
                                                    TimeLoop.config.hurtLoopedPlayers = newHurtLoopedPlayers;
                                                    TimeLoop.config.save();
                                                    
                                                    TimeLoop.executeCommand("mocap settings playback invulnerable_playback " + !newHurtLoopedPlayers);
                                                    
                                                    context.getSource().sendMessage(Text.literal("Hurting looped players is set to: " + newHurtLoopedPlayers));
                                                    LOOP_COMMANDS_LOGGER.info("Hurting looped players set to {}", newHurtLoopedPlayers);
                                                    return 1;
                                                })))
                        )

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.stopLoop();
                            
                            mod.startTimeOfDay = 0;
                            TimeLoop.config.startTimeOfDay = 0;
                            
                            mod.timeSetting = 13000;
                            TimeLoop.config.timeSetting = 0;
                            
                            mod.ticksLeft = mod.loopLengthTicks;
                            TimeLoop.config.ticksLeft = TimeLoop.config.loopLengthTicks;
                            
                            mod.trackItems = false;
                            TimeLoop.config.trackItems = false;
                            
                            mod.loopType = LoopTypes.TICKS;
                            TimeLoop.config.loopType = LoopTypes.TICKS;
                            
                            mod.displayTimeInTicks = false;
                            TimeLoop.config.displayTimeInTicks = false;
                            
                            TimeLoop.executeCommand("mocap playback stop_all");
                            TimeLoop.loopSceneManager.forEachPlayerSceneName(playerSceneName -> {
                                TimeLoop.executeCommand(String.format("mocap scenes remove %s", playerSceneName));
                                TimeLoop.executeCommand(String.format("mocap scenes add %s", playerSceneName));
                            });

                            mod.loopIteration = 0;
                            TimeLoop.config.loopIteration = 0;
                            TimeLoop.config.save();
                            context.getSource().sendMessage(Text.literal("Loop reset!"));
                            return 1;
                        }))));
    }
}