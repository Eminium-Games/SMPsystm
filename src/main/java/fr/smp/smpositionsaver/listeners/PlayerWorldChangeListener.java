package fr.smp.smpositionsaver.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import fr.smp.smpositionsaver.SMPPositionSaver;

public class PlayerWorldChangeListener implements org.bukkit.event.Listener {

    private final SMPPositionSaver plugin;
    // recording state: true if player is in an SMP world, false otherwise
    private final java.util.Map<java.util.UUID, Boolean> recordingState = new java.util.concurrent.ConcurrentHashMap<>();
    // pending teleport state
    private final java.util.Map<java.util.UUID, Boolean> pendingTeleport = new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerWorldChangeListener(SMPPositionSaver plugin) {
        this.plugin = plugin;
        startPositionMonitoring();
    }

    private void startPositionMonitoring() {
        // start a task that checks player positions every second
        // in SMP worlds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    String worldName = player.getWorld().getName();
                    boolean isSmp = plugin.getConfigManager().isSmpWorld(worldName);
                    // update recording state
                    recordingState.put(player.getUniqueId(), isSmp);
                    // don't save if a teleport is pending
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

        // if the player joins an SMP world and we should restore position
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
        // if player moves to an SMP world from a non-SMP world and
        // was not already recording
        if (toIsSmp && !fromIsSmp && !wasRecording) {
            if (plugin.getPositionManager().hasLastPosition(player.getUniqueId())) {
                // mark teleport as pending
                pendingTeleport.put(player.getUniqueId(), true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPositionManager().restorePosition(player)) {
                        // final check after teleport
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            fr.smp.smpositionsaver.models.PlayerPosition lastPos = plugin.getPositionManager()
                                    .getLastPosition(player.getUniqueId());
                            if (lastPos != null) {
                                org.bukkit.Location expectedLoc = new org.bukkit.Location(
                                        plugin.getServer().getWorld(lastPos.getWorldName()),
                                        lastPos.getX(),
                                        lastPos.getY(),
                                        lastPos.getZ());
                                org.bukkit.Location currentLoc = player.getLocation();
                                double distance = currentLoc.distance(expectedLoc);
                                if (distance <= 5.0) {
                                    // teleport finished, start recording
                                    recordingState.put(player.getUniqueId(), true);
                                    pendingTeleport.put(player.getUniqueId(), false);
                                } else {
                                    pendingTeleport.put(player.getUniqueId(), false);
                                }
                            }
                        }, 20L);
                    } else {
                        // teleport failed, do not start recording
                        pendingTeleport.put(player.getUniqueId(), false);
                    }
                }, 20L);
            } else {
                recordingState.put(player.getUniqueId(), true);
                pendingTeleport.put(player.getUniqueId(), false);
            }
        } else if (!toIsSmp) {
            // if the player leaves an SMP world, stop recording
            recordingState.put(player.getUniqueId(), false);
            pendingTeleport.put(player.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String worldName = player.getWorld().getName();
        // Only clear saved positions if configured and the death happened in an SMP
        // world
        if (plugin.getConfigManager().clearOnDeath() && plugin.getConfigManager().isSmpWorld(worldName)) {
            plugin.getPositionManager().removePlayerPositions(player.getUniqueId());
        }
    }
}
