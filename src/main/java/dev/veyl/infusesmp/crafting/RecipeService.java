package dev.veyl.infusesmp.crafting;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RecipeService implements Listener {
    private static final Map<String,String[]> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("emerald",new String[]{"WILD_ARMOR_TRIM_SMITHING_TEMPLATE","SNIFFER_EGG","WILD_ARMOR_TRIM_SMITHING_TEMPLATE","OMINOUS_BOTTLE","EMERALD_BLOCK","OMINOUS_BOTTLE","ENDER_CHEST","ENCHANTING_TABLE","ENDER_CHEST"});
        PATTERNS.put("feather",new String[]{"BREEZE_ROD","PHANTOM_MEMBRANE","BREEZE_ROD","HEAVY_CORE","FEATHER","HEAVY_CORE","BREEZE_ROD","PHANTOM_MEMBRANE","BREEZE_ROD"});
        PATTERNS.put("fire",new String[]{"NETHERITE_UPGRADE_SMITHING_TEMPLATE","RESPAWN_ANCHOR","NETHERITE_UPGRADE_SMITHING_TEMPLATE","NETHERITE_INGOT","MUSIC_DISC_PIGSTEP","NETHERITE_INGOT","NETHERITE_UPGRADE_SMITHING_TEMPLATE","RESPAWN_ANCHOR","NETHERITE_UPGRADE_SMITHING_TEMPLATE"});
        PATTERNS.put("frost",new String[]{"FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","VERDANT_FROGLIGHT","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","GOAT_HORN","MUSIC_DISC_CREATOR","GOAT_HORN","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","VERDANT_FROGLIGHT","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("haste",new String[]{"DIAMOND_BLOCK","BEACON","DIAMOND_BLOCK","GOLD_BLOCK","DEEPSLATE_EMERALD_ORE","GOLD_BLOCK","DIAMOND_BLOCK","NETHERITE_PICKAXE","DIAMOND_BLOCK"});
        PATTERNS.put("heart",new String[]{"POTION","TOTEM_OF_UNDYING","POTION","ENCHANTED_GOLDEN_APPLE","BEETROOT","ENCHANTED_GOLDEN_APPLE","POTION","TOTEM_OF_UNDYING","POTION"});
        PATTERNS.put("invisibility",new String[]{"ENDER_EYE","MUSIC_DISC_5","ENDER_EYE","SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE","OMINOUS_TRIAL_KEY","SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE","ENDER_EYE","RECOVERY_COMPASS","ENDER_EYE"});
        PATTERNS.put("ocean",new String[]{"TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","HEART_OF_THE_SEA","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","TRIDENT","CONDUIT","TRIDENT","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","HEART_OF_THE_SEA","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("regeneration",new String[]{"END_CRYSTAL","HONEY_BLOCK","END_CRYSTAL","VERDANT_FROGLIGHT","AXOLOTL_BUCKET","VERDANT_FROGLIGHT","END_CRYSTAL","MUSIC_DISC_CREATOR_MUSIC_BOX","END_CRYSTAL"});
        PATTERNS.put("speed",new String[]{"RABBIT_FOOT","SADDLE","RABBIT_FOOT","DUNE_ARMOR_TRIM_SMITHING_TEMPLATE","DIAMOND_HORSE_ARMOR","EYE_ARMOR_TRIM_SMITHING_TEMPLATE","RABBIT_FOOT","NETHERITE_BOOTS","RABBIT_FOOT"});
        PATTERNS.put("strength",new String[]{"PLAYER_HEAD","RIB_ARMOR_TRIM_SMITHING_TEMPLATE","PLAYER_HEAD","NETHERITE_SWORD","WITHER_ROSE","NETHERITE_AXE","PLAYER_HEAD","RIB_ARMOR_TRIM_SMITHING_TEMPLATE","PLAYER_HEAD"});
        PATTERNS.put("thunder",new String[]{"BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","LODESTONE","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","TRIDENT","CREEPER_HEAD","TRIDENT","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","LODESTONE","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("ender",new String[]{"BLACK_SHULKER_BOX","DRAGON_HEAD","BLACK_SHULKER_BOX","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","DRAGON_EGG","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","BLACK_SHULKER_BOX","ELYTRA","BLACK_SHULKER_BOX"});
    }

    private final InfuseSMP plugin;
    private final EffectService effects;
    private final File file;
    private final Map<String,Integer> crafted = new HashMap<>();

    public RecipeService(InfuseSMP plugin,EffectService effects){
        this.plugin=plugin;this.effects=effects;this.file=new File(plugin.getDataFolder(),"crafting.yml");
        load();registerRecipes();
    }

    private void registerRecipes(){
        int registered=0;
        for(Map.Entry<String,String[]> entry:PATTERNS.entrySet()){
            InfuseEffect effect=InfuseEffect.fromId(entry.getKey());if(effect==null)continue;
            Material[] materials=new Material[9];boolean complete=true;
            for(int i=0;i<9;i++){
                materials[i]=Material.matchMaterial(entry.getValue()[i]);
                if(materials[i]==null){complete=false;plugin.getLogger().warning("Skipping "+effect.id()+" recipe: "+entry.getValue()[i]+" is unavailable on this Minecraft version.");}
            }
            if(!complete)continue;
            NamespacedKey key=new NamespacedKey(plugin,"effect_"+effect.id());
            plugin.getServer().removeRecipe(key);
            ShapedRecipe recipe=new ShapedRecipe(key,effects.createEffectItem(effect,false));
            recipe.shape("ABC","DEF","GHI");
            char[] symbols="ABCDEFGHI".toCharArray();
            for(int i=0;i<9;i++)recipe.setIngredient(symbols[i],materials[i]);
            if(plugin.getServer().addRecipe(recipe))registered++;
        }
        plugin.getLogger().info("Registered "+registered+" of "+PATTERNS.size()+" S3 crafting recipes.");
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event){
        ItemStack result=event.getInventory().getResult();
        InfuseEffect effect=effects.effectFromItem(result);
        if(effect==null)return;
        int amount=crafted.getOrDefault(effect.id(),0);
        int limit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".craft-limit",-1);
        if(limit>=0&&amount>=limit){event.getInventory().setResult(new ItemStack(Material.AIR));return;}
        int augmentLimit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".augment-limit",0);
        event.getInventory().setResult(effects.createEffectItem(effect,amount<augmentLimit));
    }

    @EventHandler
    public void onCraft(CraftItemEvent event){
        InfuseEffect effect=effects.effectFromItem(event.getRecipe().getResult());
        if(effect==null)return;
        crafted.merge(effect.id(),1,Integer::sum);
        save();
    }

    private void load(){
        if(!file.exists())return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        for(String id:yaml.getKeys(false))crafted.put(id,yaml.getInt(id));
    }
    public void save(){
        YamlConfiguration yaml=new YamlConfiguration();
        for(Map.Entry<String,Integer> entry:crafted.entrySet())yaml.set(entry.getKey(),entry.getValue());
        try{if(!plugin.getDataFolder().exists())plugin.getDataFolder().mkdirs();yaml.save(file);}
        catch(IOException ex){plugin.getLogger().severe("Could not save crafting counts: "+ex.getMessage());}
    }
}
