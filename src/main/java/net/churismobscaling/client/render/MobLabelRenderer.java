package net.churismobscaling.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.api.MobLevelApi;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles rendering a Borderlands-style HUD above mobs, displaying their name,
 * level, and health. The HUD appears when a player looks at a mob and persists
 * for a configurable duration.
 */
public class MobLabelRenderer {
    // Map to store entities and their label expiration times
    private static final Map<UUID, TrackedEntity> TRACKED_ENTITIES = new HashMap<>();

    // --- Constants for HUD rendering ---

    // Positioning and Scale
    private static final float HUD_Y_OFFSET_ABOVE_ENTITY = 0.5f;
    private static final float HUD_CAMERA_FACING_ROTATION_DEGREES = 180.0F;
    private static final float HUD_BASE_SCALE_IN_3D_WORLD = 0.025f;

    // Layout and Dimensions (in screen pixels for a base scale of 1.0)
    private static final int HUD_ELEMENT_WIDTH = 160;
    private static final int NAME_AREA_HEIGHT = 12; // Vertical space allocated for the name
    private static final int HEALTH_BAR_HEIGHT = 6;
    private static final int PADDING_GENERAL = 4; // General padding around elements

    // Text Rendering
    private static final int TEXT_OUTLINE_OFFSET_PIXELS = 1;
    private static final int MAX_LIGHT_TEXTURE_UV = 15728880; // Full brightness for text/UI
    private static final String LEVEL_TEXT_PREFIX = "LV";
    private static final String HEALTH_TEXT_FORMAT_PATTERN = "%.0f/%.0f";
    private static final int HEALTH_TEXT_Y_OFFSET_FROM_BAR_TOP = -10; // Negative for above

    // Colors (ARGB format: Alpha, Red, Green, Blue)
    private static final int COLOR_BLACK_OPAQUE = 0xFF000000;
    private static final int COLOR_WHITE_OPAQUE = 0xFFFFFFFF;
    private static final int COLOR_GOLD_OPAQUE = 0xFFFFAA00; // Standard gold color for level
    private static final int COLOR_HEALTH_BAR_BACKGROUND_OPAQUE = 0xFF333333; // Dark gray
    private static final int COLOR_HEALTH_HIGH_OPAQUE = 0xFF00FF00;         // Green
    private static final int COLOR_HEALTH_MEDIUM_OPAQUE = 0xFFFFFF00;       // Yellow
    private static final int COLOR_HEALTH_LOW_OPAQUE = 0xFFFF0000;          // Red
    private static final int COLOR_HEALTH_BAR_SEGMENT_TRANSLUCENT = 0x70FFFFFF; // Semi-transparent white

    // Health Bar Logic
    private static final double HEALTH_PERCENTAGE_HIGH_THRESHOLD = 0.75; // Above this is green
    private static final double HEALTH_PERCENTAGE_MEDIUM_THRESHOLD = 0.35; // Above this is yellow, else red
    private static final int HEALTH_BAR_OUTLINE_THICKNESS_PIXELS = 2;
    private static final int HEALTH_BAR_SEGMENT_COUNT = 10;
    private static final int HEALTH_BAR_SEGMENT_LINE_THICKNESS_PIXELS = 1;

    /**
     * Holds information about a mob entity being tracked for HUD display.
     * This includes the entity itself, its level, calculated damage multiplier,
     * and the time when its HUD display should expire.
     */
    private static class TrackedEntity {
        public final LivingEntity entity;
        public final int level;
        @SuppressWarnings("unused")
        public final float damageMultiplier; // Retained for potential future use, not currently displayed
        public long expirationTime;

        public TrackedEntity(LivingEntity entity, int level, float damageMultiplier, long expirationTime) {
            this.entity = entity;
            this.level = level;
            this.damageMultiplier = damageMultiplier;
            this.expirationTime = expirationTime;
        }
    }

    /**
     * Registers the mob label renderer with Fabric's world rendering events.
     * This method sets up a callback to be invoked after entities are rendered in the world.
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            // Ensure player and world are available
            if (client.player == null || client.world == null) {
                return;
            }

            // Update tracked status of the entity the player is currently looking at
            HitResult crosshairTarget = client.crosshairTarget;
            if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult) crosshairTarget;
                Entity targetedEntity = entityHitResult.getEntity();

                if (targetedEntity instanceof LivingEntity && ChurisMobScalingMain.getConfig().getEnableMobLabelHud()) {
                    LivingEntity livingTarget = (LivingEntity) targetedEntity;
                    UUID targetUuid = livingTarget.getUuid();
                    long newExpirationTime = System.currentTimeMillis() + ChurisMobScalingMain.getConfig().getMobLabelHudDuration();

                    if (TRACKED_ENTITIES.containsKey(targetUuid)) {
                        // If already tracked, just update its expiration time
                        TRACKED_ENTITIES.get(targetUuid).expirationTime = newExpirationTime;
                    } else {
                        // If new, get its level (important: don't recalculate, use stored NBT data)
                        int level = MobLevelApi.getEntityLevel(livingTarget);
                        if (level > 0) { // Only track mobs with a valid level
                            float damageMultiplier = MobLevelApi.getAttackMultiplier(level);
                            TRACKED_ENTITIES.put(targetUuid,
                                new TrackedEntity(livingTarget, level, damageMultiplier, newExpirationTime));
                        }
                    }
                }
            }

            // Iterate through all tracked entities to render HUDs and manage expirations
            List<UUID> entitiesToRemove = new ArrayList<>();
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<UUID, TrackedEntity> entry : TRACKED_ENTITIES.entrySet()) {
                TrackedEntity tracked = entry.getValue();

                // Check for expiration or invalid entity state
                if (currentTime > tracked.expirationTime || tracked.entity.isRemoved() || !tracked.entity.isAlive()) {
                    entitiesToRemove.add(entry.getKey());
                    continue;
                }

                // Render the HUD for valid, non-expired tracked entities
                renderBorderlandsStyleHUD(
                    tracked.entity,
                    context,
                    tracked.level,
                    tracked.entity.getHealth(),
                    tracked.entity.getMaxHealth(),
                    // tracked.damageMultiplier, // Not currently displayed, but available
                    tracked.entity.getName().getString()
                );
            }

            // Remove entities that have expired or become invalid
            for (UUID uuid : entitiesToRemove) {
                TRACKED_ENTITIES.remove(uuid);
            }
        });
    }

    /**
     * Renders the main HUD elements for a given entity.
     * This includes the entity's name, level, and health bar.
     *
     * @param entity The living entity for which to render the HUD.
     * @param context The world render context.
     * @param level The mob's level.
     * @param health The current health of the mob.
     * @param maxHealth The maximum health of the mob.
     * @param entityName The display name of the entity.
     */
    private static void renderBorderlandsStyleHUD(LivingEntity entity, WorldRenderContext context,
                                                int level, float health, float maxHealth,
                                                String entityName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) { // Should not happen if register() checks client.player
            return;
        }

        MatrixStack matrices = context.matrixStack();
        TextRenderer textRenderer = client.textRenderer;
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        setupRenderState();
        matrices.push();

        positionHudAboveEntity(entity, context, matrices);

        // HUD layout calculations (centered horizontally)
        float hudX = -HUD_ELEMENT_WIDTH / 2.0f;
        // Y position is tricky due to elements stacking; we'll place elements from top down.
        // Let's assume y=0 is the baseline for the top of the first element.
        float currentY = 0; // Start Y for the topmost element (name)

        // Draw entity name
        drawEntityName(matrices, immediate, textRenderer, entityName, hudX, currentY);
        currentY += NAME_AREA_HEIGHT; // Move Y down for the next section (level + health bar)

        // Draw level indicator (left-aligned)
        float levelTextX = hudX + PADDING_GENERAL;
        float levelTextY = currentY + PADDING_GENERAL + 1; // +1 for slight visual adjustment
        drawLevelIndicator(matrices, immediate, textRenderer, level, levelTextX, levelTextY);

        // Health bar (right of level indicator)
        float levelIndicatorWidth = textRenderer.getWidth(Text.literal(LEVEL_TEXT_PREFIX + level)) + (PADDING_GENERAL * 2);
        drawHealthBar(matrices, immediate, textRenderer, health, maxHealth,
                      hudX, currentY, PADDING_GENERAL, NAME_AREA_HEIGHT, // NAME_AREA_HEIGHT is effectively previous section's height
                      HUD_ELEMENT_WIDTH, HEALTH_BAR_HEIGHT, levelIndicatorWidth);

        immediate.draw(); // Draw all batched elements
        matrices.pop();
        resetRenderState();
    }

    /**
     * Sets up OpenGL render states for transparent UI rendering.
     * Disables depth testing and culling, enables blending.
     */
    private static void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Standard alpha blending
        RenderSystem.disableDepthTest();  // Draw on top of everything
        RenderSystem.disableCull();       // Draw both sides if scaled negatively (though not intended here)
    }

    /**
     * Resets OpenGL render states to their defaults after HUD rendering.
     * Re-enables depth testing and culling, disables blending.
     */
    private static void resetRenderState() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Positions and orients the MatrixStack for rendering the HUD above the entity, facing the camera.
     *
     * @param entity The target entity.
     * @param context The world render context.
     * @param matrices The MatrixStack to transform.
     */
    private static void positionHudAboveEntity(LivingEntity entity, WorldRenderContext context, MatrixStack matrices) {
        matrices.translate(
            entity.getX() - context.camera().getPos().x,
            entity.getY() - context.camera().getPos().y + entity.getHeight() + HUD_Y_OFFSET_ABOVE_ENTITY,
            entity.getZ() - context.camera().getPos().z
        );

        // Rotate HUD to face the camera
        matrices.multiply(context.camera().getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(HUD_CAMERA_FACING_ROTATION_DEGREES));

        // Scale the HUD for visibility in the 3D world, applying user preference
        float userScaleFactor = ChurisMobScalingMain.getConfig().getMobLabelHudScale();
        float finalScale = HUD_BASE_SCALE_IN_3D_WORLD * userScaleFactor;
        matrices.scale(-finalScale, -finalScale, finalScale); // Negative X to correct mirroring from Y rotation
    }

    /**
     * Draws text with a solid black outline by rendering the text four times offset, then once normally.
     *
     * @param matrices The MatrixStack.
     * @param immediate The VertexConsumerProvider for drawing.
     * @param textRenderer The TextRenderer instance.
     * @param mainText The primary text to draw (colored).
     * @param outlineText The text used for the outline (typically black).
     * @param x The X position of the text.
     * @param y The Y position of the text.
     */
    private static void drawTextWithOutline(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                           TextRenderer textRenderer, Text mainText, Text outlineText,
                                           float x, float y) {
        // Draw black outline by offsetting in four directions
        float ox = TEXT_OUTLINE_OFFSET_PIXELS; // outline X offset
        float oy = TEXT_OUTLINE_OFFSET_PIXELS; // outline Y offset

        textRenderer.draw(outlineText, x - ox, y, COLOR_BLACK_OPAQUE, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, 0, MAX_LIGHT_TEXTURE_UV);
        textRenderer.draw(outlineText, x + ox, y, COLOR_BLACK_OPAQUE, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, 0, MAX_LIGHT_TEXTURE_UV);
        textRenderer.draw(outlineText, x, y - oy, COLOR_BLACK_OPAQUE, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, 0, MAX_LIGHT_TEXTURE_UV);
        textRenderer.draw(outlineText, x, y + oy, COLOR_BLACK_OPAQUE, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, 0, MAX_LIGHT_TEXTURE_UV);

        // Draw the main text on top, using POLYGON_OFFSET to prevent z-fighting with its own outline
        textRenderer.draw(mainText, x, y, COLOR_WHITE_OPAQUE, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, MAX_LIGHT_TEXTURE_UV);
    }

    /**
     * Draws the entity's name at the top of the HUD.
     *
     * @param matrices The MatrixStack.
     * @param immediate The VertexConsumerProvider.
     * @param textRenderer The TextRenderer.
     * @param entityName The name of the entity.
     * @param xPos The base X position for the HUD elements.
     * @param yPos The Y position for the name text.
     */
    private static void drawEntityName(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                      TextRenderer textRenderer, String entityName, float xPos, float yPos) {
        Text nameText = Text.literal(entityName).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(true));
        Text borderNameText = Text.literal(entityName).setStyle(Style.EMPTY.withColor(Formatting.BLACK).withBold(true)); // Outline uses black

        float nameX = xPos + PADDING_GENERAL;
        float nameY = yPos + PADDING_GENERAL;

        drawTextWithOutline(matrices, immediate, textRenderer, nameText, borderNameText, nameX, nameY);
    }

    /**
     * Draws the level indicator (e.g., "LV50") on the HUD.
     *
     * @param matrices The MatrixStack.
     * @param immediate The VertexConsumerProvider.
     * @param textRenderer The TextRenderer.
     * @param level The entity's level.
     * @param x The X position for the level text.
     * @param y The Y position for the level text.
     */
    private static void drawLevelIndicator(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                          TextRenderer textRenderer, int level, float x, float y) {
        String levelString = LEVEL_TEXT_PREFIX + level;
        Text levelText = Text.literal(levelString).setStyle(Style.EMPTY.withColor(COLOR_GOLD_OPAQUE).withBold(true));
        Text borderLevelText = Text.literal(levelString).setStyle(Style.EMPTY.withColor(Formatting.BLACK).withBold(true));

        drawTextWithOutline(matrices, immediate, textRenderer, levelText, borderLevelText, x, y);
    }

    /**
     * Draws the health bar, including its border, background, filled portion, and segments.
     * Also draws the health text (current/max) above the bar.
     *
     * @param matrices The MatrixStack.
     * @param immediate The VertexConsumerProvider.
     * @param textRenderer The TextRenderer.
     * @param health Current health.
     * @param maxHealth Maximum health.
     * @param hudX Base X position of the HUD.
     * @param sectionY Y position for this section (below the name).
     * @param padding General padding value.
     * @param previousSectionHeight Height of the section above (e.g., name area), used for Y calculation.
     * @param hudWidth Total width of the HUD.
     * @param barHeight Height of the health bar itself.
     * @param spaceTakenByLevelIndicator Width occupied by the level indicator to its left.
     */
    private static void drawHealthBar(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                     TextRenderer textRenderer, float health, float maxHealth,
                                     float hudX, float sectionY, int padding, int previousSectionHeight,
                                     int hudWidth, int barHeight, float spaceTakenByLevelIndicator) {
        // Health bar positioning and dimensions
        float healthBarAvailableWidth = hudWidth - spaceTakenByLevelIndicator - (padding * 2); // Width available after level and side paddings
        float healthBarX = hudX + spaceTakenByLevelIndicator + padding;
        float healthBarY = sectionY + padding; // Y position of the health bar itself

        float healthPercent = (maxHealth > 0) ? (health / maxHealth) : 0;
        float filledBarWidth = healthBarAvailableWidth * healthPercent;
        int healthBarColor = getHealthBarFillColor(healthPercent);

        // Draw health bar components (border, background, fill, segments)
        drawHealthBarVisuals(matrices, healthBarX, healthBarY, healthBarAvailableWidth, barHeight, filledBarWidth, healthBarColor);

        // Draw health text (current/max) above the health bar
        String healthString = String.format(HEALTH_TEXT_FORMAT_PATTERN, Math.max(0, health), maxHealth); // Ensure health isn't negative
        Text mainHealthText = Text.literal(healthString).setStyle(Style.EMPTY.withColor(Formatting.WHITE));
        Text outlineHealthText = Text.literal(healthString).setStyle(Style.EMPTY.withColor(Formatting.BLACK));

        float healthTextWidth = textRenderer.getWidth(mainHealthText);
        float healthTextX = healthBarX + (healthBarAvailableWidth - healthTextWidth) / 2; // Centered on the health bar
        float healthTextY = healthBarY + HEALTH_TEXT_Y_OFFSET_FROM_BAR_TOP; // Positioned above the bar

        drawTextWithOutline(matrices, immediate, textRenderer, mainHealthText, outlineHealthText, healthTextX, healthTextY);
    }

    /**
     * Determines the fill color of the health bar based on the health percentage.
     *
     * @param healthPercent The current health percentage (0.0 to 1.0).
     * @return The ARGB color value for the health bar fill.
     */
    private static int getHealthBarFillColor(float healthPercent) {
        if (healthPercent > HEALTH_PERCENTAGE_HIGH_THRESHOLD) {
            return COLOR_HEALTH_HIGH_OPAQUE; // Green
        } else if (healthPercent > HEALTH_PERCENTAGE_MEDIUM_THRESHOLD) {
            return COLOR_HEALTH_MEDIUM_OPAQUE; // Yellow
        } else {
            return COLOR_HEALTH_LOW_OPAQUE; // Red
        }
    }

    /**
     * Draws the visual components of the health bar: border, background, filled portion, and segments.
     *
     * @param matrices The MatrixStack.
     * @param barX The X position of the health bar.
     * @param barY The Y position of the health bar.
     * @param totalWidth The total width of the health bar.
     * @param barHeight The height of the health bar.
     * @param filledWidth The width of the filled portion of the health bar.
     * @param fillColor The color of the filled portion.
     */
    private static void drawHealthBarVisuals(MatrixStack matrices, float barX, float barY,
                                               float totalWidth, int barHeight,
                                               float filledWidth, int fillColor) {
        // Draw health bar border (black outline)
        float border = HEALTH_BAR_OUTLINE_THICKNESS_PIXELS;
        drawRectangle(matrices, barX - border, barY - border,
                     totalWidth + (border * 2), barHeight + (border * 2), COLOR_BLACK_OPAQUE);

        // Draw health bar background (dark gray)
        drawRectangle(matrices, barX, barY, totalWidth, barHeight, COLOR_HEALTH_BAR_BACKGROUND_OPAQUE);

        // Draw health bar filled portion
        if (filledWidth > 0) {
            drawRectangle(matrices, barX, barY, filledWidth, barHeight, fillColor);
        }

        // Draw health segments (vertical lines)
        float segmentWidthInterval = totalWidth / HEALTH_BAR_SEGMENT_COUNT;
        for (int i = 1; i < HEALTH_BAR_SEGMENT_COUNT; i++) {
            float segmentLineX = barX + (segmentWidthInterval * i);
            // Only draw segment lines if they are within the currently filled portion of the bar
            if (segmentLineX < barX + filledWidth) {
                drawRectangle(matrices, segmentLineX, barY,
                              HEALTH_BAR_SEGMENT_LINE_THICKNESS_PIXELS, barHeight,
                              COLOR_HEALTH_BAR_SEGMENT_TRANSLUCENT);
            }
        }
    }

    /**
     * Utility method to draw a simple, colored, untextured rectangle.
     *
     * @param matrices The MatrixStack.
     * @param x The X position of the rectangle.
     * @param y The Y position of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color The ARGB color of the rectangle.
     */
    private static void drawRectangle(MatrixStack matrices, float x, float y, float width, float height, int color) {
        // Extract ARGB components
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        float x1 = x;
        float x2 = x + width;
        float y1 = y;
        float y2 = y + height;

        org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        // BufferBuilder bufferBuilder = tessellator.getBuffer(); // Old way
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Define vertices for the quad
        // Texture coordinates (u, v) are not used for solid color but are part of the vertex format.
        // Overlay and Light are set to standard values for UI elements.
        // Normal is set for a 2D quad facing the camera.
        float u = 0, v = 0; // Texture coordinates
        int overlay = 0;    // No overlay
        float normalX = 0, normalY = 0, normalZ = 1; // Standard normal

        bufferBuilder.vertex(matrix, x1, y1, 0).color(red, green, blue, alpha).texture(u, v).overlay(overlay).light(MAX_LIGHT_TEXTURE_UV).normal(normalX, normalY, normalZ);
        bufferBuilder.vertex(matrix, x1, y2, 0).color(red, green, blue, alpha).texture(u, v).overlay(overlay).light(MAX_LIGHT_TEXTURE_UV).normal(normalX, normalY, normalZ);
        bufferBuilder.vertex(matrix, x2, y2, 0).color(red, green, blue, alpha).texture(u, v).overlay(overlay).light(MAX_LIGHT_TEXTURE_UV).normal(normalX, normalY, normalZ);
        bufferBuilder.vertex(matrix, x2, y1, 0).color(red, green, blue, alpha).texture(u, v).overlay(overlay).light(MAX_LIGHT_TEXTURE_UV).normal(normalX, normalY, normalZ);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}
