package fr.smp.smpositionsaver.listeners;

import fr.smp.smpositionsaver.SMPPositionSaver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerWorldChangeListener implements Listener {
    
    private final SMPPositionSaver plugin;
    
    public PlayerWorldChangeListener(SMPPositionSaver plugin) {
        this.plugin = plugin;
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
        
        // Si le joueur quitte un monde SMP et qu'on doit sauvegarder la position
        if (plugin.getConfigManager().isSmpWorld(currentWorld) && plugin.getConfigManager().saveOnQuit()) {
            plugin.getPositionManager().savePosition(player, currentWorld);
            String message = plugin.getConfigManager().getMessage("position-saved", "world", currentWorld);
            player.sendMessage(message);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();
        
        plugin.getConfigManager().debugLog(player.getName() + " a changé de monde: " + fromWorld + " -> " + toWorld);
        
        boolean fromIsSmp = plugin.getConfigManager().isSmpWorld(fromWorld);
        boolean toIsSmp = plugin.getConfigManager().isSmpWorld(toWorld);
        
        // Si le joueur quitte un monde SMP, sauvegarder sa position dans le monde quitté
        if (fromIsSmp) {
            // Créer une location dans le monde quitté avec les coordonnées actuelles du joueur
            Location fromLocation = new Location(
                event.getFrom(), 
                player.getLocation().getX(), 
                player.getLocation().getY(), 
                player.getLocation().getZ(), 
                player.getLocation().getYaw(), 
                player.getLocation().getPitch()
            );
            
            plugin.getPositionManager().savePosition(player, fromLocation, fromWorld);
            String message = plugin.getConfigManager().getMessage("position-saved", "world", fromWorld);
            player.sendMessage(message);
        }
        
        // Si le joueur entre dans un monde SMP, restaurer sa dernière position SMP si elle existe
        if (toIsSmp) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                // Restaurer après un petit délai pour éviter les conflits
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPositionManager().restorePosition(player)) {
                        String message = plugin.getConfigManager().getMessage("position-restored", "world", toWorld);
                        player.sendMessage(message);
                    }
                }, 10L); // 0.5 seconde de délai
            } else {
                String message = plugin.getConfigManager().getMessage("no-position-saved");
                player.sendMessage(message);
            }
        }
    }
}
