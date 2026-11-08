package dev.veyl.infusesmp;

import dev.veyl.infusesmp.command.InfuseCommand;
import dev.veyl.infusesmp.crafting.RecipeService;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.listener.CombatListener;
import dev.veyl.infusesmp.listener.PlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class InfuseSMP extends JavaPlugin {
    private PlayerDataStore dataStore;
    private EffectService effectService;
    private RecipeService recipeService;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        extractBundledResourcePack();
        dataStore = new PlayerDataStore(this);
        dataStore.load();
        effectService = new EffectService(this, dataStore);
        recipeService = new RecipeService(this, effectService);
        getServer().getPluginManager().registerEvents(recipeService, this);
        InfuseCommand executor = new InfuseCommand(this, effectService);
        for (String name : new String[]{"infuse", "activate", "trust", "swap", "recipes"}) {
            PluginCommand command = getCommand(name);
            if (command != null) { command.setExecutor(executor); command.setTabCompleter(executor); }
        }
        getServer().getPluginManager().registerEvents(new CombatListener(effectService), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, effectService, dataStore), this);
        long interval = Math.max(20L, getConfig().getLong("settings.autosave-seconds", 60L) * 20L);
        getServer().getScheduler().runTaskTimer(this, dataStore::save, interval, interval);
        getServer().getScheduler().runTaskTimer(this, effectService::refreshPassives, 20L, 20L);
        getLogger().info("InfuseSMP S3 enabled.");
    }
    @Override public void onDisable() {
        if (dataStore != null) dataStore.save();
        if (recipeService != null) recipeService.save();
    }
    private void extractBundledResourcePack() {
        Path output = getDataFolder().toPath().resolve("resourcepack/InfuseSMP-S3-Original.zip");
        if (Files.exists(output)) return;
        try (InputStream input = getResource("resourcepack/InfuseSMP-S3-Original.zip.b64")) {
            if (input == null) {
                getLogger().warning("Bundled resource pack was not found in the plugin jar.");
                return;
            }
            Files.createDirectories(output.getParent());
            Files.write(output, Base64.getDecoder().decode(input.readAllBytes()));
            getLogger().info("Extracted original S3 resource pack to " + output);
        } catch (Exception ex) {
            getLogger().severe("Could not extract bundled resource pack: " + ex.getMessage());
        }
    }

    private void saveResourceIfMissing(String name) {
        if (!getDataFolder().toPath().resolve(name).toFile().exists()) saveResource(name, false);
    }
    public EffectService effects() { return effectService; }
    public PlayerDataStore data() { return dataStore; }
}
