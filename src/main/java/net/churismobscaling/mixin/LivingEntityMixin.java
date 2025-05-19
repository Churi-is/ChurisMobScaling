package net.churismobscaling.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.api.MobLevelApi;
import net.churismobscaling.accessor.LevelledMob;
import net.churismobscaling.util.LevelCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for {@link LivingEntity} to modify game mechanics related to mob scaling.
 * This includes adjusting loot drops and experience points based on mob levels
 * as defined by the ChurisMobScaling mod.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private static final float LOOT_MULTIPLIER_EPSILON = 0.001f;
    private static final float NEUTRAL_LOOT_MULTIPLIER = 1.0f;
    private static final int MINIMUM_XP_DROP = 1;

    /*
     * Injects into the dropLoot method to modify loot drops based on mob level.
     * This enhances loot drops for higher level mobs based on the loot modifier.
     * The method takes control of loot generation if the calculated lootMultiplier
     * deviates significantly from 1.0.
     *
     * @param damageSource The source of damage that killed the entity.
     * @param causedByPlayer True if the entity was killed by a player.
     * @param ci CallbackInfo for cancellable injection; used to prevent original loot drop.
     */
    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void modifyLootDrops(DamageSource damageSource, boolean causedByPlayer, CallbackInfo ci) {
        // Only apply loot modifications if the mod is enabled.
        if (!ChurisMobScalingMain.getConfig().getEnableMod()) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Loot modification requires a ServerWorld context.
        if (!(entity.getWorld() instanceof ServerWorld)) {
            return;
        }
        
        // Loot scaling only applies to entities that are designated as "LevelledMob".
        if (!(entity instanceof LevelledMob)) {
            return;
        }
        
        int level = MobLevelApi.getEntityLevel(entity);
        
        // Mob level must be positive to apply custom loot logic.
        if (level <= 0) {
            return;
        }
        
        float lootMultiplier = LevelCalculator.calculateLootMultiplier(level);
        
        // If the loot multiplier is effectively 1.0 (neutral), let vanilla handle loot drops.
        if (Math.abs(lootMultiplier - NEUTRAL_LOOT_MULTIPLIER) <= LOOT_MULTIPLIER_EPSILON) {
            return;
        }
        
        // Take control of loot drops from this point.
        ci.cancel();
        
        RegistryKey<LootTable> registryKey = entity.getLootTable();
        // If the entity has no associated loot table, no custom loot can be dropped.
        if (registryKey == null) {
            return;
        }
        
        ServerWorld serverWorld = (ServerWorld) entity.getWorld();
        LootTable lootTable = serverWorld.getServer().getReloadableRegistries().getLootTable(registryKey);
        
        LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder(serverWorld))
            .add(LootContextParameters.THIS_ENTITY, entity)
            .add(LootContextParameters.ORIGIN, entity.getPos())
            .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
            .addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker())
            .addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());
        
        LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);
        
        // If lootMultiplier is zero or negative, no loot should be dropped.
        if (lootMultiplier <= 0.0f) {
            // No loot is generated in this case.
            return;
        }

        // Casting to int truncates, effectively flooring for positive numbers.
        int wholeRolls = (int) lootMultiplier;

        // Calculate the chance for an additional partial roll.
        float fractionalChance = lootMultiplier - wholeRolls;

        // Generate loot for the guaranteed whole rolls.
        for (int i = 0; i < wholeRolls; i++) {
            // Use a slightly different seed for each full loot table generation to ensure variety.
            // Adding 'i + 1' to the entity's loot table seed helps differentiate sequential rolls.
            // This approach is generally sufficient for variety in typical scenarios.
            lootTable.generateLoot(lootContextParameterSet,
                                entity.getLootTableSeed() + i + 1, 
                                entity::dropStack);
        }

        // Handle the fractional chance for one additional roll.
        // This occurs if there's a remaining fractional chance and a random roll succeeds.
        if (fractionalChance > 0.0f && entity.getRandom().nextFloat() < fractionalChance) {
            // Use a seed different from those used in the whole rolls to maintain variety.
            // Adding 'wholeRolls + 1' places this seed after the last whole roll's seed,
            // ensuring it's unique even if wholeRolls is 0.
            lootTable.generateLoot(lootContextParameterSet,
                                entity.getLootTableSeed() + wholeRolls + 1,
                                entity::dropStack);
        }
    }

    /*
     * Injects into the getXpToDrop method to scale experience points (XP) dropped by mobs.
     * The XP is modified based on the mob's level and the configured XP multiplier.
     *
     * @param world The server world where the entity is. (From target method)
     * @param attacker The entity that attacked, possibly null. (From target method, not directly used for level scaling)
     * @param cir CallbackInfoReturnable for modifying the return value (XP amount).
     */
    @Inject(method = "getXpToDrop(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;)I",
            at = @At("RETURN"),
            cancellable = true)
    private void scaleXpDrop(ServerWorld world, @org.jetbrains.annotations.Nullable Entity attacker, CallbackInfoReturnable<Integer> cir) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        // XP scaling only applies to MobEntity instances.
        // This check prevents issues if 'thisEntity' is, for example, a player.
        if (!(thisEntity instanceof MobEntity)) {
            return;
        }

        // Only apply XP scaling if the mod is enabled.
        if (!ChurisMobScalingMain.getConfig().getEnableMod()) {
            return;
        }

        // This is the XP calculated by vanilla logic, including enchantments.
        int baseXpFromVanillaLogic = cir.getReturnValue();

        // If vanilla logic determined no XP should drop, respect that.
        if (baseXpFromVanillaLogic <= 0) {
            return;
        }

        MobEntity mobEntity = (MobEntity) thisEntity;
        
        int level = MobLevelApi.getEntityLevel(mobEntity);
        float xpMultiplier = MobLevelApi.getXpMultiplier(level);

        // Apply the level-based XP multiplier.
        int scaledXp = Math.round(baseXpFromVanillaLogic * xpMultiplier);

        // Ensure that if any XP was to be dropped, at least MINIMUM_XP_DROP is awarded.
        cir.setReturnValue(Math.max(MINIMUM_XP_DROP, scaledXp));
    }
}
