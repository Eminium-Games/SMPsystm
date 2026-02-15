package fr.smp.smpositionsaver.managers;

import fr.smp.smpositionsaver.SMPPositionSaver;
import fr.smp.smpositionsaver.models.PlayerPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PositionManager {
    
    private final SMPPositionSaver plugin;
    private final Map<UUID, Map<String, PlayerPosition>> playerPositions;
    private final File positionsFile;
    private FileConfiguration positionsConfig;
    
    public PositionManager(SMPPositionSaver plugin) {
        this.plugin = plugin;
        this.playerPositions = new ConcurrentHashMap<>();
        this.positionsFile = new File(plugin.getDataFolder(), "positions.yml");
        loadPositions();
    }
    
    private void loadPositions() {
        if (!positionsFile.exists()) {
            try {
                positionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Impossible de créer le fichier positions.yml: " + e.getMessage());
                return;
            }
        }
        
        positionsConfig = YamlConfiguration.loadConfiguration(positionsFile);
        
        // Charger les positions en mémoire
        ConfigurationSection playersSection = positionsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String playerUuid : playersSection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerUuid);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID invalide dans positions.yml: " + playerUuid);
                    continue;
                }
                
                Map<String, PlayerPosition> worldPositions = new HashMap<>();
                ConfigurationSection worldsSection = playersSection.getConfigurationSection(playerUuid);
                
                if (worldsSection != null) {
                    for (String worldName : worldsSection.getKeys(false)) {
                        ConfigurationSection posSection = worldsSection.getConfigurationSection(worldName);
                        if (posSection != null) {
                            PlayerPosition position = new PlayerPosition(
                                worldName,
                                posSection.getDouble("x"),
                                posSection.getDouble("y"),
                                posSection.getDouble("z"),
                                (float) posSection.getDouble("yaw"),
                                (float) posSection.getDouble("pitch")
                            );
                            worldPositions.put(worldName, position);
                        }
                    }
                }
                
                playerPositions.put(uuid, worldPositions);
            }
        }
        
        plugin.getLogger().info("Positions chargées pour " + playerPositions.size() + " joueurs.");
    }
    
    public void savePosition(Player player, String worldName) {
        UUID playerId = player.getUniqueId();
        Location location = player.getLocation();
        
        PlayerPosition position = new PlayerPosition(location);
        
        // Sauvegarder en mémoire
        playerPositions.computeIfAbsent(playerId, k -> new HashMap<>()).put(worldName, position);
        
        // Sauvegarder dans le fichier
        String playerPath = "players." + playerId.toString() + "." + worldName;
        positionsConfig.set(playerPath + ".x", location.getX());
        positionsConfig.set(playerPath + ".y", location.getY());
        positionsConfig.set(playerPath + ".z", location.getZ());
        positionsConfig.set(playerPath + ".yaw", location.getYaw());
        positionsConfig.set(playerPath + ".pitch", location.getPitch());
        
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Position sauvegardée pour " + player.getName() + " dans " + worldName);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder la position: " + e.getMessage());
        }
    }
    
    public PlayerPosition getLastPosition(UUID playerId, String worldName) {
        Map<String, PlayerPosition> worldPositions = playerPositions.get(playerId);
        if (worldPositions != null) {
            return worldPositions.get(worldName);
        }
        return null;
    }
    
    public boolean hasLastPosition(UUID playerId, String worldName) {
        Map<String, PlayerPosition> worldPositions = playerPositions.get(playerId);
        return worldPositions != null && worldPositions.containsKey(worldName);
    }
    
    public boolean restorePosition(Player player, String worldName) {
        PlayerPosition position = getLastPosition(player.getUniqueId(), worldName);
        if (position == null) {
            return false;
        }
        
        World world = plugin.getServer().getWorld(position.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("Monde non trouvé: " + position.getWorldName());
            return false;
        }
        
        Location location = new Location(
            world,
            position.getX(),
            position.getY(),
            position.getZ(),
            position.getYaw(),
            position.getPitch()
        );
        
        player.teleport(location);
        plugin.getConfigManager().debugLog("Position restaurée pour " + player.getName() + " dans " + worldName);
        return true;
    }
    
    public void saveAllPositions() {
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Toutes les positions ont été sauvegardées.");
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder toutes les positions: " + e.getMessage());
        }
    }
    
    public void removePlayerPositions(UUID playerId) {
        playerPositions.remove(playerId);
        positionsConfig.set("players." + playerId.toString(), null);
        
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Positions supprimées pour le joueur " + playerId);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de supprimer les positions du joueur: " + e.getMessage());
        }
    }
}
