package dev.veyl.infusesmp;

import dev.veyl.infusesmp.command.InfuseCommand;
import dev.veyl.infusesmp.crafting.RecipeService;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.listener.CombatListener;
import dev.veyl.infusesmp.listener.PlayerListener;
import dev.veyl.infusesmp.ritual.RitualService;
import dev.veyl.infusesmp.menu.MenuService;
import dev.veyl.infusesmp.message.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import dev.veyl.infusesmp.resourcepack.ResourcePackAdapter;
import net.kyori.adventure.resource.ResourcePackInfo;

public final class InfuseSMP extends JavaPlugin {
    private PlayerDataStore dataStore;
    private EffectService effectService;
    private RecipeService recipeService;
    private RitualService ritualService;
    private MenuService menuService;
    private MessageService messageService;
    private ResourcePackInfo resourcePackInfo;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("recipes.yml");
        messageService = new MessageService(this);
        try {
            ResourcePackAdapter.extract(this);
            resourcePackInfo = ResourcePackAdapter.createResourcePackInfo();
            getLogger().info("Prepared automatic S3 resource-pack delivery for " + getServer().getBukkitVersion() + ".");
        } catch (Exception ex) {
            getLogger().severe("Could not prepare automatic resource-pack delivery: " + ex.getMessage());
        }
        dataStore = new PlayerDataStore(this);
        dataStore.load();
        effectService = new EffectService(this, dataStore);
        ritualService = new RitualService(this, effectService);
        getServer().getPluginManager().registerEvents(ritualService, this);
        recipeService = new RecipeService(this, effectService, ritualService);
        getServer().getPluginManager().registerEvents(recipeService, this);
        menuService = new MenuService(this, effectService, recipeService);
        getServer().getPluginManager().registerEvents(menuService, this);
        InfuseCommand executor = new InfuseCommand(this, effectService);
        for (String name : new String[]{"infuse", "activate", "trust", "swap", "recipes", "drain"}) {
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
        if (ritualService != null) ritualService.stop();
    }
    private void saveResourceIfMissing(String name) {
        if (!getDataFolder().toPath().resolve(name).toFile().exists()) saveResource(name, false);
    }
    public EffectService effects() { return effectService; }
    public MenuService menus() { return menuService; }
    public MessageService messages() { return messageService; }
    public RecipeService recipes() { return recipeService; }
    public PlayerDataStore data() { return dataStore; }
    public ResourcePackInfo resourcePackInfo() { return resourcePackInfo; }
}
