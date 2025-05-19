package net.churismobscaling.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.client.render.util.RenderUtils;
import net.churismobscaling.config.ModConfig;
import net.churismobscaling.util.LevelCalculator;

/**
 * Renders an indicator showing the player's current location level.
 * This HUD displays the area's difficulty level and the corresponding
 * enemy damage multiplier.
 */
public class LocationLevelRenderer {

    // General HUD display constants
    private static final int HUD_PADDING_HORIZONTAL = 4;
    private static final int HUD_PADDING_VERTICAL = 4;
    private static final int HUD_INTERNAL_PADDING = 5;
    private static final int HUD_BOX_CONTENT_WIDTH_PADDING = HUD_INTERNAL_PADDING * 2; // Left and right internal padding
    private static final int HUD_BOX_HEIGHT = 28;
    private static final int HUD_TEXT_INTERLINE_SPACING = 10; // Vertical distance between start of line 1 and start of line 2

    // Colors
    private static final int HUD_BACKGROUND_COLOR = 0xA0000000; // Semi-transparent black
    private static final int HUD_TEXT_COLOR = 0xFFFFFFFF;       // White

    // Rendering constants
    private static final int FULL_BRIGHTNESS_LIGHT_LEVEL = 15728880;
    // Thresholds for level coloring
    private static final float LEVEL_COLOR_THRESHOLD_GREEN_YELLOW = 0.3f;
    private static final float LEVEL_COLOR_THRESHOLD_YELLOW_GOLD = 0.6f;
    private static final float LEVEL_COLOR_THRESHOLD_GOLD_RED = 0.8f;

    // Thresholds for damage multiplier coloring
    private static final float DAMAGE_COLOR_THRESHOLD_GREEN_YELLOW = 1.3f;
    private static final float DAMAGE_COLOR_THRESHOLD_YELLOW_GOLD = 2.0f;
    private static final float DAMAGE_COLOR_THRESHOLD_GOLD_RED = 3.0f;
    
    /**
     * Registers the renderer with Fabric's event system.
     * This sets up the callback for rendering the location level HUD.
     */
    public static void register() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            
            // Only process if we have a player, are in-game, and HUD is not hidden
            if (client.player == null || client.world == null || client.options.hudHidden) {
                return;
            }
            
            // Check if location level HUD is enabled in config
            ModConfig config = ChurisMobScalingMain.getConfig();
            if (!config.getEnableMod() || !config.getEnableLocationLevelHud()) {
                return;
            }
            
            renderLocationLevelHUD(matrices.getMatrices(), client, config);
        });
    }
    
    /**
     * Renders the location level HUD on the screen.
     * This includes the area level and enemy damage multiplier.
     * 
     * @param matrices The MatrixStack for rendering.
     * @param client The MinecraftClient instance.
     * @param config The current mod configuration.
     */
    private static void renderLocationLevelHUD(MatrixStack matrices, MinecraftClient client, ModConfig config) {
        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        
        int level = LevelCalculator.calculateLevelAtLocation(client.world, playerX, playerZ);
        float damageMultiplier = LevelCalculator.calculateAttackMultiplier(level);
        
        // Format damage multiplier as a percentage increase (e.g., 1.5x -> +50%), if it is negative add - if its positive add +
        String damageIncreaseText = String.format("%s%d%%", (damageMultiplier < 1 ? "" : "+"), Math.round((damageMultiplier - 1) * 100));
        
        // Create text components for the HUD
        Text levelText = Text.literal("Area Level: ")
            .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
            .append(Text.literal(String.valueOf(level))
            .setStyle(Style.EMPTY.withColor(getLevelColor(level, config)).withBold(true)));
        
        Text damageText = Text.literal("Enemy Damage: ")
            .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
            .append(Text.literal(damageIncreaseText)
            .setStyle(Style.EMPTY.withColor(getDangerColor(damageMultiplier, config))));
        
        TextRenderer textRenderer = client.textRenderer;
        int levelWidth = textRenderer.getWidth(levelText);
        int damageWidth = textRenderer.getWidth(damageText);
        int contentWidth = Math.max(levelWidth, damageWidth);
        int totalWidth = contentWidth + HUD_BOX_CONTENT_WIDTH_PADDING;
        int totalHeight = HUD_BOX_HEIGHT;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Determine HUD position based on configuration
        int posX;
        int posY;
        HudPosition hudPosition = HudPosition.fromIndex(config.getLocationLevelHudPosition());
        
        switch (hudPosition) {
            case TOP_RIGHT:
                posX = screenWidth - totalWidth - HUD_PADDING_HORIZONTAL;
                posY = HUD_PADDING_VERTICAL;
                break;
            case BOTTOM_LEFT:
                posX = HUD_PADDING_HORIZONTAL;
                posY = screenHeight - totalHeight - HUD_PADDING_VERTICAL;
                break;
            case BOTTOM_RIGHT:
                posX = screenWidth - totalWidth - HUD_PADDING_HORIZONTAL;
                posY = screenHeight - totalHeight - HUD_PADDING_VERTICAL;
                break;
            case TOP_LEFT:
            default: // Default to top-left if unknown
                posX = HUD_PADDING_HORIZONTAL;
                posY = HUD_PADDING_VERTICAL;
                break;
        }
        
        RenderUtils.setupBlendDisableDepth();
        
        float scale = config.getLocationLevelHudScale();
        boolean isScaled = scale != 1.0f;

        if (isScaled) {
            matrices.push();
            // Translate to pivot point, scale, then translate back
            matrices.translate(posX, posY, RenderUtils.RECTANGLE_DEFAULT_Z_OFFSET);
            matrices.scale(scale, scale, 1.0f);
            matrices.translate(-posX, -posY, RenderUtils.RECTANGLE_DEFAULT_Z_OFFSET);
            
            // Adjust position for scaling to keep it in the correct corner
            if (hudPosition == HudPosition.TOP_RIGHT || hudPosition == HudPosition.BOTTOM_RIGHT) {
                posX = (int) ((screenWidth - totalWidth * scale) - HUD_PADDING_HORIZONTAL);
            }
            if (hudPosition == HudPosition.BOTTOM_LEFT || hudPosition == HudPosition.BOTTOM_RIGHT) {
                posY = (int) ((screenHeight - totalHeight * scale) - HUD_PADDING_VERTICAL);
            }
        }
        
        // Draw background with slight transparency
        RenderUtils.drawSolidRectangle(matrices, posX, posY, totalWidth, totalHeight, HUD_BACKGROUND_COLOR, FULL_BRIGHTNESS_LIGHT_LEVEL);
        
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Draw text lines
        textRenderer.draw(
            levelText,
            posX + HUD_INTERNAL_PADDING,
            posY + HUD_INTERNAL_PADDING,
            HUD_TEXT_COLOR,
            false, // hasShadow
            matrices.peek().getPositionMatrix(),
            immediate,
            TextRenderer.TextLayerType.NORMAL,
            0, // backgroundColor - transparent
            FULL_BRIGHTNESS_LIGHT_LEVEL
        );
        
        textRenderer.draw(
            damageText,
            posX + HUD_INTERNAL_PADDING,
            posY + HUD_INTERNAL_PADDING + HUD_TEXT_INTERLINE_SPACING,
            HUD_TEXT_COLOR,
            false, // hasShadow
            matrices.peek().getPositionMatrix(),
            immediate,
            TextRenderer.TextLayerType.NORMAL,
            0, // backgroundColor - transparent
            FULL_BRIGHTNESS_LIGHT_LEVEL
        );
        
        immediate.draw(); // Explicitly draw the buffer
        
        if (isScaled) {
            matrices.pop(); // Restore matrix state if scaling was applied
        }
        
        RenderUtils.resetDepthDisableBlend();
    }
    
    /**
     * Determines the color for the level text based on its difficulty.
     * 
     * @param level The current area level.
     * @param config The mod configuration.
     * @return The Formatting color for the level text.
     */
    private static Formatting getLevelColor(int level, ModConfig config) {
        int maxLevel = config.getMaxLevel();
        if (maxLevel <= 0) { // Avoid division by zero or negative max level
            return Formatting.WHITE;
        }
        float levelPercent = (float) level / maxLevel;
        
        if (levelPercent < LEVEL_COLOR_THRESHOLD_GREEN_YELLOW) {
            return Formatting.GREEN;
        } else if (levelPercent < LEVEL_COLOR_THRESHOLD_YELLOW_GOLD) {
            return Formatting.YELLOW;
        } else if (levelPercent < LEVEL_COLOR_THRESHOLD_GOLD_RED) {
            return Formatting.GOLD;
        } else {
            return Formatting.RED;
        }
    }
    
    /**
     * Determines the color for the damage multiplier text based on its value.
     * 
     * @param damageMultiplier The current damage multiplier.
     * @param config The mod configuration (currently unused but kept for consistency).
     * @return The Formatting color for the damage text.
     */
    private static Formatting getDangerColor(float damageMultiplier, ModConfig config) {
        // config parameter is unused here but kept for API consistency with getLevelColor
        // if future changes require it.
        if (damageMultiplier < DAMAGE_COLOR_THRESHOLD_GREEN_YELLOW) {
            return Formatting.GREEN;
        } else if (damageMultiplier < DAMAGE_COLOR_THRESHOLD_YELLOW_GOLD) {
            return Formatting.YELLOW;
        } else if (damageMultiplier < DAMAGE_COLOR_THRESHOLD_GOLD_RED) {
            return Formatting.GOLD;
        } else {
            return Formatting.RED;
        }
    }
}
