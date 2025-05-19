# Churi's Mob Scaling

Churi's Mob Scaling is a Minecraft mod that introduces a dynamic difficulty system by scaling mob strength based on various factors, primarily their distance from the world's spawn point. As players venture further out, they will encounter more challenging enemies with increased health, attack damage, and potentially better loot and more experience points.

This mod is heavily inspired by [RPG Difficulty by Globox](https://modrinth.com/mod/rpgdifficulty) but features a distinct implementation and a different overall style.

## Features

*   **Distance-Based Difficulty:** Mobs get stronger the further they are from the world spawn.
*   **Configurable Scaling:** Many aspects of the scaling can be configured, including maximum level, noise-based level variation, spawn influence radius, and dimension-specific level offsets.
*   **Stat Adjustments:** Mob health and attack damage are scaled according to their calculated level.
*   **Loot and XP Scaling:** Loot drops and experience points can also be configured to scale with mob level.
*   **Borderlands-Style Mob HUD:** An optional in-game HUD displays the name, level, and health of targeted mobs, reminiscent of the UI in the Borderlands series. This HUD is configurable in terms of scale and display duration.
*   **Mob Blacklist:** Specific mobs can be excluded from the scaling system via configuration.

## How it Works

The mod calculates a level for each mob based on:

1.  **Distance from Spawn:** The primary factor.
2.  **Noise-Based Variation:** A configurable OpenSimplex noise algorithm adds regional variation to mob levels.
3.  **Spawn Influence Radius:** Within a configurable radius around the spawn point, mob levels are scaled down to provide a gentler starting experience.
4.  **Dimension Offsets:** Different dimensions (like The Nether or The End) can have base level offsets.

Based on this calculated level, the mod then applies multipliers to the mob's base health, attack damage, loot drops, and experience reward. These multipliers are also configurable with minimum and maximum bonus caps.

## Building from Source

To build Churi's Mob Scaling from source, you will need:

*   Java Development Kit (JDK) - Version 21 or higher is recommended (as per `fabric.mod.json`).
*   Gradle

Clone the repository and run the following command in the project's root directory:

```bash
./gradlew build
```

This will compile the mod and produce a JAR file in the `build/libs/` directory.

## Credits

*   **Globox:** For the original RPG Difficulty mod, which served as a major inspiration for this project.