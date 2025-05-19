package net.churismobscaling;

import net.fabricmc.api.ModInitializer;
import net.churismobscaling.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the Churis Mob Scaling Mod.
 * Initializes the mod, loads configuration, and registers necessary systems.
 */
public class ChurisMobScalingMain implements ModInitializer {
    public static final String MOD_ID = "churismobscaling";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // The main configuration instance
    private static ModConfig CONFIG;

    /**
     * Initializes the mod when Fabric loads it.
     * This method is called by Fabric during the mod loading process.
     * It logs the initialization, loads the mod configuration, and registers systems.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Churis Mob Scaling Mod");

        CONFIG = ModConfig.loadOrCreateConfig();

        LOGGER.info("Churis Mob Scaling Mod Initialized");
    }

    /**
     * Gets the loaded mod configuration.
     *
     * @return The {@link ModConfig} instance containing the mod's settings.
     */
    public static ModConfig getConfig() {
        return CONFIG;
    }
}
