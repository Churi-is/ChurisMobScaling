package net.churismobscaling.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.config.ModConfig;

/**
 * Utility class for calculating mob levels, attribute multipliers, and other
 * gameplay-affecting statistics based on configuration and world location.
 * This class provides static methods to determine mob difficulty dynamically.
 */
public class LevelCalculator {

    private static final int DEFAULT_MOB_LEVEL = 1;
    private static final int MINIMUM_MOB_LEVEL = 1;
    // OpenSimplexNoise generates values in the range [-1, 1].
    // To normalize to [0, 1], we add 1 (making it [0, 2]) and then divide by 2.
    private static final double NOISE_NORMALIZATION_ADDEND = 1.0;
    private static final double NOISE_NORMALIZATION_DIVISOR = 2.0;

    /**
     * Calculates the appropriate level for a mob based on its type, location, and game configuration.
     *
     * @param entity The living entity (expected to be a MobEntity) for which to calculate the level.
     * @return The calculated mob level. Returns {@value #DEFAULT_MOB_LEVEL} if the entity is not a
     *         MobEntity, the world is null, the mod is disabled, or the entity is blacklisted.
     */
    public static int calculateMobLevel(LivingEntity entity) {
        if (!(entity instanceof MobEntity) || entity.getWorld() == null) {
            return DEFAULT_MOB_LEVEL; // Entity is not a mob or has no world context.
        }

        ModConfig config = ChurisMobScalingMain.getConfig();

        if (!config.getEnableMod()) {
            return DEFAULT_MOB_LEVEL; // Mod is disabled via configuration.
        }

        // TODO: Try using entity.getType().getRegistryEntry().registryKey().getValue().toString()
        // or similar for a more robust and potentially namespace-aware entity ID.
        String entityId = entity.getType().toString();
        if (config.getMobBlacklist().contains(entityId)) {
            return DEFAULT_MOB_LEVEL; // Entity type is blacklisted.
        }

        World world = entity.getWorld();
        double x = entity.getX();
        double z = entity.getZ();

        return calculateLevelAtLocation(world, x, z);
    }

    /**
     * Calculates the mob level for a specific location within a world.
     * This method considers noise patterns, distance from spawn, and dimension-specific offsets.
     *
     * @param world The world in which the location exists.
     * @param x The x-coordinate of the location.
     * @param z The z-coordinate of the location.
     * @return The calculated level, clamped to a minimum of {@value #MINIMUM_MOB_LEVEL}.
     *         Returns {@value #DEFAULT_MOB_LEVEL} if the world is null or the mod is disabled.
     */
    public static int calculateLevelAtLocation(World world, double x, double z) {
        if (world == null) {
            return DEFAULT_MOB_LEVEL; // Cannot calculate level without world context.
        }

        ModConfig config = ChurisMobScalingMain.getConfig();
        if (!config.getEnableMod()) {
            return DEFAULT_MOB_LEVEL; // Mod is disabled.
        }

        double baseLevelFactor = calculateBaseLevelFactor(x, z, world);
        double potentialLevel = baseLevelFactor * config.getMaxLevel();
        double distanceFromSpawn = Math.sqrt(x * x + z * z);
        double finalLevel;

        // Scale level if within the spawn influence radius.
        // Closer to (0,0) results in a level closer to MINIMUM_MOB_LEVEL.
        if (distanceFromSpawn < config.getSpawnInfluenceRadius()) {
            double spawnScaleFactor = distanceFromSpawn / config.getSpawnInfluenceRadius();
            finalLevel = MathHelper.lerp(spawnScaleFactor, MINIMUM_MOB_LEVEL, potentialLevel);
        } else {
            finalLevel = potentialLevel;
        }

        // Apply any dimension-specific level offsets.
        RegistryKey<World> dimensionKey = world.getRegistryKey();
        String dimensionId = dimensionKey.getValue().toString();
        finalLevel += config.getDimensionLevelOffsets().getOrDefault(dimensionId, 0);

        // Ensure the final level is an integer and at least MINIMUM_MOB_LEVEL.
        return Math.max(MINIMUM_MOB_LEVEL, (int) Math.round(finalLevel));
    }

    /**
     * Calculates a base level factor using OpenSimplex noise for a given location.
     * This factor is a value, typically between 0.0 and 1.0, influenced by the world seed
     * and configuration settings for noise scale and minimum noise percentage.
     *
     * @param x The x-coordinate for noise calculation.
     * @param z The z-coordinate for noise calculation.
     * @param world The world, used to obtain the seed for the noise generator.
     * @return The calculated base level factor.
     */
    public static double calculateBaseLevelFactor(double x, double z, World world) {
        ModConfig config = ChurisMobScalingMain.getConfig();

        double noiseInput1 = x / config.getNoiseScale();
        double noiseInput2 = z / config.getNoiseScale();

        long seed;
        if (world instanceof ServerWorld) {
            seed = ((ServerWorld) world).getSeed();
        } else {
            // Fallback to a consistent seed based on dimension ID if not a ServerWorld.
            // This ensures deterministic behavior (e.g., for client-side predictions)
            // for a given dimension when the server seed is unavailable.
            seed = world.getRegistryKey().getValue().hashCode();
        }

        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        double noiseValue = noise.sample(noiseInput1, noiseInput2); // Output is in range [-1, 1]

        // Normalize noiseValue from [-1, 1] to [0, 1]
        double normalizedNoise = (noiseValue + NOISE_NORMALIZATION_ADDEND) / NOISE_NORMALIZATION_DIVISOR;

        // Apply minimum noise level percentage, ensuring the factor is not below this minimum.
        // This scales the normalized noise into the range [minNoiseLevelPercentage, 1.0].
        return normalizedNoise * (1.0 - config.getMinNoiseLevelPercentage())
                + config.getMinNoiseLevelPercentage();
    }

    /**
     * Calculates the health multiplier for a mob based on its level.
     * The multiplier scales between a configured minimum and maximum bonus.
     *
     * @param level The mob's level. Expected to be >= {@value #MINIMUM_MOB_LEVEL}.
     * @return The calculated health multiplier (e.g., 1.0f for no bonus, >1.0f for increased health).
     */
    public static float calculateHealthMultiplier(int level) {
        ModConfig config = ChurisMobScalingMain.getConfig();
        int maxLevel = config.getMaxLevel();
        float minBonus = config.getMinTotalHealthBonus();
        float maxBonus = config.getMaxTotalHealthBonus();

        // If the configured maxLevel is at or below the minimum possible level,
        // or if the current level is at the minimum, the mob is at its base progression.
        if (maxLevel <= MINIMUM_MOB_LEVEL || level <= MINIMUM_MOB_LEVEL) {
            // Apply the configured minimum bonus. Clamp to handle potential config where minBonus > maxBonus.
            return 1.0f + MathHelper.clamp(minBonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));
        }

        // Determine how far the current level has progressed from MINIMUM_MOB_LEVEL towards maxLevel.
        // progressFactor is a value from 0.0 (at MINIMUM_MOB_LEVEL) to 1.0 (at maxLevel).
        float progressFactor = (float) (level - MINIMUM_MOB_LEVEL) / (maxLevel - MINIMUM_MOB_LEVEL);
        // Clamp progressFactor to [0, 1] to handle levels outside the expected [MINIMUM_MOB_LEVEL, maxLevel] range.
        progressFactor = MathHelper.clamp(progressFactor, 0.0f, 1.0f);

        // Linearly interpolate the actual bonus amount based on the progressFactor,
        // scaling between the configured minBonus and maxBonus.
        // float bonus = minBonus + progressFactor * (maxBonus - minBonus);
        float bonus = MathHelper.lerp(progressFactor, minBonus, maxBonus);

        // Ensure the calculated bonus strictly adheres to the [minBonus, maxBonus] range.
        // This is a safeguard, especially if minBonus > maxBonus is possible in the configuration.
        bonus = MathHelper.clamp(bonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));

        // The final multiplier is 100% (1.0f) plus the calculated bonus.
        return 1.0f + bonus;
    }

    /**
     * Calculates the attack damage multiplier for a mob based on its level.
     * The multiplier scales between a configured minimum and maximum bonus.
     *
     * @param level The mob's level. Expected to be >= {@value #MINIMUM_MOB_LEVEL}.
     * @return The calculated attack damage multiplier.
     */
    public static float calculateAttackMultiplier(int level) {
        ModConfig config = ChurisMobScalingMain.getConfig();
        int maxLevel = config.getMaxLevel();
        float minBonus = config.getMinTotalAttackBonus();
        float maxBonus = config.getMaxTotalAttackBonus();

        if (maxLevel <= MINIMUM_MOB_LEVEL || level <= MINIMUM_MOB_LEVEL) {
            return 1.0f + MathHelper.clamp(minBonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));
        }

        float progressFactor = (float) (level - MINIMUM_MOB_LEVEL) / (maxLevel - MINIMUM_MOB_LEVEL);
        progressFactor = MathHelper.clamp(progressFactor, 0.0f, 1.0f);

        float bonus = minBonus + progressFactor * (maxBonus - minBonus);
        bonus = MathHelper.clamp(bonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));

        return 1.0f + bonus;
    }

    /**
     * Calculates the loot multiplier for a mob based on its level.
     * The multiplier scales between a configured minimum and maximum bonus.
     *
     * @param level The mob's level. Expected to be >= {@value #MINIMUM_MOB_LEVEL}.
     * @return The calculated loot multiplier.
     */
    public static float calculateLootMultiplier(int level) {
        ModConfig config = ChurisMobScalingMain.getConfig();
        int maxLevel = config.getMaxLevel();
        float minBonus = config.getMinTotalLootBonus();
        float maxBonus = config.getMaxTotalLootBonus();

        if (maxLevel <= MINIMUM_MOB_LEVEL || level <= MINIMUM_MOB_LEVEL) {
            return 1.0f + MathHelper.clamp(minBonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));
        }

        float progressFactor = (float) (level - MINIMUM_MOB_LEVEL) / (maxLevel - MINIMUM_MOB_LEVEL);
        progressFactor = MathHelper.clamp(progressFactor, 0.0f, 1.0f);

        float bonus = minBonus + progressFactor * (maxBonus - minBonus);
        bonus = MathHelper.clamp(bonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));

        return 1.0f + bonus;
    }

    /**
     * Calculates the XP multiplier for a mob based on its level.
     * The multiplier scales between a configured minimum and maximum bonus.
     *
     * @param level The mob's level. Expected to be >= {@value #MINIMUM_MOB_LEVEL}.
     * @return The calculated XP multiplier.
     */
    public static float calculateXpMultiplier(int level) {
        ModConfig config = ChurisMobScalingMain.getConfig();
        int maxLevel = config.getMaxLevel();
        float minBonus = config.getMinTotalXpBonus();
        float maxBonus = config.getMaxTotalXpBonus();

        if (maxLevel <= MINIMUM_MOB_LEVEL || level <= MINIMUM_MOB_LEVEL) {
            return 1.0f + MathHelper.clamp(minBonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));
        }

        float progressFactor = (float) (level - MINIMUM_MOB_LEVEL) / (maxLevel - MINIMUM_MOB_LEVEL);
        progressFactor = MathHelper.clamp(progressFactor, 0.0f, 1.0f);

        float bonus = minBonus + progressFactor * (maxBonus - minBonus);
        bonus = MathHelper.clamp(bonus, Math.min(minBonus, maxBonus), Math.max(minBonus, maxBonus));

        return 1.0f + bonus;
    }
}
