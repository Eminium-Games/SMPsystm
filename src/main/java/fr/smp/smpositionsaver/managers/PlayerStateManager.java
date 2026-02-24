package fr.smp.smpositionsaver.managers;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Simple manager that stores per-player boolean state (in-SMP or not) in a YAML
 * file.
 */
public class PlayerStateManager {
    private final Plugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public PlayerStateManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_smp_states.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        load();
    }

    private void load() {
        if (!file.exists()) {
            cfg = YamlConfiguration.loadConfiguration(new File(""));
            // start with empty config
            cfg = new YamlConfiguration();
            save();
            return;
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized boolean getState(UUID uuid) {
        String key = uuid.toString();
        if (cfg.contains(key)) {
            return cfg.getBoolean(key, false);
        }
        return false;
    }

    public synchronized void setState(UUID uuid, boolean state) {
        String key = uuid.toString();
        cfg.set(key, state);
        save();
    }

    private synchronized void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save player_smp_states.yml: " + e.getMessage());
        }
    }
}
