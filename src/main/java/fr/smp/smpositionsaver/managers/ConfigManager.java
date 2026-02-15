package fr.smp.smpositionsaver.managers;

import java.util.List;
import java.util.logging.Level;

import fr.smp.smpositionsaver.SMPPositionSaver;

public class ConfigManager {
    
    private final SMPPositionSaver plugin;
    
    public ConfigManager(SMPPositionSaver plugin) {
        this.plugin = plugin;
    }
    
    public List<String> getSmpWorlds() {
        return plugin.getConfig().getStringList("smp-worlds");
    }
    
    public boolean isSmpWorld(String worldName) {
        return getSmpWorlds().contains(worldName);
    }
    
    public boolean saveOnQuit() {
        return plugin.getConfig().getBoolean("options.save-on-quit", true);
    }
    
    public boolean restoreOnJoin() {
        return plugin.getConfig().getBoolean("options.restore-on-join", true);
    }
    
    public boolean isDebug() {
        return plugin.getConfig().getBoolean("options.debug", false);
    }
    
    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "Message non trouvé: " + key);
    }
    
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        plugin.getLogger().info(getMessage("config-reloaded"));
    }
    
    public void debugLog(String message) {
        if (isDebug()) {
            plugin.getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
}
