package com.vltno.timeloop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TimeLoopConfig {
    // These values will be loaded/saved from/to the JSON config file.bossbar set minecraft:loop_info
    public String scenePrefix = "loop_scene";
    public boolean firstStart = true;
    public int loopIteration = 1;
    public boolean isLooping = false;
    public int loopLengthTicks = 6000; // Default: 6000 ticks (i.e. 5 minutes)
    public int maxLoops = 0; // No limit by default
    public long timeSetting = 13000;
    public long startTimeOfDay = 0;
    public boolean trackTimeOfDay = true;
    public int ticksLeft;

    public boolean showLoopInfo = true;
    public boolean displayTimeInTicks = false;
    public boolean trackItems = false;
    public LoopTypes loopType = LoopTypes.TICKS;
    public boolean trackChat = false;
    public boolean hurtLoopedPlayers = false;

    public Map<String, PlayerData> recordingPlayers = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    /**
     * Loads the configuration from the world directory.
     * If the file does not exist, a default config is created and saved.
     *
     * @param configDir the config directory (usually obtained from FabricLoader)
     * @return an instance of LoopConfig
     */
    public static TimeLoopConfig load(Path configDir) {
        configPath = configDir.resolve("timeloop.json");
        TimeLoopConfig config = null;

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                config = GSON.fromJson(reader, TimeLoopConfig.class);
            } catch (Exception e) {
                System.err.println("Failed to load config file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (config == null) {
            config = new TimeLoopConfig();
            System.err.println("Config file not found or invalid. Generating a default configuration.");
        }

        // Validate recordingPlayers field and provide defaults if necessary
        if (config.recordingPlayers == null || !(config.recordingPlayers instanceof Map)) {
            System.err.println("Invalid or missing recordingPlayers data in config. Initializing with an empty map.");
            config.recordingPlayers = new HashMap<>();
        }

        config.save();
        return config;
    }

    /**
     * Saves the current configuration to disk.
     */
    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}