package net.churismobscaling.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.churismobscaling.accessor.LevelledMob;
import net.churismobscaling.util.LevelCalculator;

/**
 * Provides a public API for interacting with the levelling system of mobs.
 * This class allows other parts of the mod or external mods to get and set
 * mob levels, and retrieve level-based multipliers for stats like health,
 * attack, loot, and XP.
 */
public class MobLevelApi {
    // NBT key for mob level
    public static final String MOB_LEVEL_KEY = "mobLevel";
    private static final int MINIMUM_MOB_LEVEL = 1;

    /**
     * Gets the level of a mob entity.
     * If the level isn't stored or is invalid (<=0), calculates and stores it (server-side).
     * On the client, if the level is not already known (e.g., synced), it may return a default.
     *
     * @param entity The entity to get the level for
     * @return The level of the entity, or {@value #MINIMUM_MOB_LEVEL} if it's not a mob or if calculation fails to produce a level greater than 0.
     */
    public static int getEntityLevel(LivingEntity entity) {
        if (!(entity instanceof MobEntity mobEntity)) {
            return MINIMUM_MOB_LEVEL;
        }

        LevelledMob levelledMob = (LevelledMob) mobEntity;
        int internalLevel = levelledMob.getLevelInternal();

        if (internalLevel <= 0) {
            int calculatedLevel = LevelCalculator.calculateMobLevel(mobEntity);
            calculatedLevel = Math.max(MINIMUM_MOB_LEVEL, calculatedLevel); // Ensure minimum level
            setEntityLevel(mobEntity, calculatedLevel);
            return calculatedLevel;
        }
        
        return internalLevel;
    }

    /**
     * Sets the level of a mob entity. Server-side operation.
     * This will update the internal level and trigger stat recalculation/application.
     * The level will be persisted when the entity is saved.
     *
     * @param entity The entity to set the level for
     * @param level  The level to set
     * @return True if the level was set (i.e., entity is a MobEntity and on server), false otherwise.
     */
    public static boolean setEntityLevel(LivingEntity entity, int level) {
        if (!(entity instanceof MobEntity mobEntity)) {
            return false;
        }

        LevelledMob levelledMob = (LevelledMob) mobEntity;
        int newLevel = Math.max(MINIMUM_MOB_LEVEL, level);
        levelledMob.setLevelInternal(newLevel);

        return true;
    }

    /**
     * Gets the health multiplier for a given level.
     * @param level The level to get the multiplier for
     * @return The health multiplier
     */
    public static float getHealthMultiplier(int level) {
        return LevelCalculator.calculateHealthMultiplier(Math.max(MINIMUM_MOB_LEVEL, level));
    }

    /**
     * Gets the attack multiplier for a given level.
     * @param level The level to get the multiplier for
     * @return The attack multiplier
     */
    public static float getAttackMultiplier(int level) {
        return LevelCalculator.calculateAttackMultiplier(Math.max(MINIMUM_MOB_LEVEL, level));
    }

    /**
     * Gets the loot multiplier for a given level.
     * @param level The level to get the multiplier for
     * @return The loot multiplier
     */
    public static float getLootMultiplier(int level) {
        return LevelCalculator.calculateLootMultiplier(Math.max(MINIMUM_MOB_LEVEL, level));
    }
    
    /**
     * Gets the XP multiplier for a given level.
     * @param level The level to get the multiplier for
     * @return The XP multiplier
     */
    public static float getXpMultiplier(int level) {
        return LevelCalculator.calculateXpMultiplier(Math.max(MINIMUM_MOB_LEVEL, level));
    }
}