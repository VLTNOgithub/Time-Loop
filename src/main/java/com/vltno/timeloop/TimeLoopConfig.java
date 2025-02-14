package com.vltno.timeloop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TimeLoopConfig {
    // These values will be loaded/saved from/to the JSON config file.
    public String sceneName = "loop_scene";
    public int loopIteration = 1;
    public boolean isLooping = false;
    public int loopTicks = 6000; // Default: 6000 ticks (i.e. 5 minutes)
    public int maxLoops = 0; //No limit by default
    public long timeSetting = 13000;
    public long timeOfDay = 0;
    public boolean loopTimeOfDay = true;
    public int ticksLeft;
    
    public boolean trackItems = false;
    public LoopTypes loopType = LoopTypes.TICKS;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    /**
     * Loads the configuration from the specified config directory.
     * If the file does not exist, a default config is created and saved.
     *
     * @param configDir the config directory (usually obtained from FabricLoader)
     * @return an instance of TheLoopConfig
     */
    public static TimeLoopConfig load(Path configDir) {
        configPath = configDir.resolve("timeloop.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                TimeLoopConfig config = GSON.fromJson(reader, TimeLoopConfig.class);
                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Create default config if none exists
        TimeLoopConfig config = new TimeLoopConfig();
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