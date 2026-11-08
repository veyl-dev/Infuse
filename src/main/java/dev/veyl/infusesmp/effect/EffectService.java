package dev.veyl.infusesmp.effect;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerData;
import dev.veyl.infusesmp.data.PlayerDataStore;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.List;

public final class EffectService {
    private final InfuseSMP plugin;
    private final PlayerDataStore store;
    private final NamespacedKey itemKey;
    public EffectService(InfuseSMP plugin,PlayerDataStore store){
        this.plugin=plugin;this.store=store;this.itemKey=new NamespacedKey(plugin,"infuse_effect");
    }
    public PlayerData data(Player player){return store.get(player.getUniqueId());}
    public int effectCount(){return InfuseEffect.values().length;}
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
        player.sendMessage(ChatColor.GREEN+"Drained "+effect.displayName()+" from your "+slot+" slot.");
        return true;
    }
    public void remove(Player player,String slot){
        PlayerData d=data(player);if(slot.equalsIgnoreCase("primary"))d.primary(null,false);else d.secondary(null,false);
        d.addHistory("remove:"+slot);applyPassive(player);
    }
    public void applyPassive(Player player){
        if(plugin.getConfig().getStringList("settings.disabled-worlds").contains(player.getWorld().getName()))return;
        PlayerData d=data(player);
        applyBasicPassive(player,d.primary());
        applyBasicPassive(player,d.secondary());
        if("ocean".equals(d.primary())||"ocean".equals(d.secondary()))applyIfMissing(player,PotionEffectType.WATER_BREATHING,40,0);
    }
    public void refreshPassives(){
        for(Player player:plugin.getServer().getOnlinePlayers())applyPassive(player);
    }
    private void applyIfMissing(Player player,PotionEffectType type,int duration,int amplifier){
        if(type!=null&&!player.hasPotionEffect(type))player.addPotionEffect(new PotionEffect(type,duration,amplifier,true,false,true));
    }
    private void applyBasicPassive(Player player,String id){
        InfuseEffect effect=InfuseEffect.fromId(id);
        if(effect==null||effect.passive()==null)return;
        applyIfMissing(player,effect.passive(),40,0);
    }
    public boolean activate(Player player,boolean primary){
        PlayerData d=data(player);String id=primary?d.primary():d.secondary();if(id==null)return false;
        InfuseEffect effect=InfuseEffect.fromId(id);if(effect==null)return false;
        String key=(primary?"p:":"s:")+id;long now=System.currentTimeMillis();
        long until=d.cooldowns().getOrDefault(key,0L);
        if(until>now){player.sendMessage(ChatColor.RED+"Spark recharging: "+((until-now+999)/1000)+"s");return false;}
        d.cooldowns().put(key,now+plugin.getConfig().getLong("cooldowns."+id,effect.cooldownSeconds())*1000L);
        d.addHistory("spark:"+(primary?"primary:":"secondary:")+id);
        player.getWorld().playSound(player.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.7f,1.5f);
        player.getWorld().spawnParticle(Particle.END_ROD,player.getLocation().add(0,1,0),18,.5,.6,.5,.02);
        runSpark(player,effect);return true;
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
    private List<Player> nearbyPlayers(Player source,double radius){
        List<Player> result=new java.util.ArrayList<>();
        for(Entity entity:source.getNearbyEntities(radius,radius,radius))if(entity instanceof Player target&&target!=source)result.add(target);
        return result;
    }
    private List<Player> nearbyEnemies(Player source,double radius){
        List<Player> result=new java.util.ArrayList<>();
        for(Player target:nearbyPlayers(source,radius)){
            if(!data(source).trusted().contains(target.getUniqueId())&&!data(target).trusted().contains(source.getUniqueId()))result.add(target);
        }
        return result;
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
                victim.damage(plugin.getConfig().getDouble("effect-settings.thunder.true-damage",1.0),attacker);
            }
            case "frost" -> {
                victim.setFreezeTicks(Math.max(victim.getFreezeTicks(),200));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,200,1));
            }
            case "invisibility" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,plugin.getConfig().getInt("effect-settings.invisibility.blindness-duration-seconds",10)*20,0));
            case "regeneration" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,60,1));
            case "emerald" -> attacker.giveExp(plugin.getConfig().getInt("effect-settings.emerald.xp-gain",1));
            case "feather" -> attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,100,0));
            default -> { }
        }
        attacker.sendActionBar(ChatColor.GREEN+(primary?"Primary":"Secondary")+" effect triggered!");
    }
    public NamespacedKey itemKey(){return itemKey;}
}
