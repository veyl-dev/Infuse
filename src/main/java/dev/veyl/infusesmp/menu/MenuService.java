package dev.veyl.infusesmp.menu;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.crafting.RecipeService;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class MenuService implements Listener {
    private final InfuseSMP plugin;
    private final EffectService effects;
    private final NamespacedKey menuKey;
    public MenuService(InfuseSMP plugin,EffectService effects){
        this.plugin=plugin;this.effects=effects;this.menuKey=new NamespacedKey(plugin,"menu_effect");
    }
    public void openEffects(Player player){
        MenuHolder holder=new MenuHolder();
        Inventory inv=Bukkit.createInventory(holder,54,ChatColor.DARK_AQUA+"InfuseSMP S3 • Effects");
        holder.inventory=inv;
        ItemStack filler=plain(Material.GRAY_STAINED_GLASS_PANE," ");
        for(int i=0;i<54;i++)inv.setItem(i,filler);
        int[] slots={10,12,14,16,19,21,23,25,28,30,32,34,40};
        InfuseEffect[] all=InfuseEffect.values();
        for(int i=0;i<Math.min(slots.length,all.length);i++){
            ItemStack item=effects.createEffectItem(all[i],false);
            ItemMeta meta=item.getItemMeta();meta.getPersistentDataContainer().set(menuKey,PersistentDataType.STRING,all[i].id());
            meta.setLore(List.of(ChatColor.GRAY+"Click to view the original S3 recipe",ChatColor.DARK_GRAY+"Regular and augmented variants"));
            item.setItemMeta(meta);inv.setItem(slots[i],item);
        }
        inv.setItem(49,plain(Material.BARRIER,ChatColor.RED+"Close"));
        player.openInventory(inv);
    }
    private void openRecipe(Player player,InfuseEffect effect){
        MenuHolder holder=new MenuHolder();Inventory inv=Bukkit.createInventory(holder,45,ChatColor.DARK_AQUA+"Recipe • "+effect.displayName());holder.inventory=inv;
        ItemStack filler=plain(Material.BLACK_STAINED_GLASS_PANE," ");for(int i=0;i<45;i++)inv.setItem(i,filler);
        String[] materials=RecipeService.recipeIngredients(effect.id());int[] slots={10,11,12,19,20,21,28,29,30};
        if(materials!=null)for(int i=0;i<9;i++){
            Material material=Material.matchMaterial(materials[i]);
            if(material!=null)inv.setItem(slots[i],new ItemStack(material));
        }
        inv.setItem(24,effects.createEffectItem(effect,false));
        inv.setItem(40,plain(Material.ARROW,ChatColor.YELLOW+"Back to effects"));
        inv.setItem(44,plain(Material.BARRIER,ChatColor.RED+"Close"));
        player.openInventory(inv);
    }
    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof MenuHolder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player))return;
        ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;
        String title=event.getView().getTitle();
        if(title.contains("Effects")){
            String id=clicked.getItemMeta().getPersistentDataContainer().get(menuKey,PersistentDataType.STRING);
            if(id==null){if(clicked.getType()==Material.BARRIER)player.closeInventory();return;}
            InfuseEffect effect=InfuseEffect.fromId(id);if(effect!=null)openRecipe(player,effect);
        }else{
            if(clicked.getType()==Material.ARROW)openEffects(player);
            else if(clicked.getType()==Material.BARRIER)player.closeInventory();
        }
    }
    private ItemStack plain(Material type,String name){
        ItemStack item=new ItemStack(type);ItemMeta meta=item.getItemMeta();meta.setDisplayName(name);item.setItemMeta(meta);return item;
    }
    private static final class MenuHolder implements InventoryHolder{
        private Inventory inventory;
        @Override public Inventory getInventory(){return inventory;}
    }
}
