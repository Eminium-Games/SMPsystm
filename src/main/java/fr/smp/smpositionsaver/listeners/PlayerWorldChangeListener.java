package fr.smp.smpositionsaver.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import fr.smp.smpositionsaver.SMPPositionSaver;

public class PlayerWorldChangeListener implements Listener {
    
    private final SMPPositionSaver plugin;
    // State d'enregistrement : True si le joueur est dans un monde SMP, False sinon
    private final java.util.Map<java.util.UUID, Boolean> recordingState = new java.util.concurrent.ConcurrentHashMap<>();
    // State de téléportation en attente
    private final java.util.Map<java.util.UUID, Boolean> pendingTeleport = new java.util.concurrent.ConcurrentHashMap<>();
    
    public PlayerWorldChangeListener(SMPPositionSaver plugin) {
        this.plugin = plugin;
        startPositionMonitoring();
    }
    
    private void startPositionMonitoring() {
        // Démarrer une tâche qui vérifie toutes les secondes les positions des joueurs dans les mondes SMP
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    String worldName = player.getWorld().getName();
                    boolean isSmp = plugin.getConfigManager().isSmpWorld(worldName);
                    // Mettre à jour le state d'enregistrement
                    recordingState.put(player.getUniqueId(), isSmp);
                    // Ne pas enregistrer si la téléportation est en attente
                    if (isSmp && !pendingTeleport.getOrDefault(player.getUniqueId(), false)) {
                        plugin.getPositionManager().savePosition(player, worldName);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        
        // Si le joueur rejoint un monde SMP et qu'on doit restaurer la position
        if (plugin.getConfigManager().isSmpWorld(currentWorld) && plugin.getConfigManager().restoreOnJoin()) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getPositionManager().restorePosition(player);
                }, 20L);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();
        
        boolean fromIsSmp = plugin.getConfigManager().isSmpWorld(fromWorld);
        boolean toIsSmp = plugin.getConfigManager().isSmpWorld(toWorld);
        boolean wasRecording = recordingState.getOrDefault(player.getUniqueId(), false);
        // Si le joueur arrive dans un monde SMP depuis un monde non-SMP et qu'il n'était pas en train d'enregistrer
        if (toIsSmp && !fromIsSmp && !wasRecording) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                // Marquer la téléportation comme en attente
                pendingTeleport.put(player.getUniqueId(), true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPositionManager().restorePosition(player)) {
                        // Vérification finale après la téléportation
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            fr.smp.smpositionsaver.models.PlayerPosition lastPos = plugin.getPositionManager().getLastPosition(player.getUniqueId());
                            if (lastPos != null) {
                                org.bukkit.Location expectedLoc = new org.bukkit.Location(
                                    plugin.getServer().getWorld(lastPos.getWorldName()),
                                    lastPos.getX(),
                                    lastPos.getY(),
                                    lastPos.getZ()
                                );
                                org.bukkit.Location currentLoc = player.getLocation();
                                double distance = currentLoc.distance(expectedLoc);
                                if (distance <= 5.0) {
                                    // Téléportation terminée, on peut commencer à enregistrer
                                    recordingState.put(player.getUniqueId(), true);
                                    pendingTeleport.put(player.getUniqueId(), false);
                                } else {
                                    pendingTeleport.put(player.getUniqueId(), false);
                                }
                            }
                        }, 20L);
                    } else {
                        // Téléportation échouée, on ne commence pas à enregistrer
                        pendingTeleport.put(player.getUniqueId(), false);
                    }
                }, 20L);
            } else {
                recordingState.put(player.getUniqueId(), true);
                pendingTeleport.put(player.getUniqueId(), false);
            }
        } else if (!toIsSmp) {
            // Si le joueur quitte un monde SMP, on arrête d'enregistrer
            recordingState.put(player.getUniqueId(), false);
            pendingTeleport.put(player.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String worldName = player.getWorld().getName();
        // Only clear saved positions if configured and the death happened in an SMP world
        if (plugin.getConfigManager().clearOnDeath() && plugin.getConfigManager().isSmpWorld(worldName)) {
            plugin.getPositionManager().removePlayerPositions(player.getUniqueId());
        }
    }
}
