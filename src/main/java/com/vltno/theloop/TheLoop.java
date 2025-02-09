package com.vltno.theloop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TheLoop implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("theloop");
    private Commands commands;
    private static MinecraftServer server;
    private ServerWorld serverWorld;

    // These fields will be initialized from the configuration file.
    public int loopIteration;
    public int looplength;
    public long timeOfDay;
    public boolean isLooping;
    public int maxLoops;
    private Timer recordingTimer;
    private List<String> recordingPlayers; // Add this field

    // The configuration object loaded from disk
    public TheLoopConfig config;
    // Get the world folder path for config/recording loading
    private Path worldFolder;


    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TheLoop mod");
        recordingPlayers = new ArrayList<>(); // Initialize the list

        // Register commands
        commands = new Commands(this);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                commands.register(dispatcher, registryAccess, environment)
        );

        // Register server started event
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Load configuration from the config folder provided by FabricLoader
            worldFolder = server.getSavePath(WorldSavePath.ROOT);
            // Path configDir = FabricLoader.getInstance().getConfigDir();
            config = TheLoopConfig.load(worldFolder);
            loopIteration = config.loopIteration;
            looplength = config.loopLength;
            isLooping = config.isLooping;
            timeOfDay = config.timeOfDay;

            TheLoop.server = server;
            this.serverWorld = server.getOverworld();
            executeCommand("mocap settings recording record_player_death false");
            executeCommand("mocap settings recording track_entities @items");
            executeCommand("mocap scenes add loop_scene");
            if (config.isLooping) {
                LOGGER.info("Loop was active in config, automatically restarting loop.");
                // Reset the in-memory flag so that startLoop() does not return early.
                isLooping = false;
                startLoop();
            }
        });

        // stop the loop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (isLooping) {
                stopLoop();
                config.isLooping = true;
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String playerName = player.getName().getString();
            recordingPlayers.add(playerName); // Add to recording list
            if (isLooping) {
                LOGGER.debug("Starting recording for newly joined player: {}", playerName);
                executeCommand(String.format("mocap recording start %s", playerName));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String playerName = player.getName().getString();
            recordingPlayers.remove(playerName); // Remove from recording list
            if (isLooping) {
                LOGGER.debug("Saving recording for Disconnecting player: {}", playerName);
                String recordingName = playerName + "_" + System.currentTimeMillis();
                executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
                executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
                if (recordingFileExists(recordingName)) {
                    executeCommand(String.format("mocap scenes add_to loop_scene %s", recordingName.toLowerCase()));
                }
            }
        });

        // Initialize timer
        recordingTimer = new Timer();
        LOGGER.info("TheLoop mod initialized successfully");
    }

    public void startLoop() {
        if (isLooping) {
            LOGGER.debug("Attempted to start already running recording loop");
            return;
        }
        isLooping = true;
        config.isLooping = true;
        timeOfDay = serverWorld.getTimeOfDay();
        config.timeOfDay = timeOfDay;
        LOGGER.info("Starting Loop");
        startRecordings();

        // Schedule the recording loop using the saved loop length from the config.
        recordingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isLooping) {
                    LOGGER.debug("Loop stopped, cancelling timer");
                    this.cancel();
                    return;
                }

                LOGGER.debug("Starting iteration {} of recording loop", loopIteration);

                saveRecordings();
                startRecordings();
                // set time of day to start of the loop
                serverWorld.setTimeOfDay(timeOfDay);

                // Restart playback commands
                executeCommand("mocap playback stop_all");
                executeCommand("mocap playback start .loop_scene");

                // Increment and update loop iteration in the config file
                loopIteration++;
                config.loopIteration = loopIteration;
                config.save();

                LOGGER.info("Completed loop iteration {}", loopIteration - 1);
            }
        }, looplength, looplength);
    }
    private void startRecordings() {
        // Start recording for every player
        for (String playerName : recordingPlayers) {
            executeCommand(String.format("mocap recording start %s", playerName));
        }
    }

    public void saveRecordings() {
        // Stop and save recordings for each player
        for (String playerName : recordingPlayers) {
            String recordingName = playerName + "_" + System.currentTimeMillis();

            LOGGER.debug("Processing recording for player: {}", playerName);
            executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
            executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
            if (recordingFileExists(recordingName)) {
                executeCommand(String.format("mocap scenes add_to loop_scene %s", recordingName.toLowerCase()));
            }
        }
    }

    public void stopLoop() {
        if (isLooping) {
            LOGGER.info("Stopping loop");
            isLooping = false;
            config.isLooping = false;
            saveRecordings();
            executeCommand("mocap playback stop_all");
            LOGGER.debug("Loop stopped!");
            if (recordingTimer != null) {
                recordingTimer.cancel();
                recordingTimer.purge();
                recordingTimer = new Timer();
                LOGGER.debug("Loop timer cancelled and reset");
            }
        }
    }

    public void executeCommand(String command) {
        if (server != null) {
            LOGGER.debug("Executing command: {}", command);
            // Execute the command without expecting a return value.
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
            LOGGER.debug("Command executed successfully: {}", command);
            // For commands like "mocap recording save" you might need an alternative method
            // to verify success (for example, by checking for expected side effects).
        } else {
            LOGGER.error("Attempted to execute command while server is null: {}", command);
        }
    }

    private boolean recordingFileExists(String recordingName) {
        // Build the complete path for the recording directory using the absolute world path
        Path recordingDir = worldFolder.resolve("mocap_files").resolve("recordings");
        Path recordingFile = recordingDir.resolve(recordingName.toLowerCase() + ".mcmocap_rec");

        boolean exists = recordingFile.toFile().exists();
        if (!exists) {
            LOGGER.error("Expected recording file does not exist: {}", recordingFile.toAbsolutePath());
        }
        return exists;
    }
}