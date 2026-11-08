# InfuseSMP S3

Standalone Java/Paper plugin conversion of the supplied Infuse SMP Season 3 Skript project.

## Current conversion status

The repository now contains a Java plugin foundation with:
- Paper build targets for the 1.21.x API line and the 26.3 API line.
- Persistent primary/secondary player effect slots, trusted-player UUIDs, cooldowns, and history.
- Thirteen S3 effect definitions and color-tinted custom potion items using the original S3 model-data IDs.
- Drinking a custom effect bottle equips it in the next available slot.
- Basic spark actions for all effect types, plus initial hit-trigger behavior for Fire, Thunder, Frost, Invisibility, Regeneration, Emerald, and Feather.
- Original S3 shaped recipes, craft limits, and augmented-effect counters.
- A bundled copy of the supplied original texture pack, adapted at startup for the detected Minecraft resource-pack format and item-model era.
- Config files separated into config.yml, messages.yml, and gui.yml.
- GitHub Actions build matrix for Java 21 / Paper 1.21 and Java 25 / Paper 26.3.

**This is still an active conversion, not a feature-complete release.** The 10-minute augmented rituals, full original passive mechanics, all projectile and damage listeners, trust-delay behavior, GUI screens, bind/control system, drain commands, region mapping, and complete animation parity still need implementation and testing.

## Target platform

- Paper and compatible forks such as Purpur.
- Target: Minecraft 1.21.x through 26.3.
- Java 21 is required for the 1.21 line; Java 25 is required by Minecraft 26.x.
- Separate build artifacts are generated for the legacy and modern API lines. Each artifact must pass CI and real-server checks before being marked tested.

## Build

Install the JDK matching the target and Gradle 9.2.1+, then run:

    gradle clean build -PtargetJava=21 -PpaperApiVersion=1.21.1-R0.1-SNAPSHOT -PtargetApi=1.21

For the modern build:

    gradle clean build -PtargetJava=25 -PpaperApiVersion=26.3.build.+ -PtargetApi=26.3

The jar is generated in build/libs/.

## Configuration

- config.yml: gameplay values, effect limits, cooldowns, and resource-pack settings.
- messages.yml: message templates.
- gui.yml: inventory GUI layout and slots.
- players.yml: player effect slots and trusted-player data.
- crafting.yml: global craft counters.

## Resource pack

The original pack is bundled and a server-version-specific ZIP is generated in plugins/InfuseSMP-S3/resourcepack/. See resourcepack/README.md. To prompt clients automatically, host that ZIP at a public URL and set settings.resource-pack.enabled, url, and sha1 in config.yml.

## Effects

Emerald, Feather, Fire, Frost, Haste, Heart, Invisibility, Ocean, Regeneration, Speed, Strength, Thunder, and Ender.

## Reference and attribution

The conversion is based on the user's supplied Infuse SMP S3 Skript archive. PluginMart/Infuse is used as a structural reference only; no source files from that repository are copied. Preserve the original resource-pack attribution and licensing notices when redistributing its assets.
