package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.effect.EffectService;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class CombatListener implements Listener {
    private final EffectService effects;
    public CombatListener(EffectService effects){this.effects=effects;}
    @EventHandler(ignoreCancelled=true)
    public void onPlayerDamage(EntityDamageByEntityEvent event){
        if(!(event.getDamager() instanceof Player attacker)||!(event.getEntity() instanceof LivingEntity victim))return;
        if(victim instanceof Player defender&&effects.hasEffect(attacker,"strength")
                &&defender.isBlocking()&&attacker.getInventory().getItemInMainHand().getType().name().endsWith("_AXE")){
            event.setCancelled(true);
            defender.setCooldown(Material.SHIELD,200);
            defender.clearActiveItem();
            defender.damage(event.getDamage()/2.0,attacker);
            defender.getWorld().playSound(defender.getLocation(),org.bukkit.Sound.ITEM_SHIELD_BREAK,1f,1f);
            return;
        }
        effects.onCombatHit(attacker,victim,event);
    }
}
