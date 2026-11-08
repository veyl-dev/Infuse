package dev.veyl.infusesmp.effect;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerData;
import dev.veyl.infusesmp.data.PlayerDataStore;
import org.bukkit.*;
import org.bukkit.entity.Player;
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
            meta.setCustomModelData(effect.modelData());
            meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,effect.id()+(augmented?":augmented":""));
            meta.setLore(List.of(ChatColor.GRAY+"Infuse SMP Season 3",
                    ChatColor.DARK_GRAY+"Drink to equip in your next available slot",
                    augmented?ChatColor.LIGHT_PURPLE+"Augmented Effect":ChatColor.GREEN+"Regular Effect"));
            item.setItemMeta(meta);
        }return item;
    }
    public boolean isEffectItem(ItemStack item){
        return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(itemKey,PersistentDataType.STRING);
    }
    public void equip(Player player,String slot,InfuseEffect effect,boolean augmented){
        PlayerData d=data(player);
        if(slot.equalsIgnoreCase("primary"))d.primary(effect.id(),augmented);else d.secondary(effect.id(),augmented);
        d.addHistory("equip:"+slot+":"+effect.id());applyPassive(player);
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
        if("ocean".equals(d.primary())||"ocean".equals(d.secondary()))applyIfMissing(player,PotionEffectType.WATER_BREATHING,100,0);
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
        applyIfMissing(player,effect.passive(),100,0);
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
            case HEART,REGENERATION -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,1));
            case SPEED,FEATHER -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,100,1));
            case STRENGTH -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,100,1));
            case FIRE -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,200,0));
            case FROST -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,100,1));
            case INVISIBILITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,100,0));
            case HASTE -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,100,2));
            case OCEAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE,200,1));
            case EMERALD -> p.giveExp(plugin.getConfig().getInt("effect-settings.emerald.xp-gain",1)*10);
            case THUNDER -> p.getWorld().strikeLightningEffect(p.getLocation());
            case ENDER -> p.teleport(p.getLocation().add(p.getLocation().getDirection().multiply(
                    plugin.getConfig().getDouble("effect-settings.ender.teleport-max-radius",15))));
        }
    }
    public void onCombatHit(Player attacker){
        PlayerData d=data(attacker);charge(attacker,d,true);charge(attacker,d,false);
    }
    private void charge(Player p,PlayerData d,boolean primary){
        String id=primary?d.primary():d.secondary();if(id==null)return;
        int value=Math.min(5,(primary?d.chargePrimary():d.chargeSecondary())+1);d.charge(primary,value);
        if(value==5)p.sendActionBar(ChatColor.GREEN+(primary?"Primary":"Secondary")+" spark fully charged!");
    }
    public boolean consumeEffectItem(Player player, ItemStack item) {
        if (!isEffectItem(item)) return false;
        String raw = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (raw == null) return false;
        boolean augmented = raw.endsWith(":augmented");
        String id = augmented ? raw.substring(0, raw.length() - 10) : raw;
        InfuseEffect effect = InfuseEffect.fromId(id);
        if (effect == null) return false;
        PlayerData d = data(player);
        String slot = d.primary() == null ? "primary" : d.secondary() == null ? "secondary" : null;
        if (slot == null) {
            player.sendMessage(ChatColor.RED + "Both Infuse slots are occupied. Drain or remove an effect first.");
            return false;
        }
        equip(player, slot, effect, augmented);
        player.sendMessage(ChatColor.GREEN + "Equipped " + effect.displayName() + " in your " + slot + " slot.");
        return true;
    }
    public NamespacedKey itemKey(){return itemKey;}
}
