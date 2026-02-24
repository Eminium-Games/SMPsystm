package fr.smp.smpositionsaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
    // (no logger filters) — we won't modify server/handler filters anymore

    @Override
    public void onEnable() {
        // save the default configuration
        saveDefaultConfig();

        // initialize managers
        this.configManager = new ConfigManager(this);
        this.positionManager = new PositionManager(this);

        // register listeners
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);

        // Use the server console sender directly (avoid wrapping it — CraftBukkit
        // requires a vanilla listener)
        org.bukkit.command.CommandSender console = getServer().getConsoleSender();

        // Player state manager: keeps a YAML mapping of player UUID -> boolean (is in
        // SMP)
        this.playerStateManager = new PlayerStateManager(this);

        // Every 10 ticks: apply or remove species attributes for each player
        getServer().getScheduler().runTaskTimer(this, () -> {
            // group by world so we can toggle gamerules per-world
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
                String entryFunc = getConfigManager().getEntryFunction();
                String exitFunc = getConfigManager().getExitFunction();
                for (Player player : players) {
                    boolean prev = playerStateManager.getState(player.getUniqueId());
                    if (isSmp && !prev) {
                        // player entered SMP according to world/config and previous state says not in
                        // SMP
                        if (configManager.isDebug()) {
                            configManager.debugLog("entering SMP: running function " + entryFunc +
                                    " for " + player.getName());
                        }
                        Bukkit.dispatchCommand(console, "execute as " + player.getName()
                                + " run function " + entryFunc);
                        playerStateManager.setState(player.getUniqueId(), true);
                    } else if (!isSmp && prev) {
                        // player left SMP: only run remove when previous state indicated they were in
                        // SMP
                        if (configManager.isDebug()) {
                            configManager.debugLog("leaving SMP: running function " + exitFunc +
                                    " for " + player.getName());
                        }
                        Bukkit.dispatchCommand(console, "execute as " + player.getName()
                                + " run function " + exitFunc);
                        playerStateManager.setState(player.getUniqueId(), false);
                    }
                    // if isSmp == prev, do nothing — avoids repeated function runs and log spam
                }
            }
        }, 0L, 10L);

        // Register the /smp command (teleport + reload subcommand)
        getCommand("smp").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("smp.reload") || !(sender instanceof Player)) {
                        configManager.reloadConfig();
                        sender.sendMessage(configManager.getMessage("config-reloaded"));
                    } else {
                        sender.sendMessage("§cPermission denied.");
                    }
                    return true;
                }

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    double x = getConfig().getDouble("smp-coordinates.x", 0);
                    double y = getConfig().getDouble("smp-coordinates.y", 64);
                    double z = getConfig().getDouble("smp-coordinates.z", 0);
                    String worldName = getConfig().getString("smp-coordinates.world", "world");
                    World world = Bukkit.getWorld(worldName);

                    if (world != null) {
                        org.bukkit.Location location = new org.bukkit.Location(world, x, y, z);
                        player.teleport(location);
                    } else {
                        player.sendMessage("§cThe specified world does not exist.");
                    }
                } else {
                    sender.sendMessage("§cOnly players can execute this command.");
                }
                return true;
            }
        });

        getLogger().info("SMPPositionSaver enabled successfully!");
    }

    @Override
    public void onDisable() {
        // No logger filters to restore.
        getLogger().info("SMPPositionSaver disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PositionManager getPositionManager() {
        return positionManager;
    }

    // Silent wrapper which delegates permissions and server info but suppresses
    // messages
    // SilentCommandSender removed: using console sender directly to remain
    // compatible with CraftBukkit.
}
