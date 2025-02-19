package com.vltno.timeloop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TimeLoop implements ModInitializer {
	public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TimeLoop");
	private Commands commands;
	private static MinecraftServer server;
	public ServerWorld serverWorld;

	public LoopBossBar loopBossBar;

	// These fields will be initialized from the configuration file.
	public int loopIteration;
	public int loopLengthTicks;
	public long startTimeOfDay; // The Time the Loop was started
	public long timeSetting;
	public boolean trackTimeOfDay;
	public boolean isLooping;
	public int maxLoops;
	private int tickCounter = 0; // Tracks elapsed ticks
	public int ticksLeft;

	public boolean showLoopInfo;
	public boolean displayTimeInTicks;
	public boolean trackItems;
	public LoopTypes loopType;

	// The configuration object loaded from disk
	public TimeLoopConfig config;
	
	// The loop scene manager object
	public LoopSceneManager loopSceneManager;

	// Get the world folder path for config/recording loading
	private Path worldFolder;


	@Override
	public void onInitialize() {
		LOOP_LOGGER.info("Initializing TimeLoop mod");

		// Register the custom ArgumentType
		ArgumentTypeRegistry.registerArgumentType(Identifier.of("timeloop",""), LoopTypesArgumentType.class, ConstantArgumentSerializer.of(LoopTypesArgumentType::new));

		// Register commands
		commands = new Commands(this);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				commands.register(dispatcher, registryAccess, environment)
		);

		// BossBar
		loopBossBar = new LoopBossBar();

		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			if (entity.isPlayer() && (loopType == LoopTypes.SLEEP) ) {
				LOOP_LOGGER.info("Player slept, looping.");
				runLoopIteration();
			}
		});
		
		// Register server started event
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Load configuration from the config folder provided by FabricLoader
			worldFolder = server.getSavePath(WorldSavePath.ROOT);
			config = TimeLoopConfig.load(worldFolder);

			// Loop scene manager
			loopSceneManager = new LoopSceneManager(config);

			loopIteration = config.loopIteration;
			loopLengthTicks = config.loopLengthTicks;
			isLooping = config.isLooping;
			startTimeOfDay = config.startTimeOfDay;
			timeSetting = config.timeSetting;
			trackTimeOfDay = config.trackTimeOfDay;
			ticksLeft = config.ticksLeft;
			
			showLoopInfo = config.showLoopInfo;
			displayTimeInTicks = config.displayTimeInTicks;
			trackItems = config.trackItems;
			loopType = config.loopType;
			
			loopSceneManager.setRecordingPlayers(config.recordingPlayers);
			
			TimeLoop.server = server;
			serverWorld = server.getOverworld();

			// Loop boss bar info
			String loopInfo = (loopType == LoopTypes.TICKS ? "Ticks Left: " + loopLengthTicks : loopType == LoopTypes.TIME_OF_DAY ? "Time left: " + (startTimeOfDay - timeSetting) : "");
			loopBossBar.visible(false);
			loopBossBar.setBossBarName(loopInfo);

			// set mocap settings
			executeCommand("mocap settings advanced experimental_release_warning false");
			executeCommand("mocap settings playback start_as_recorded true");
			executeCommand("mocap settings recording record_player_death false");
			executeCommand("mocap settings recording entity_tracking_distance 1");
			
			updateEntitiesToTrack(trackItems);
			
			try {
				loopSceneManager.forEachPlayerSceneName(playerSceneName -> {
					executeCommand(String.format("mocap scenes add %s", playerSceneName));
				});
			} catch (Error e) {
				LOOP_LOGGER.error("Failed to add player scenes to mocap scenes: {}", e.getMessage());
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

			loopSceneManager.addPlayer(playerName);
			loopBossBar.addPlayer(player);
			
			executeCommand(String.format("mocap scenes add %s", loopSceneManager.getPlayerSceneName(playerName)));
			
			if (config.firstStart) {
				config.firstStart = false;
				config.save();
				
				LOOP_LOGGER.info("First start detected, sending message to ops.");
				
				if (server.getPlayerManager().isOperator(player.getGameProfile())) {
					player.sendMessage(Text.literal(("Use '/loop start' to start the time loop!")));
				}
			}
			
			if (isLooping) {
				LOOP_LOGGER.info("Starting recording for newly joined player: {}", playerName);
				executeCommand(String.format("mocap recording start %s", playerName));
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			String playerName = player.getName().getString();
			
			loopSceneManager.removePlayer(playerName);
			loopBossBar.removePlayer(player);
			if (isLooping) {
				saveRecordings();
				loopSceneManager.saveRecordingPlayers();
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (loopType != LoopTypes.DEATH) { return; }
			runLoopIteration();
		});
		
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (loopType == LoopTypes.SLEEP || loopType == LoopTypes.DEATH) { return; }

			if (isLooping) {
				if (loopType == LoopTypes.TIME_OF_DAY) {
					if (timeSetting <= startTimeOfDay) { // prevent stupid 1 tick loop bug
						startTimeOfDay = 0;
						config.startTimeOfDay = 0;}

					long time = (serverWorld.getTimeOfDay() > 24000 ? serverWorld.getTimeOfDay() % 24000 : serverWorld.getTimeOfDay());

					long timeLeft = (time > timeSetting) ? Math.abs(serverWorld.getTimeOfDay() - (2 * timeSetting)) : Math.abs(time - timeSetting);

					updateInfoBar((int)timeSetting, (int)timeLeft);
					if (Math.abs(timeSetting - timeLeft) >= timeSetting) {
						runLoopIteration();
					}
				}

				else if (loopType == LoopTypes.TICKS) {
					tickCounter++;
					ticksLeft = loopLengthTicks - tickCounter;

					updateInfoBar(loopLengthTicks, ticksLeft);
					if (tickCounter >= loopLengthTicks) {
						tickCounter = 0; // Reset counter
						ticksLeft = loopLengthTicks; // Reset
						runLoopIteration();
					}
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
        if (showLoopInfo) { loopBossBar.visible(true); }
		isLooping = true;
		config.isLooping = true;
		tickCounter = 0;
		ticksLeft = loopLengthTicks;
		LOOP_LOGGER.info("Starting Loop");
		startRecordings();
	}

	/**
	 * Runs the next iteration of the loop.
	 */
	private void runLoopIteration() {
		LOOP_LOGGER.info("Starting iteration {} of loop", loopIteration);
		saveRecordings();
		removeOldSceneEntries();
		startRecordings();
		if (trackTimeOfDay) { serverWorld.setTimeOfDay(startTimeOfDay); }
		executeCommand("mocap playback stop_all including_others");
		
		loopSceneManager.forEachPlayerSceneName(playerSceneName -> {
			executeCommand(String.format("mocap playback start .%s", playerSceneName));
		});
		
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
		loopSceneManager.forEachRecordingPlayer(playerName -> {
			executeCommand(String.format("mocap recording start %s", playerName));
		});
	}

	/**
	 * Saves the recordings.
	 */
	public void saveRecordings() {
		// Stop and save recordings for each player
		loopSceneManager.forEachRecordingPlayer(playerName -> {
			String recordingName = playerName + "_" + System.currentTimeMillis();

			String playerSceneName = loopSceneManager.getPlayerSceneName(playerName);

			LOOP_LOGGER.info("Processing recording for player: {}", playerName);
			executeCommand(String.format("mocap recording stop -+mc.%s.1", playerName));
			executeCommand(String.format("mocap recording save %s -+mc.%s.1", recordingName.toLowerCase(), playerName));
			if (recordingFileExists(recordingName)) {
				executeCommand(String.format("mocap scenes add_to %s %s", playerSceneName, recordingName.toLowerCase()));
			}
		});
	}

	/**
	 * Stops the loop.
	 */
	public void stopLoop() {
		if (isLooping) {
			LOOP_LOGGER.info("Stopping loop");
			isLooping = false;
			config.isLooping = false;
			loopBossBar.visible(false);
			saveRecordings();
			loopSceneManager.saveRecordingPlayers();
			executeCommand("mocap playback stop_all including_others");
			tickCounter = 0;
			ticksLeft = loopLengthTicks;
			LOOP_LOGGER.info("Loop stopped!");
		}
	}

	public void updateInfoBar(int time, int timeLeft) {
		if (showLoopInfo && isLooping) {
			if (displayTimeInTicks) { loopBossBar.setBossBarName("Time Left: " + timeLeft); }
			else {loopBossBar.setBossBarName("Time Left: " + convertTicksToTime(timeLeft));}

			loopBossBar.setBossBarPercentage(time, timeLeft);
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
	 * The method checks if there are more recorded subscenes in the scene file than the value specified by maxLoops. If so,
	 * it removes the oldest entries to maintain the desired number. The updated data is then saved back to the file.
	 *
	 */
	private void removeOldSceneEntries() {
		if (isLooping && maxLoops > 1) {
			Path sceneDir = worldFolder.resolve("mocap_files").resolve("scenes");

			List<Path> sceneFiles = new ArrayList<>();
			loopSceneManager.forEachRecordingPlayer(playerSceneName -> {
				if (playerSceneName != null && !playerSceneName.isBlank()) {
					sceneFiles.add(sceneDir.resolve(playerSceneName + ".mcmocap_scene"));
				} else {
					LOOP_LOGGER.warn("Invalid playerSceneName encountered: {}", playerSceneName);
				}
			});

			if (sceneFiles.isEmpty()) {
				LOOP_LOGGER.warn("No scene files found to process.");
			}

			for (Path sceneFile : sceneFiles) {
				if (sceneFile.toFile().exists()) {
					try {
						String jsonContent = new String(Files.readAllBytes(sceneFile));
						JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
						JsonArray subScenes = jsonObject.getAsJsonArray("subscenes");

						if (subScenes.size() > maxLoops) {
							int entriesToRemove = subScenes.size() - maxLoops;
							for (int i = 0; i < entriesToRemove; i++) {
								subScenes.remove(0); // Remove the oldest
							}
							jsonObject.add("subScenes", subScenes);
							Files.write(sceneFile, jsonObject.toString().getBytes());
							LOOP_LOGGER.info("Removed old scene entries for file: {}", sceneFile);
						}
					} catch (IOException e) {
						LOOP_LOGGER.error("Failed to process scene file: {}", sceneFile, e);
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
		executeCommand(String.format("mocap settings recording track_entities %s", entitiesToTrack));
		executeCommand(String.format("mocap settings playback play_entities %s", entitiesToTrack));
	}

	/**
	 * Converts time in ticks to HH:MM:SS
	 *
	 * @param ticksLeft A int value.
	 */
	public String convertTicksToTime(int ticksLeft) {
		int timeLeft = ticksLeft / 20;
		int hours = timeLeft / 3600;
		int minutes = (timeLeft % 3600) / 60;
		int seconds = timeLeft % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}

// use this in the future
// mocap playback start -_._._   starts pending recordings (results in a smoother experience)

// need to store the amount of playbacks stated for each player to make maxLoops work with this
// mocap playback stop 009--LuigiByte.LuigiByte.1