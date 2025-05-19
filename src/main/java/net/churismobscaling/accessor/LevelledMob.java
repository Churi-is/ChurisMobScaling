package net.churismobscaling.accessor;

/**
 * Interface to be implemented by MobEntity via mixin.
 * Provides access to the raw stored level of the mob.
 * This interface allows the ChurisMobScaling mod to manage and manipulate
 * mob difficulty levels throughout the game.
 */
public interface LevelledMob {
    /**
     * Gets the raw stored level of the mob.
     * 
     * @return The raw stored level, or a sentinel value (0 or -1) if not set.
     */
    int getLevelInternal();
    
    /**
     * Sets the raw level of the mob.
     * 
     * @param level The level to set for the mob entity.
     */
    void setLevelInternal(int level);
}
