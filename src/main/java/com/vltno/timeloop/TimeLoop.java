package com.vltno.timeloop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TimeLoop implements ModInitializer {
	public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TimeLoop");
	private Commands commands;
	private static MinecraftServer server;
	private ServerWorld serverWorld;

	// These fields will be initialized from the configuration file.
	public int loopIteration;
	public int loopTicks;
	public long timeOfDay; // Tracks the time of day
	public long timeSetting;
	public boolean trackTimeOfDay;
	public boolean isLooping;
	public int maxLoops;
	public String sceneName;
	private int tickCounter = 0; // Tracks elapsed ticks
	public int ticksLeft;
	private List<String> recordingPlayers; // Add this field
	
	public boolean showLoopInfo;
	public boolean trackItems;
	public LoopTypes loopType;

	// The configuration object loaded from disk
	public TimeLoopConfig config;

	// Get the world folder path for config/recording loading
	private Path worldFolder;


	@Override
	public void onInitialize() {
		LOOP_LOGGER.info("Initializing TimeLoop mod");
		recordingPlayers = new ArrayList<>(); // Initialize the list
		
		// Register the custom ArgumentType
		ArgumentTypeRegistry.registerArgumentType(Identifier.of("timeloop",""), LoopTypesArgumentType.class, ConstantArgumentSerializer.of(LoopTypesArgumentType::new));

		// Register commands
		commands = new Commands(this);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				commands.register(dispatcher, registryAccess, environment)
		);

		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			LOOP_LOGGER.info("Loop type: " + loopType);
			if (entity.isPlayer() && (loopType == LoopTypes.SLEEP) ) {
				LOOP_LOGGER.info("PLAYER SLEPT, LOOPING");
				runLoopIteration();
			}
		});
		
		// Register server started event
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Load configuration from the config folder provided by FabricLoader
			worldFolder = server.getSavePath(WorldSavePath.ROOT);
			config = TimeLoopConfig.load(worldFolder);

			loopIteration = config.loopIteration;
			loopTicks = config.loopTicks;
			isLooping = config.isLooping;
			timeOfDay = config.timeOfDay;
			timeSetting = config.timeSetting;
			trackTimeOfDay = config.trackTimeOfDay;
			ticksLeft = config.ticksLeft;
			sceneName = config.sceneName;
			
			showLoopInfo = config.showLoopInfo;
			trackItems = config.trackItems;
			loopType = config.loopType;
			
			TimeLoop.server = server;
			this.serverWorld = server.getOverworld();
			
			executeCommand("gamerule setCommandFeedback false");
			
			String loopInfo = (loopType == LoopTypes.TICKS ? "Ticks Left: " + loopTicks : loopType == LoopTypes.TIME_OF_DAY ? "Time left: " + (timeOfDay - timeSetting) : "");
			
			executeCommand(String.format("bossbar add loop_info \"%s\"", loopInfo));
			executeCommand("bossbar set minecraft:loop_info color yellow");
			executeCommand(String.format("bossbar set minecraft:loop_info value %s", loopTicks));
			executeCommand(String.format("bossbar set minecraft:loop_info max %s", loopTicks));
			executeCommand("bossbar set minecraft:loop_info players @a");
			
			executeCommand("mocap settings advanced experimental_release_warning false");
			executeCommand("mocap settings playback start_as_recorded true");
			executeCommand("mocap settings recording record_player_death false");
			
			executeCommand("mocap settings recording entity_tracking_distance 1");
			
			updateEntitiesToTrack(trackItems);
			
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
				LOOP_LOGGER.info("Starting recording for newly joined player: {}", playerName);
				executeCommand(String.format("mocap recording start %s", playerName));
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			String playerName = player.getName().getString();
			recordingPlayers.remove(playerName); // Remove from recording list
			if (isLooping) {
				LOOP_LOGGER.info("Saving recording for Disconnected player: {}", playerName);
				String recordingName = playerName + "_" + System.currentTimeMillis();
				executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
				executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
				if (recordingFileExists(recordingName)) {
					executeCommand(String.format("mocap scenes add_to %s %s", sceneName, recordingName.toLowerCase()));
				}
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (loopType != LoopTypes.DEATH) { return; }
			runLoopIteration();
		});
		
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (loopType == LoopTypes.SLEEP || loopType == LoopTypes.DEATH) { return; }
			if (loopType == LoopTypes.TIME_OF_DAY) { timeOfDay = serverWorld.getTimeOfDay(); };
			if (isLooping) {
				tickCounter++;
				ticksLeft = loopTicks - tickCounter;
				if (showLoopInfo && (loopType == LoopTypes.TICKS || loopType == LoopTypes.TIME_OF_DAY)) {
					executeCommand(String.format("bossbar set loop_info value %s", ticksLeft));
					executeCommand(String.format("bossbar set loop_info name \"%s\"", (loopType == LoopTypes.TICKS ? "Ticks Left: " + ticksLeft : loopType == LoopTypes.TIME_OF_DAY ? "Time left: " + (timeOfDay - timeSetting) : "")));
				}
				if (tickCounter >= loopTicks || ( timeOfDay == timeSetting && (loopType == LoopTypes.TIME_OF_DAY)) ) {
					tickCounter = 0; // Reset counter
					ticksLeft = loopTicks; // Reset
					runLoopIteration();
				}
			}
		});
		
		LOOP_LOGGER.info("TimeLoop mod initialized successfully");
	}

	/**
	 * Starts and initialises the loop.
	 */
	public void startLoop() {
		if (isLooping) {
			LOOP_LOGGER.info("Attempted to start already running recording loop");
			return;
		}
		if (showLoopInfo && (loopType == LoopTypes.TICKS || loopType == LoopTypes.TIME_OF_DAY)) {
			executeCommand("bossbar set minecraft:loop_info visible false");
			executeCommand("bossbar set minecraft:loop_info players @a");
		}
		isLooping = true;
		config.isLooping = true;
		timeOfDay = serverWorld.getTimeOfDay();
		config.timeOfDay = timeOfDay;
		tickCounter = 0;
		ticksLeft = loopTicks;
		LOOP_LOGGER.info("Starting Loop");
		startRecordings();
	}

	/**
	 * Runs the next iteration of the loop.
	 */
	private void runLoopIteration() {
		LOOP_LOGGER.info("Starting iteration {} of recording loop", loopIteration);
		saveRecordings();
		removeOldSceneEntries();
		startRecordings();
		if (trackTimeOfDay) { serverWorld.setTimeOfDay(timeOfDay); }
		executeCommand("mocap playback stop_all including_others");
		executeCommand(String.format("mocap playback start .%s", sceneName));
		loopIteration++;
		config.loopIteration = loopIteration;
		config.save();
		LOOP_LOGGER.info("Completed loop iteration {}", loopIteration - 1);
	}

	/**
	 * Starts the recordings.
	 */
	private void startRecordings() {
		// Start recording for every player
		for (String playerName : recordingPlayers) {
			executeCommand(String.format("mocap recording start %s", playerName));
		}
	}

	/**
	 * Saves the recordings.
	 */
	public void saveRecordings() {
		// Stop and save recordings for each player
		for (String playerName : recordingPlayers) {
			String recordingName = playerName + "_" + System.currentTimeMillis();

			LOOP_LOGGER.info("Processing recording for player: {}", playerName);
			executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
			executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
			if (recordingFileExists(recordingName)) {
				executeCommand(String.format("mocap scenes add_to %s %s", sceneName, recordingName.toLowerCase()));
			}
		}
	}

	/**
	 * Stops the loop.
	 */
	public void stopLoop() {
		if (isLooping) {
			if (showLoopInfo && (loopType == LoopTypes.TICKS || loopType == LoopTypes.TIME_OF_DAY)) { executeCommand("bossbar set minecraft:loop_info visible false"); }
			LOOP_LOGGER.info("Stopping loop");
			isLooping = false;
			config.isLooping = false;
			saveRecordings();
			executeCommand("mocap playback stop_all including_others");
			tickCounter = 0;
			ticksLeft = loopTicks;
			LOOP_LOGGER.info("Loop stopped!");
		}
	}

	/**
	 * Executes a minecraft chat command.
	 */
	public void executeCommand(String command) {
		if (server != null) {
			LOOP_LOGGER.info("Executing command: {}", command);
			// Execute the command without expecting a return value.
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
			LOOP_LOGGER.info("Command executed successfully: {}", command);
			// For commands like "mocap recording save" you might need an alternative method
			// to verify success (for example, by checking for expected side effects).
		} else {
			LOOP_LOGGER.error("Attempted to execute command while server is null: {}", command);
		}
	}

	/**
	 * Checks if a recording file with the specified name exists in the predefined recordings directory.
	 * The method returns a boolean indicating the existence of the file.
	 *
	 * @param recordingName The name of the recording file to check, without the file extension.
	 * @return true if the recording file exists, false otherwise.
	 */
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

	/**
	 * Removes outdated entries from the scene file to ensure the number of subscenes does not exceed the maximum allowed loops.
	 *
	 * The method checks if there are more recorded subscenes in the scene file than the value specified by maxLoops. If so, 
	 * it removes the oldest entries to maintain the desired number. The updated data is then saved back to the file.
	 *
	 */
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

	/**
	 * Updates the entities to be tracked for recording and playback settings.
	 * This method modifies the entities being tracked based on the specified parameter,
	 * enabling or disabling item tracking.
	 *
	 * @param items A boolean value. If true, includes items in the tracking list. 
	 *              If false, excludes items and tracks only vehicles.
	 */
	public void updateEntitiesToTrack(boolean items) {
		String entitiesToTrack = "@vehicles" + (items ? ";@items" : "");
		executeCommand(String.format("mocap settings playback record_entities %s", entitiesToTrack));
		executeCommand(String.format("mocap settings playback play_entities %s", entitiesToTrack));
	}
}

// use this instead is the future
// mocap playback stop_all including_others
// mocap playback start -_._._

// need to store the amount of playbacks stated for each player to make maxLoops work with this
// mocap playback stop 009--LuigiByte.LuigiByte.1