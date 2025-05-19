package net.churismobscaling.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class RenderUtils {

    public static final float RECTANGLE_DEFAULT_Z_OFFSET = 0.0f;
    private static final float RECTANGLE_DEFAULT_TEXTURE_U = 0.0f;
    private static final float RECTANGLE_DEFAULT_TEXTURE_V = 0.0f;
    private static final int RECTANGLE_DEFAULT_OVERLAY = 0;
    private static final float RECTANGLE_DEFAULT_NORMAL_X = 0.0f;
    private static final float RECTANGLE_DEFAULT_NORMAL_Y = 0.0f;
    private static final float RECTANGLE_DEFAULT_NORMAL_Z = 1.0f;

    /**
     * Sets up OpenGL render state for typical 2D HUD rendering.
     * Enables blending with default function and disables depth testing.
     */
    public static void setupBlendDisableDepth() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }

    /**
     * Resets OpenGL render state after 2D HUD rendering.
     * Enables depth testing and disables blending.
     */
    public static void resetDepthDisableBlend() {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Sets up OpenGL render state for 3D in-world UI rendering.
     * Enables blending, disables depth testing, and disables culling.
     */
    public static void setupBlendDisableDepthDisableCull() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Standard alpha blending
        RenderSystem.disableDepthTest();  // Draw on top of everything
        RenderSystem.disableCull();       // Draw both sides if scaled negatively
    }

    /**
     * Resets OpenGL render state after 3D in-world UI rendering.
     * Enables depth testing, enables culling, and disables blending.
     */
    public static void resetDepthCullDisableBlend() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Utility method to draw a filled rectangle with a solid color.
     *
     * @param matrices The MatrixStack for rendering.
     * @param x The x-coordinate of the top-left corner.
     * @param y The y-coordinate of the top-left corner.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param color The color of the rectangle (ARGB format).
     * @param lightLevel The light level for rendering (e.g., MinecraftClient.FULL_BRIGHTNESS_LIGHT_LEVEL).
     */
    public static void drawSolidRectangle(MatrixStack matrices, float x, float y, float width, float height, int color, int lightLevel) {
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        float x1 = x;
        float x2 = x + width;
        float y1 = y;
        float y2 = y + height;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = tessellator.begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL // Using a more common format that includes texture and light
        );

        bufferBuilder.vertex(matrix, x1, y1, RECTANGLE_DEFAULT_Z_OFFSET)
            .color(red, green, blue, alpha)
            .texture(RECTANGLE_DEFAULT_TEXTURE_U, RECTANGLE_DEFAULT_TEXTURE_V)
            .light(lightLevel)
            .overlay(RECTANGLE_DEFAULT_OVERLAY) // Added overlay for completeness, though default
            .normal(RECTANGLE_DEFAULT_NORMAL_X, RECTANGLE_DEFAULT_NORMAL_Y, RECTANGLE_DEFAULT_NORMAL_Z);
        bufferBuilder.vertex(matrix, x1, y2, RECTANGLE_DEFAULT_Z_OFFSET)
            .color(red, green, blue, alpha)
            .texture(RECTANGLE_DEFAULT_TEXTURE_U, RECTANGLE_DEFAULT_TEXTURE_V)
            .light(lightLevel)
            .overlay(RECTANGLE_DEFAULT_OVERLAY)
            .normal(RECTANGLE_DEFAULT_NORMAL_X, RECTANGLE_DEFAULT_NORMAL_Y, RECTANGLE_DEFAULT_NORMAL_Z);
        bufferBuilder.vertex(matrix, x2, y2, RECTANGLE_DEFAULT_Z_OFFSET)
            .color(red, green, blue, alpha)
            .texture(RECTANGLE_DEFAULT_TEXTURE_U, RECTANGLE_DEFAULT_TEXTURE_V)
            .light(lightLevel)
            .overlay(RECTANGLE_DEFAULT_OVERLAY)
            .normal(RECTANGLE_DEFAULT_NORMAL_X, RECTANGLE_DEFAULT_NORMAL_Y, RECTANGLE_DEFAULT_NORMAL_Z);
        bufferBuilder.vertex(matrix, x2, y1, RECTANGLE_DEFAULT_Z_OFFSET)
            .color(red, green, blue, alpha)
            .texture(RECTANGLE_DEFAULT_TEXTURE_U, RECTANGLE_DEFAULT_TEXTURE_V)
            .light(lightLevel)
            .overlay(RECTANGLE_DEFAULT_OVERLAY)
            .normal(RECTANGLE_DEFAULT_NORMAL_X, RECTANGLE_DEFAULT_NORMAL_Y, RECTANGLE_DEFAULT_NORMAL_Z);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}
