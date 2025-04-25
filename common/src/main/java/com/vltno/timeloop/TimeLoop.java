package com.vltno.timeloop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TimeLoop {
	public static final Logger LOOP_LOGGER = LoggerFactory.getLogger("TimeLoop");
	public static MinecraftServer server;
	public static ServerWorld serverWorld;

	public static LoopBossBar loopBossBar;

	// These fields will be initialized from the configuration file.
	public static int loopIteration;
	public static int loopLengthTicks;
	public static long startTimeOfDay; // The Time the Loop was started
	public static long timeSetting;
	public static boolean trackTimeOfDay;
	public static boolean isLooping;
	public static int maxLoops;
	public static int tickCounter = 0; // Tracks elapsed ticks
	public static int ticksLeft;

	public static boolean showLoopInfo;
	public static boolean displayTimeInTicks;
	public static boolean trackItems;
	public static LoopTypes loopType;
	public static boolean trackChat;
	public static boolean hurtLoopedPlayers;

	// The configuration object loaded from disk
	public static TimeLoopConfig config;

	// The loop scene manager object
	public static LoopSceneManager loopSceneManager;

	// Get the world folder path for config/recording loading
	public static Path worldFolder;

	public static boolean isDedicatedServer;

	public static void init(boolean isDedicatedServer) {
		TimeLoop.isDedicatedServer = isDedicatedServer;

		loopBossBar = new LoopBossBar();
		
		LOOP_LOGGER.info("Initializing TimeLoop mod (Common)");
	}
	
	/**
	 * Executes a minecraft chat command.
	 */
	public static void executeCommand(String command) {
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
	 * Runs the next iteration of the loop.
	 */
	public static void runLoopIteration() {
		LOOP_LOGGER.info("Starting iteration {} of loop", loopIteration);
		saveRecordings();
		removeOldSceneEntries();
		startRecordings();
		if (trackTimeOfDay) { serverWorld.setTimeOfDay(startTimeOfDay); }
		TimeLoop.executeCommand("mocap playback stop_all including_others");

		loopSceneManager.forEachRecordingPlayer(playerData -> {
			String playerName = playerData.getName();
			String playerNickname = playerData.getNickname();
			String playerSkin = playerData.getSkin();

			String playerSceneName = loopSceneManager.getPlayerSceneName(playerName);
			TimeLoop.executeCommand(String.format("mocap playback start .%s %s skin_from_player %s", playerSceneName, playerNickname, playerSkin));
		});

		loopIteration++;
		config.loopIteration = loopIteration;
		config.save();
		LOOP_LOGGER.info("Completed loop iteration {}", loopIteration - 1);
	}
	
	/**
	 * Starts and initialises the loop.
	 */
	public static void startLoop() {
		if (isLooping) {
			LOOP_LOGGER.info("Attempted to start already running recording loop");
			return;
		}
		if (showLoopInfo) {
			loopBossBar.visible(loopType.equals(LoopTypes.TICKS) || loopType.equals(LoopTypes.TIME_OF_DAY));
		}

		isLooping = true;
		config.isLooping = true;
		tickCounter = 0;
		ticksLeft = loopLengthTicks;
		LOOP_LOGGER.info("Starting Loop");
		startRecordings();
	}

	/**
	 * Starts the recordings.
	 */
	public static void startRecordings() {
		// Start recording for every player
		loopSceneManager.forEachRecordingPlayer(playerData -> {
			String playerName = playerData.getName();
			executeCommand(String.format("mocap recording start %s", playerName));
		});
	}

	/**
	 * Saves the recordings.
	 */
	public static void saveRecordings() {
		// Stop and save recordings for each player
		loopSceneManager.forEachRecordingPlayer(playerData -> {
			String playerName = playerData.getName();
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
	public static void stopLoop() {
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

	/**
	 * Checks if a recording file with the specified name exists in the predefined recordings directory.
	 * The method returns a boolean indicating the existence of the file.
	 *
	 * @param recordingName The name of the recording file to check, without the file extension.
	 * @return true if the recording file exists, false otherwise.
	 */
	private static boolean recordingFileExists(String recordingName) {
		// Build the complete path for the recording directory using the absolute world path
		Path recordingDir = worldFolder.resolve("mocap_files").resolve("recordings");
		Path recordingFile = recordingDir.resolve(recordingName.toLowerCase() + ".mcmocap_rec");

		boolean exists = recordingFile.toFile().exists();
		if (!exists) {
			LOOP_LOGGER.error("Expected recording file does not exist: {}", recordingFile.toAbsolutePath());
		}
		return exists;
	}

	public static void modifyPlayerAttributes(String targetPlayerName, String newPlayerNickname, String newSkin) {
		String playerSceneName = loopSceneManager.getPlayerSceneName(targetPlayerName);
		executeCommand(String.format("mocap scenes modify .%s %s player_skin skin_from_player %s", playerSceneName, newPlayerNickname, newSkin));

		loopSceneManager.forEachRecordingPlayer(playerData -> {
			if (playerData.getName().equals(targetPlayerName)) {
				playerData.setNickname(newPlayerNickname);
				playerData.setSkin(newSkin);
				LOOP_LOGGER.info("Modified loop attributes for player '{}' -> '{}' with skin '{}'", targetPlayerName, newPlayerNickname, newSkin);
			}
		});
	}

	/**
	 * Updates the entities to be tracked for recording and playback settings.
	 * This method modifies the entities being tracked based on the specified parameter,
	 * enabling or disabling item tracking.
	 *
	 * @param items A boolean value. If true, includes items in the tracking list.
	 *              If false, excludes items and tracks only vehicles.
	 */
	public static void updateEntitiesToTrack(boolean items) {
		String entitiesToTrack = "@vehicles" + (items ? ";@items" : "");
		executeCommand(String.format("mocap settings recording track_entities %s", entitiesToTrack));
		executeCommand(String.format("mocap settings playback play_entities %s", entitiesToTrack));
	}
	
	public static void updateInfoBar(int time, int timeLeft) {
		if (showLoopInfo && isLooping) {
			if (displayTimeInTicks) { loopBossBar.setBossBarName("Time Left: " + timeLeft); }
			else {loopBossBar.setBossBarName("Time Left: " + convertTicksToTime(timeLeft));}

			loopBossBar.setBossBarPercentage(time, timeLeft);
		}
	}

	/**
	 * Removes outdated entries from the scene file to ensure the number of subscenes does not exceed the maximum allowed loops.
	 * The method checks if there are more recorded subscenes in the scene file than the value specified by maxLoops. If so,
	 * it removes the oldest entries to maintain the desired number. The updated data is then saved back to the file.
	 *
	 */
	private static void removeOldSceneEntries() {
		if (isLooping && maxLoops > 1) {
			Path sceneDir = worldFolder.resolve("mocap_files").resolve("scenes");

			List<Path> sceneFiles = new ArrayList<>();
			loopSceneManager.forEachRecordingPlayer(playerData -> {
				String playerSceneName = loopSceneManager.getPlayerSceneName(playerData.getName());
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
						JsonParser parser = new JsonParser();
						JsonObject jsonObject = parser.parse(jsonContent).getAsJsonObject();
						JsonArray subScenes = jsonObject.getAsJsonArray("subscenes");

						if (subScenes.size() > maxLoops) {
							int entriesToRemove = subScenes.size() - maxLoops;
							JsonArray newSubScenes = new JsonArray();
							for (int i = entriesToRemove; i < subScenes.size(); i++) {
								newSubScenes.add(subScenes.get(i));
							}
							jsonObject.add("subScenes", newSubScenes);
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
	 * Converts time in ticks to HH:MM:SS
	 *
	 * @param ticksLeft A int value.
	 */
	public static String convertTicksToTime(int ticksLeft) {
		int timeLeft = ticksLeft / 20;
		int hours = timeLeft / 3600;
		int minutes = (timeLeft % 3600) / 60;
		int seconds = timeLeft % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}