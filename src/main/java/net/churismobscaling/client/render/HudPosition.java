package net.churismobscaling.client.render;

/**
 * Represents the possible positions for a Heads-Up Display (HUD) element
 * on the screen. Each position is associated with a unique index.
 */
public enum HudPosition {
    TOP_LEFT(0),
    TOP_RIGHT(1),
    BOTTOM_LEFT(2),
    BOTTOM_RIGHT(3);
    
    private final int index;
    
    /**
     * Constructs a HudPosition enum constant.
     *
     * @param index The unique integer index associated with this HUD position.
     */
    HudPosition(int index) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    /**
     * Retrieves a {@code HudPosition} based on its integer index.
     * <p>
     * Iterates through all available HUD positions and returns the one
     * matching the given index. If no position matches the index,
     * {@code TOP_LEFT} is returned as a default.
     *
     * @param index The integer index of the desired HUD position.
     * @return The {@code HudPosition} corresponding to the given index,
     *         or {@code TOP_LEFT} if the index is not found.
     */
    public static HudPosition fromIndex(int index) {
        for (HudPosition position : values()) {
            if (position.getIndex() == index) {
                return position;
            }
        }
        return TOP_LEFT; // Default
    }
}
