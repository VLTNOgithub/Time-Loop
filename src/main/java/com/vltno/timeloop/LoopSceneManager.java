package com.vltno.timeloop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoopSceneManager {
    private TimeLoopConfig config;
    private String scenePrefix;
    private List<String> recordingPlayers;

    // Constructor to initialize recordingPlayers
    public LoopSceneManager(TimeLoopConfig config) {
        this.config = config;
        this.scenePrefix = config.scenePrefix;
        this.recordingPlayers = new ArrayList<>();
    }

    // Method to add a player to the recordingPlayers list
    public void addPlayer(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            recordingPlayers.add(playerName);
        } else {
            System.out.println("Invalid player name. Player not added.");
        }
    }

    // Method to remove a specific player from the recordingPlayers list
    public void removePlayer(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            boolean removed = recordingPlayers.remove(playerName);
            if (removed) {
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
        for (String player : recordingPlayers) {
            playerSceneNames.add(getPlayerSceneName(player));
        }
        return playerSceneNames;
    }

    public void forEachPlayerSceneName(Consumer<String> action) {
        getAllPlayerSceneNames().forEach(action);
    }
    
    public void forEachRecordingPlayer(Consumer<String> action) {
        recordingPlayers.forEach(action);
    }
    
    public void setRecordingPlayers(List<String> recordingPlayers) {
        this.recordingPlayers = recordingPlayers;
    }
    
    public void saveRecordingPlayers() {
        config.recordingPlayers = this.recordingPlayers;
    }
}
