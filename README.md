# SMP Position Saver

Plugin Bukkit pour Minecraft 1.21.8 qui permet de sauvegarder et restaurer les positions des joueurs dans les mondes SMP.

## Fonctionnalités

- **Sauvegarde automatique** : Sauvegarde la position du joueur lorsqu'il quitte un monde SMP
- **Restauration automatique** : Restaure la dernière position sauvegardée lorsqu'un joueur entre dans un monde SMP
- **Configuration flexible** : Liste configurable des mondes considérés comme SMP
- **Support multi-mondes** : Gère plusieurs mondes SMP simultanément
- **Stockage YAML** : Positions sauvegardées dans un fichier `positions.yml`

## Installation

1. Compilez le plugin avec Maven :
   ```bash
   mvn clean package
   ```

2. Copiez le fichier JAR généré dans `target/` vers le dossier `plugins/` de votre serveur Minecraft

3. Redémarrez le serveur ou rechargez les plugins

## Configuration

Le fichier `config.yml` se générera automatiquement dans le dossier du plugin :

```yaml
# Configuration des mondes SMP
smp-worlds:
  - "world"
  - "world_nether"
  - "world_the_end"
  - "survival"
  - "survival_nether"
  - "survival_the_end"

# Messages personnalisables
messages:
  position-saved: "§aVotre position a été sauvegardée dans {world}!"
  position-restored: "§aVous avez été téléporté à votre dernière position dans {world}!"
  no-position-saved: "§cAucune position sauvegardée pour ce monde."
  config-reloaded: "§aConfiguration rechargée avec succès!"

# Options
options:
  save-on-quit: true
  restore-on-join: true
  debug: false
```

### Configuration des mondes SMP

Ajoutez ou retirez des mondes dans la section `smp-worlds` pour définir quels mondes sont considérés comme faisant partie du groupe SMP.

- Les noms peuvent être des correspondances exactes ou utiliser le caractère joker `*`.
  Par exemple `world_*` couvrira `world_1`, `world_nether`, etc.

### Options

- `save-on-quit` : Sauvegarder la position lorsque le joueur quitte le serveur
- `restore-on-join` : Restaurer la position lorsque le joueur rejoint le serveur
- `debug` : Activer les logs de débogage

## Fonctionnement

### Quand un joueur entre dans un monde SMP :

1. Le plugin vérifie si une position a été sauvegardée pour ce joueur dans ce monde
2. Si oui, le joueur est téléporté à sa dernière position sauvegardée
3. Si non, un message l'informe qu'aucune position n'est disponible

### Quand un joueur quitte un monde SMP :

1. Le plugin sauvegarde automatiquement la position actuelle du joueur
2. Les coordonnées (x, y, z), la direction (yaw, pitch) et le monde sont enregistrés
3. Un message confirme la sauvegarde

### Stockage des données

Les positions sont sauvegardées dans le fichier `plugins/SMPPositionSaver/positions.yml` avec cette structure :

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

- `smppositionsaver.use` : Permet d'utiliser le système de sauvegarde de position (par défaut : true)

## Commandes

Ce plugin ne nécessite aucune commande pour fonctionner. Tout est automatique.

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

Pour toute question ou problème, veuillez consulter les logs du serveur ou activer le mode debug dans la configuration.
