package fr.smp.smpositionsaver;

import fr.smp.smpositionsaver.listeners.PlayerWorldChangeListener;
import fr.smp.smpositionsaver.managers.ConfigManager;
import fr.smp.smpositionsaver.managers.PositionManager;
import org.bukkit.plugin.java.JavaPlugin;

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
}
