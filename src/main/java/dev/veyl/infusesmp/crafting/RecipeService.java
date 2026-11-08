package dev.veyl.infusesmp.crafting;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import dev.veyl.infusesmp.ritual.RitualService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
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
    private final InfuseSMP plugin;
    private final EffectService effects;
    private final RitualService rituals;
    private final File counterFile,recipeFile;
    private final Map<String,Integer> regularCrafted=new HashMap<>(),augmentedCrafted=new HashMap<>();
    private YamlConfiguration recipes;

    public RecipeService(InfuseSMP plugin,EffectService effects,RitualService rituals){
        this.plugin=plugin;this.effects=effects;this.rituals=rituals;
        this.counterFile=new File(plugin.getDataFolder(),"crafting.yml");
        this.recipeFile=new File(plugin.getDataFolder(),"recipes.yml");
        loadCounters();reload();
    }
    public void reload(){
        if(!recipeFile.exists())plugin.saveResource("recipes.yml",false);
        recipes=YamlConfiguration.loadConfiguration(recipeFile);
        registerRecipes();
    }
    public String[] recipeIngredients(String id){
        String base=recipes.contains("aug_"+id+".shape")?"aug_"+id:id;
        List<String> shape=recipes.getStringList(base+".shape");
        ConfigurationSection ingredients=recipes.getConfigurationSection(base+".ingredients");
        if(shape.isEmpty()||ingredients==null)return null;
        String[] flattened=new String[9];int index=0;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            if(row>=shape.size()||col>=shape.get(row).length()){flattened[index++]=null;continue;}
            String symbol=String.valueOf(shape.get(row).charAt(col));
            flattened[index++]=symbol.equals(" ")?null:ingredients.getString(symbol);
        }
        return flattened;
    }
    private void registerRecipes(){
        int count=0;
        for(InfuseEffect effect:InfuseEffect.values()){
            int regularLimit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".craft-limit",-1);
            int augmentedLimit=plugin.getConfig().getInt("effects.crafting."+effect.id()+".augment-limit",0);
            int regularCount=regularCrafted.getOrDefault(effect.id(),0);
            int augmentedCount=augmentedCrafted.getOrDefault(effect.id(),0);
            boolean regularAvailable=regularLimit<0||regularCount<regularLimit;
            boolean augmentedAvailable=augmentedLimit<0||augmentedCount<augmentedLimit;
            boolean augmented=!regularAvailable&&augmentedAvailable;
            if(!regularAvailable&&!augmentedAvailable)continue;
            String recipeId=augmented&&recipes.contains("aug_"+effect.id()+".shape")?"aug_"+effect.id():effect.id();
            List<String> shape=recipes.getStringList(recipeId+".shape");
            ConfigurationSection ingredients=recipes.getConfigurationSection(recipeId+".ingredients");
            if(shape.isEmpty()||ingredients==null){plugin.getLogger().warning("No valid recipe definition for "+recipeId);continue;}
            Material[] materials=new Material[9];boolean valid=true;
            int index=0;
            for(int row=0;row<3;row++)for(int col=0;col<3;col++){
                if(row>=shape.size()||col>=shape.get(row).length()){index++;continue;}
                String symbol=String.valueOf(shape.get(row).charAt(col));
                if(symbol.equals(" ")){index++;continue;}
                String name=ingredients.getString(symbol);
                Material material=name==null?null:Material.matchMaterial(name);
                if(material==null){valid=false;plugin.getLogger().warning("Invalid ingredient '"+name+"' in "+recipeId); }
                materials[index++]=material;
            }
            if(!valid)continue;
            NamespacedKey key=new NamespacedKey(plugin,"effect_"+effect.id());
            plugin.getServer().removeRecipe(key);
            ItemStack output=effects.createEffectItem(effect,augmented);
            ShapedRecipe recipe=new ShapedRecipe(key,output);
            recipe.shape(shape.toArray(String[]::new));
            for(String symbol:ingredients.getKeys(false)){
                String materialName=ingredients.getString(symbol);
                Material material=materialName==null?null:Material.matchMaterial(materialName);
                if(material!=null&&!symbol.isEmpty())recipe.setIngredient(symbol.charAt(0),material);
            }
            if(plugin.getServer().addRecipe(recipe))count++;
        }
        plugin.getLogger().info("Registered "+count+" S3 effect recipes from recipes.yml.");
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
        InfuseEffect effect=effects.effectFromItem(result);if(effect==null)return;
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
    private void loadCounters(){
        if(!counterFile.exists())return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(counterFile);
        for(String id:yaml.getKeys(false)){
            regularCrafted.put(id,yaml.getInt(id+".regular",yaml.getInt(id+".normal",0)));
            augmentedCrafted.put(id,yaml.getInt(id+".augmented",0));
        }
    }
    public void save(){
        YamlConfiguration yaml=new YamlConfiguration();
        for(InfuseEffect effect:InfuseEffect.values()){
            String id=effect.id();
            yaml.set(id+".regular",regularCrafted.getOrDefault(id,0));
            yaml.set(id+".augmented",augmentedCrafted.getOrDefault(id,0));
        }
        try{if(!plugin.getDataFolder().exists())plugin.getDataFolder().mkdirs();yaml.save(counterFile);}
        catch(IOException ex){plugin.getLogger().severe("Could not save crafting counters: "+ex.getMessage());}
    }
}
