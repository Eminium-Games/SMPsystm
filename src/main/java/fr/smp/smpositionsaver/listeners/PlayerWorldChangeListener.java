package fr.smp.smpositionsaver.listeners;

import fr.smp.smpositionsaver.SMPPositionSaver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerWorldChangeListener implements Listener {
    
    private final SMPPositionSaver plugin;
    
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
                    if (plugin.getConfigManager().isSmpWorld(worldName)) {
                        // Sauvegarder la position actuelle du joueur dans le monde SMP
                        // C'est sa vraie dernière position, 1 seconde avant de potentiellement quitter
                        plugin.getPositionManager().savePosition(player, worldName);
                        plugin.getConfigManager().debugLog("Position surveillée pour " + player.getName() + " dans " + worldName);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Démarrer après 1 seconde, puis toutes les secondes (20 ticks)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        
        plugin.getConfigManager().debugLog(player.getName() + " a rejoint le serveur dans le monde: " + currentWorld);
        
        // Si le joueur rejoint un monde SMP et qu'on doit restaurer la position
        if (plugin.getConfigManager().isSmpWorld(currentWorld) && plugin.getConfigManager().restoreOnJoin()) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                // Restaurer la position après un petit délai pour éviter les conflits
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPositionManager().restorePosition(player)) {
                        String message = plugin.getConfigManager().getMessage("position-restored", "world", currentWorld);
                        player.sendMessage(message);
                    }
                }, 20L); // 1 seconde de délai
            } else {
                String message = plugin.getConfigManager().getMessage("no-position-saved");
                player.sendMessage(message);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String currentWorld = player.getWorld().getName();
        
        plugin.getConfigManager().debugLog(player.getName() + " a quitté le serveur depuis le monde: " + currentWorld);
        
        // Pas de sauvegarde au quit car la position a déjà été sauvegardée
        // 1 seconde avant par la tâche de surveillance continue
        // La sauvegarde contient déjà la vraie dernière position du joueur
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();
        
        plugin.getConfigManager().debugLog(player.getName() + " a changé de monde: " + fromWorld + " -> " + toWorld);
        
        boolean fromIsSmp = plugin.getConfigManager().isSmpWorld(fromWorld);
        boolean toIsSmp = plugin.getConfigManager().isSmpWorld(toWorld);
        
        // Pas de sauvegarde au changement de monde car la position a déjà été sauvegardée
        // 1 seconde avant par la tâche de surveillance continue
        // La sauvegarde contient déjà la vraie dernière position du joueur dans le monde quitté
        
        // Si le joueur entre dans un monde SMP, restaurer sa dernière position SMP si elle existe
        if (toIsSmp) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                // Restaurer après un délai plus long pour éviter les conflits
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPositionManager().restorePosition(player)) {
                        String message = plugin.getConfigManager().getMessage("position-restored", "world", toWorld);
                        player.sendMessage(message);
                        
                        // Vérification finale après la téléportation
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            PlayerPosition lastPos = plugin.getPositionManager().getLastPosition(player.getUniqueId());
                            if (lastPos != null) {
                                Location expectedLoc = new Location(
                                    plugin.getServer().getWorld(lastPos.getWorldName()),
                                    lastPos.getX(),
                                    lastPos.getY(),
                                    lastPos.getZ()
                                );
                                Location currentLoc = player.getLocation();
                                
                                double distance = currentLoc.distance(expectedLoc);
                                if (distance > 5.0) { // Si le joueur est à plus de 5 blocs de la position attendue
                                    plugin.getLogger().warning("Le joueur " + player.getName() + " n'a pas été correctement téléporté. Distance: " + distance);
                                    player.sendMessage("§cLa téléportation a échoué. Veuillez réessayer.");
                                } else {
                                    plugin.getConfigManager().debugLog("Téléportation vérifiée avec succès pour " + player.getName());
                                }
                            }
                        }, 20L); // 1 seconde après la restauration
                    }
                }, 30L); // 1.5 secondes de délai
            } else {
                String message = plugin.getConfigManager().getMessage("no-position-saved");
                player.sendMessage(message);
            }
        }
    }
}
