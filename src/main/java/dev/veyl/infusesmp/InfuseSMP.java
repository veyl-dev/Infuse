package dev.veyl.infusesmp;

import dev.veyl.infusesmp.command.InfuseCommand;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.listener.CombatListener;
import dev.veyl.infusesmp.listener.PlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfuseSMP extends JavaPlugin {
    private PlayerDataStore dataStore;
    private EffectService effectService;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        dataStore = new PlayerDataStore(this);
        dataStore.load();
        effectService = new EffectService(this, dataStore);
        InfuseCommand executor = new InfuseCommand(this, effectService);
        for (String name : new String[]{"infuse", "activate", "trust", "swap", "recipes"}) {
            PluginCommand command = getCommand(name);
            if (command != null) { command.setExecutor(executor); command.setTabCompleter(executor); }
        }
        getServer().getPluginManager().registerEvents(new CombatListener(effectService), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, effectService, dataStore), this);
        long interval = Math.max(20L, getConfig().getLong("settings.autosave-seconds", 60L) * 20L);
        getServer().getScheduler().runTaskTimer(this, dataStore::save, interval, interval);
        getLogger().info("InfuseSMP S3 enabled.");
    }
    @Override public void onDisable() { if (dataStore != null) dataStore.save(); }
    private void saveResourceIfMissing(String name) {
        if (!getDataFolder().toPath().resolve(name).toFile().exists()) saveResource(name, false);
    }
    public EffectService effects() { return effectService; }
    public PlayerDataStore data() { return dataStore; }
}
