# SMP Position Saver

A Bukkit plugin for Minecraft 1.21.8 that saves and restores player positions in SMP worlds.

## Features

- **Auto save**: Saves player position when they leave an SMP world
- **Auto restore**: Restores the last saved position when a player enters an SMP world
- **Flexible configuration**: Configurable list of worlds treated as SMP
- **Multi-world support**: Handles multiple SMP worlds at once
- **YAML storage**: Positions stored in `positions.yml`

## Installation

1. Compile the plugin with Maven:
   ```bash
   mvn clean package
   ```

2. Copy the generated JAR from `target/` to your server's `plugins/` folder.

3. Restart the server or reload the plugins.

## Configuration

The `config.yml` file will be generated automatically in the plugin folder:

```yaml
# SMP worlds configuration
smp-worlds:
  - "world"
  - "world_nether"
  - "world_the_end"
  - "survival"
  - "survival_nether"
  - "survival_the_end"

# Customizable messages
messages:
position-saved: "§aYour position has been saved in {world}!"
    position-restored: "§aYou have been teleported to your last position in {world}!"
    no-position-saved: "§cNo saved position for this world."
    config-reloaded: "§aConfiguration reloaded successfully!"

# Options
options:
  save-on-quit: true
  restore-on-join: true
  clear-on-death: true
  debug: false
  # Datapack functions executed when entering/exiting an SMP world
  entry-function: "datapack:functions/enter_smp"
  exit-function: "datapack:functions/exit_smp"
```

### SMP worlds configuration

Add or remove worlds in the `smp-worlds` section to define which worlds
belong to the SMP group.

- Names may be exact or use the wildcard `*`.
  For instance `world_*` will match `world_1`, `world_nether`, etc.

### Options

- `save-on-quit`: Save the position when the player leaves the server
- `restore-on-join`: Restore the position when the player joins the server
- `clear-on-death`: Remove saved coordinates when a player dies in an SMP world
- `debug`: Enable debug logging

## How it works

### When a player enters an SMP world:

1. The plugin checks if a position was saved for that player in this world
2. If so, the player is teleported to their last saved position
3. Otherwise a message informs them that no position is available

### When a player leaves an SMP world:

1. The plugin automatically saves the player's current position
2. Coordinates (x, y, z), orientation (yaw, pitch) and world are recorded
3. A message confirms the save

### Data storage

Positions are saved in `plugins/SMPPositionSaver/positions.yml` with this structure:

```yaml
players:
  <uuid-du-joueur>:
    <nom-du-monde>:
      x: 123.45
      y: 64.0
      z: -456.78
      yaw: 90.0
      pitch: 0.0
```

## Permissions

- `smppositionsaver.use`: Allows use of the position saving system (default: true)

## Commands

- `/smp`: teleports the player to the SMP coordinates defined in the configuration.
- `/smp reload`: reloads the configuration file; requires permission
  `smp.reload` (op by default).

This command isn't required for normal operation but can be useful after editing
`config.yml` without restarting the server.

## Compatibilité

- **Minecraft** : 1.21.8
- **API** : Spigot/Paper 1.21+
- **Java** : 21+

## Développement

Le plugin est structuré avec les composants suivants :

- `SMPPositionSaver` : Classe principale du plugin
- `ConfigManager` : Gestionnaire de configuration
- `PositionManager` : Gestionnaire des positions des joueurs
- `PlayerWorldChangeListener` : Listener pour les événements de changement de monde
- `PlayerPosition` : Modèle pour représenter une position sauvegardée

## Support

For any questions or issues, please check the server logs or enable debug mode in the configuration.
