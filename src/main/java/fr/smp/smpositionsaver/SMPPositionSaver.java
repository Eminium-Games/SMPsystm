package fr.smp.smpositionsaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.smp.smpositionsaver.listeners.PlayerWorldChangeListener;
import fr.smp.smpositionsaver.managers.ConfigManager;
import fr.smp.smpositionsaver.managers.PositionManager;

public class SMPPositionSaver extends JavaPlugin {
    
    private ConfigManager configManager;
    private PositionManager positionManager;
    
    @Override
    public void onEnable() {
        // Sauvegarder la configuration par défaut
        saveDefaultConfig();
        
        // Initialiser les managers
        this.configManager = new ConfigManager(this);
        this.positionManager = new PositionManager(this);
        
        // Enregistrer les listeners
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);

        // Create a silent console sender to suppress sendMessage feedback
        org.bukkit.command.CommandSender silentConsole = new SilentCommandSender(getServer().getConsoleSender());

        // Scheduler: run per-player functions
        // Every tick: execute function bracken:player/tick as each player in SMP worlds
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Grouper les joueurs par monde pour minimiser les changements de gamerules
            Map<World, List<Player>> byWorld = new HashMap<>();
            for (Player player : getServer().getOnlinePlayers()) {
                String worldName = player.getWorld().getName();
                if (getConfigManager().isSmpWorld(worldName)) {
                    World w = player.getWorld();
                    byWorld.computeIfAbsent(w, k -> new ArrayList<>()).add(player);
                }
            }

            for (Map.Entry<World, List<Player>> entry : byWorld.entrySet()) {
                World world = entry.getKey();
                List<Player> players = entry.getValue();

                Boolean prevSend = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
                Boolean prevLog = world.getGameRuleValue(GameRule.LOG_ADMIN_COMMANDS);
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);

                for (Player player : players) {
                    Bukkit.dispatchCommand(silentConsole, "execute as " + player.getName() + " run function bracken:player/tick");
                }

                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, prevSend != null ? prevSend : true);
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, prevLog != null ? prevLog : true);
            }
        }, 0L, 1L);

        // Every 10 ticks: apply or remove species attributes for each player
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Grouper par monde afin de désactiver/rétablir les gamerules par monde
            Map<World, List<Player>> byWorld = new HashMap<>();
            for (Player player : getServer().getOnlinePlayers()) {
                World w = player.getWorld();
                byWorld.computeIfAbsent(w, k -> new ArrayList<>()).add(player);
            }

            for (Map.Entry<World, List<Player>> entry : byWorld.entrySet()) {
                World world = entry.getKey();
                List<Player> players = entry.getValue();

                Boolean prevSend = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
                Boolean prevLog = world.getGameRuleValue(GameRule.LOG_ADMIN_COMMANDS);
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);

                boolean isSmp = getConfigManager().isSmpWorld(world.getName());
                for (Player player : players) {
                    if (isSmp) {
                        Bukkit.dispatchCommand(silentConsole, "execute as " + player.getName() + " run function bracken:player/attributes/apply_species");
                    } else {
                        Bukkit.dispatchCommand(silentConsole, "execute as " + player.getName() + " run function bracken:player/attributes/remove_all");
                    }
                }

                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, prevSend != null ? prevSend : true);
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, prevLog != null ? prevLog : true);
            }
        }, 0L, 10L);
        
        getLogger().info("SMPPositionSaver activé avec succès!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("SMPPositionSaver désactivé!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PositionManager getPositionManager() {
        return positionManager;
    }

    // Silent wrapper which delegates permissions and server info but suppresses messages
    private static class SilentCommandSender implements org.bukkit.command.CommandSender {
        private final org.bukkit.command.CommandSender delegate;

        SilentCommandSender(org.bukkit.command.CommandSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public void sendMessage(String message) {
            // suppress
        }

        @Override
        public void sendMessage(String[] messages) {
            // suppress
        }

        @Override
        public void sendMessage(java.util.UUID sender, String message) {
            // suppress
        }

        @Override
        public void sendMessage(java.util.UUID sender, String[] messages) {
            // suppress
        }

        @Override
        public org.bukkit.Server getServer() {
            return delegate.getServer();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isPermissionSet(String name) {
            return delegate.isPermissionSet(name);
        }

        @Override
        public boolean isPermissionSet(org.bukkit.permissions.Permission perm) {
            return delegate.isPermissionSet(perm);
        }

        @Override
        public boolean hasPermission(String name) {
            return delegate.hasPermission(name);
        }

        @Override
        public boolean hasPermission(org.bukkit.permissions.Permission perm) {
            return delegate.hasPermission(perm);
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) {
            return delegate.addAttachment(plugin, name, value);
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) {
            return delegate.addAttachment(plugin);
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
            return delegate.addAttachment(plugin, name, value, ticks);
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) {
            return delegate.addAttachment(plugin, ticks);
        }

        @Override
        public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {
            delegate.removeAttachment(attachment);
        }

        @Override
        public void recalculatePermissions() {
            delegate.recalculatePermissions();
        }

        @Override
        public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() {
            return delegate.getEffectivePermissions();
        }

        @Override
        public boolean isOp() {
            return delegate.isOp();
        }

        @Override
        public void setOp(boolean value) {
            delegate.setOp(value);
        }

        @Override
        public org.bukkit.command.CommandSender.Spigot spigot() {
            return delegate.spigot();
        }
    }
}
