package dev.veyl.infusesmp.command;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.data.PlayerData;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class InfuseCommand implements CommandExecutor, TabCompleter {
    private final InfuseSMP plugin; private final EffectService effects;
    public InfuseCommand(InfuseSMP plugin,EffectService effects){this.plugin=plugin;this.effects=effects;}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        String root=command.getName().toLowerCase(Locale.ROOT);
        if(root.equals("activate"))return activate(sender,args);
        if(root.equals("drain"))return drain(sender,args);
        if(root.equals("swap")){
            if(!(sender instanceof Player p))return true;
            effects.data(p).swap();effects.applyPassive(p);p.sendMessage(ChatColor.GREEN+"Your Infuse slots have been swapped.");return true;
        }
        if(root.equals("trust"))return trust(sender,args);
        if(root.equals("recipes")){
            if(sender instanceof Player player)plugin.menus().openEffects(player); else sender.sendMessage(ChatColor.GREEN+"S3 shaped recipes are registered at brewing stands. Use /infuse gui in-game to view them.");
            return true;
        }
        return infuse(sender,args);
    }
    private boolean drain(CommandSender sender,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Players only.");return true;}
        if(args.length!=1||(!args[0].equalsIgnoreCase("primary")&&!args[0].equalsIgnoreCase("secondary"))){
            sender.sendMessage(ChatColor.RED+"Usage: /drain <primary|secondary>");return true;
        }
        if(!effects.drain(player,args[0]))sender.sendMessage(ChatColor.RED+"Cannot drain that slot. It may be empty or your inventory may be full.");
        return true;
    }
    private boolean activate(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return true;}
        if(args.length!=1){sender.sendMessage(ChatColor.RED+"Usage: /activate <primary|secondary>");return true;}
        boolean primary=args[0].equalsIgnoreCase("primary")||args[0].equalsIgnoreCase("p");
        if(!primary&&!args[0].equalsIgnoreCase("secondary")&&!args[0].equalsIgnoreCase("s")){sender.sendMessage(ChatColor.RED+"Choose primary or secondary.");return true;}
        if(!effects.activate(p,primary))sender.sendMessage(ChatColor.RED+"No effect equipped in that slot or spark unavailable.");
        return true;
    }
    private boolean infuse(CommandSender sender,String[] args){
        if(args.length==0||args[0].equalsIgnoreCase("help")){
            sender.sendMessage(ChatColor.GREEN+"InfuseSMP S3");
            sender.sendMessage(ChatColor.GRAY+"/infuse gui | effects | get [player] | add <player> <slot> <effect> [augmented]");
            sender.sendMessage(ChatColor.GRAY+"/infuse remove <player> <slot> | give <player> <effect> [augmented] | reload");
            return true;
        }
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "effects" -> sender.sendMessage(ChatColor.GREEN+"Effects: "+String.join(", ",Arrays.stream(InfuseEffect.values()).map(InfuseEffect::id).toList()));
            case "gui" -> { if(sender instanceof Player player)plugin.menus().openEffects(player); else sender.sendMessage("Players only."); }
            case "reload" -> {
                if(!sender.hasPermission("infusesmp.reload"))return deny(sender);
                plugin.reloadConfig();sender.sendMessage(ChatColor.GREEN+"InfuseSMP S3 configuration reloaded.");
            }
            case "get" -> {
                Player target=args.length>1?Bukkit.getPlayerExact(args[1]):sender instanceof Player p?p:null;
                if(target==null){sender.sendMessage(ChatColor.RED+"Player not found. Specify an online player.");return true;}
                PlayerData d=effects.data(target);
                sender.sendMessage(ChatColor.GREEN+target.getName()+" — Primary: "+value(d.primary())+" | Secondary: "+value(d.secondary()));
            }
            case "add" -> {
                if(!sender.hasPermission("infusesmp.manage"))return deny(sender);
                if(args.length<4){sender.sendMessage(ChatColor.RED+"/infuse add <player> <primary|secondary> <effect> [augmented]");return true;}
                Player target=Bukkit.getPlayerExact(args[1]);InfuseEffect effect=InfuseEffect.fromId(args[3]);
                if(target==null||effect==null||(!args[2].equalsIgnoreCase("primary")&&!args[2].equalsIgnoreCase("secondary"))){sender.sendMessage(ChatColor.RED+"Invalid player, slot or effect.");return true;}
                boolean augmented=args.length>4&&Boolean.parseBoolean(args[4]);
                effects.equip(target,args[2],effect,augmented);sender.sendMessage(ChatColor.GREEN+"Equipped "+effect.id()+" on "+target.getName());
            }
            case "remove" -> {
                if(!sender.hasPermission("infusesmp.manage"))return deny(sender);
                if(args.length<3){sender.sendMessage(ChatColor.RED+"/infuse remove <player> <primary|secondary>");return true;}
                Player target=Bukkit.getPlayerExact(args[1]);
                if(target==null||(!args[2].equalsIgnoreCase("primary")&&!args[2].equalsIgnoreCase("secondary"))){sender.sendMessage(ChatColor.RED+"Invalid player or slot.");return true;}
                effects.remove(target,args[2]);sender.sendMessage(ChatColor.GREEN+"Effect removed.");
            }
            case "give" -> {
                if(!sender.hasPermission("infusesmp.give"))return deny(sender);
                if(args.length<3){sender.sendMessage(ChatColor.RED+"/infuse give <player> <effect> [augmented]");return true;}
                Player target=Bukkit.getPlayerExact(args[1]);InfuseEffect effect=InfuseEffect.fromId(args[2]);
                if(target==null||effect==null){sender.sendMessage(ChatColor.RED+"Invalid player or effect.");return true;}
                target.getInventory().addItem(effects.createEffectItem(effect,args.length>3&&Boolean.parseBoolean(args[3])));
                sender.sendMessage(ChatColor.GREEN+"Given "+effect.id()+" to "+target.getName());
            }
            default -> sender.sendMessage(ChatColor.RED+"Unknown subcommand. Use /infuse help.");
        }
        return true;
    }
    private boolean trust(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return true;}
        PlayerData d=effects.data(p);
        if(args.length==0||args[0].equalsIgnoreCase("list")){
            List<String> names=new ArrayList<>();for(UUID id:d.trusted()){String n=Bukkit.getOfflinePlayer(id).getName();names.add(n==null?id.toString():n);}
            p.sendMessage(ChatColor.GREEN+"Trusted: "+(names.isEmpty()?"none":String.join(", ",names)));return true;
        }
        if(args[0].equalsIgnoreCase("clear")){d.trusted().clear();p.sendMessage(ChatColor.YELLOW+"Trust list cleared.");return true;}
        if(args.length<2){p.sendMessage(ChatColor.RED+"/trust <add|remove> <player>");return true;}
        Player target=Bukkit.getPlayerExact(args[1]);if(target==null){p.sendMessage(ChatColor.RED+"Player must be online.");return true;}
        if(args[0].equalsIgnoreCase("add")){
            int max=plugin.getConfig().getInt("settings.trust-list.maximum",3);
            if(d.trusted().size()>=max){p.sendMessage(ChatColor.RED+"Trust list is full.");return true;}
            d.trusted().add(target.getUniqueId());p.sendMessage(ChatColor.GREEN+"Trusted "+target.getName());
        }else if(args[0].equalsIgnoreCase("remove")){
            d.trusted().remove(target.getUniqueId());p.sendMessage(ChatColor.YELLOW+"Untrusted "+target.getName());
        }
        return true;
    }
    private boolean deny(CommandSender s){s.sendMessage(ChatColor.RED+"You do not have permission.");return true;}
    private String value(String s){return s==null?"None":s;}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(c.getName().equalsIgnoreCase("activate"))return args.length==1?List.of("primary","secondary"):List.of();
        if(c.getName().equalsIgnoreCase("trust"))return args.length==1?List.of("add","remove","list","clear"):args.length==2?Bukkit.getOnlinePlayers().stream().map(Player::getName).toList():List.of();
        if(c.getName().equalsIgnoreCase("infuse")){
            if(args.length==1)return List.of("help","gui","effects","get","add","remove","give","reload");
            if(args.length==2&&List.of("get","add","remove","give").contains(args[0]))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            if(args.length==3&&List.of("add","remove").contains(args[0]))return List.of("primary","secondary");
            if(args.length==4&&args[0].equalsIgnoreCase("add"))return Arrays.stream(InfuseEffect.values()).map(InfuseEffect::id).toList();
        }return List.of();
    }
}
