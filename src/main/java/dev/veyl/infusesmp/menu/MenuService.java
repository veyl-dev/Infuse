package dev.veyl.infusesmp.menu;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.crafting.RecipeService;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.List;

public final class MenuService implements Listener {
    private final InfuseSMP plugin;
    private final EffectService effects;
    private final RecipeService recipes;
    private final NamespacedKey menuKey;
    private YamlConfiguration gui;
    public MenuService(InfuseSMP plugin,EffectService effects,RecipeService recipes){
        this.plugin=plugin;this.effects=effects;this.recipes=recipes;this.menuKey=new NamespacedKey(plugin,"menu_effect");
        reload();
    }
    public void reload(){gui=YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(),"gui.yml"));}
    public void openEffects(Player player){
        MenuHolder holder=new MenuHolder();
        int rows=Math.max(1,Math.min(6,gui.getInt("effects.rows",6)));
        Inventory inv=Bukkit.createInventory(holder,rows*9,color(gui.getString("effects.title","&3InfuseSMP S3 • Effects")));
        holder.inventory=inv;
        ItemStack filler=plain(material("effects.filler",Material.GRAY_STAINED_GLASS_PANE)," ");
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,filler);
        List<Integer> slots=gui.getIntegerList("effects.slots");
        InfuseEffect[] all=InfuseEffect.values();
        for(int i=0;i<Math.min(slots.size(),all.length);i++){
            int slot=slots.get(i);if(slot<0||slot>=inv.getSize())continue;
            ItemStack item=effects.createEffectItem(all[i],false);
            ItemMeta meta=item.getItemMeta();meta.getPersistentDataContainer().set(menuKey,PersistentDataType.STRING,all[i].id());
            meta.setLore(List.of(ChatColor.GRAY+"Click to view the original S3 recipe",ChatColor.DARK_GRAY+"Regular and augmented variants"));
            item.setItemMeta(meta);inv.setItem(slot,item);
        }
        set(inv,"effects.close-slot",49,plain(Material.BARRIER,ChatColor.RED+"Close"));
        player.openInventory(inv);
    }
    private void openRecipe(Player player,InfuseEffect effect){
        MenuHolder holder=new MenuHolder();
        int rows=Math.max(1,Math.min(6,gui.getInt("recipe.rows",5)));
        Inventory inv=Bukkit.createInventory(holder,rows*9,color(gui.getString("recipe.title","&3Recipe • {effect}").replace("{effect}",effect.displayName())));
        holder.inventory=inv;
        ItemStack filler=plain(material("recipe.filler",Material.BLACK_STAINED_GLASS_PANE)," ");
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,filler);
        String[] materials=recipes.recipeIngredients(effect.id());
        List<Integer> slots=gui.getIntegerList("recipe.ingredient-slots");
        if(materials!=null)for(int i=0;i<Math.min(9,slots.size());i++){
            int slot=slots.get(i);if(slot<0||slot>=inv.getSize())continue;
            Material mat=Material.matchMaterial(materials[i]);if(mat!=null)inv.setItem(slot,new ItemStack(mat));
        }
        set(inv,"recipe.output-slot",24,effects.createEffectItem(effect,false));
        set(inv,"recipe.back-slot",40,plain(Material.ARROW,ChatColor.YELLOW+"Back to effects"));
        set(inv,"recipe.close-slot",44,plain(Material.BARRIER,ChatColor.RED+"Close"));
        player.openInventory(inv);
    }
    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof MenuHolder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player))return;
        ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;
        String title=event.getView().getTitle();
        if(title.toLowerCase().contains("effects")){
            String id=clicked.getItemMeta().getPersistentDataContainer().get(menuKey,PersistentDataType.STRING);
            if(id==null){if(clicked.getType()==Material.BARRIER)player.closeInventory();return;}
            InfuseEffect effect=InfuseEffect.fromId(id);if(effect!=null)openRecipe(player,effect);
        }else{
            if(clicked.getType()==Material.ARROW)openEffects(player);
            else if(clicked.getType()==Material.BARRIER)player.closeInventory();
        }
    }
    private void set(Inventory inv,String path,int fallback,ItemStack item){int slot=gui.getInt(path,fallback);if(slot>=0&&slot<inv.getSize())inv.setItem(slot,item);}
    private Material material(String path,Material fallback){Material value=Material.matchMaterial(gui.getString(path,""));return value==null?fallback:value;}
    private String color(String value){return ChatColor.translateAlternateColorCodes('&',value);}
    private ItemStack plain(Material type,String name){ItemStack item=new ItemStack(type);ItemMeta meta=item.getItemMeta();meta.setDisplayName(name);item.setItemMeta(meta);return item;}
    private static final class MenuHolder implements InventoryHolder{
        private Inventory inventory;
        @Override public Inventory getInventory(){return inventory;}
    }
}
