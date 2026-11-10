# InfuseSMP S3

**A Minecraft survival ability plugin built around collectible Infuse effects, powerful sparks, custom crafting, and augmented rituals.**

InfuseSMP S3 brings thirteen distinct effects into your server. Players can collect effect items, equip a primary and secondary ability, trigger their sparks, craft upgrades, and combine effects with combat, exploration, and progression.

> **Project status:** Active development. Features and configuration may change before the first stable release.

## Features

### 13 unique Infuse effects

Each effect has its own passive behavior, spark ability, cooldown, duration, and augmented variant.

| Effect | Theme |
|---|---|
| Emerald | Experience, loot, and mining bonuses |
| Feather | Fall protection, wind bursts, and aerial attacks |
| Fire | Fire-based combat, arrows, and lava interactions |
| Frost | Freezing effects and temporary snow trails |
| Haste | Faster breaking and temporary tool enchantments |
| Heart | Health-focused passives and healing sparks |
| Invisibility | Stealth and concealment |
| Ocean | Water-based utility, air pressure, and pulling targets |
| Regeneration | Healing and restorative effects |
| Speed | Mobility and dash abilities |
| Strength | Enhanced combat and shield interactions |
| Thunder | Lightning strikes and chain damage |
| Ender | Teleportation and linked curse effects |

### Equipment and ability system

- Equip one **primary** and one **secondary** effect.
- Use regular or augmented Infuse items.
- Activate each equipped effect's spark independently.
- Configure spark controls to use swap-hands or drop.
- Swap equipped slots or drain an effect back into an item.
- Save player equipment, trust lists, controls, and effect history between restarts.
- Configure cooldowns, durations, hit counters, and effect behavior.

### Crafting and augmented rituals

- Configure shaped crafting recipes in `recipes.yml`.
- Set regular and augmented crafting limits for each effect.
- Create augmented effects through brewing-stand rituals.
- Track ritual progress with a boss bar.
- Configure ritual duration, Ender ritual duration, beacon visuals, and brewing-stand protection.

### Multiplayer and server controls

- Player trust lists with configurable capacity and action delay.
- Configurable incoming and outgoing hit-counter triggers.
- Choose whether effects can drop on natural death.
- Disable effects in selected worlds or configured coordinate regions.
- Admin commands for checking, giving, equipping, and removing effects.
- Reload supported configuration files without restarting the server.

### Menus and customization

- In-game effect and recipe guide menus.
- Configure menu titles, sizes, slots, and filler items in `gui.yml`.
- Customize messages and help text in `messages.yml` using MiniMessage formatting.
- Tune gameplay behavior, cooldowns, durations, controls, and resource-pack delivery in `config.yml`.

### Included resource pack

The original S3 texture pack is bundled with the plugin. GitHub Actions builds and publishes version-matched ZIPs for supported Minecraft releases to the [S3 resource-pack release](https://github.com/veyl-dev/Infuse/releases/tag/s3-resourcepack).

On startup, the plugin selects the matching pack, calculates its SHA-1, and automatically sends a resource-pack request when a player joins. No external hosting, URL, or SHA-1 configuration is required.

Automatic delivery is enabled by default. Set `settings.resource-pack.auto-download: false` in `config.yml` only if you intentionally want to disable it. A local adapted copy is also stored in:

```
plugins/InfuseSMP-S3/resourcepack/
```

## Commands

### Player commands

| Command | Description |
|---|---|
| `/infuse help` | Show the command help |
| `/infuse gui` | Open the Infuse effects guide |
| `/infuse effects` | List all available effects |
| `/infuse controls` | View your primary and secondary spark controls |
| `/infuse bind <primary\|secondary> <swap\|drop>` | Change a spark activation control |
| `/infuse get [player]` | View equipped effects |
| `/activate <primary\|secondary>` | Activate an equipped spark |
| `/drain <primary\|secondary>` | Return an equipped effect to an item |
| `/swap` | Swap primary and secondary effects |
| `/trust list` | View your trusted players |
| `/trust add <player>` | Add a player to your trust list |
| `/trust remove <player>` | Remove a player from your trust list |
| `/trust clear` | Clear your trust list |
| `/recipes` | Open the recipe guide |

**Aliases:** `/infuse` also has the `/infusesmp` alias. `/activate` has `/a` and `/use` aliases.

### Admin commands

| Command | Description | Permission |
|---|---|---|
| `/infuse reload` | Reload supported configuration files | `infusesmp.reload` |
| `/infuse add <player> <slot> <effect> [augmented]` | Equip an effect on a player's slot | `infusesmp.manage` |
| `/infuse remove <player> <slot>` | Remove an effect from a player's slot | `infusesmp.manage` |
| `/infuse give <player> <effect> [augmented]` | Give an Infuse effect item | `infusesmp.give` |

For the `slot` argument, use `primary` or `secondary`. For the optional `augmented` argument, use `true` or `false`.

## Permissions

| Permission | Default | Purpose |
|---|---|---|
| `infusesmp.use` | Everyone | Basic plugin use |
| `infusesmp.admin` | Operators | Parent permission for admin actions |
| `infusesmp.reload` | Operators | Reload configuration |
| `infusesmp.manage` | Operators | Equip and remove player effects |
| `infusesmp.give` | Operators | Give effect items |

The `infusesmp.admin` permission includes the reload, manage, and give permissions.

## Configuration files

The plugin creates and maintains these files in `plugins/InfuseSMP-S3/`:

| File | Purpose |
|---|---|
| `config.yml` | Main gameplay settings, effects, cooldowns, durations, controls, disabled worlds/regions, trust settings, and resource-pack options |
| `messages.yml` | Customizable player messages and help text |
| `gui.yml` | Effect and recipe menu layouts, titles, slots, and filler items |
| `recipes.yml` | Shaped crafting recipe layouts and ingredients |
| `players.yml` | Automatically saved player data, equipped slots, controls, trust lists, and history |
| `crafting.yml` | Automatically saved crafting counters and limits |

Custom Model Data identifiers are fixed internally and are not exposed as editable configuration options.

## Installation

1. Download the JAR built for your Minecraft server version.
2. Stop your Paper server.
3. Place the JAR in the server's `plugins` folder.
4. Start the server and allow the plugin to generate its configuration files.
5. Review `config.yml`, `messages.yml`, `gui.yml`, and `recipes.yml`.
6. Restart the server or use `/infuse reload` after changing supported settings.

Use only the artifact that matches your server version.

## Resource pack delivery

To send the included resource pack to players automatically:

1. Start the server once so the plugin generates the version-specific ZIP.
2. Find the generated ZIP in `plugins/InfuseSMP-S3/resourcepack/`.
3. Upload that ZIP to a public HTTPS file host.
4. Calculate the SHA-1 hash of the exact uploaded ZIP.
5. Edit the following settings in `config.yml`:

```yaml
settings:
  resource-pack:
    enabled: true
    url: "https://your-host.example/InfuseSMP-S3.zip"
    sha1: "YOUR_40_CHARACTER_SHA1"
    prompt: "Install the InfuseSMP S3 resource pack for the complete experience."
```

6. Restart the server and test the download prompt with a client on your target Minecraft version.

The plugin does not provide file hosting. The resource pack should be checked in-game on each supported client version before public distribution.

## Compatibility and builds

The project currently provides separate CI build targets:

| Minecraft target | Java | Paper API target |
|---|---:|---|
| 1.21.x | 21 | 1.21.1 API line |
| 26.3 | 25 | 26.3 API line |

These targets are independently compiled by the GitHub Actions workflow. Use the matching artifact from the workflow's **Build** run.

### Build from source

For the 1.21 API line:

```bash
gradle clean build -PtargetJava=21 -PpaperApiVersion=1.21.1-R0.1-SNAPSHOT -PtargetApi=1.21
```

For Minecraft 26.3:

```bash
gradle clean build -PtargetJava=25 -PpaperApiVersion=26.3.build.+ -PtargetApi=26.3
```

The compiled JAR files are placed in `build/libs/`.

## Development and testing

InfuseSMP S3 is under active development. CI compilation confirms that the source builds against the configured API targets; it does not replace live gameplay testing.

Before a stable release, test the plugin on real Paper servers for each supported version, including:

- All thirteen effects, passives, sparks, cooldowns, and augmented variants.
- Crafting recipes, crafting limits, brewing rituals, and ritual cancellation.
- Player data saving and loading across server restarts.
- PvP and PvE interactions, including damage, projectiles, death drops, and trust behavior.
- GUI layouts, messages, and command permissions.
- Resource-pack loading and the appearance of every custom item in-game.

## Contributing and bug reports

Bug reports and feature suggestions are welcome through the repository's [GitHub Issues](https://github.com/veyl-dev/Infuse/issues). When reporting a problem, include:

- Minecraft server version and software (Paper or compatible fork).
- Java version.
- InfuseSMP S3 build or commit.
- Steps to reproduce the issue.
- Relevant server logs and configuration excerpts (remove private information).

## Project links

- **Source code:** [veyl-dev/Infuse](https://github.com/veyl-dev/Infuse)
- **Issues and feature requests:** [GitHub Issues](https://github.com/veyl-dev/Infuse/issues)
- **Build artifacts:** [GitHub Actions](https://github.com/veyl-dev/Infuse/actions)

## Credits

The original S3 resource pack and its included attribution notices are preserved in the project. The project also acknowledges PluginMart/Infuse as a reference for the plugin's structure and gameplay context.
