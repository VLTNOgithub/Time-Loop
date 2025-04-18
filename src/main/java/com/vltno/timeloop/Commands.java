package com.vltno.timeloop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoopCommands");
    private final TimeLoop mod;

    public Commands(TimeLoop mod) {
        this.mod = mod;
    }
    
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loop")
                .then(CommandManager.literal("start")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            if (!mod.isLooping) {
                                mod.startTimeOfDay = mod.serverWorld.getTimeOfDay();
                                mod.config.startTimeOfDay = mod.startTimeOfDay;
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
                                LOGGER.info("Loop stopped");
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
                            LOGGER.info("Status requested: {}", status);
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
                                            mod.config.loopType = newLoopType;
                                            mod.config.save();

                                            // Hide BossBar when loopType is not Ticks or TimeOfDay
                                            if (mod.showLoopInfo) {
                                                mod.loopBossBar.visible(newLoopType.equals(LoopTypes.TICKS) || newLoopType.equals(LoopTypes.TIME_OF_DAY));
                                            }

                                            context.getSource().sendMessage(Text.literal("Looping type is set to: " + newLoopType));
                                            LOGGER.info("Loop type set to {}", newLoopType);
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
                                            mod.config.loopLengthTicks = newTicks;

                                            mod.ticksLeft = newTicks;
                                            mod.config.ticksLeft = newTicks;

                                            mod.config.save();
                                            
                                            context.getSource().sendMessage(Text.literal("Loop length is set to: " + newTicks + " ticks"));
                                            LOGGER.info("Loop length set to {} ticks", newTicks);
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
                                            mod.config.maxLoops = maxLoops;
                                            mod.config.save();
                                            context.getSource().sendMessage(Text.literal("Max loops is set to: " + maxLoops));
                                            LOGGER.info("Max loops set to {}", maxLoops);
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
                                            mod.config.timeSetting = newTime;
                                            mod.config.save();
                                            context.getSource().sendMessage(Text.literal("Time of day is set to: " + newTime));
                                            LOGGER.info("Time of day set to {}", newTime);
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
                                                    mod.config.trackTimeOfDay = newTrackTimeOfDay;
                                                    mod.config.save();

                                                    context.getSource().sendMessage(Text.literal("Track time of day is set to: " + newTrackTimeOfDay));
                                                    LOGGER.info("Track time of day set to {}", newTrackTimeOfDay);
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
                                                    mod.config.trackItems = newTrackItems;
                                                    mod.config.save();

                                                    mod.updateEntitiesToTrack(newTrackItems);

                                                    context.getSource().sendMessage(Text.literal("Track items is set to: " + newTrackItems));
                                                    LOGGER.info("Track items set to {}", newTrackItems);
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
                                                    mod.config.displayTimeInTicks = newDisplayTimeInTicks;
                                                    mod.config.save();

                                                    context.getSource().sendMessage(Text.literal("Display time in ticks is set to: " + newDisplayTimeInTicks));
                                                    LOGGER.info("Display time in ticks set to {}", newDisplayTimeInTicks);
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
                                                    mod.config.showLoopInfo = newShowLoopInfo;
                                                    mod.config.save();

                                                    if (newShowLoopInfo) {
                                                        mod.loopBossBar.visible(mod.loopType.equals(LoopTypes.TICKS) || mod.loopType.equals(LoopTypes.TIME_OF_DAY));
                                                    }

                                                    context.getSource().sendMessage(Text.literal("Showing loop info is set to: " + newShowLoopInfo));
                                                    LOGGER.info("Show loop info set to {}", newShowLoopInfo);
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
                                                    mod.config.trackChat = newTrackChat;
                                                    mod.config.save();
                                                    
                                                    mod.executeCommand("mocap settings recording chat_recording " + newTrackChat);
                                                    
                                                    context.getSource().sendMessage(Text.literal("Tracking chat is set to: " + newTrackChat));
                                                    LOGGER.info("Tracking chat set to {}", newTrackChat);
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
                                                    mod.config.hurtLoopedPlayers = newHurtLoopedPlayers;
                                                    mod.config.save();
                                                    
                                                    mod.executeCommand("mocap settings playback invulnerable_playback " + !newHurtLoopedPlayers);
                                                    
                                                    context.getSource().sendMessage(Text.literal("Hurting looped players is set to: " + newHurtLoopedPlayers));
                                                    LOGGER.info("Hurting looped players set to {}", newHurtLoopedPlayers);
                                                    return 1;
                                                })))
                        )

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.stopLoop();
                            
                            mod.startTimeOfDay = 0;
                            mod.config.startTimeOfDay = 0;
                            
                            mod.timeSetting = 13000;
                            mod.config.timeSetting = 0;
                            
                            mod.ticksLeft = mod.loopLengthTicks;
                            mod.config.ticksLeft = mod.config.loopLengthTicks;
                            
                            mod.trackItems = false;
                            mod.config.trackItems = false;
                            
                            mod.loopType = LoopTypes.TICKS;
                            mod.config.loopType = LoopTypes.TICKS;
                            
                            mod.displayTimeInTicks = false;
                            mod.config.displayTimeInTicks = false;
                            
                            mod.executeCommand("mocap playback stop_all");
                            mod.loopSceneManager.forEachPlayerSceneName(playerSceneName -> {
                                mod.executeCommand(String.format("mocap scenes remove %s", playerSceneName));
                                mod.executeCommand(String.format("mocap scenes add %s", playerSceneName));
                            });

                            mod.loopIteration = 0;
                            mod.config.loopIteration = 0;
                            mod.config.save();
                            context.getSource().sendMessage(Text.literal("Loop reset!"));
                            return 1;
                        }))));
    }
}