package dev.veyl.infusesmp.crafting;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import dev.veyl.infusesmp.ritual.RitualService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RecipeService implements Listener {
    private static final Map<String,String[]> PATTERNS=new LinkedHashMap<>();
    static {
        PATTERNS.put("emerald",new String[]{"WILD_ARMOR_TRIM_SMITHING_TEMPLATE","SNIFFER_EGG","WILD_ARMOR_TRIM_SMITHING_TEMPLATE","OMINOUS_BOTTLE","EMERALD_BLOCK","OMINOUS_BOTTLE","ENDER_CHEST","ENCHANTING_TABLE","ENDER_CHEST"});
        PATTERNS.put("feather",new String[]{"BREEZE_ROD","PHANTOM_MEMBRANE","BREEZE_ROD","FEATHER","HEAVY_CORE","FEATHER","BREEZE_ROD","PHANTOM_MEMBRANE","BREEZE_ROD"});
        PATTERNS.put("fire",new String[]{"NETHERITE_UPGRADE_SMITHING_TEMPLATE","RESPAWN_ANCHOR","NETHERITE_UPGRADE_SMITHING_TEMPLATE","NETHERITE_INGOT","MUSIC_DISC_PIGSTEP","NETHERITE_INGOT","NETHERITE_UPGRADE_SMITHING_TEMPLATE","RESPAWN_ANCHOR","NETHERITE_UPGRADE_SMITHING_TEMPLATE"});
        PATTERNS.put("frost",new String[]{"FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","PEARLESCENT_FROGLIGHT","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","GOAT_HORN","MUSIC_DISC_CREATOR","GOAT_HORN","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE","PEARLESCENT_FROGLIGHT","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("haste",new String[]{"DIAMOND_BLOCK","BEACON","DIAMOND_BLOCK","GOLD_BLOCK","DEEPSLATE_EMERALD_ORE","GOLD_BLOCK","DIAMOND_BLOCK","NETHERITE_PICKAXE","DIAMOND_BLOCK"});
        PATTERNS.put("heart",new String[]{"POTION","TOTEM_OF_UNDYING","POTION","ENCHANTED_GOLDEN_APPLE","BEETROOT","ENCHANTED_GOLDEN_APPLE","POTION","TOTEM_OF_UNDYING","POTION"});
        PATTERNS.put("invisibility",new String[]{"ENDER_EYE","MUSIC_DISC_5","ENDER_EYE","SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE","OMINOUS_TRIAL_KEY","SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE","ENDER_EYE","RECOVERY_COMPASS","ENDER_EYE"});
        PATTERNS.put("ocean",new String[]{"TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","HEART_OF_THE_SEA","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","TRIDENT","CONDUIT","TRIDENT","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE","HEART_OF_THE_SEA","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("regeneration",new String[]{"END_CRYSTAL","HONEY_BLOCK","END_CRYSTAL","PEARLESCENT_FROGLIGHT","AXOLOTL_BUCKET","PEARLESCENT_FROGLIGHT","END_CRYSTAL","MUSIC_DISC_CREATOR_MUSIC_BOX","END_CRYSTAL"});
        PATTERNS.put("speed",new String[]{"RABBIT_FOOT","SADDLE","RABBIT_FOOT","DUNE_ARMOR_TRIM_SMITHING_TEMPLATE","DIAMOND_HORSE_ARMOR","EYE_ARMOR_TRIM_SMITHING_TEMPLATE","RABBIT_FOOT","NETHERITE_BOOTS","RABBIT_FOOT"});
        PATTERNS.put("strength",new String[]{"PLAYER_HEAD","RIB_ARMOR_TRIM_SMITHING_TEMPLATE","PLAYER_HEAD","NETHERITE_SWORD","WITHER_ROSE","NETHERITE_AXE","PLAYER_HEAD","RIB_ARMOR_TRIM_SMITHING_TEMPLATE","PLAYER_HEAD"});
        PATTERNS.put("thunder",new String[]{"BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","LODESTONE","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","TRIDENT","CREEPER_HEAD","TRIDENT","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE","LODESTONE","BOLT_ARMOR_TRIM_SMITHING_TEMPLATE"});
        PATTERNS.put("ender",new String[]{"DRAGON_HEAD","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","DRAGON_HEAD","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","ENDER_EYE","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","DRAGON_HEAD","SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","DRAGON_HEAD"});
    }

    private final InfuseSMP plugin;
    private final EffectService effects;
    private final RitualService rituals;
    private final File file;
    private final Map<String,Integer> regularCrafted=new HashMap<>(),augmentedCrafted=new HashMap<>();

    public RecipeService(InfuseSMP plugin,EffectService effects,RitualService rituals){
        this.plugin=plugin;this.effects=effects;this.rituals=rituals;this.file=new File(plugin.getDataFolder(),"crafting.yml");
        load();registerRecipes();
    }
    private void registerRecipes(){
        int count=0;
        for(Map.Entry<String,String[]> entry:PATTERNS.entrySet()){
            InfuseEffect effect=InfuseEffect.fromId(entry.getKey());if(effect==null)continue;
            int regularLimit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".craft-limit",-1);
            int augmentedLimit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".augment-limit",0);
            boolean augmented=regularLimit>=0&&regularCrafted.getOrDefault(effect.id(),0)>=regularLimit
                    &&augmentedCrafted.getOrDefault(effect.id(),0)<augmentedLimit;
            if(regularLimit>=0&&regularCrafted.getOrDefault(effect.id(),0)>=regularLimit&&!augmented)continue;
            Material[] ingredients=new Material[9];boolean valid=true;
            for(int i=0;i<9;i++){
                ingredients[i]=Material.matchMaterial(entry.getValue()[i]);
                if(ingredients[i]==null){valid=false;plugin.getLogger().warning("Unavailable recipe ingredient "+entry.getValue()[i]+" for "+effect.id());}
            }
            if(!valid)continue;
            NamespacedKey key=new NamespacedKey(plugin,"effect_"+effect.id());
            plugin.getServer().removeRecipe(key);
            ShapedRecipe recipe=new ShapedRecipe(key,effects.createEffectItem(effect,augmented));
            recipe.shape("ABC","DEF","GHI");
            char[] slots="ABCDEFGHI".toCharArray();
            for(int i=0;i<9;i++)recipe.setIngredient(slots[i],ingredients[i]);
            if(plugin.getServer().addRecipe(recipe))count++;
        }
        plugin.getLogger().info("Registered "+count+" S3 effect recipes.");
    }
    @EventHandler
    public void onBrewingStand(PlayerInteractEvent event){
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        Block block=event.getClickedBlock();if(block==null||block.getType()!=Material.BREWING_STAND)return;
        if(rituals.isAt(block.getLocation())){event.setCancelled(true);return;}
        event.setCancelled(true);
        event.getPlayer().openWorkbench(block.getLocation(),true);
    }
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event){
        ItemStack result=event.getInventory().getResult();
        InfuseEffect effect=effects.effectFromItem(result);
        if(effect==null)return;
        if(effects.isAugmentedItem(result)){
            int limit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".augment-limit",0);
            if(limit>=0&&augmentedCrafted.getOrDefault(effect.id(),0)>=limit)event.getInventory().setResult(new ItemStack(Material.AIR));
        }else{
            int limit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".craft-limit",-1);
            if(limit>=0&&regularCrafted.getOrDefault(effect.id(),0)>=limit)event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }
    @EventHandler
    public void onCraft(CraftItemEvent event){
        InfuseEffect effect=effects.effectFromItem(event.getRecipe().getResult());if(effect==null)return;
        if(event.isShiftClick()){event.setCancelled(true);event.getWhoClicked().sendMessage("§cInfuse effects cannot be shift-crafted.");return;}
        if(effects.isAugmentedItem(event.getRecipe().getResult())){
            event.setCancelled(true);
            if(event.getInventory().getLocation()==null||event.getInventory().getLocation().getBlock().getType()!=Material.BREWING_STAND){
                event.getWhoClicked().sendMessage("§cAugmented effects must be crafted at a brewing stand.");return;
            }
            if(rituals.isActive()){event.getWhoClicked().sendMessage("§cAnother augmented ritual is already active.");return;}
            if(!(event.getWhoClicked() instanceof org.bukkit.entity.Player player))return;
            if(!rituals.start(player,effect,event.getInventory().getLocation())){
                player.sendMessage("§cCould not start the augmented ritual.");return;
            }
            ItemStack[] matrix=event.getInventory().getMatrix();
            for(ItemStack ingredient:matrix)if(ingredient!=null&&ingredient.getType()!=Material.AIR)ingredient.setAmount(ingredient.getAmount()-1);
            event.getInventory().setMatrix(matrix);
            player.closeInventory();
            augmentedCrafted.merge(effect.id(),1,Integer::sum);
        }else{
            regularCrafted.merge(effect.id(),1,Integer::sum);
        }
        save();registerRecipes();
    }
    private void load(){
        if(!file.exists())return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        for(String id:yaml.getKeys(false)){
            regularCrafted.put(id,yaml.getInt(id+".regular",yaml.getInt(id+".normal",0)));
            augmentedCrafted.put(id,yaml.getInt(id+".augmented",0));
        }
    }
    public void save(){
        YamlConfiguration yaml=new YamlConfiguration();
        for(String id:PATTERNS.keySet()){
            yaml.set(id+".regular",regularCrafted.getOrDefault(id,0));
            yaml.set(id+".augmented",augmentedCrafted.getOrDefault(id,0));
        }
        try{if(!plugin.getDataFolder().exists())plugin.getDataFolder().mkdirs();yaml.save(file);}
        catch(IOException ex){plugin.getLogger().severe("Could not save crafting counters: "+ex.getMessage());}
    }
}
