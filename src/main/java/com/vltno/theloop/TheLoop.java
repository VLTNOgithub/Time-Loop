package com.vltno.theloop;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class TheLoop implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("theloop");
    private static MinecraftServer server;

    // These fields will be initialized from the configuration file.
    private int loopIteration;
    private int looplength;
    private Timer recordingTimer;
    private boolean isRecording = false;

    // The configuration object loaded from disk
    private TheLoopConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TheLoop mod");

        // Load configuration from the config folder provided by FabricLoader
        Path configDir = FabricLoader.getInstance().getConfigDir();
        config = TheLoopConfig.load(configDir);
        loopIteration = config.loopIteration;
        looplength = config.loopLength;

        // Register server starting event
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            TheLoop.server = server;
        });
        // Register server started event
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            executeCommand("mocap scenes add main_scene");
        });

        // Register player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String playerName = player.getName().getString();

            if (isRecording) {
                LOGGER.debug("Starting recording for newly joined player: {}", playerName);
                executeCommand(String.format("mocap recording start %s", playerName));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String playerName = player.getName().getString();
            String recordingName = playerName + "_" + loopIteration;
            executeCommand(String.format("mocap recording stop %s", playerName));
            boolean saved = executeCommand(String.format("mocap recording save %s", recordingName.toLowerCase()));
            if (saved) {
                executeCommand(String.format("mocap scenes add_to main_scene %s", recordingName.toLowerCase()));
            }
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("loop")
                    .then(CommandManager.literal("start")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> {
                                startLoop();
                                context.getSource().sendMessage(Text.literal("Recording loop started"));
                                LOGGER.info("Recording loop started by {}", context.getSource().getName());
                                return 1;
                            }))
                    .then(CommandManager.literal("stop")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> {
                                stopRecordingLoop();
                                context.getSource().sendMessage(Text.literal("Recording loop stopped"));
                                LOGGER.info("Recording loop stopped");
                                return 1;
                            }))
                    .then(CommandManager.literal("status")
                            .executes(context -> {
                                String status = isRecording ?
                                        "Loop is active. Current iteration: " + loopIteration :
                                        "Loop is inactive. Last iteration: " + loopIteration;
                                context.getSource().sendMessage(Text.literal(status));
                                LOGGER.debug("Status requested by {}: {}", context.getSource().getName(), status);
                                return 1;
                            }))
                    .then(CommandManager.literal("setlength")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("value", IntegerArgumentType.integer(1000)) // Minimum 1000 ms
                                .executes(context -> {
                                    int newLength = IntegerArgumentType.getInteger(context, "value");
                                    looplength = newLength;
                                    config.loopLength = newLength;
                                    config.save();
                                    context.getSource().sendMessage(Text.literal("Loop length set to " + newLength + " ms"));
                                    LOGGER.info("Loop length set to {} ms", newLength);
                                    return 1;
                                })))
                    .then(CommandManager.literal("reset")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> {
                                stopRecordingLoop();
                                executeCommand("mocap scenes remove main_scene");
                                executeCommand("mocap scenes add main_scene");
                                loopIteration = 0;
                                LOGGER.debug("Loop reset!");
                                // Broadcast status to operators
                                server.getPlayerManager().getPlayerList().forEach(player -> {
                                    if (player.hasPermissionLevel(2)) {
                                        player.sendMessage(Text.literal("Loop reset!"));
                                    }
                                });
                                return 1;
                            })));
        });

        // Initialize timer
        recordingTimer = new Timer();
        LOGGER.info("TheLoop mod initialized successfully");
    }

    public void startLoop() {
        if (isRecording) {
            LOGGER.debug("Attempted to start already running recording loop");
            return;
        }
        isRecording = true;
        LOGGER.info("Starting Loop");
        
        executeCommand("mocap settings recordPlayerDeath false");

        // Start recording for every currently online player
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String playerName = player.getName().getString();
            executeCommand(String.format("mocap recording start %s", playerName));
        }

        // Schedule the recording loop using the saved loop length from the config.
        recordingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRecording) {
                    LOGGER.debug("Loop stopped, cancelling timer");
                    this.cancel();
                    return;
                }

                LOGGER.debug("Starting iteration {} of recording loop", loopIteration);

                // Stop and save recordings for each player
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    String playerName = player.getName().getString();
                    String recordingName = playerName + "_" + loopIteration;

                    LOGGER.debug("Processing recording for player: {}", playerName);
                    executeCommand("mocap recording stop");
                    boolean saved = executeCommand(String.format("mocap recording save %s", recordingName.toLowerCase()));
                    if (saved) {
                        executeCommand(String.format("mocap scenes addTo .main_scene %s", recordingName.toLowerCase()));
                    }
                    // start recording
                    executeCommand(String.format("mocap recording start %s", playerName));
                }

                // Broadcast status to operators
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    if (player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("Loop iteration " + loopIteration + " completed"));
                    }
                });

                // Restart playback commands
                executeCommand("mocap playing stopAll");
                executeCommand("mocap playing start .main_scene");

                // Increment and update loop iteration in the config file
                loopIteration++;
                config.loopIteration = loopIteration;
                config.save();

                LOGGER.info("Completed loop iteration {}", loopIteration - 1);
            }
        }, looplength, looplength);
    }

    public void stopRecordingLoop() {
        LOGGER.info("Stopping loop");
        isRecording = false;
        if (recordingTimer != null) {
            recordingTimer.cancel();
            recordingTimer.purge();
            recordingTimer = new Timer();
            LOGGER.debug("Loop timer cancelled and reset");
        }
    }

    /**
     * Executes a server command.
     *
     * @param command the command to execute.
     * @return true if the command executed without exception.
     */
    private boolean executeCommand(String command) {
        if (server != null) {
            LOGGER.debug("Executing command: {}", command);
            try {
                server.getCommandManager().executeWithPrefix(
                        server.getCommandSource(),
                        command
                );
                LOGGER.debug("Command executed successfully: {}", command);
                return true;
            } catch (Exception e) {
                LOGGER.error("Error executing command {}: {}", command, e.getMessage());
                return false;
            }
        } else {
            LOGGER.error("Attempted to execute command while server is null: {}", command);
            return false;
        }
    }
}
