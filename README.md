# InfuseSMP S3

A standalone Java/Paper plugin port of the Infuse SMP Season 3 effect system, built in this repository as InfuseSMP-S3.

## Build validation

GitHub Actions builds two artifacts:

| Target | Java | Paper API | CI |
|---|---:|---|---|
| Minecraft 1.21.x | 21 | 1.21.1 API line | Passed |
| Minecraft 26.3 | 25 | 26.3 API line | Passed |

The build workflow uploads a separate JAR artifact for each target. Download the latest artifacts from the repository's Actions → Build workflow.

## Implemented systems

- Thirteen S3 effect types: Emerald, Feather, Fire, Frost, Haste, Heart, Invisibility, Ocean, Regeneration, Speed, Strength, Thunder and Ender.
- Primary and secondary equipped effect slots, regular and augmented potion items, persisted player data, cooldowns, charge counters and history.
- Effect passives and sparks, including movement, combat, food/XP, potion, damage and projectile interactions.
- Temporary Haste tool enchantments and Emerald Looting enchantments, with restoration of the player's original enchantment levels.
- Feather fall protection, wind-charge interactions and landing attacks; Frost snow trails with throttled placement and automatic cleanup.
- Fire arrows and lava movement; Ocean air drain and pull; Thunder chain lightning; Strength combat and shield interactions; Ender dragon-breath curse links and mob execution.
- Configurable hit-counter decay and incoming/outgoing hit triggers.
- Player trust lists with a maximum size and configurable action delay.
- Primary/secondary spark key bindings (swap-hands and drop controls), plus /infuse bind.
- /drain to return an equipped effect to an item, /swap, /trust, /activate, /recipes and /infuse administration commands.
- Editable shaped recipes in recipes.yml, regular craft limits, augmented craft limits and protected brewing-stand rituals.
- Ten-minute augmented rituals by default, with a one-hour Ender ritual, boss bar and optional beacon beam.
- Effect and recipe guide inventories configured through gui.yml.
- Configurable messages through messages.yml.
- Disabled-world and coordinate-based disabled-region mapping.
- Configurable effect drops on death.
- Original S3 texture pack bundled and adapted for the detected server resource-pack format.

## Commands

| Command | Purpose |
|---|---|
| /infuse gui | Open the effect and recipe guide |
| /infuse effects | List effect types |
| /infuse controls | Show spark bindings |
| /infuse bind primary/secondary swap/drop | Change a spark binding |
| /infuse get [player] | View equipped effects |
| /infuse add [player] [slot] [effect] [augmented] | Admin equip |
| /infuse remove [player] [slot] | Admin remove |
| /infuse give [player] [effect] [augmented] | Give an effect item |
| /activate primary/secondary | Activate a spark |
| /drain primary/secondary | Drain an equipped effect |
| /swap | Swap primary and secondary slots |
| /trust add/remove/list/clear [player] | Manage trusted players |
| /recipes | Open the recipe guide |

## Configuration files

- config.yml — effect behavior, cooldowns, durations, world/region rules, controls, trust, death drops and resource-pack settings.
- messages.yml — MiniMessage-formatted messages and help.
- gui.yml — menu titles, sizes, slots and filler items.
- recipes.yml — editable recipe shapes and ingredients.
- players.yml — persisted player slots, trust lists, controls and history.
- crafting.yml — persisted regular and augmented craft counters.

## Resource pack setup

The original texture pack is bundled in the plugin JAR as a Base64-encoded ZIP. On startup the plugin extracts a server-version-specific ZIP into:

plugins/InfuseSMP-S3/resourcepack/

To automatically prompt clients, host the generated ZIP at a public HTTPS URL and set:

    settings:
      resource-pack:
        enabled: true
        url: "https://your-host/InfuseSMP-S3.zip"
        sha1: "YOUR_40_CHARACTER_SHA1"
        prompt: "&aDownload the Infuse SMP S3 resource pack."

The plugin prepares the ZIP but does not host it. Check resourcepack/README.md for format details.

## Build locally

For the 1.21 API line:

    gradle clean build -PtargetJava=21 -PpaperApiVersion=1.21.1-R0.1-SNAPSHOT -PtargetApi=1.21

For Minecraft 26.3:

    gradle clean build -PtargetJava=25 -PpaperApiVersion=26.3.build.+ -PtargetApi=26.3

Output: build/libs/.

## Verification scope and remaining QA

Both target artifacts compile successfully in GitHub Actions. That is compile validation, not proof of every gameplay interaction on a live server. Before a public release, test both JARs on real Paper/Purpur servers, verify each recipe and ritual, inspect the texture pack in-client on both version families, and exercise all effects in PvP and PvE.

The conversion work in this repository has used the S3 effect definitions and the public PluginMart/Infuse repository as structural/behavior references. The original standalone Skript source file was not available in the repository or the accessible file library during this pass, so exact line-by-line parity against a separate Skript archive has not been certified. If a different Skript file is the authoritative source, add it to the repository for a final parity audit.

## Attribution

The original texture pack and its included attribution notices are preserved. PluginMart/Infuse is used as a structural reference; its source files are not copied into this repository.
