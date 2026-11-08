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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Projectile;

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
    @EventHandler public void onQuit(PlayerQuitEvent event){effects.restoreAllTemporaryEnchants(event.getPlayer());store.save();}
    @EventHandler public void onHeldItem(PlayerItemHeldEvent event){plugin.getServer().getScheduler().runTask(plugin,()->effects.applyPassive(event.getPlayer()));}
    @EventHandler public void onInventoryClose(InventoryCloseEvent event){for(var item:event.getView().getTopInventory().getContents())effects.restoreTemporaryEnchants(item);}
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
        var player=event.getPlayer();var data=effects.data(player);
        if(plugin.getConfig().getBoolean("settings.controls.enabled",true)&&"drop".equalsIgnoreCase(data.secondaryControl())&&data.secondary()!=null){
            effects.activate(player,false);event.setCancelled(true);return;
        }
        effects.restoreTemporaryEnchants(event.getItemDrop().getItemStack());
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        var player=event.getEntity();
        effects.restoreAllTemporaryEnchants(player);
        if(event.getKeepInventory())return;
        if(!plugin.getConfig().getBoolean("settings.drop-on-natural-death",true)&&player.getKiller()==null)return;
        if(effects.isEffectBlocked(player))return;
        var data=effects.data(player);
        String mode=plugin.getConfig().getString("settings.effect-drop-mode","random").toLowerCase(java.util.Locale.ROOT);
        if(mode.equals("none"))return;
        String slot;
        if(mode.equals("only_primary"))slot="primary";
        else if(mode.equals("only_secondary"))slot="secondary";
        else if(mode.equals("prefer_primary"))slot=data.primary()!=null?"primary":"secondary";
        else if(mode.equals("prefer_secondary"))slot=data.secondary()!=null?"secondary":"primary";
        else if(data.primary()==null)slot="secondary";
        else if(data.secondary()==null)slot="primary";
        else slot=java.util.concurrent.ThreadLocalRandom.current().nextBoolean()?"primary":"secondary";
        String id=slot.equals("primary")?data.primary():data.secondary();
        if(id==null)return;
        var effect=dev.veyl.infusesmp.effect.InfuseEffect.fromId(id);if(effect==null)return;
        boolean augmented=slot.equals("primary")?data.primaryAugmented():data.secondaryAugmented();
        event.getDrops().add(effects.createEffectItem(effect,augmented));
        effects.remove(player,slot);
    }
    @EventHandler(ignoreCancelled=true)
    public void onExperienceGain(PlayerExpChangeEvent event){
        var player=event.getPlayer();if(effects.isEmeraldLocked(player)){event.setAmount(0);return;}if(!effects.hasEffect(player,"emerald"))return;
        double multiplier=effects.isSparkActive(player,"emerald")
                ?plugin.getConfig().getDouble("effect-settings.emerald.spark-xp-multiplier",4.0)
                :plugin.getConfig().getDouble("effect-settings.emerald.xp-multiplier",2.0);
        event.setAmount((int)Math.round(event.getAmount()*multiplier));
    }
    @EventHandler(ignoreCancelled=true)
    public void onFallDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof org.bukkit.entity.Player player))return;
        if(event.getCause()!=EntityDamageEvent.DamageCause.FALL)return;
        if(effects.hasEffect(player,"feather"))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void onFoodChange(FoodLevelChangeEvent event){
        if(event.getEntity() instanceof org.bukkit.entity.Player player){if(effects.isEmeraldLocked(player)){event.setCancelled(true);return;}if(effects.hasEffect(player,"regeneration"))event.setFoodLevel(20);}
    }
    @EventHandler(ignoreCancelled=true)
    public void onConsumePassives(PlayerItemConsumeEvent event){
        var player=event.getPlayer();
        if(effects.hasEffect(player,"heart")){
            if(event.getItem().getType()==org.bukkit.Material.ENCHANTED_GOLDEN_APPLE)
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION,2400,4));
            else player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION,600,0));
        }
        if(effects.hasEffect(player,"regeneration")){
            plugin.getServer().getScheduler().runTask(plugin,()->player.setSaturation(Math.min(20f,player.getSaturation()+6f)));
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onFireBow(EntityShootBowEvent event){
        if(!(event.getEntity() instanceof org.bukkit.entity.Player player))return;
        if(!effects.hasEffect(player,"fire"))return;
        if(event.getForce()>=1&&event.getProjectile() instanceof Projectile projectile)projectile.setFireTicks(100);
    }
    @EventHandler(ignoreCancelled=true)
    public void onMovePassives(PlayerMoveEvent event){
        var player=event.getPlayer();
        if(event.getTo()==null||event.getFrom().getBlockX()==event.getTo().getBlockX()&&event.getFrom().getBlockY()==event.getTo().getBlockY()&&event.getFrom().getBlockZ()==event.getTo().getBlockZ())return;
        if(effects.hasEffect(player,"fire")&&player.isInLava()){
            var direction=player.getLocation().getDirection().normalize().multiply(plugin.getConfig().getDouble("effect-settings.fire.lava-walk-speed",.6));
            player.setVelocity(direction);
        }
        if(effects.hasEffect(player,"feather")&&player.isOnGround()&&player.getFallDistance()>3){
            double radius=plugin.getConfig().getDouble("effect-settings.feather.land-radius",4);
            for(var entity:player.getNearbyEntities(radius,radius,radius)){
                if(entity instanceof org.bukkit.entity.LivingEntity target&&target!=player
                        &&!(target instanceof org.bukkit.entity.Player other&&effects.data(player).trusted().contains(other.getUniqueId()))){
                    target.damage(plugin.getConfig().getDouble("effect-settings.feather.land-damage",8),player);
                    target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0,1,0)));
                }
            }
            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,player.getLocation(),35,.5,.2,.5,.1);
            player.getWorld().playSound(player.getLocation(),org.bukkit.Sound.ITEM_MACE_SMASH_GROUND_HEAVY,1f,1f);
            player.setFallDistance(0);
        }
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
        plugin.messages().send(player,"effect-added",java.util.Map.of("effect",effect.displayName(),"slot",slot));
    }
}
