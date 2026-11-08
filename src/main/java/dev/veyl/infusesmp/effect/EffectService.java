package dev.veyl.infusesmp.effect;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerData;
import dev.veyl.infusesmp.data.PlayerDataStore;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.List;

public final class EffectService {
    private final InfuseSMP plugin;
    private final PlayerDataStore store;
    private final NamespacedKey itemKey;
    private final NamespacedKey hasteFortuneKey,hasteEfficiencyKey,hasteUnbreakingKey,emeraldLootingKey;
    public EffectService(InfuseSMP plugin,PlayerDataStore store){
        this.plugin=plugin;this.store=store;this.itemKey=new NamespacedKey(plugin,"infuse_effect");
        this.hasteFortuneKey=new NamespacedKey(plugin,"haste_original_fortune");this.hasteEfficiencyKey=new NamespacedKey(plugin,"haste_original_efficiency");
        this.hasteUnbreakingKey=new NamespacedKey(plugin,"haste_original_unbreaking");this.emeraldLootingKey=new NamespacedKey(plugin,"emerald_original_looting");
    }
    public PlayerData data(Player player){return store.get(player.getUniqueId());}
    public int effectCount(){return InfuseEffect.values().length;}
    public boolean hasEffect(Player player,String id){PlayerData d=data(player);return id.equals(d.primary())||id.equals(d.secondary());}
    public boolean isEmeraldLocked(Player player){return data(player).cooldowns().getOrDefault("emerald-lock",0L)>System.currentTimeMillis();}
    public boolean isEffectBlocked(Player player){return isEffectBlocked(player.getLocation());}
    public boolean isEffectBlocked(org.bukkit.Location location){
        if(plugin.getConfig().getStringList("settings.disabled-worlds").contains(location.getWorld().getName()))return true;
        ConfigurationSection regions=plugin.getConfig().getConfigurationSection("regions.disabled");
        if(regions==null)return false;
        for(String name:regions.getKeys(false)){
            ConfigurationSection region=regions.getConfigurationSection(name);if(region==null)continue;
            if(!location.getWorld().getName().equals(region.getString("world","")))continue;
            double minX=region.getDouble("min.x"),minY=region.getDouble("min.y"),minZ=region.getDouble("min.z");
            double maxX=region.getDouble("max.x"),maxY=region.getDouble("max.y"),maxZ=region.getDouble("max.z");
            if(location.getX()>=Math.min(minX,maxX)&&location.getX()<=Math.max(minX,maxX)
                    &&location.getY()>=Math.min(minY,maxY)&&location.getY()<=Math.max(minY,maxY)
                    &&location.getZ()>=Math.min(minZ,maxZ)&&location.getZ()<=Math.max(minZ,maxZ))return true;
        }
        return false;
    }
    public ItemStack createEffectItem(InfuseEffect effect,boolean augmented){
        ItemStack item=new ItemStack(Material.POTION);PotionMeta meta=(PotionMeta)item.getItemMeta();
        if(meta!=null){
            meta.setDisplayName((augmented?ChatColor.LIGHT_PURPLE:ChatColor.GREEN)+effect.displayName()+" Effect");
            meta.setColor(effect.color());
            meta.setCustomModelData(effect.modelData()+(augmented?100:0));
            meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,effect.id()+(augmented?":augmented":""));
            meta.setLore(List.of(ChatColor.GRAY+"Infuse SMP Season 3",
                    ChatColor.DARK_GRAY+"Drink to equip in your next available slot",
                    augmented?ChatColor.LIGHT_PURPLE+"Augmented Effect":ChatColor.GREEN+"Regular Effect"));
            item.setItemMeta(meta);
        }return item;
    }
    public InfuseEffect effectFromItem(ItemStack item){
        if(!isEffectItem(item))return null;
        String raw=item.getItemMeta().getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);
        if(raw==null)return null;
        return InfuseEffect.fromId(raw.endsWith(":augmented")?raw.substring(0,raw.length()-10):raw);
    }
    public boolean isAugmentedItem(ItemStack item){
        if(!isEffectItem(item))return false;
        String raw=item.getItemMeta().getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);
        return raw!=null&&raw.endsWith(":augmented");
    }
    public boolean isEffectItem(ItemStack item){
        return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(itemKey,PersistentDataType.STRING);
    }
    public boolean consumeEffectItem(Player player,ItemStack item){
        InfuseEffect effect=effectFromItem(item);
        if(effect==null)return false;
        PlayerData d=data(player);
        if(!plugin.getConfig().getBoolean("settings.allow-same-effects",false)&&effect.id().equals(d.primary())||!plugin.getConfig().getBoolean("settings.allow-same-effects",false)&&effect.id().equals(d.secondary())){
            player.sendMessage(ChatColor.RED+"You cannot equip the same effect twice.");return false;
        }
        String slot=d.primary()==null?"primary":d.secondary()==null?"secondary":null;
        if(slot==null){
            player.sendMessage(ChatColor.RED+"Both Infuse slots are occupied. Drain or remove an effect first.");
            return false;
        }
        equip(player,slot,effect,isAugmentedItem(item));
        player.sendMessage(ChatColor.GREEN+effect.displayName()+" equipped in your "+slot+" slot.");
        return true;
    }
    public void equip(Player player,String slot,InfuseEffect effect,boolean augmented){
        PlayerData d=data(player);
        if(!plugin.getConfig().getBoolean("settings.allow-same-effects",false)){
            String other=slot.equalsIgnoreCase("primary")?d.secondary():d.primary();
            if(effect.id().equals(other)){player.sendMessage(ChatColor.RED+"You cannot equip the same effect twice.");return;}
        }
        if(slot.equalsIgnoreCase("primary"))d.primary(effect.id(),augmented);else d.secondary(effect.id(),augmented);
        d.addHistory("equip:"+slot+":"+effect.id());applyPassive(player);
    }
    public boolean drain(Player player,String slot){
        PlayerData d=data(player);
        String id=slot.equalsIgnoreCase("primary")?d.primary():d.secondary();
        if(id==null)return false;
        if(player.getInventory().firstEmpty()==-1)return false;
        InfuseEffect effect=InfuseEffect.fromId(id);
        if(effect==null)return false;
        boolean augmented=slot.equalsIgnoreCase("primary")?d.primaryAugmented():d.secondaryAugmented();
        remove(player,slot);
        player.getInventory().addItem(createEffectItem(effect,augmented));
        plugin.messages().send(player,"drained",java.util.Map.of("effect",effect.displayName(),"slot",slot));
        return true;
    }
    public void remove(Player player,String slot){
        PlayerData d=data(player);if(slot.equalsIgnoreCase("primary"))d.primary(null,false);else d.secondary(null,false);
        d.addHistory("remove:"+slot);applyPassive(player);
    }
    public void applyPassive(Player player){
        if(isEffectBlocked(player)){restoreTemporaryEnchants(player.getInventory().getItemInMainHand());return;}
        PlayerData d=data(player);
        refreshHeldItemEnchants(player);
        applyBasicPassive(player,d.primary());
        applyBasicPassive(player,d.secondary());
        if("ocean".equals(d.primary())||"ocean".equals(d.secondary())){
            int airDrain=plugin.getConfig().getInt("effect-settings.ocean.passive-air-drain",5);
            double radius=plugin.getConfig().getDouble("effect-settings.ocean.passive-radius",8);
            for(Player target:nearbyEnemies(player,radius)){
                int air=Math.max(-20,target.getRemainingAir()-airDrain);target.setRemainingAir(air);
                if(air<=0)target.damage(plugin.getConfig().getDouble("effect-settings.ocean.passive-damage",1.0),player);
            }
        }
        if("ender".equals(d.primary())||"ender".equals(d.secondary())){
            double radius=plugin.getConfig().getDouble("effect-settings.ender.passive-radius",10);
            for(Player target:nearbyPlayers(player,radius)){
                if(!d.trusted().contains(target.getUniqueId()))applyIfMissing(target,PotionEffectType.GLOWING,40,0);
            }
        }
    }
    private void refreshHeldItemEnchants(Player player){
        ItemStack item=player.getInventory().getItemInMainHand();
        if(item==null||item.getType()==Material.AIR)return;
        PlayerData d=data(player);
        boolean tool=item.getType().name().endsWith("_PICKAXE")||item.getType().name().endsWith("_AXE")
                ||item.getType().name().endsWith("_SHOVEL")||item.getType().name().endsWith("_HOE");
        if(tool&&("haste".equals(d.primary())||"haste".equals(d.secondary()))){
            applyTemporaryEnchant(item,Enchantment.FORTUNE,hasteFortuneKey,plugin.getConfig().getInt("effect-settings.haste.fortune-level",5));
            applyTemporaryEnchant(item,Enchantment.EFFICIENCY,hasteEfficiencyKey,plugin.getConfig().getInt("effect-settings.haste.efficiency-level",10));
            applyTemporaryEnchant(item,Enchantment.UNBREAKING,hasteUnbreakingKey,plugin.getConfig().getInt("effect-settings.haste.unbreaking-level",5));
        }else{
            restoreTemporaryEnchant(item,hasteFortuneKey,Enchantment.FORTUNE);
            restoreTemporaryEnchant(item,hasteEfficiencyKey,Enchantment.EFFICIENCY);
            restoreTemporaryEnchant(item,hasteUnbreakingKey,Enchantment.UNBREAKING);
        }
        if(item.getType().name().endsWith("_SWORD")&&("emerald".equals(d.primary())||"emerald".equals(d.secondary())))
            applyTemporaryEnchant(item,Enchantment.LOOTING,emeraldLootingKey,plugin.getConfig().getInt("effect-settings.emerald.looting-level",5));
        else restoreTemporaryEnchant(item,emeraldLootingKey,Enchantment.LOOTING);
    }
    private void applyTemporaryEnchant(ItemStack item,Enchantment enchant,NamespacedKey key,int level){
        ItemMeta meta=item.getItemMeta();if(meta==null)return;
        var pdc=meta.getPersistentDataContainer();
        if(!pdc.has(key,PersistentDataType.INTEGER))pdc.set(key,PersistentDataType.INTEGER,meta.hasEnchant(enchant)?meta.getEnchantLevel(enchant):-1);
        meta.addEnchant(enchant,Math.max(1,level),true);item.setItemMeta(meta);
    }
    private void restoreTemporaryEnchant(ItemStack item,NamespacedKey key,Enchantment enchant){
        if(item==null||item.getType()==Material.AIR||!item.hasItemMeta())return;
        ItemMeta meta=item.getItemMeta();var pdc=meta.getPersistentDataContainer();
        if(!pdc.has(key,PersistentDataType.INTEGER))return;
        int original=pdc.get(key,PersistentDataType.INTEGER);
        if(original<0)meta.removeEnchant(enchant);else meta.addEnchant(enchant,original,true);
        pdc.remove(key);item.setItemMeta(meta);
    }
    public void restoreTemporaryEnchants(ItemStack item){
        restoreTemporaryEnchant(item,hasteFortuneKey,Enchantment.FORTUNE);
        restoreTemporaryEnchant(item,hasteEfficiencyKey,Enchantment.EFFICIENCY);
        restoreTemporaryEnchant(item,hasteUnbreakingKey,Enchantment.UNBREAKING);
        restoreTemporaryEnchant(item,emeraldLootingKey,Enchantment.LOOTING);
    }
    public void restoreAllTemporaryEnchants(Player player){
        for(ItemStack item:player.getInventory().getContents())restoreTemporaryEnchants(item);
        restoreTemporaryEnchants(player.getInventory().getItemInOffHand());
    }
    public void refreshPassives(){
        for(Player player:plugin.getServer().getOnlinePlayers())applyPassive(player);
    }
    private void applyIfMissing(Player player,PotionEffectType type,int duration,int amplifier){
        if(type!=null&&!player.hasPotionEffect(type))player.addPotionEffect(new PotionEffect(type,duration,amplifier,true,false,true));
    }
    private void applyBasicPassive(Player player,String id){
        if(id==null)return;
        switch(id){
            case "emerald" -> applyIfMissing(player,PotionEffectType.HERO_OF_THE_VILLAGE,40,0);
            case "fire" -> applyIfMissing(player,PotionEffectType.FIRE_RESISTANCE,40,0);
            case "frost" -> {
                Material under=player.getLocation().subtract(0,1,0).getBlock().getType();
                if(under==Material.ICE||under==Material.PACKED_ICE||under==Material.BLUE_ICE||under==Material.SNOW_BLOCK)
                    applyIfMissing(player,PotionEffectType.SPEED,40,1);
            }
            case "haste" -> applyIfMissing(player,PotionEffectType.HASTE,40,0);
            case "heart" -> applyIfMissing(player,PotionEffectType.HEALTH_BOOST,40,2);
            case "invisibility" -> applyIfMissing(player,PotionEffectType.INVISIBILITY,40,0);
            case "ocean" -> {
                applyIfMissing(player,PotionEffectType.WATER_BREATHING,40,0);
                applyIfMissing(player,PotionEffectType.DOLPHINS_GRACE,40,0);
            }
            case "regeneration" -> applyIfMissing(player,PotionEffectType.REGENERATION,40,0);
            case "speed" -> applyIfMissing(player,PotionEffectType.SPEED,40,0);
            default -> { }
        }
    }
    public boolean activate(Player player,boolean primary){
        if(isEffectBlocked(player))return false;
        PlayerData d=data(player);String id=primary?d.primary():d.secondary();if(id==null)return false;
        InfuseEffect effect=InfuseEffect.fromId(id);if(effect==null)return false;
        boolean augmented=primary?d.primaryAugmented():d.secondaryAugmented();
        String slot=primary?"primary":"secondary";
        String key=(primary?"p:":"s:")+id;long now=System.currentTimeMillis();
        long until=d.cooldowns().getOrDefault(key,0L);
        if(until>now){plugin.messages().send(player,"cooldown",java.util.Map.of("time",((until-now+999)/1000)+"s"));return false;}
        long duration=plugin.getConfig().getLong((augmented?"augmented-durations.":"durations.")+id,plugin.getConfig().getLong("durations."+id,10));
        long cooldown=plugin.getConfig().getLong((augmented?"augmented-cooldowns.":"cooldowns.")+id,plugin.getConfig().getLong("cooldowns."+id,effect.cooldownSeconds()));
        d.cooldowns().put(key,now+(duration+cooldown)*1000L);
        d.cooldowns().put("active:"+slot+":"+id,now+duration*1000L);
        d.addHistory("spark:"+(primary?"primary:":"secondary:")+id);
        player.getWorld().playSound(player.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.7f,1.5f);
        player.getWorld().spawnParticle(Particle.END_ROD,player.getLocation().add(0,1,0),18,.5,.6,.5,.02);
        runSpark(player,effect);
        runTimedSpark(player,effect,duration*20L);
        plugin.messages().send(player,"spark-used",java.util.Map.of("slot",slot));
        return true;
    }
    private void runSpark(Player p,InfuseEffect e){
        switch(e){
            case EMERALD -> {
                p.giveExp(40);
                p.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE,200,4));
                for(Player ally:nearbyPlayers(p,8))if(data(p).trusted().contains(ally.getUniqueId()))ally.giveExp(10);
            }
            case FEATHER -> {
                p.setVelocity(p.getVelocity().setY(2.0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,120,0));
            }
            case FIRE -> {
                for(Player target:nearbyEnemies(p,6)){
                    target.setFireTicks(Math.max(target.getFireTicks(),200));
                    target.setVelocity(target.getVelocity().setY(1.15));
                }
                p.getWorld().playSound(p.getLocation(),Sound.ITEM_FIRECHARGE_USE,1f,0.8f);
            }
            case FROST -> {
                for(Player target:nearbyEnemies(p,8)){
                    target.setFreezeTicks(Math.max(target.getFreezeTicks(),600));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,200,1));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,200,1));
                }
                p.getWorld().spawnParticle(Particle.SNOWFLAKE,p.getLocation(),80,4,1,4,.05);
            }
            case HASTE -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,200,2));
            case HEART -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST,200,4));
                p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+10));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,1));
            }
            case INVISIBILITY -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,200,0));
                for(Player ally:nearbyPlayers(p,8))if(data(p).trusted().contains(ally.getUniqueId()))ally.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,200,0));
                for(Player target:nearbyEnemies(p,8)){
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,100,0));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,100,0));
                }
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE,p.getLocation().add(0,1,0),70,3,1,3,.03);
            }
            case OCEAN -> {
                for(Player target:nearbyEnemies(p,8)){
                    Vector pull=p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(.85);
                    pull.setY(.15);target.setVelocity(pull);
                }
                p.getWorld().spawnParticle(Particle.BUBBLE,p.getLocation(),90,4,1,4,.04);
            }
            case REGENERATION -> {
                p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+4));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,160,2));
                for(Player ally:nearbyPlayers(p,8)){
                    if(data(p).trusted().contains(ally.getUniqueId())){
                        ally.setHealth(Math.min(ally.getMaxHealth(),ally.getHealth()+4));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,1));
                    }
                }
            }
            case SPEED -> {
                Vector dash=p.getLocation().getDirection().normalize().multiply(plugin.getConfig().getDouble("effect-settings.speed.dash-power",2.6));
                dash.setY(dash.getY()*plugin.getConfig().getDouble("effect-settings.speed.dash-damping",.6));
                p.setVelocity(dash);
                for(Player target:nearbyEnemies(p,2))target.damage(2.0,p);
            }
            case STRENGTH -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,200,1));
            case THUNDER -> {
                int count=0;
                for(Player target:nearbyEnemies(p,16)){
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(plugin.getConfig().getDouble("effect-settings.thunder.true-damage",1.0),p);
                    if(++count>=5)break;
                }
            }
            case ENDER -> {
                org.bukkit.block.Block block=p.getTargetBlockExact((int)plugin.getConfig().getDouble("effect-settings.ender.teleport-max-radius",15));
                if(block==null){p.sendMessage(ChatColor.RED+"No safe target in range.");break;}
                org.bukkit.Location destination=block.getLocation().add(.5,1,.5);
                if(!destination.getBlock().isPassable()||!destination.clone().add(0,1,0).getBlock().isPassable()){
                    p.sendMessage(ChatColor.RED+"That location is not safe.");break;
                }
                destination.setDirection(p.getLocation().getDirection());
                p.teleport(destination);
            }
        }
    }
    private void runTimedSpark(Player p,InfuseEffect effect,long durationTicks){
        switch(effect){
            case FIRE -> new org.bukkit.scheduler.BukkitRunnable(){
                long elapsed=0;
                public void run(){
                    if(!p.isOnline()||elapsed>=durationTicks){cancel();p.getWorld().spawnParticle(Particle.EXPLOSION,p.getLocation(),1);return;}
                    for(Player target:nearbyEnemies(p,plugin.getConfig().getDouble("effect-settings.fire.spark-radius",5))){
                        target.setFireTicks(Math.max(target.getFireTicks(),100));
                        target.damage(plugin.getConfig().getDouble("effect-settings.fire.spark-damage",4.0),p);
                    }
                    elapsed+=20;
                }
            }.runTaskTimer(plugin,0L,20L);
            case FROST -> {
                for(Player target:nearbyEnemies(p,plugin.getConfig().getDouble("effect-settings.frost.spark-radius",5))){
                    target.setFreezeTicks(Math.max(target.getFreezeTicks(),(int)durationTicks));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,(int)durationTicks,1,true,false,true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,(int)durationTicks,1,true,false,true));
                }
            }
            case INVISIBILITY -> new org.bukkit.scheduler.BukkitRunnable(){
                long elapsed=0;
                public void run(){
                    if(!p.isOnline()||elapsed>=durationTicks){cancel();return;}
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,40,0,true,false,false));
                    for(Player ally:nearbyPlayers(p,10))if(data(p).trusted().contains(ally.getUniqueId()))ally.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,40,0,true,false,false));
                    for(Player target:nearbyEnemies(p,10)){
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,0,true,false,false));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,20,0,true,false,false));
                    }
                    p.getWorld().spawnParticle(Particle.LARGE_SMOKE,p.getLocation().add(0,1,0),12,1,1,1,.01);
                    elapsed+=20;
                }
            }.runTaskTimer(plugin,0L,20L);
            case OCEAN -> new org.bukkit.scheduler.BukkitRunnable(){
                long elapsed=0;
                public void run(){
                    if(!p.isOnline()||elapsed>=durationTicks){cancel();return;}
                    for(Player target:nearbyEnemies(p,plugin.getConfig().getDouble("effect-settings.ocean.pull-radius",5))){
                        Vector pull=p.getLocation().toVector().subtract(target.getLocation().toVector());
                        if(pull.lengthSquared()>0.001)target.setVelocity(pull.normalize().multiply(plugin.getConfig().getDouble("effect-settings.ocean.pull-strength",.3)));
                    }
                    elapsed+=10;
                }
            }.runTaskTimer(plugin,0L,10L);
            case THUNDER -> {
                java.util.Map<java.util.UUID,Integer> strikes=new java.util.HashMap<>();
                new org.bukkit.scheduler.BukkitRunnable(){
                    long elapsed=0;
                    public void run(){
                        if(!p.isOnline()||elapsed>=durationTicks){cancel();return;}
                        for(Player target:nearbyEnemies(p,plugin.getConfig().getDouble("effect-settings.thunder.spark-radius",10))){
                            int hits=strikes.getOrDefault(target.getUniqueId(),0);
                            if(hits>=plugin.getConfig().getInt("effect-settings.thunder.strikes-per-player",3))continue;
                            target.getWorld().strikeLightningEffect(target.getLocation());
                            target.damage(plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0),p);
                            strikes.put(target.getUniqueId(),hits+1);
                        }
                        elapsed+=20;
                    }
                }.runTaskTimer(plugin,0L,20L);
            }
            default -> { }
        }
    }
    public boolean isSparkActive(Player player,String id){
        PlayerData d=data(player);
        long now=System.currentTimeMillis();
        return d.cooldowns().entrySet().stream().anyMatch(e->e.getKey().endsWith(":"+id)&&e.getKey().startsWith("active:")&&e.getValue()>now);
    }
    private List<Player> nearbyPlayers(Player source,double radius){
        List<Player> result=new java.util.ArrayList<>();
        for(Entity entity:source.getNearbyEntities(radius,radius,radius))if(entity instanceof Player target&&target!=source)result.add(target);
        return result;
    }
    private List<Player> nearbyEnemies(Player source,double radius){
        List<Player> result=new java.util.ArrayList<>();
        for(Player target:nearbyPlayers(source,radius)){
            if(!data(source).trusted().contains(target.getUniqueId())&&!data(target).trusted().contains(source.getUniqueId())&&!isEffectBlocked(target))result.add(target);
        }
        return result;
    }
    public void onCombatHit(Player attacker,LivingEntity victim,EntityDamageByEntityEvent event){
        if(isEffectBlocked(attacker)||isEffectBlocked(victim.getLocation()))return;
        PlayerData d=data(attacker);
        if("strength".equals(d.primary())||"strength".equals(d.secondary())){
            double missing=Math.max(0,attacker.getMaxHealth()-attacker.getHealth());
            double damage=event.getDamage()+missing*0.3;
            if(!(victim instanceof Player))damage*=2.0;
            event.setDamage(damage);
        }
        if(!(victim instanceof Player)&&isSparkActive(attacker,"ender"))victim.setHealth(0.0);
        if(victim instanceof Player defender){
            PlayerData taken=data(defender);
            int hits=taken.chargeTaken()+1;
            if(hits>=10){
                taken.chargeTaken(0);
                if(hasEffect(defender,"feather")){
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,100,2));
                    defender.setVelocity(new Vector(0,.5,0));
                    org.bukkit.entity.WindCharge charge=defender.getWorld().spawn(defender.getLocation().add(0,1,0),org.bukkit.entity.WindCharge.class);
                    charge.setShooter(defender);
                    Vector direction=attacker.getLocation().toVector().subtract(charge.getLocation().toVector());
                    if(direction.lengthSquared()>.001)charge.setVelocity(direction.normalize());
                }
                if(hasEffect(defender,"emerald"))data(attacker).cooldowns().put("emerald-lock",System.currentTimeMillis()+plugin.getConfig().getLong("effect-settings.emerald.lock-duration-seconds",10)*1000L);
            }else taken.chargeTaken(hits);
            onCombatHit(attacker,defender);
        }
    }
    public void onCombatHit(Player attacker,Player victim){
        processHit(attacker,victim,true);
        processHit(attacker,victim,false);
    }
    private void processHit(Player attacker,Player victim,boolean primary){
        PlayerData d=data(attacker);
        String id=primary?d.primary():d.secondary();
        if(id==null)return;
        int threshold=plugin.getConfig().getInt("effect-settings."+id+".trigger-hits",0);
        if(threshold<=0)return;
        int charge=(primary?d.chargePrimary():d.chargeSecondary())+1;
        if(charge<threshold){
            d.charge(primary,charge);
            attacker.sendActionBar(ChatColor.YELLOW+(primary?"Primary":"Secondary")+" charge: "+charge+"/"+threshold);
            return;
        }
        d.charge(primary,0);
        switch(id){
            case "fire" -> victim.setFireTicks(Math.max(victim.getFireTicks(),plugin.getConfig().getInt("effect-settings.fire.ignite-ticks",100)));
            case "thunder" -> {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.damage(plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0),attacker);
                chainLightning(attacker,victim,new java.util.HashSet<>(java.util.Set.of(attacker.getUniqueId(),victim.getUniqueId())),0);
            }
            case "frost" -> {
                victim.setFreezeTicks(Math.max(victim.getFreezeTicks(),200));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,200,1));
            }
            case "invisibility" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,plugin.getConfig().getInt("effect-settings.invisibility.blindness-duration-seconds",10)*20,0));
            case "regeneration" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,60,1));
            case "emerald" -> attacker.giveExp(plugin.getConfig().getInt("effect-settings.emerald.xp-gain",1));
            case "feather" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,100,0));
            case "heart" -> attacker.setHealth(Math.min(attacker.getMaxHealth(),attacker.getHealth()+4));
            case "haste" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,60,2));
            case "ocean" -> { victim.setRemainingAir(Math.max(-20,victim.getRemainingAir()-10)); if(victim.getRemainingAir()<=0)victim.damage(1.0,attacker); }
            case "speed" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,2));
            case "strength" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,60,1));
            case "ender" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,100,0));
            default -> { }
        }
        attacker.sendActionBar(ChatColor.GREEN+(primary?"Primary":"Secondary")+" effect triggered!");
    }
    private void chainLightning(Player owner,Player from,java.util.Set<java.util.UUID> hit,int depth){
        int max=plugin.getConfig().getInt("effect-settings.thunder.lightning-chain-max",10);
        if(depth>=max||!owner.isOnline()||isEffectBlocked(owner))return;
        long delay=20L;
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            Player next=null;double radius=plugin.getConfig().getDouble("effect-settings.thunder.lightning-chain-radius",12);
            for(Player candidate:nearbyPlayers(from,radius)){
                if(hit.contains(candidate.getUniqueId()))continue;
                if(data(owner).trusted().contains(candidate.getUniqueId())||data(candidate).trusted().contains(owner.getUniqueId()))continue;
                next=candidate;break;
            }
            if(next==null)return;
            hit.add(next.getUniqueId());
            next.getWorld().strikeLightningEffect(next.getLocation());
            next.damage(plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0),owner);
            chainLightning(owner,next,hit,depth+1);
        },delay);
    }
    public NamespacedKey itemKey(){return itemKey;}
}
