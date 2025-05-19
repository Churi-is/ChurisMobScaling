package net.churismobscaling;

import net.fabricmc.api.ClientModInitializer;
import net.churismobscaling.client.render.LocationLevelRenderer;
import net.churismobscaling.client.render.MobLabelRenderer;

/**
 * Handles the client-side initialization for the Churis Mob Scaling mod.
 * This class is responsible for registering client-specific components,
 * such as custom renderers, when the game starts on the client.
 */
public class ChurisMobScalingClient implements ClientModInitializer {
    /**
     * Called when the mod is initialized on the client side as per the
     * FabricMC lifecycle. This method sets up all client-specific
     * functionalities for the mod.
     *
     * Responsibilities include:
     * - Logging the initialization process.
     * - Registering the {@link MobLabelRenderer}.
     * - Registering the {@link LocationLevelRenderer}.
     */
    @Override
    public void onInitializeClient() {
        ChurisMobScalingMain.LOGGER.info("Initializing Churis Mob Scaling Mod Client");
        
        MobLabelRenderer.register();
        ChurisMobScalingMain.LOGGER.info("Mob label renderer initialized");
        
        LocationLevelRenderer.register();
        ChurisMobScalingMain.LOGGER.info("Location level renderer initialized");
        
        
        ChurisMobScalingMain.LOGGER.info("Churis Mob Scaling Mod Client Initialized");
    }
}
