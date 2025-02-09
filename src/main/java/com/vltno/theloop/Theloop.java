package com.vltno.theloop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Theloop implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("theloop");
    private Commands commands;
    private static MinecraftServer server;
    private ServerWorld serverWorld;

    // These fields will be initialized from the configuration file.
    public int loopIteration;
    public int looplength;
    public long timeOfDay;
    private Timer recordingTimer;
    public boolean isLooping;
    private List<String> recordingPlayers; // Add this field

    // The configuration object loaded from disk
    public TheLoopConfig config;


    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TheLoop mod");
        recordingPlayers = new ArrayList<>(); // Initialize the list

        // Load configuration from the config folder provided by FabricLoader
        Path configDir = FabricLoader.getInstance().getConfigDir();
        config = TheLoopConfig.load(configDir);
        loopIteration = config.loopIteration;
        looplength = config.loopLength;
        isLooping = config.isLooping;
        timeOfDay = config.timeOfDay;

        // Register commands
        commands = new Commands(this);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                commands.register(dispatcher, registryAccess, environment)
        );

        // Register server started event
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Theloop.server = server;
            this.serverWorld = server.getOverworld();
            executeCommand("mocap settings recording record_player_death false");
            executeCommand("mocap settings recording track_entities @items");
            executeCommand("mocap scenes add main_scene");
            if (config.isLooping) {
                LOGGER.info("Loop was active in config, automatically restarting loop.");
                // Reset the in-memory flag so that startLoop() does not return early.
                isLooping = false;
                startLoop();
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
                String recordingName = playerName + "_" + loopIteration + "_" + System.currentTimeMillis();
                executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
                executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
                if (recordingFileExists(recordingName)) {
                    executeCommand(String.format("mocap scenes add_to main_scene %s", recordingName.toLowerCase()));
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

                // Broadcast status to operators
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    if (player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("Loop iteration " + loopIteration + " completed"));
                    }
                });

                // Restart playback commands
                executeCommand("mocap playback stop_all");
                executeCommand("mocap playback start .main_scene");

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
            String recordingName = playerName + "_" + loopIteration + "_" + System.currentTimeMillis();

            LOGGER.debug("Processing recording for player: {}", playerName);
            executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
            executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
            if (recordingFileExists(recordingName)) {
                executeCommand(String.format("mocap scenes add_to main_scene %s", recordingName.toLowerCase()));
            }
        }
    }

    public void stopRecordingLoop() {
        if (isLooping) {
            LOGGER.info("Stopping loop");
            isLooping = false;
            config.isLooping = false;
            saveRecordings();
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
        // Construct the directory path using the system's file separator for cross-platform compatibility.
        File recordingDir = new File("world" + File.separator + "mocap_files" + File.separator + "recordings");
        // The expected file name has the .mcmocap_rec extension.
        File file = new File(recordingDir, recordingName.toLowerCase() + ".mcmocap_rec");
        boolean exists = file.exists();
        if (!exists) {
            LOGGER.error("Expected recording file does not exist: {}", file.getAbsolutePath());
        }
        return exists;
    }
}
