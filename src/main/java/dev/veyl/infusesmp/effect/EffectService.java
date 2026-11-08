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
    private final NamespacedKey itemKey,enderCurseKey,enderCurseSourceKey;
    private final NamespacedKey hasteFortuneKey,hasteEfficiencyKey,hasteUnbreakingKey,emeraldLootingKey;
    public EffectService(InfuseSMP plugin,PlayerDataStore store){
        this.plugin=plugin;this.store=store;this.itemKey=new NamespacedKey(plugin,"infuse_effect");
        this.enderCurseKey=new NamespacedKey(plugin,"ender_curse_until");this.enderCurseSourceKey=new NamespacedKey(plugin,"ender_curse_source");
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
            meta.setLore(effectLore(effect,augmented));
            item.setItemMeta(meta);
        }return item;
    }

    private List<String> effectLore(InfuseEffect effect,boolean augmented){
        String icon;
        List<String> passive;
        List<String> spark;
        switch(effect){
            case EMERALD -> {icon="💲";passive=List.of("Looting 5","Permanent Hero of the village","2.0x XP gain","Consumables have a 50.0% chance of not being consumed","Receiving 10 hits lock the attacker's food and XP");spark=List.of("Max level enchanting without bookshelves","Each hit steals XP from your enemy","Hero of the village 5.0","4.0x XP gain","Consumables have a 75.0% chance not to be consumed","Share XP with nearby teammates (hit them)");}
            case FEATHER -> {icon="☁";passive=List.of("No Fall damage","Immune to kinetic energy","Windburst on all attacks","Falling from higher deals more damage","Windcharges have 2.0x velocity and 0.5x use speed","Receiving 10 hits spawns an orbiting wind charge(crouch to use)");spark=List.of("Launches the player upward","Falling from higher up deals 2.0 hearts on impact","Impact radius and bounce height scale with the distance fallen","Enemies hit by the shockwave are given slow falling","Crouch to cancel the bounce");}
            case FIRE -> {icon="🔥";passive=List.of("Permanent Fire Resistance","Propel yourself through lava","Lava breaks your fall","Snowballs turn into fireballs","5th hit sets enemies on fire","Flame on ranged attacks","Fire spreads to nearby enemies that touch each other","Extinguish nearby water sources when on fire");spark=List.of("Ignite all players before all launching them into the air","Extinguishes all nearby water sources and set flammable blocks on fire");}
            case FROST -> {icon="❄";passive=List.of("10th hit freezes enemies and puts windcharges on cooldown","Frosted enemies create ice trails and get Mining Fatigue","1.5x speed and 30.0% KB resistance on ice/snow","Cools down and solidifies lava","Frost on ranged attacks");spark=List.of("Apply frost and mining fatigue 2.0 on hit","2.0x speed on ice/snow");}
            case THUNDER -> {icon="⚡";passive=List.of("Smite players on every 10th hit (5th if raining)","Thunder chains to nearby enemies up to 10 times","Tridents smite enemies when thrown");spark=List.of("Smite all players up to 5 times","Radius increases the more enemies are inside");}
            case HASTE -> {icon="⛏";passive=List.of("Break blocks faster","Tools have Fortune 5 and Unbreaking 10","Enemies have reduced I-Frames","Each hit increases attack speed","Attack speed decays every 1.0s");spark=List.of("Insta mine all blocks");}
            case HEART -> {icon="❤";passive=List.of("+5 Hearts","Food items give 2.0 absorption hearts","Egaps give 10.0 absorption hearts","Hit teammates to give them your extra HP as absorption hearts");spark=List.of("+10.0 hearts","Heals you the amount of damage you dealt in the past 30.0s");}
            case INVISIBILITY -> {icon="👁";passive=List.of("Invisibility","No entity collision","Mobs don't target you","Immune to wither and darkness","Obscures kill credit","Blindness on ranged attacks","10th hit gives blindness");spark=List.of("Creates a smoke screen","Applies wither and blindness to enemies","Gives true invis to you and people in your trust list","Smoke radius increases with more teammates inside");}
            case OCEAN -> {icon="🌊";passive=List.of("Dolphin's Grace and Waterbreathing","Drown nearby enemies","Standing in rain increases drowning damage","Nearby teammates get water breathing","Tridents have loyalty 5 and pull enemies");spark=List.of("Creates a whirlpool that pulls in nearby enemies");}
            case REGENERATION -> {icon="+";passive=List.of("No hunger loss","Permanent Regeneration","Every hit gives Regeneration 2 for 3.0s","All food gives +3.0 saturation bars","Eat anytime","10th hit takes away 1.0 hunger bar from your target");spark=List.of("Damage dealth heals you and nearby teammates");}
            case SPEED -> {icon="⏩";passive=List.of("+0.2x movement speed","Each hit increases speed level","Getting hit lowers speed level","Each hit increases speed level","Improved projectile speed, accuracy and range");spark=List.of("Dash forward dealing damage");}
            case STRENGTH -> {icon="🛡";passive=List.of("Shield disables are 2.0x longer","Damages shields by 20 when disabling","0.5x damage penetrates when disabling shields","2.0x damage to all mobs","+0.3 attack damage for every 1HP Lost","Piercing on ranged attacks");spark=List.of("All attacks are critical");}
            case ENDER -> {icon="⭐";passive=List.of("All nearby untrusted players have glowing","Use dragon's breath to shoot powerful fireballs that curse players","Curse untrusted players on hit which shares damage with all cursed players");spark=List.of("Teleport to the cursor position within a 15 block radius","Instantly kills any mob and curses players","Cursing a player resets the spark cooldown");}
            default -> throw new IllegalArgumentException("Unsupported effect: "+effect);
        }
        List<String> lore=new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY+"ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
        for(String line:passive)lore.add(ChatColor.GRAY+icon+" "+line);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY+"ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
        for(String line:spark)lore.add(ChatColor.GRAY+icon+" "+line);
        lore.add("");
        lore.add(augmented?ChatColor.LIGHT_PURPLE+"Augmented Effect":ChatColor.GREEN+"Regular Effect");
        lore.add(ChatColor.DARK_GRAY+"Drink to equip in your next available slot");
        return lore;
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
        PlayerData current=data(player);
        if(plugin.getConfig().getBoolean("settings.lock-effect-slots",false)&&(current.primary()!=null||current.secondary()!=null)){
            player.sendMessage(ChatColor.RED+"Your effect slots are locked.");return false;
        }
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
        plugin.messages().send(player,"effect-added",java.util.Map.of("effect",effect.displayName(),"slot",slot));
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
        if(plugin.getConfig().getBoolean("settings.lock-effect-slots",false))return false;
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
                    thunderDamage(target,p,plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0));
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
                            thunderDamage(target,p,plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0));
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
    public void applyEnderCurse(Player target,Player source){
        long expiry=System.currentTimeMillis()+plugin.getConfig().getLong("effect-settings.ender.curse-duration-seconds",10)*1000L;
        target.getPersistentDataContainer().set(enderCurseKey,PersistentDataType.LONG,expiry);
        target.getPersistentDataContainer().set(enderCurseSourceKey,PersistentDataType.STRING,source.getUniqueId().toString());
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,plugin.getConfig().getInt("effect-settings.ender.curse-duration-seconds",10)*20,0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,plugin.getConfig().getInt("effect-settings.ender.curse-duration-seconds",10)*20,0));
    }
    public void shareEnderCurseDamage(Player victim,double damage){
        var pdc=victim.getPersistentDataContainer();
        long expiry=pdc.getOrDefault(enderCurseKey,PersistentDataType.LONG,0L);
        if(expiry<=System.currentTimeMillis()){pdc.remove(enderCurseKey);pdc.remove(enderCurseSourceKey);return;}
        String source=pdc.get(enderCurseSourceKey,PersistentDataType.STRING);if(source==null)return;
        double share=plugin.getConfig().getDouble("effect-settings.ender.curse-damage-share",0.5);
        for(Player cursed:plugin.getServer().getOnlinePlayers()){
            if(cursed.equals(victim))continue;
            long otherExpiry=cursed.getPersistentDataContainer().getOrDefault(enderCurseKey,PersistentDataType.LONG,0L);
            String otherSource=cursed.getPersistentDataContainer().get(enderCurseSourceKey,PersistentDataType.STRING);
            if(otherExpiry>System.currentTimeMillis()&&source.equals(otherSource))
                cursed.setHealth(Math.max(0.0,cursed.getHealth()-damage*share));
        }
    }
    public void thunderDamage(LivingEntity target,Player attacker,double amount){
        if(plugin.getConfig().getBoolean("settings.allow-true-damage",true))
            target.setHealth(Math.max(0.0,target.getHealth()-Math.max(0.0,amount)));
        else target.damage(amount,attacker);
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
            long now=System.currentTimeMillis();
            long decay=plugin.getConfig().getLong("settings.hit-counter-decay-seconds",15)*1000L;
            long lastTaken=taken.cooldowns().getOrDefault("last-hit-taken",0L);
            int hits=(now-lastTaken>decay?0:taken.chargeTaken())+1;
            if(hits>=10){
                taken.chargeTaken(0);taken.cooldowns().remove("last-hit-taken");
                if(hasEffect(defender,"feather")){
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,100,2));
                    defender.setVelocity(new Vector(0,.5,0));
                    org.bukkit.entity.WindCharge charge=defender.getWorld().spawn(defender.getLocation().add(0,1,0),org.bukkit.entity.WindCharge.class);
                    charge.setShooter(defender);
                    Vector direction=attacker.getLocation().toVector().subtract(charge.getLocation().toVector());
                    if(direction.lengthSquared()>.001)charge.setVelocity(direction.normalize());
                }
                if(hasEffect(defender,"emerald"))data(attacker).cooldowns().put("emerald-lock",System.currentTimeMillis()+plugin.getConfig().getLong("effect-settings.emerald.lock-duration-seconds",10)*1000L);
            }else {taken.chargeTaken(hits);taken.cooldowns().put("last-hit-taken",now);}
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
        long now=System.currentTimeMillis();
        String hitTimeKey="last-hit:"+(primary?"primary":"secondary");
        long decay=plugin.getConfig().getLong("settings.hit-counter-decay-seconds",15)*1000L;
        int previous=(now-d.cooldowns().getOrDefault(hitTimeKey,0L)>decay)?0:(primary?d.chargePrimary():d.chargeSecondary());
        int charge=previous+1;
        if(charge<threshold){
            d.charge(primary,charge);d.cooldowns().put(hitTimeKey,now);
            attacker.sendActionBar(ChatColor.YELLOW+(primary?"Primary":"Secondary")+" charge: "+charge+"/"+threshold);
            return;
        }
        d.charge(primary,0);d.cooldowns().remove(hitTimeKey);
        switch(id){
            case "fire" -> victim.setFireTicks(Math.max(victim.getFireTicks(),plugin.getConfig().getInt("effect-settings.fire.ignite-ticks",100)));
            case "thunder" -> {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                thunderDamage(victim,attacker,plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0));
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
            thunderDamage(next,owner,plugin.getConfig().getDouble("effect-settings.thunder.true-damage",2.0));
            chainLightning(owner,next,hit,depth+1);
        },delay);
    }
    public NamespacedKey itemKey(){return itemKey;}
}
