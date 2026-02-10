# Tier Tagger

A client-side Minecraft mod that displays player tier rankings from multiple tier list APIs directly in-game — above player heads and in the tab list.

## Features

- **Multi-source support** — Tier Tests, MCTiers, SubTiers, PVPTiers
- **Cross-API fallback** — resolve tiers across sources when the selected one has no data
- **Nametag display** — prefix or suffix tier tags on player nametags
- **Tab list display** — show tiers in the player tab overlay
- **Player lookup** — keybind (B) to look up the nearest player's tiers
- **Player search** — search any player by username from the config screen
- **Configurable** — gamemode, display mode, icon style, region display, MCTiers format
- **Peak tiers** — shows peak tier history (Tier Tests source)
- **Config keybind** — press N to open the in-game config screen

## Project Structure

### 1.21–1.21.8 (root)
- **common/** — shared code (API clients, config, data models, HUD, mixins, screens, utilities)
- **fabric/** — Fabric platform module
- **neoforge/** — NeoForge platform module

### 1.21.9–1.21.11 (v11/)
- **v11/common/** — shared code adapted for 1.21.9+ API changes
- **v11/fabric/** — Fabric platform module
- **v11/neoforge/** — NeoForge platform module

## Building

```bash
# Build 1.21–1.21.8
./gradlew collectJars

# Build 1.21.9–1.21.11
cd v11
./gradlew collectJars
```

All final JARs are collected into the `out/` folder.

## Configuration

Config file: `config/tiertagger.yml` (created on first launch)

| Setting | Description |
|---------|-------------|
| `tier-source` | API source (Tier Tests, MCTiers, SubTiers, PVPTiers) |
| `game-mode` | Selected gamemode index |
| `tier-display-mode` | Highest Fallback, Selected Only, Ranking, Cross-API |
| `display-type` | Prefix or Suffix |
| `icon-type` | Classic (Unicode) or MCTiers (PNG) |
| `use-mc-tiers-format` | Use HT/LT format instead of letter grades |
| `show-region` | Show player region tag |
| `show-in-tab` | Show tiers in the tab list |

## Keybinds

| Key | Action |
|-----|--------|
| N | Open config screen |
| B | Look up nearest player |

## License

See [LICENSE](LICENSE).
