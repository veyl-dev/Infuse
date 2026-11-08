package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

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
    public void onSwapControl(PlayerSwapHandItemsEvent event){
        if(!plugin.getConfig().getBoolean("settings.controls.enabled",true))return;
        var player=event.getPlayer();var data=effects.data(player);
        if(!"swap".equalsIgnoreCase(data.primaryControl()))return;
        if(data.primary()==null)return;
        effects.activate(player,true);event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void onDropControl(PlayerDropItemEvent event){
        if(!plugin.getConfig().getBoolean("settings.controls.enabled",true))return;
        var player=event.getPlayer();var data=effects.data(player);
        if(!"drop".equalsIgnoreCase(data.secondaryControl()))return;
        if(data.secondary()==null)return;
        effects.activate(player,false);event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void onEffectDrink(PlayerItemConsumeEvent event){
        if(!effects.isEffectItem(event.getItem()))return;
        var player=event.getPlayer();
        var effect=effects.effectFromItem(event.getItem());
        if(effect==null){event.setCancelled(true);return;}
        var data=effects.data(player);
        String slot=data.primary()==null?"primary":data.secondary()==null?"secondary":null;
        if(slot==null){player.sendMessage(org.bukkit.ChatColor.RED+"Both Infuse slots are occupied. Drain or remove an effect first.");event.setCancelled(true);return;}
        effects.equip(player,slot,effect,effects.isAugmentedItem(event.getItem()));
        player.sendMessage(org.bukkit.ChatColor.GREEN+effect.displayName()+" equipped in your "+slot+" slot.");
    }
}
