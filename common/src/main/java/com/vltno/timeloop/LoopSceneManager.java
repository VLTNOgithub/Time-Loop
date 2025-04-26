package com.vltno.timeloop;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LoopSceneManager {
    private TimeLoopConfig config;
    private String scenePrefix;
    private Map<String, PlayerData> recordingPlayers;

    // Constructor to initialize recordingPlayers map
    public LoopSceneManager(TimeLoopConfig config) {
        this.config = config;
        this.scenePrefix = config.scenePrefix;
        this.recordingPlayers = new HashMap<>();
    }

    // Method to add a player to the recordingPlayers map
    public void addPlayer(List<String> args) {
        String playerName = args.get(0);
        
        String nickname = args.size() > 1 ? args.get(1) : null;
        String skin = args.size() > 2 ? args.get(2) : null;
        String rewindPosition = args.size() > 3 ? args.get(3) : null;
        
        if (playerName == null || playerName.isEmpty()) {
            TimeLoop.LOOP_LOGGER.error("Player name is null or empty. Skipping player addition.");
            return;
        }

        String tempNickname = (nickname == null || nickname.isEmpty()) ? playerName : nickname;
        String tempSkin = (skin == null || skin.isEmpty()) ? playerName : skin;
        
        Vec3 tempRewindPosition = Vec3.ZERO;
        
        if (rewindPosition != null && !rewindPosition.isEmpty()) {
            try {
                String processedString = rewindPosition.trim();
                
                if ((processedString.startsWith("(") && processedString.endsWith(")")) ||
                        (processedString.startsWith("[") && processedString.endsWith("]"))) {
                    processedString = processedString.substring(1, processedString.length() - 1).trim();
                }
                
                String[] positionParts = processedString.split("\\s*,\\s*");
                
                if (positionParts.length == 3) {
                    double x = Double.parseDouble(positionParts[0]);
                    double y = Double.parseDouble(positionParts[1]);
                    double z = Double.parseDouble(positionParts[2]);
                    tempRewindPosition = new Vec3(x, y, z);
                }
            } catch (NumberFormatException e) {
                TimeLoop.LOOP_LOGGER.error("Invalid rewind position format. Skipping rewind position.");
            }
        }
        
        PlayerData playerData = new PlayerData(playerName, tempNickname, tempSkin, tempRewindPosition);
        
        recordingPlayers.put(playerName, playerData);
    }


    // Method to remove a specific player from the recordingPlayers map
    public void removePlayer(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            PlayerData removed = recordingPlayers.remove(playerName);
            if (removed != null) {
                TimeLoop.LOOP_LOGGER.info("Player '" + playerName + "' removed successfully.");
            } else {
                TimeLoop.LOOP_LOGGER.info("Player '" + playerName + "' not found in the list.");
            }
        } else {
            TimeLoop.LOOP_LOGGER.info("Invalid player name. Player not removed.");
        }
    }


    // Method to generate playerSceneName for a given player
    public String getPlayerSceneName(String playerName) {
        return (playerName.startsWith(scenePrefix)) ? playerName : (scenePrefix + "_" + playerName).toLowerCase();
    }

    // Method to get all playerSceneNames for recordingPlayers
    public List<String> getAllPlayerSceneNames() {
        List<String> playerSceneNames = new ArrayList<>();
        for (PlayerData player : recordingPlayers.values()) {
            playerSceneNames.add(getPlayerSceneName(player.getName()));
        }
        return playerSceneNames;
    }

    public void forEachPlayerSceneName(Consumer<String> action) {
        getAllPlayerSceneNames().forEach(action);
    }

    // Method to perform an action for each recording player
    public void forEachRecordingPlayer(Consumer<PlayerData> action) {
        recordingPlayers.values().forEach(action);
    }
    
    // Method to set recording players with a new map
    public void setRecordingPlayers(Map<String, PlayerData> recordingPlayers) {
        this.recordingPlayers = recordingPlayers;
    }

    public void saveRecordingPlayers() {
        config.recordingPlayers = new HashMap<>(recordingPlayers);
    }
}
