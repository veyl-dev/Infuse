package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.effect.EffectService;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {
    private final EffectService effects;
    public CombatListener(EffectService effects){this.effects=effects;}
    @EventHandler(ignoreCancelled=true)
    public void onPlayerDamage(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim)effects.onCombatHit(attacker,victim,event);
    }
}
