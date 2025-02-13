package com.vltno.theloop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TheLoopConfig {
    // These values will be loaded/saved from/to the JSON config file.
    public String sceneName = "loop_scene";
    public int loopIteration = 1;
    public boolean isLooping = false;
    public int loopLength = 6000; // Default: 6000 ticks (i.e. 5 minutes)
    public int maxLoops = 0; //No limit by default
    public long timeSetting = 0;
    public long timeOfDay = 0;
    public boolean loopBasedOnTimeOfDay;
    public boolean loopOnSleep;
    public int ticksLeft;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    /**
     * Loads the configuration from the specified config directory.
     * If the file does not exist, a default config is created and saved.
     *
     * @param configDir the config directory (usually obtained from FabricLoader)
     * @return an instance of TheLoopConfig
     */
    public static TheLoopConfig load(Path configDir) {
        configPath = configDir.resolve("theloop.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                TheLoopConfig config = GSON.fromJson(reader, TheLoopConfig.class);
                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Create default config if none exists
        TheLoopConfig config = new TheLoopConfig();
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