# InfuseSMP S3

Standalone Java/Paper plugin conversion of the supplied Infuse SMP Season 3 Skript project.

## Project status

The initial Java plugin foundation is in place: Gradle build, Paper plugin metadata, modular effect definitions, player persistence, core command framework, combat event hooks, and separated configuration files. The detailed S3 effect mechanics, ritual crafting, GUIs, resource-pack packaging, and compatibility testing are still being ported; this is not yet a feature-complete release.

## Target platform

- Paper and compatible forks such as Purpur.
- Compatibility target: Minecraft 1.21.x through 26.3.
- Java 21 for the 1.21 line; Java 25 is required by Minecraft 26.x.
- Each version line must be validated in CI and on a real server before being marked tested.

## Build

Install JDK 21 and Gradle 8.10+, then run `gradle build`. The jar is generated in `build/libs/`.

## Configuration

- `config.yml`: gameplay values, effect limits, cooldowns, resource-pack settings.
- `messages.yml`: messages.
- `gui.yml`: inventory GUI layout and slots.
- Player data is stored in the plugin data folder.

## Effects

Emerald, Feather, Fire, Frost, Haste, Heart, Invisibility, Ocean, Regeneration, Speed, Strength, Thunder, and Ender.

## Reference and attribution

The conversion is based on the user's supplied Infuse SMP S3 Skript archive. PluginMart/Infuse is used as a structural reference only. No source files from that repository are copied. Preserve the original resource-pack attribution and licensing notices when redistributing its assets.
