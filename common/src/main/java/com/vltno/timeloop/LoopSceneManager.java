package com.vltno.timeloop;

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
        List<String> nickname = args.size() > 1 ? args.subList(1, args.size()) : null;
        List<String> skin = args.size() > 2 ? args.subList(2, args.size()) : null;
        
        if (playerName != null && !playerName.isEmpty()) {
            String tempNickname = (nickname == null || nickname.isEmpty()) ? playerName : nickname.getFirst();
            String tempSkin = (skin == null || skin.isEmpty()) ? playerName : skin.getFirst();
            
            // Use player name as the key and store a PlayerData object
            recordingPlayers.put(playerName, new PlayerData(playerName, tempNickname, tempSkin));
        } else {
            System.out.println("Invalid player data. Player not added.");
        }
    }


    // Method to remove a specific player from the recordingPlayers map
    public void removePlayer(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            PlayerData removed = recordingPlayers.remove(playerName);
            if (removed != null) {
                System.out.println("Player '" + playerName + "' removed successfully.");
            } else {
                System.out.println("Player '" + playerName + "' not found in the list.");
            }
        } else {
            System.out.println("Invalid player name. Player not removed.");
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
