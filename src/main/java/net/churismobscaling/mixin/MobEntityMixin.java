package net.churismobscaling.mixin;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.api.MobLevelApi;
import net.churismobscaling.accessor.LevelledMob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for MobEntity to add custom level scaling mechanics.
 * This includes managing mob levels, applying stat modifiers based on level,
 * and persisting level data according to ChurisMobScalingMain mod's configuration.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements LevelledMob {

    @Unique
    private static final int LEVEL_NOT_YET_CALCULATED = 0;
    @Unique
    private static final int MINIMUM_MOB_LEVEL = 1;
    @Unique
    private static final float BASE_MULTIPLIER = 1.0f;
    // Used to determine if a stat multiplier is significant enough to apply a modifier.
    @Unique
    private static final float MODIFIER_APPLICATION_THRESHOLD = 0.001f;

    @Unique
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("churismobscaling", "health_modifier");
    @Unique
    private static final Identifier ATTACK_MODIFIER_ID = Identifier.of("churismobscaling", "attack_modifier");

    @Unique
    private int mobLevel = LEVEL_NOT_YET_CALCULATED;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // Injected method to handle mob initialization after the original initialize method.
    // Sets the mob's level if it hasn't been calculated or loaded yet, and the mod is enabled.
    @Inject(
        method = "initialize(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/EntityData;)Lnet/minecraft/entity/EntityData;",
        at = @At("TAIL")
    )
    private void onInitialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                          SpawnReason spawnReason, @Nullable EntityData entityData,
                                          CallbackInfoReturnable<EntityData> cir) {
        if (!ChurisMobScalingMain.getConfig().getEnableMod()) {
            return;
        }

        // For newly spawned entities. Entities loaded from NBT have levels set by readCustomDataFromNbt.
        if (this.mobLevel == LEVEL_NOT_YET_CALCULATED) {
            MobEntity currentMob = (MobEntity) (Object) this;
            // MobLevelApi.getEntityLevel calculates, sets the internal level via setLevelInternal (which applies stats).
            MobLevelApi.getEntityLevel(currentMob);
        }
    }

    // Applies health and attack damage modifiers based on the mob's level.
    // This method is called when a mob's level is set (e.g. via setLevelInternal) or loaded from NBT.
    @Unique
    private void applyStatModifiers(int level) {
        float healthMultiplier = MobLevelApi.getHealthMultiplier(level);
        float attackMultiplier = MobLevelApi.getAttackMultiplier(level);

        EntityAttributeInstance healthAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.removeModifier(HEALTH_MODIFIER_ID); // Remove any existing modifier first.
            // Apply new modifier only if the level is valid and the multiplier causes a significant change from the base.
            if (level >= MINIMUM_MOB_LEVEL && Math.abs(healthMultiplier - BASE_MULTIPLIER) > MODIFIER_APPLICATION_THRESHOLD) {
                EntityAttributeModifier healthModifier = new EntityAttributeModifier(
                    HEALTH_MODIFIER_ID,
                    healthMultiplier - BASE_MULTIPLIER, // Modifier value is the difference from the base (e.g., 0.2 for 20% increase).
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
                healthAttribute.addPersistentModifier(healthModifier);
            }
            this.setHealth(this.getMaxHealth()); // Ensure current health is updated to the new max health.
        }

        EntityAttributeInstance damageAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.removeModifier(ATTACK_MODIFIER_ID); // Remove any existing modifier first.
            // Apply new modifier only if the level is valid and the multiplier causes a significant change from the base.
            if (level >= MINIMUM_MOB_LEVEL && Math.abs(attackMultiplier - BASE_MULTIPLIER) > MODIFIER_APPLICATION_THRESHOLD) {
                EntityAttributeModifier damageModifier = new EntityAttributeModifier(
                    ATTACK_MODIFIER_ID,
                    attackMultiplier - BASE_MULTIPLIER, // Modifier value is the difference from the base.
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
                damageAttribute.addPersistentModifier(damageModifier);
            }
        }
    }

    // Injected method to write the mob's level to NBT data during world save/entity serialization.
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        // Only save the level if it's a valid, calculated level (not in its initial uncalculated state).
        if (this.mobLevel >= MINIMUM_MOB_LEVEL) {
            nbt.putInt(MobLevelApi.MOB_LEVEL_KEY, this.mobLevel);
        }
    }

    // Injected method to read the mob's level from NBT data when the entity is loaded.
    // This is typically called before onInitialize for existing entities being loaded from disk.
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(MobLevelApi.MOB_LEVEL_KEY, NbtCompound.INT_TYPE)) {
            int loadedLevel = nbt.getInt(MobLevelApi.MOB_LEVEL_KEY);
            // Ensure loaded level is at least the minimum defined level.
            this.mobLevel = Math.max(MINIMUM_MOB_LEVEL, loadedLevel);
            // Apply stats immediately for loaded mobs, as their level is now known.
            // This is crucial because onInitialize might not re-apply if mobLevel is already set.
            if (ChurisMobScalingMain.getConfig().getEnableMod()) { // Also check if mod is enabled before applying stats
                applyStatModifiers(this.mobLevel);
            }
        }
        // If MOB_LEVEL_KEY is not present, mobLevel remains LEVEL_NOT_YET_CALCULATED.
        // If it's a new mob (not loaded from NBT), onInitialize will handle level calculation.
    }

    /**
     * Gets the internal level of this mob.
     * The level indicates its scaled difficulty and stats.
     *
     * @return The current level of the mob. Returns {@code LEVEL_NOT_YET_CALCULATED} (0)
     *         if the level has not been determined or loaded yet.
     */
    @Override
    public int getLevelInternal() {
        return this.mobLevel;
    }

    /**
     * Sets the internal level of this mob and applies corresponding stat modifiers.
     * The level will be clamped to a minimum of {@code MINIMUM_MOB_LEVEL} (1).
     * This method should be the primary way to change a mob's level programmatically
     * within the mod's logic.
     *
     * @param level The new level to set for the mob.
     */
    @Override
    public void setLevelInternal(int level) {
        // Ensure the new level is at least the minimum allowed.
        int newLevel = Math.max(MINIMUM_MOB_LEVEL, level);
        this.mobLevel = newLevel;
        // Apply stat modifiers whenever the level is explicitly set, if the mod is enabled.
        if (ChurisMobScalingMain.getConfig().getEnableMod()) {
            applyStatModifiers(this.mobLevel);
        }
    }
}