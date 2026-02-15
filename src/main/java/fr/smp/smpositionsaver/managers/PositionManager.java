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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PositionManager {
    
    private final SMPPositionSaver plugin;
    private final Map<UUID, PlayerPosition> lastSmpPosition;
    private final File positionsFile;
    private FileConfiguration positionsConfig;
    
    public PositionManager(SMPPositionSaver plugin) {
        this.plugin = plugin;
        this.lastSmpPosition = new ConcurrentHashMap<>();
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
        
        // Charger les positions en mémoire - une seule position par joueur
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
                
                ConfigurationSection playerSection = playersSection.getConfigurationSection(playerUuid);
                if (playerSection != null) {
                    // Charger la dernière position SMP sauvegardée
                    ConfigurationSection posSection = playerSection.getConfigurationSection("last_smp_position");
                    if (posSection != null) {
                        PlayerPosition position = new PlayerPosition(
                            posSection.getString("world"),
                            posSection.getDouble("x"),
                            posSection.getDouble("y"),
                            posSection.getDouble("z"),
                            (float) posSection.getDouble("yaw"),
                            (float) posSection.getDouble("pitch")
                        );
                        lastSmpPosition.put(uuid, position);
                    }
                }
            }
        }
        
        plugin.getLogger().info("Positions SMP chargées pour " + lastSmpPosition.size() + " joueurs.");
    }
    
    public void savePosition(Player player, String worldName) {
        savePosition(player, player.getLocation(), worldName);
    }
    
    public void savePosition(Player player, Location location, String worldName) {
        UUID playerId = player.getUniqueId();
        
        PlayerPosition position = new PlayerPosition(location);
        
        // Sauvegarder en mémoire
        lastSmpPosition.put(playerId, position);
        
        // Sauvegarder dans le fichier - une seule entrée par joueur
        String playerPath = "players." + playerId.toString();
        positionsConfig.set(playerPath + ".last_smp_position.world", location.getWorld().getName());
        positionsConfig.set(playerPath + ".last_smp_position.x", location.getX());
        positionsConfig.set(playerPath + ".last_smp_position.y", location.getY());
        positionsConfig.set(playerPath + ".last_smp_position.z", location.getZ());
        positionsConfig.set(playerPath + ".last_smp_position.yaw", location.getYaw());
        positionsConfig.set(playerPath + ".last_smp_position.pitch", location.getPitch());
        
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Position SMP sauvegardée pour " + player.getName() + " dans " + worldName);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder la position: " + e.getMessage());
        }
    }
    
    public PlayerPosition getLastPosition(UUID playerId) {
        return lastSmpPosition.get(playerId);
    }
    
    public boolean hasLastPosition(UUID playerId) {
        return lastSmpPosition.containsKey(playerId);
    }
    
    public boolean restorePosition(Player player) {
        PlayerPosition position = getLastPosition(player.getUniqueId());
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
        plugin.getConfigManager().debugLog("Position SMP restaurée pour " + player.getName() + " dans " + position.getWorldName());
        return true;
    }
    
    public void saveAllPositions() {
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Toutes les positions SMP ont été sauvegardées.");
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder toutes les positions: " + e.getMessage());
        }
    }
    
    public void removePlayerPositions(UUID playerId) {
        lastSmpPosition.remove(playerId);
        positionsConfig.set("players." + playerId.toString(), null);
        
        try {
            positionsConfig.save(positionsFile);
            plugin.getConfigManager().debugLog("Positions SMP supprimées pour le joueur " + playerId);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de supprimer les positions du joueur: " + e.getMessage());
        }
    }
}
