package dev.veyl.infusesmp.listener;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerDataStore;
import dev.veyl.infusesmp.effect.EffectService;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.Trident;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.DragonFireball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class PlayerListener implements Listener {
    private final InfuseSMP plugin;
    private final EffectService effects;
    private final PlayerDataStore store;
    private final java.util.Map<java.util.UUID,Long> frostTrailCooldown=new java.util.HashMap<>();
    public PlayerListener(InfuseSMP plugin,EffectService effects,PlayerDataStore store){
        this.plugin=plugin;this.effects=effects;this.store=store;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event){
        var player=event.getPlayer();
        if(plugin.getConfig().getBoolean("settings.roll-first-join",false)){
            var data=effects.data(player);
            if(data.primary()==null&&data.secondary()==null){
                var pool=java.util.Arrays.stream(dev.veyl.infusesmp.effect.InfuseEffect.values()).toList();
                if(!pool.isEmpty()){
                    var selected=pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
                    effects.equip(player,"primary",selected,false);
                    player.sendMessage("§aYour first Infuse effect is "+selected.displayName()+"!");
                }
            }
        }
        effects.applyPassive(player);
        if(plugin.getConfig().getBoolean("settings.resource-pack.auto-download",true)
                &&plugin.resourcePackInfo()!=null){
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || plugin.resourcePackInfo() == null) return;
                var request=ResourcePackRequest.resourcePackRequest()
                        .packs(plugin.resourcePackInfo())
                        .required(true)
                        .prompt(Component.text("InfuseSMP S3 requires its resource pack for the original effect textures and icons."))
                        .build();
                player.sendResourcePacks(request);
                plugin.getLogger().info("Sent the S3 resource pack request to " + player.getName() + ".");
            }, 40L);
        }
    }
    @EventHandler
    public void onResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent event) {
        String status = event.getStatus().name();
        if (status.equals("SUCCESSFULLY_LOADED")) {
            plugin.getLogger().info(event.getPlayer().getName() + " loaded the InfuseSMP S3 resource pack.");
        } else {
            plugin.getLogger().warning(event.getPlayer().getName() + " resource-pack status: " + status + ".");
            if (status.equals("FAILED_DOWNLOAD") || status.equals("INVALID_URL") || status.equals("FAILED_RELOAD")) {
                event.getPlayer().sendMessage("§cInfuseSMP S3 resource pack failed to load. Please reconnect or contact a server administrator.");
            }
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event){effects.restoreAllTemporaryEnchants(event.getPlayer());store.save();}
    @EventHandler public void onHeldItem(PlayerItemHeldEvent event){plugin.getServer().getScheduler().runTask(plugin,()->effects.applyPassive(event.getPlayer()));}
    @EventHandler public void onInventoryClose(InventoryCloseEvent event){for(var item:event.getView().getTopInventory().getContents())effects.restoreTemporaryEnchants(item);}
    @EventHandler(ignoreCancelled=true)
    public void onSwapControl(PlayerSwapHandItemsEvent event){
        if(!plugin.getConfig().getBoolean("settings.controls.enabled",true))return;
        var player=event.getPlayer();if(effects.isEffectBlocked(player))return;
        var data=effects.data(player);
        if("swap".equalsIgnoreCase(data.primaryControl())&&data.primary()!=null){
            effects.activate(player,true);event.setCancelled(true);
        }else if("swap".equalsIgnoreCase(data.secondaryControl())&&data.secondary()!=null){
            effects.activate(player,false);event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onDropControl(PlayerDropItemEvent event){
        var player=event.getPlayer();var data=effects.data(player);
        if(plugin.getConfig().getBoolean("settings.controls.enabled",true)&&!effects.isEffectBlocked(player)){
            if("drop".equalsIgnoreCase(data.primaryControl())&&data.primary()!=null){
                effects.activate(player,true);event.setCancelled(true);return;
            }
            if("drop".equalsIgnoreCase(data.secondaryControl())&&data.secondary()!=null){
                effects.activate(player,false);event.setCancelled(true);return;
            }
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
        if(slot.equals("primary"))data.primary(null,false);else data.secondary(null,false);
        data.addHistory("death-drop:"+slot+":"+id);
    }
    @EventHandler(ignoreCancelled=true)
    public void onExperienceGain(PlayerExpChangeEvent event){
        var player=event.getPlayer();if(effects.isEmeraldLocked(player)){event.setAmount(0);return;}if(!effects.hasEffect(player,"emerald")||effects.isEffectBlocked(player))return;
        double multiplier=effects.isSparkActive(player,"emerald")
                ?plugin.getConfig().getDouble("effect-settings.emerald.spark-xp-multiplier",4.0)
                :plugin.getConfig().getDouble("effect-settings.emerald.xp-multiplier",2.0);
        event.setAmount((int)Math.round(event.getAmount()*multiplier));
    }
    private void createSnowTrail(org.bukkit.entity.Player player){
        long now=System.currentTimeMillis();
        if(now-frostTrailCooldown.getOrDefault(player.getUniqueId(),0L)<500L)return;
        frostTrailCooldown.put(player.getUniqueId(),now);
        int radius=Math.max(1,Math.min(5,plugin.getConfig().getInt("effect-settings.frost.snow-changing-radius",3)));
        int duration=Math.max(1,plugin.getConfig().getInt("effect-settings.frost.snow-duration-seconds",5));
        Block center=player.getLocation().getBlock();
        java.util.List<Block> placed=new java.util.ArrayList<>();
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius)continue;
            Block ground=center.getRelative(x,-1,z);
            Block snow=ground.getRelative(BlockFace.UP);
            if(!ground.getType().isSolid()||snow.getType()!=org.bukkit.Material.AIR)continue;
            snow.setType(org.bukkit.Material.SNOW,false);placed.add(snow);
        }
        if(!placed.isEmpty())plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            for(Block snow:placed)if(snow.getType()==org.bukkit.Material.SNOW)snow.setType(org.bukkit.Material.AIR,false);
        },duration*20L);
    }
    @EventHandler(ignoreCancelled=true)
    public void onFallDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof org.bukkit.entity.Player player))return;
        if(event.getCause()!=EntityDamageEvent.DamageCause.FALL)return;
        if(effects.hasEffect(player,"feather")&&!effects.isEffectBlocked(player))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void onFoodChange(FoodLevelChangeEvent event){
        if(event.getEntity() instanceof org.bukkit.entity.Player player){if(effects.isEmeraldLocked(player)){event.setCancelled(true);return;}if(effects.hasEffect(player,"regeneration")&&!effects.isEffectBlocked(player))event.setFoodLevel(20);}
    }
    @EventHandler(ignoreCancelled=true)
    public void onConsumePassives(PlayerItemConsumeEvent event){
        var player=event.getPlayer();
        if(effects.hasEffect(player,"heart")&&!effects.isEffectBlocked(player)){
            if(event.getItem().getType()==org.bukkit.Material.ENCHANTED_GOLDEN_APPLE)
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION,2400,4));
            else player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION,600,0));
        }
        if(effects.hasEffect(player,"regeneration")&&!effects.isEffectBlocked(player)){
            plugin.getServer().getScheduler().runTask(plugin,()->player.setSaturation(Math.min(20f,player.getSaturation()+6f)));
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onSpecialProjectileUse(PlayerInteractEvent event){
        if(event.getAction()!=Action.RIGHT_CLICK_AIR&&event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        var player=event.getPlayer();var item=event.getItem();if(item==null||effects.isEffectBlocked(player))return;
        if(effects.hasEffect(player,"ender")&&item.getType()==org.bukkit.Material.DRAGON_BREATH&&!player.hasCooldown(org.bukkit.Material.DRAGON_BREATH)){
            DragonFireball fireball=player.launchProjectile(DragonFireball.class);
            fireball.setIsIncendiary(false);fireball.setYield(0f);fireball.setVelocity(fireball.getVelocity().multiply(2.0));
            item.setAmount(item.getAmount()-1);player.setCooldown(org.bukkit.Material.DRAGON_BREATH,600);
            event.setCancelled(true);
        }
        if(effects.hasEffect(player,"feather")&&item.getType()==org.bukkit.Material.WIND_CHARGE&&!player.hasCooldown(org.bukkit.Material.WIND_CHARGE)){
            player.setCooldown(org.bukkit.Material.WIND_CHARGE,5);
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onSpecialProjectileLaunch(ProjectileLaunchEvent event){
        if(event.getEntity() instanceof WindCharge charge&&charge.getShooter() instanceof org.bukkit.entity.Player player
                &&effects.hasEffect(player,"feather")&&!effects.isEffectBlocked(player)){
            charge.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(2.0));
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onSpecialProjectileHit(ProjectileHitEvent event){
        if(event.getEntity() instanceof Trident trident&&trident.getShooter() instanceof org.bukkit.entity.Player player
                &&effects.hasEffect(player,"thunder")&&!effects.isEffectBlocked(player)
                &&event.getHitEntity() instanceof org.bukkit.entity.LivingEntity target&&!effects.isEffectBlocked(target.getLocation())){
            target.getWorld().strikeLightningEffect(target.getLocation());effects.thunderDamage(target,player,2.0);
        }
        if(event.getEntity() instanceof DragonFireball fireball&&fireball.getShooter() instanceof org.bukkit.entity.Player player
                &&effects.hasEffect(player,"ender")&&event.getHitEntity() instanceof org.bukkit.entity.LivingEntity target
                &&!effects.isEffectBlocked(target.getLocation())){
            target.damage(6.0,player);
            if(target instanceof org.bukkit.entity.Player cursed)effects.applyEnderCurse(cursed,player);
            else target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER,100,1));
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onFireBow(EntityShootBowEvent event){
        if(!(event.getEntity() instanceof org.bukkit.entity.Player player))return;
        if(!effects.hasEffect(player,"fire")||effects.isEffectBlocked(player))return;
        if(event.getForce()>=1&&event.getProjectile() instanceof Projectile projectile)projectile.setFireTicks(100);
    }
    @EventHandler(ignoreCancelled=true)
    public void onMovePassives(PlayerMoveEvent event){
        var player=event.getPlayer();
        if(event.getTo()==null||event.getFrom().getBlockX()==event.getTo().getBlockX()&&event.getFrom().getBlockY()==event.getTo().getBlockY()&&event.getFrom().getBlockZ()==event.getTo().getBlockZ())return;
        if(effects.hasEffect(player,"frost")&&!effects.isEffectBlocked(player))createSnowTrail(player);
        if(effects.hasEffect(player,"fire")&&!effects.isEffectBlocked(player)&&player.isInLava()){
            var direction=player.getLocation().getDirection().normalize().multiply(plugin.getConfig().getDouble("effect-settings.fire.lava-walk-speed",.6));
            player.setVelocity(direction);
        }
        if(effects.hasEffect(player,"feather")&&effects.isSparkActive(player,"feather")&&!effects.isEffectBlocked(player)&&player.isOnGround()&&player.getFallDistance()>3){
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
        if(!effects.consumeEffectItem(event.getPlayer(),event.getItem()))event.setCancelled(true);
    }
}
