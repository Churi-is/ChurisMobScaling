package net.churismobscaling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.client.render.HudPosition;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the configuration settings for the Churis Mob Scaling mod.
 * This class handles loading settings from a JSON file, saving settings to the file,
 * and providing access to individual configuration values.
 * Default values are provided if a configuration file is not found or is invalid.
 */
public class ModConfig {
    private static final String CONFIG_FILE_EXTENSION = ".json";

    // Default values for configuration settings
    public static final boolean DEFAULT_ENABLE_MOD = true;
    public static final int DEFAULT_MAX_LEVEL = 100;
    public static final int MIN_MAX_LEVEL = 1;
    public static final int MAX_MAX_LEVEL = 200;
    public static final float DEFAULT_NOISE_SCALE = 2048.0f;
    public static final float DEFAULT_MIN_NOISE_LEVEL_PERCENTAGE = 0.25f;
    public static final float MIN_MIN_NOISE_LEVEL_PERCENTAGE = 0f;
    public static final float MAX_MIN_NOISE_LEVEL_PERCENTAGE = 1f;
    public static final int DEFAULT_SPAWN_INFLUENCE_RADIUS = 3500;
    public static final int MIN_SPAWN_INFLUENCE_RADIUS = 100;
    public static final int MAX_SPAWN_INFLUENCE_RADIUS = 10000;

    public static final float DEFAULT_MIN_TOTAL_HEALTH_BONUS = -0.75f;
    public static final float MIN_MIN_TOTAL_HEALTH_BONUS = -0.99f;
    public static final float MAX_MIN_TOTAL_HEALTH_BONUS = 0f;
    public static final float DEFAULT_MAX_TOTAL_HEALTH_BONUS = 10.0f;
    public static final float MIN_MAX_TOTAL_HEALTH_BONUS = 0f;

    public static final float DEFAULT_MIN_TOTAL_ATTACK_BONUS = -0.75f;
    public static final float MIN_MIN_TOTAL_ATTACK_BONUS = -0.99f;
    public static final float MAX_MIN_TOTAL_ATTACK_BONUS = 0f;
    public static final float DEFAULT_MAX_TOTAL_ATTACK_BONUS = 5.0f;
    public static final float MIN_MAX_TOTAL_ATTACK_BONUS = 0f;

    public static final float DEFAULT_MIN_TOTAL_LOOT_BONUS = -0.2f;
    public static final float MIN_MIN_TOTAL_LOOT_BONUS = -0.99f;
    public static final float MAX_MIN_TOTAL_LOOT_BONUS = 0f;
    public static final float DEFAULT_MAX_TOTAL_LOOT_BONUS = 4.0f;
    public static final float MIN_MAX_TOTAL_LOOT_BONUS = 0f;

    public static final float DEFAULT_MIN_TOTAL_XP_BONUS = -0.2f;
    public static final float MIN_MIN_TOTAL_XP_BONUS = -0.99f;
    public static final float MAX_MIN_TOTAL_XP_BONUS = 0f;
    public static final float DEFAULT_MAX_TOTAL_XP_BONUS = 12.0f;
    public static final float MIN_MAX_TOTAL_XP_BONUS = 0f;

    public static final boolean DEFAULT_ENABLE_MOB_LABEL_HUD = true;
    public static final float DEFAULT_MOB_LABEL_HUD_SCALE = 0.5f;
    public static final float MIN_MOB_LABEL_HUD_SCALE = 0.5f;
    public static final float MAX_MOB_LABEL_HUD_SCALE = 2.0f;
    public static final int DEFAULT_MOB_LABEL_HUD_DURATION = 1500;
    public static final int MIN_MOB_LABEL_HUD_DURATION = 500;
    public static final int MAX_MOB_LABEL_HUD_DURATION = 10000;

    public static final boolean DEFAULT_ENABLE_LOCATION_LEVEL_HUD = false;
    public static final float DEFAULT_LOCATION_LEVEL_HUD_SCALE = 1.0f;
    public static final float MIN_LOCATION_LEVEL_HUD_SCALE = 0.5f;
    public static final float MAX_LOCATION_LEVEL_HUD_SCALE = 2.0f;
    // HudPosition.TOP_LEFT is an enum, not a magic number, so no constant needed here.

    // Core settings
    private boolean enableMod = DEFAULT_ENABLE_MOD;
    private int maxLevel = DEFAULT_MAX_LEVEL;
    private float noiseScale = DEFAULT_NOISE_SCALE;
    private float minNoiseLevelPercentage = DEFAULT_MIN_NOISE_LEVEL_PERCENTAGE;
    private int spawnInfluenceRadius = DEFAULT_SPAWN_INFLUENCE_RADIUS;

    // Health modifiers
    private float minTotalHealthBonus = DEFAULT_MIN_TOTAL_HEALTH_BONUS;
    private float maxTotalHealthBonus = DEFAULT_MAX_TOTAL_HEALTH_BONUS;

    // Attack modifiers
    private float minTotalAttackBonus = DEFAULT_MIN_TOTAL_ATTACK_BONUS;
    private float maxTotalAttackBonus = DEFAULT_MAX_TOTAL_ATTACK_BONUS;

    // Loot modifiers
    private float minTotalLootBonus = DEFAULT_MIN_TOTAL_LOOT_BONUS;
    private float maxTotalLootBonus = DEFAULT_MAX_TOTAL_LOOT_BONUS;
    
    // XP modifiers
    private float minTotalXpBonus = DEFAULT_MIN_TOTAL_XP_BONUS;
    private float maxTotalXpBonus = DEFAULT_MAX_TOTAL_XP_BONUS;

    // Visual effects settings
    
    // Mob label HUD settings
    private boolean enableMobLabelHud = DEFAULT_ENABLE_MOB_LABEL_HUD;
    private float mobLabelHudScale = DEFAULT_MOB_LABEL_HUD_SCALE;
    private int mobLabelHudDuration = DEFAULT_MOB_LABEL_HUD_DURATION;
    
    // Location level HUD settings
    private boolean enableLocationLevelHud = DEFAULT_ENABLE_LOCATION_LEVEL_HUD;
    private float locationLevelHudScale = DEFAULT_LOCATION_LEVEL_HUD_SCALE;
    private int locationLevelHudPosition = HudPosition.TOP_LEFT.getIndex(); // Default to top left position

    // Dimension settings
    private boolean useDimensionOriginForDistance = false;
    private Map<String, Integer> dimensionLevelOffsets = new HashMap<>();

    // Mob blacklist
    private List<String> mobBlacklist = new ArrayList<>();

    /**
     * Constructs a ModConfig instance with default settings.
     * Initializes default dimension level offsets and the mob blacklist.
     */
    public ModConfig() {
        // Initialize default dimension offsets
        dimensionLevelOffsets.put("minecraft:the_nether", 10);
        dimensionLevelOffsets.put("minecraft:the_end", 20);
        
        // Initialize default mob blacklist
        mobBlacklist.add("minecraft:armor_stand");
        mobBlacklist.add("minecraft:item_frame");
        mobBlacklist.add("minecraft:glow_item_frame");
        mobBlacklist.add("minecraft:painting");
        mobBlacklist.add("minecraft:marker");
        mobBlacklist.add("minecraft:ender_dragon");
        mobBlacklist.add("minecraft:wither");
    }

    /**
     * Loads the configuration from the JSON file, or creates a new one with default
     * values if the file doesn't exist or an error occurs during reading.
     * The configuration file is named based on the Mod ID.
     *
     * @return The loaded or newly created {@link ModConfig} instance.
     */
    public static ModConfig loadOrCreateConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, ChurisMobScalingMain.MOD_ID + CONFIG_FILE_EXTENSION);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ModConfig config;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, ModConfig.class);
                ChurisMobScalingMain.LOGGER.info("Loaded Churis Mob Scaling configuration");
                // Ensure config is not null if file is empty or malformed leading to null
                if (config == null) {
                    ChurisMobScalingMain.LOGGER.warn("Config file was empty or malformed, creating new default config.");
                    config = new ModConfig();
                    // Attempt to save the new default config immediately
                    config.saveConfig(); // This will use the new saveConfig which logs info/errors
                } else {
                    // TODO: Maybe implement a migration system to handle versioning and adding
                    // new default fields that might be missing from older config files. This would
                    // ensure backward compatibility as the mod evolves.
                }
                return config;
            } catch (IOException | com.google.gson.JsonSyntaxException e) { // Catch JsonSyntaxException as well
                ChurisMobScalingMain.LOGGER.error("Failed to read or parse config file, creating new default config.", e);
            }
        }

        // If we got here, the file doesn't exist or there was an error reading/parsing it
        config = new ModConfig();
        try {
            if (!configDir.exists()) {
                if (!configDir.mkdirs()) {
                    ChurisMobScalingMain.LOGGER.error("Failed to create config directory: " + configDir.getAbsolutePath());
                    // Not returning here, will try to write to current dir if path is just filename
                }
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
                ChurisMobScalingMain.LOGGER.info("Created new Churis Mob Scaling configuration: " + configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            ChurisMobScalingMain.LOGGER.error("Failed to write initial config file: " + configFile.getAbsolutePath(), e);
        }

        return config;
    }

    /**
     * Saves the current configuration state to the JSON file.
     * If the configuration directory does not exist, it will be created.
     * The configuration file is named based on the Mod ID.
     */
    public void saveConfig() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, ChurisMobScalingMain.MOD_ID + CONFIG_FILE_EXTENSION);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            if (!configDir.exists()) {
                if (!configDir.mkdirs()) {
                    ChurisMobScalingMain.LOGGER.error("Failed to create config directory during save: " + configDir.getAbsolutePath());
                    return; // Don't proceed if directory creation fails
                }
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(this, writer);
                ChurisMobScalingMain.LOGGER.info("Saved Churis Mob Scaling configuration: " + configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            ChurisMobScalingMain.LOGGER.error("Failed to write config file: " + configFile.getAbsolutePath(), e);
        }
    }

    public boolean getEnableMod() {
        return enableMod;
    }

    public void setEnableMod(boolean enableMod) {
        this.enableMod = enableMod;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public float getNoiseScale() {
        return noiseScale;
    }

    public void setNoiseScale(float noiseScale) {
        this.noiseScale = noiseScale;
    }

    public float getMinNoiseLevelPercentage() {
        return minNoiseLevelPercentage;
    }

    public void setMinNoiseLevelPercentage(float minNoiseLevelPercentage) {
        this.minNoiseLevelPercentage = minNoiseLevelPercentage;
    }

    public int getSpawnInfluenceRadius() {
        return spawnInfluenceRadius;
    }

    public void setSpawnInfluenceRadius(int spawnInfluenceRadius) {
        this.spawnInfluenceRadius = spawnInfluenceRadius;
    }

    public float getMinTotalHealthBonus() {
        return minTotalHealthBonus;
    }

    public void setMinTotalHealthBonus(float minTotalHealthBonus) {
        this.minTotalHealthBonus = minTotalHealthBonus;
    }

    public float getMaxTotalHealthBonus() {
        return maxTotalHealthBonus;
    }

    public void setMaxTotalHealthBonus(float maxTotalHealthBonus) {
        this.maxTotalHealthBonus = maxTotalHealthBonus;
    }

    public float getMinTotalAttackBonus() {
        return minTotalAttackBonus;
    }

    public void setMinTotalAttackBonus(float minTotalAttackBonus) {
        this.minTotalAttackBonus = minTotalAttackBonus;
    }

    public float getMaxTotalAttackBonus() {
        return maxTotalAttackBonus;
    }

    public void setMaxTotalAttackBonus(float maxTotalAttackBonus) {
        this.maxTotalAttackBonus = maxTotalAttackBonus;
    }

    public float getMinTotalLootBonus() {
        return minTotalLootBonus;
    }

    public void setMinTotalLootBonus(float minTotalLootBonus) {
        this.minTotalLootBonus = minTotalLootBonus;
    }

    public float getMaxTotalLootBonus() {
        return maxTotalLootBonus;
    }

    public void setMaxTotalLootBonus(float maxTotalLootBonus) {
        this.maxTotalLootBonus = maxTotalLootBonus;
    }

    public float getMinTotalXpBonus() {
        return minTotalXpBonus;
    }

    public void setMinTotalXpBonus(float minTotalXpBonus) {
        this.minTotalXpBonus = minTotalXpBonus;
    }

    public float getMaxTotalXpBonus() {
        return maxTotalXpBonus;
    }

    public void setMaxTotalXpBonus(float maxTotalXpBonus) {
        this.maxTotalXpBonus = maxTotalXpBonus;
    }

    public boolean getUseDimensionOriginForDistance() {
        return useDimensionOriginForDistance;
    }

    public void setUseDimensionOriginForDistance(boolean useDimensionOriginForDistance) {
        this.useDimensionOriginForDistance = useDimensionOriginForDistance;
    }

    public Map<String, Integer> getDimensionLevelOffsets() {
        return dimensionLevelOffsets;
    }

    public void setDimensionLevelOffsets(Map<String, Integer> dimensionLevelOffsets) {
        this.dimensionLevelOffsets = dimensionLevelOffsets;
    }

    public List<String> getMobBlacklist() {
        return mobBlacklist;
    }

    public void setMobBlacklist(List<String> mobBlacklist) {
        this.mobBlacklist = mobBlacklist;
    }
    
    public boolean getEnableMobLabelHud() {
        return enableMobLabelHud;
    }
    
    public void setEnableMobLabelHud(boolean enableMobLabelHud) {
        this.enableMobLabelHud = enableMobLabelHud;
    }
    
    /**
     * Gets the scale factor for the mob label HUD.
     *
     * @return The scale factor for the mob label HUD (default: 0.5f).
     */
    public float getMobLabelHudScale() {
        return mobLabelHudScale;
    }
    
    /**
     * Sets the scale factor for the mob label HUD.
     *
     * @param mobLabelHudScale The scale factor (0.5 - 2.0 recommended)
     */
    public void setMobLabelHudScale(float mobLabelHudScale) {
        this.mobLabelHudScale = mobLabelHudScale;
    }
    
    /**
     * Gets the display duration for the mob label HUD in milliseconds.
     *
     * @return The duration in milliseconds before the mob label disappears (default: 1500ms).
     */
    public int getMobLabelHudDuration() {
        return mobLabelHudDuration;
    }
    
    /**
     * Sets the display duration for the mob label HUD.
     *
     * @param mobLabelHudDuration The duration in milliseconds
     */
    public void setMobLabelHudDuration(int mobLabelHudDuration) {
        this.mobLabelHudDuration = mobLabelHudDuration;
    }
    
    public boolean getEnableLocationLevelHud() {
        return enableLocationLevelHud;
    }
    
    public void setEnableLocationLevelHud(boolean enableLocationLevelHud) {
        this.enableLocationLevelHud = enableLocationLevelHud;
    }
    
    /**
     * Gets the scale factor for the location level HUD.
     *
     * @return The scale factor for the location level HUD (default: 1.0f).
     */
    public float getLocationLevelHudScale() {
        return locationLevelHudScale;
    }
    
    /**
     * Sets the scale factor for the location level HUD.
     *
     * @param locationLevelHudScale The scale factor (0.5 - 2.0 recommended)
     */
    public void setLocationLevelHudScale(float locationLevelHudScale) {
        this.locationLevelHudScale = locationLevelHudScale;
    }
    
    /**
     * Gets the position for the location level HUD.
     *
     * @return The position (0 = top left, 1 = top right, 2 = bottom left, 3 = bottom right).
     */
    public int getLocationLevelHudPosition() {
        return locationLevelHudPosition;
    }
    
    /**
     * Sets the position for the location level HUD.
     *
     * @param locationLevelHudPosition The position (0 = top left, 1 = top right, 2 = bottom left, 3 = bottom right).
     */
    public void setLocationLevelHudPosition(int locationLevelHudPosition) {
        this.locationLevelHudPosition = locationLevelHudPosition;
    }
}
