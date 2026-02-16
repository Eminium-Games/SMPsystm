package fr.smp.smpositionsaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.smp.smpositionsaver.listeners.PlayerWorldChangeListener;
import fr.smp.smpositionsaver.managers.ConfigManager;
import fr.smp.smpositionsaver.managers.PlayerStateManager;
import fr.smp.smpositionsaver.managers.PositionManager;

public class SMPPositionSaver extends JavaPlugin {
    
    private ConfigManager configManager;
    private PositionManager positionManager;
    private PlayerStateManager playerStateManager;
    // Thread-local flag used by the logger filter to suppress specific log lines
    private static final ThreadLocal<Boolean> suppressFunctionLogs = ThreadLocal.withInitial(() -> false);
    private Filter previousRootFilter;
    private Map<Handler, Filter> previousHandlerFilters = new HashMap<>();
    private Map<Logger, Filter> previousLoggerFilters = new HashMap<>();
    
    @Override
    public void onEnable() {
        // Sauvegarder la configuration par défaut
        saveDefaultConfig();
        
        // Initialiser les managers
        this.configManager = new ConfigManager(this);
        this.positionManager = new PositionManager(this);
        
        // Enregistrer les listeners
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);

        // Use the server console sender directly (avoid wrapping it — CraftBukkit requires a vanilla listener)
        org.bukkit.command.CommandSender console = getServer().getConsoleSender();

        // Player state manager: keeps a YAML mapping of player UUID -> boolean (is in SMP)
        this.playerStateManager = new PlayerStateManager(this);

        // Install a temporary root logger filter that will suppress "Running function" messages
        Logger rootLogger = Logger.getLogger("");
        this.previousRootFilter = rootLogger.getFilter();
        final Filter capturedPrev = this.previousRootFilter;
        Filter functionFilter = new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                if (suppressFunctionLogs.get() && record.getMessage() != null && record.getMessage().contains("Running function")) {
                    return false;
                }
                if (capturedPrev != null && capturedPrev != this) {
                    try {
                        return capturedPrev.isLoggable(record);
                    } catch (Throwable t) {
                        return true;
                    }
                }
                return true;
            }
        };
        rootLogger.setFilter(functionFilter);
        // Also apply the same filter to existing handlers to ensure suppression across handlers
        for (Handler h : rootLogger.getHandlers()) {
            previousHandlerFilters.put(h, h.getFilter());
            h.setFilter(functionFilter);
        }
        // Apply filter to all known loggers and their handlers to catch messages from other logger names
        LogManager lm = LogManager.getLogManager();
        java.util.Enumeration<String> names = lm.getLoggerNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            try {
                Logger l = Logger.getLogger(name);
                if (l != null) {
                    previousLoggerFilters.put(l, l.getFilter());
                    l.setFilter(functionFilter);
                    for (Handler h : l.getHandlers()) {
                        if (!previousHandlerFilters.containsKey(h)) {
                            previousHandlerFilters.put(h, h.getFilter());
                            h.setFilter(functionFilter);
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore any issues enumerating loggers
            }
        }

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

                try {
                    suppressFunctionLogs.set(true);
                    for (Player player : players) {
                        Bukkit.dispatchCommand(console, "execute as " + player.getName() + " run function bracken:player/tick");
                    }
                } finally {
                    suppressFunctionLogs.set(false);
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
                try {
                    suppressFunctionLogs.set(true);
                    for (Player player : players) {
                        boolean prev = playerStateManager.getState(player.getUniqueId());
                        if (isSmp && !prev) {
                            // player entered SMP according to world/config and previous state says not in SMP
                            Bukkit.dispatchCommand(console, "execute as " + player.getName() + " run function bracken:player/attributes/apply_species");
                            playerStateManager.setState(player.getUniqueId(), true);
                        } else if (!isSmp && prev) {
                            // player left SMP: only run remove when previous state indicated they were in SMP
                            Bukkit.dispatchCommand(console, "execute as " + player.getName() + " run function bracken:player/attributes/remove_all");
                            playerStateManager.setState(player.getUniqueId(), false);
                        }
                        // if isSmp == prev, do nothing — avoids repeated function runs and log spam
                    }
                } finally {
                    suppressFunctionLogs.set(false);
                }

                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, prevSend != null ? prevSend : true);
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, prevLog != null ? prevLog : true);
            }
        }, 0L, 10L);
        
        getLogger().info("SMPPositionSaver activé avec succès!");
    }
    
    @Override
    public void onDisable() {
        // Restore any previously-set root logger filter
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setFilter(this.previousRootFilter);
        // Restore previous handler filters
        for (Map.Entry<Handler, Filter> e : previousHandlerFilters.entrySet()) {
            try {
                e.getKey().setFilter(e.getValue());
            } catch (Exception ex) {
                // ignore failures during shutdown
            }
        }
        // Restore previous logger filters
        for (Map.Entry<Logger, Filter> e : previousLoggerFilters.entrySet()) {
            try {
                e.getKey().setFilter(e.getValue());
            } catch (Exception ex) {
                // ignore failures during shutdown
            }
        }
        getLogger().info("SMPPositionSaver désactivé!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PositionManager getPositionManager() {
        return positionManager;
    }

    // Silent wrapper which delegates permissions and server info but suppresses messages
    // SilentCommandSender removed: using console sender directly to remain compatible with CraftBukkit.
}
