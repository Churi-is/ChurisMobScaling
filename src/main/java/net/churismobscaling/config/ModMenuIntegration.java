package net.churismobscaling.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.churismobscaling.ChurisMobScalingMain;
import net.churismobscaling.client.render.HudPosition;

/**
 * Integrates the mod's configuration with the ModMenu library,
 * providing a graphical interface for users to adjust settings.
 * This class implements {@link ModMenuApi} to register the configuration screen.
 */
public class ModMenuIntegration implements ModMenuApi {
    /**
     * Provides the factory for creating the mod's configuration screen.
     * This method is called by ModMenu to display the config screen.
     *
     * @return A {@link ConfigScreenFactory} that constructs the configuration screen.
     */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen;
    }

    // Creates and configures the settings screen for the mod.
    // This method defines all the categories, options, and their behaviors
    // that will be displayed to the user in the ModMenu configuration interface.
    private Screen createConfigScreen(Screen parent) {
        ModConfig config = ChurisMobScalingMain.getConfig();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.churismobscaling.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        ConfigCategory coreCategory = builder.getOrCreateCategory(Text.translatable("config.churismobscaling.category.core"));
        
        coreCategory.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.churismobscaling.enable_mod"),
                config.getEnableMod())
                .setDefaultValue(ModConfig.DEFAULT_ENABLE_MOD)
                .setSaveConsumer(config::setEnableMod)
                .setTooltip(Text.translatable("config.churismobscaling.enable_mod.tooltip"))
                .build());
                
        coreCategory.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.churismobscaling.max_level"),
                config.getMaxLevel(), 
                ModConfig.MIN_MAX_LEVEL, 
                ModConfig.MAX_MAX_LEVEL)
                .setDefaultValue(ModConfig.DEFAULT_MAX_LEVEL)
                .setSaveConsumer(config::setMaxLevel)
                .setTooltip(Text.translatable("config.churismobscaling.max_level.tooltip"))
                .build());
                
        coreCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.noise_scale"),
                config.getNoiseScale())
                .setDefaultValue(ModConfig.DEFAULT_NOISE_SCALE)
                .setSaveConsumer(config::setNoiseScale)
                .setTooltip(Text.translatable("config.churismobscaling.noise_scale.tooltip"))
                .build());
                
        coreCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.min_noise_level_percentage"),
                config.getMinNoiseLevelPercentage())
                .setDefaultValue(ModConfig.DEFAULT_MIN_NOISE_LEVEL_PERCENTAGE)
                .setMin(ModConfig.MIN_MIN_NOISE_LEVEL_PERCENTAGE)
                .setMax(ModConfig.MAX_MIN_NOISE_LEVEL_PERCENTAGE)
                .setSaveConsumer(config::setMinNoiseLevelPercentage)
                .setTooltip(Text.translatable("config.churismobscaling.min_noise_level_percentage.tooltip"))
                .build());
                
        coreCategory.addEntry(entryBuilder.startIntField(
                Text.translatable("config.churismobscaling.spawn_influence_radius"),
                config.getSpawnInfluenceRadius())
                .setDefaultValue(ModConfig.DEFAULT_SPAWN_INFLUENCE_RADIUS)
                .setMin(ModConfig.MIN_SPAWN_INFLUENCE_RADIUS)
                .setMax(ModConfig.MAX_SPAWN_INFLUENCE_RADIUS)
                .setSaveConsumer(config::setSpawnInfluenceRadius)
                .setTooltip(Text.translatable("config.churismobscaling.spawn_influence_radius.tooltip"))
                .build());
                
        ConfigCategory statsCategory = builder.getOrCreateCategory(Text.translatable("config.churismobscaling.category.mob_stats"));
                
        statsCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.min_total_health_bonus"),
                config.getMinTotalHealthBonus())
                .setDefaultValue(ModConfig.DEFAULT_MIN_TOTAL_HEALTH_BONUS)
                .setMin(ModConfig.MIN_MIN_TOTAL_HEALTH_BONUS)
                .setMax(ModConfig.MAX_MIN_TOTAL_HEALTH_BONUS)
                .setSaveConsumer(config::setMinTotalHealthBonus)
                .setTooltip(Text.translatable("config.churismobscaling.min_total_health_bonus.tooltip"))
                .build());
                
        statsCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.max_total_health_bonus"),
                config.getMaxTotalHealthBonus())
                .setDefaultValue(ModConfig.DEFAULT_MAX_TOTAL_HEALTH_BONUS)
                .setMin(ModConfig.MIN_MAX_TOTAL_HEALTH_BONUS)
                .setSaveConsumer(config::setMaxTotalHealthBonus)
                .setTooltip(Text.translatable("config.churismobscaling.max_total_health_bonus.tooltip"))
                .build());
                
        statsCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.min_total_attack_bonus"),
                config.getMinTotalAttackBonus())
                .setDefaultValue(ModConfig.DEFAULT_MIN_TOTAL_ATTACK_BONUS)
                .setMin(ModConfig.MIN_MIN_TOTAL_ATTACK_BONUS)
                .setMax(ModConfig.MAX_MIN_TOTAL_ATTACK_BONUS)
                .setSaveConsumer(config::setMinTotalAttackBonus)
                .setTooltip(Text.translatable("config.churismobscaling.min_total_attack_bonus.tooltip"))
                .build());
                
        statsCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.max_total_attack_bonus"),
                config.getMaxTotalAttackBonus())
                .setDefaultValue(ModConfig.DEFAULT_MAX_TOTAL_ATTACK_BONUS)
                .setMin(ModConfig.MIN_MAX_TOTAL_ATTACK_BONUS)
                .setSaveConsumer(config::setMaxTotalAttackBonus)
                .setTooltip(Text.translatable("config.churismobscaling.max_total_attack_bonus.tooltip"))
                .build());
                
        ConfigCategory lootCategory = builder.getOrCreateCategory(Text.translatable("config.churismobscaling.category.loot"));
                
        lootCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.min_total_loot_bonus"),
                config.getMinTotalLootBonus())
                .setDefaultValue(ModConfig.DEFAULT_MIN_TOTAL_LOOT_BONUS)
                .setMin(ModConfig.MIN_MIN_TOTAL_LOOT_BONUS)
                .setMax(ModConfig.MAX_MIN_TOTAL_LOOT_BONUS)
                .setSaveConsumer(config::setMinTotalLootBonus)
                .setTooltip(Text.translatable("config.churismobscaling.min_total_loot_bonus.tooltip"))
                .build());
                
        lootCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.max_total_loot_bonus"),
                config.getMaxTotalLootBonus())
                .setDefaultValue(ModConfig.DEFAULT_MAX_TOTAL_LOOT_BONUS)
                .setMin(ModConfig.MIN_MAX_TOTAL_LOOT_BONUS)
                .setSaveConsumer(config::setMaxTotalLootBonus)
                .setTooltip(Text.translatable("config.churismobscaling.max_total_loot_bonus.tooltip"))
                .build());
                
        lootCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.min_total_xp_bonus"),
                config.getMinTotalXpBonus())
                .setDefaultValue(ModConfig.DEFAULT_MIN_TOTAL_XP_BONUS)
                .setMin(ModConfig.MIN_MIN_TOTAL_XP_BONUS)
                .setMax(ModConfig.MAX_MIN_TOTAL_XP_BONUS)
                .setSaveConsumer(config::setMinTotalXpBonus)
                .setTooltip(Text.translatable("config.churismobscaling.min_total_xp_bonus.tooltip"))
                .build());
                
        lootCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.max_total_xp_bonus"),
                config.getMaxTotalXpBonus())
                .setDefaultValue(ModConfig.DEFAULT_MAX_TOTAL_XP_BONUS)
                .setMin(ModConfig.MIN_MAX_TOTAL_XP_BONUS)
                .setSaveConsumer(config::setMaxTotalXpBonus)
                .setTooltip(Text.translatable("config.churismobscaling.max_total_xp_bonus.tooltip"))
                .build());

        ConfigCategory visualCategory = builder.getOrCreateCategory(Text.translatable("config.churismobscaling.category.visual"));
                
        visualCategory.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.churismobscaling.enable_mob_label_hud"),
                config.getEnableMobLabelHud())
                .setDefaultValue(ModConfig.DEFAULT_ENABLE_MOB_LABEL_HUD)
                .setSaveConsumer(config::setEnableMobLabelHud)
                .setTooltip(Text.translatable("config.churismobscaling.enable_mob_label_hud.tooltip"))
                .build());
                
        visualCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.mob_label_hud_scale"),
                config.getMobLabelHudScale())
                .setDefaultValue(ModConfig.DEFAULT_MOB_LABEL_HUD_SCALE)
                .setMin(ModConfig.MIN_MOB_LABEL_HUD_SCALE)
                .setMax(ModConfig.MAX_MOB_LABEL_HUD_SCALE)
                .setSaveConsumer(config::setMobLabelHudScale)
                .setTooltip(Text.translatable("config.churismobscaling.mob_label_hud_scale.tooltip"))
                .build());
                
        visualCategory.addEntry(entryBuilder.startIntSlider(
                Text.translatable("config.churismobscaling.mob_label_hud_duration"),
                config.getMobLabelHudDuration(), 
                ModConfig.MIN_MOB_LABEL_HUD_DURATION, 
                ModConfig.MAX_MOB_LABEL_HUD_DURATION)
                .setDefaultValue(ModConfig.DEFAULT_MOB_LABEL_HUD_DURATION)
                .setSaveConsumer(config::setMobLabelHudDuration)
                .setTooltip(Text.translatable("config.churismobscaling.mob_label_hud_duration.tooltip"))
                .build());
                
        visualCategory.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.churismobscaling.enable_location_level_hud"),
                config.getEnableLocationLevelHud())
                .setDefaultValue(ModConfig.DEFAULT_ENABLE_LOCATION_LEVEL_HUD)
                .setSaveConsumer(config::setEnableLocationLevelHud)
                .setTooltip(Text.translatable("config.churismobscaling.enable_location_level_hud.tooltip"))
                .build());
                
        visualCategory.addEntry(entryBuilder.startFloatField(
                Text.translatable("config.churismobscaling.location_level_hud_scale"),
                config.getLocationLevelHudScale())
                .setDefaultValue(ModConfig.DEFAULT_LOCATION_LEVEL_HUD_SCALE)
                .setMin(ModConfig.MIN_LOCATION_LEVEL_HUD_SCALE)
                .setMax(ModConfig.MAX_LOCATION_LEVEL_HUD_SCALE)
                .setSaveConsumer(config::setLocationLevelHudScale)
                .setTooltip(Text.translatable("config.churismobscaling.location_level_hud_scale.tooltip"))
                .build());
                
        visualCategory.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("config.churismobscaling.location_level_hud_position"),
                HudPosition.class,
                HudPosition.fromIndex(config.getLocationLevelHudPosition()))
                .setDefaultValue(HudPosition.TOP_LEFT) // This is already an enum constant
                .setSaveConsumer(value -> config.setLocationLevelHudPosition(value.getIndex()))
                .setEnumNameProvider(value -> Text.translatable("config.churismobscaling.location_level_hud_position." + ((HudPosition)value).name().toLowerCase()))
                .setTooltip(Text.translatable("config.churismobscaling.location_level_hud_position.tooltip"))
                .build());
                
        builder.setSavingRunnable(config::saveConfig);
        
        return builder.build();
    }
}
