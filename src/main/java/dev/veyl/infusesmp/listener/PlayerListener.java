package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final InfuseSMP plugin;
    private final EffectService effects;
    private final PlayerDataStore store;
    public PlayerListener(InfuseSMP plugin,EffectService effects,PlayerDataStore store){
        this.plugin=plugin;this.effects=effects;this.store=store;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event){
        effects.applyPassive(event.getPlayer());
        if(plugin.getConfig().getBoolean("settings.resource-pack.enabled",false)){
            String url=plugin.getConfig().getString("settings.resource-pack.url","");
            String hash=plugin.getConfig().getString("settings.resource-pack.sha1","");
            if(url!=null&&!url.isBlank()){
                if(hash!=null&&hash.matches("(?i)[0-9a-f]{40}"))event.getPlayer().setResourcePack(url,hash);
                else event.getPlayer().setResourcePack(url);
            }
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event){store.save();}
    @EventHandler(ignoreCancelled=true)
    public void onEffectDrink(PlayerItemConsumeEvent event){
        if(!effects.isEffectItem(event.getItem()))return;
        if(!effects.consumeEffectItem(event.getPlayer(),event.getItem()))event.setCancelled(true);
    }
}
