package com.vltno.theloop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

public class TheLoop implements ModInitializer {
	public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TheLoop");
	private Commands commands;
	private static MinecraftServer server;
	private ServerWorld serverWorld;

	// These fields will be initialized from the configuration file.
	public int loopIteration;
	public int loopLength;
	public long timeOfDay;
	public long timeSetting;
	public boolean loopBasedOnTimeOfDay;
	public boolean loopOnSleep;
	public boolean isLooping;
	public int maxLoops;
	public String sceneName;
	private int tickCounter = 0; // Tracks elapsed ticks
	public int ticksLeft;
	private List<String> recordingPlayers; // Add this field

	// The configuration object loaded from disk
	public TheLoopConfig config;

	// Get the world folder path for config/recording loading
	private Path worldFolder;


	@Override
	public void onInitialize() {
		LOOP_LOGGER.info("Initializing TheLoop mod");
		recordingPlayers = new ArrayList<>(); // Initialize the list

		// Register commands
		commands = new Commands(this);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				commands.register(dispatcher, registryAccess, environment)
		);

		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			LOOP_LOGGER.info("Loop on sleep: " + loopOnSleep);
			if (entity.isPlayer() && loopOnSleep) {
				LOOP_LOGGER.info("PLAYER SLEPT, LOOPING");
				runLoopIteration();
			}
		});
		
		// Register server started event
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Load configuration from the config folder provided by FabricLoader
			worldFolder = server.getSavePath(WorldSavePath.ROOT);
			config = TheLoopConfig.load(worldFolder);

			loopIteration = config.loopIteration;
			loopLength = config.loopLength;
			isLooping = config.isLooping;
			timeOfDay = config.timeOfDay;
			timeSetting = config.timeSetting;
			loopBasedOnTimeOfDay = config.loopBasedOnTimeOfDay;
			loopOnSleep = config.loopOnSleep;
			ticksLeft = config.ticksLeft;
			sceneName = config.sceneName;
			
			TheLoop.server = server;
			this.serverWorld = server.getOverworld();
			
			executeCommand("mocap settings advanced experimental_release_warning false");
			executeCommand("mocap settings playback start_as_recorded true");
			executeCommand("mocap settings recording record_player_death false");
			executeCommand("mocap settings playback play_entities @none");
			executeCommand(String.format("mocap scenes add %s", sceneName));
			if (config.isLooping) {
				LOOP_LOGGER.info("Loop was active in config, automatically restarting loop.");
				// Reset the in-memory flag so that startLoop() does not return early.
				isLooping = false;
				executeCommand(String.format("mocap playback start .%s", sceneName));
				startLoop();
			}
		});

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
				LOOP_LOGGER.debug("Starting recording for newly joined player: {}", playerName);
				executeCommand(String.format("mocap recording start %s", playerName));
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			String playerName = player.getName().getString();
			recordingPlayers.remove(playerName); // Remove from recording list
			if (isLooping) {
				LOOP_LOGGER.debug("Saving recording for Disconnected player: {}", playerName);
				String recordingName = playerName + "_" + System.currentTimeMillis();
				executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
				executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
				if (recordingFileExists(recordingName)) {
					executeCommand(String.format("mocap scenes add_to %s %s", sceneName, recordingName.toLowerCase()));
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (loopOnSleep) { return; }
			if (loopBasedOnTimeOfDay) { timeOfDay = serverWorld.getTimeOfDay(); };
			if (isLooping) {
				tickCounter++;
				ticksLeft = loopLength - tickCounter;
				if (tickCounter >= loopLength || ( timeOfDay == timeSetting && loopBasedOnTimeOfDay) ) {
					tickCounter = 0; // Reset counter
					ticksLeft = loopLength; // Reset
					runLoopIteration();
				}
			}
		});

		LOOP_LOGGER.info("TheLoop mod initialized successfully");
	}

	public void startLoop() {
		if (isLooping) {
			LOOP_LOGGER.debug("Attempted to start already running recording loop");
			return;
		}
		isLooping = true;
		config.isLooping = true;
		timeOfDay = serverWorld.getTimeOfDay();
		config.timeOfDay = timeOfDay;
		tickCounter = 0;
		ticksLeft = loopLength;
		LOOP_LOGGER.info("Starting Loop");
		startRecordings();
	}

	private void runLoopIteration() {
		LOOP_LOGGER.debug("Starting iteration {} of recording loop", loopIteration);
		saveRecordings();
		removeOldSceneEntries();
		startRecordings();
		serverWorld.setTimeOfDay(timeOfDay);
		executeCommand("mocap playback stop_all including_others");
		executeCommand(String.format("mocap playback start .%s", sceneName));
		loopIteration++;
		config.loopIteration = loopIteration;
		config.save();
		LOOP_LOGGER.info("Completed loop iteration {}", loopIteration - 1);
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

			LOOP_LOGGER.debug("Processing recording for player: {}", playerName);
			executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
			executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
			if (recordingFileExists(recordingName)) {
				executeCommand(String.format("mocap scenes add_to %s %s", sceneName, recordingName.toLowerCase()));
			}
		}
	}

	public void stopLoop() {
		if (isLooping) {
			LOOP_LOGGER.info("Stopping loop");
			isLooping = false;
			config.isLooping = false;
			saveRecordings();
			executeCommand("mocap playback stop_all including_others");
			tickCounter = 0;
			ticksLeft = loopLength;
			LOOP_LOGGER.debug("Loop stopped!");
		}
	}

	public void executeCommand(String command) {
		if (server != null) {
			LOOP_LOGGER.debug("Executing command: {}", command);
			// Execute the command without expecting a return value.
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
			LOOP_LOGGER.debug("Command executed successfully: {}", command);
			// For commands like "mocap recording save" you might need an alternative method
			// to verify success (for example, by checking for expected side effects).
		} else {
			LOOP_LOGGER.error("Attempted to execute command while server is null: {}", command);
		}
	}

	private boolean recordingFileExists(String recordingName) {
		// Build the complete path for the recording directory using the absolute world path
		Path recordingDir = worldFolder.resolve("mocap_files").resolve("recordings");
		Path recordingFile = recordingDir.resolve(recordingName.toLowerCase() + ".mcmocap_rec");

		boolean exists = recordingFile.toFile().exists();
		if (!exists) {
			LOOP_LOGGER.error("Expected recording file does not exist: {}", recordingFile.toAbsolutePath());
		}
		return exists;
	}

	private void removeOldSceneEntries() {
		if (isLooping) {
			if (maxLoops > 1) {
				Path sceneDir = worldFolder.resolve("mocap_files").resolve("scenes");
				Path sceneFile = sceneDir.resolve(sceneName+".mcmocap_scene");

				// Check if the scene file exists
				if (sceneFile.toFile().exists()) {
					try {
						// Load the scene data from the file
						String jsonContent = new String(java.nio.file.Files.readAllBytes(sceneFile));

						// Parse the content
						com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonContent).getAsJsonObject();
						com.google.gson.JsonArray subScenes = jsonObject.getAsJsonArray("subscenes");

						// Check if we have more scenes than maxLoops
						if (subScenes.size() > maxLoops) {
							// Calculate the number of scenes to remove
							int entriesToRemove = subScenes.size() - maxLoops;
							// Remove the excess entries (removing from the start of the array)
							for (int i = 0; i < entriesToRemove; i++) {
								subScenes.remove(0); // Remove the first (oldest) entry
							}

							// Update the JSON object with the modified subScenes array
							jsonObject.add("subScenes", subScenes);

							// Write the updated JSON back to the file
							java.nio.file.Files.write(sceneFile, jsonObject.toString().getBytes());
							LOOP_LOGGER.info("Removed old scene entries to maintain maxLoops: {}", maxLoops);
						}
					} catch (java.io.IOException e) {
						LOOP_LOGGER.error("Failed to read or write scene file: {}", sceneFile, e);
					}
				} else {
					LOOP_LOGGER.error("Scene file does not exist: {}", sceneFile);
				}
			}
		}
	}
}

// use this instead is the future
// mocap playback stop_all including_others
// mocap playback start -_._._

// need to store the amount of playbacks stated for each player to make maxLoops work with this
// mocap playback stop 009--LuigiByte.LuigiByte.1