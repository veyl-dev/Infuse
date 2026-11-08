package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final InfuseSMP plugin;private final EffectService effects;private final PlayerDataStore store;
    public PlayerListener(InfuseSMP plugin,EffectService effects,PlayerDataStore store){this.plugin=plugin;this.effects=effects;this.store=store;}
    @EventHandler public void onJoin(PlayerJoinEvent event){effects.applyPassive(event.getPlayer());}
    @EventHandler public void onQuit(PlayerQuitEvent event){store.save();}
}
