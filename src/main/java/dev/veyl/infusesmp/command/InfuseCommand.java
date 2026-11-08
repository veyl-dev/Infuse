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
            if(plugin.getConfig().getBoolean("settings.lock-effect-slots",false)){p.sendMessage(ChatColor.RED+"Your effect slots are locked.");return true;}effects.data(p).swap();effects.applyPassive(p);plugin.messages().send(p,"swapped");return true;
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
        if(!effects.drain(player,args[0]))plugin.messages().send(sender,"drain-failed");
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
            plugin.messages().sendList(sender,"help");
            return true;
        }
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "effects" -> sender.sendMessage(ChatColor.GREEN+"Effects: "+String.join(", ",Arrays.stream(InfuseEffect.values()).map(InfuseEffect::id).toList()));
            case "controls" -> { if(sender instanceof Player p){PlayerData d=effects.data(p);p.sendMessage(ChatColor.GREEN+"Primary: "+d.primaryControl()+" | Secondary: "+d.secondaryControl());p.sendMessage(ChatColor.GRAY+"Use /infuse bind <primary|secondary> <swap|drop> to change controls.");} }
            case "bind" -> bind(sender,args);
            case "gui" -> { if(sender instanceof Player player)plugin.menus().openEffects(player); else sender.sendMessage("Players only."); }
            case "reload" -> {
                if(!sender.hasPermission("infusesmp.reload"))return deny(sender);
                plugin.reloadConfig();plugin.messages().reload();plugin.menus().reload();plugin.recipes().reload();plugin.messages().send(sender,"reloaded");
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
    private boolean bind(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return true;}
        if(args.length!=3||(!args[1].equalsIgnoreCase("primary")&&!args[1].equalsIgnoreCase("secondary"))
                ||(!args[2].equalsIgnoreCase("swap")&&!args[2].equalsIgnoreCase("drop"))){
            p.sendMessage(ChatColor.RED+"Usage: /infuse bind <primary|secondary> <swap|drop>");return true;
        }
        effects.data(p).control(args[1].equalsIgnoreCase("primary"),args[2].toLowerCase(Locale.ROOT));
        plugin.messages().send(p,"bind-set",Map.of("slot",args[1],"control",args[2]));
        return true;
    }
    private boolean trust(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return true;}
        PlayerData d=effects.data(p);
        if(args.length==0||args[0].equalsIgnoreCase("list")){
            List<String> names=new ArrayList<>();for(UUID id:d.trusted()){String n=Bukkit.getOfflinePlayer(id).getName();names.add(n==null?id.toString():n);}
            p.sendMessage(ChatColor.GREEN+"Trusted: "+(names.isEmpty()?"none":String.join(", ",names)));return true;
        }
        if(args[0].equalsIgnoreCase("clear")){
            if(!trustDelayReady(p,d))return true;
            d.trusted().clear();d.lastTrustAction(System.currentTimeMillis());plugin.messages().send(p,"trust-cleared");return true;
        }
        if(args.length<2){p.sendMessage(ChatColor.RED+"/trust <add|remove> <player>");return true;}
        if(!args[0].equalsIgnoreCase("add")&&!args[0].equalsIgnoreCase("remove")){p.sendMessage(ChatColor.RED+"Choose add or remove.");return true;}
        if(!trustDelayReady(p,d))return true;
        Player target=Bukkit.getPlayerExact(args[1]);if(target==null){p.sendMessage(ChatColor.RED+"Player must be online.");return true;}
        if(target.equals(p)){p.sendMessage(ChatColor.RED+"You cannot trust yourself.");return true;}
        if(args[0].equalsIgnoreCase("add")){
            int max=plugin.getConfig().getInt("settings.trust-list.maximum",3);
            if(d.trusted().contains(target.getUniqueId())){p.sendMessage(ChatColor.YELLOW+"That player is already trusted.");return true;}
            if(d.trusted().size()>=max){plugin.messages().send(p,"trust-full");return true;}
            d.trusted().add(target.getUniqueId());plugin.messages().send(p,"trust-added",Map.of("player",target.getName()));
        }else{
            if(!d.trusted().remove(target.getUniqueId())){p.sendMessage(ChatColor.YELLOW+"That player is not on your trust list.");return true;}
            plugin.messages().send(p,"trust-removed",Map.of("player",target.getName()));
        }
        d.lastTrustAction(System.currentTimeMillis());
        return true;
    }
    private boolean trustDelayReady(Player player,PlayerData data){
        long delay=plugin.getConfig().getLong("settings.trust-list.action-delay-seconds",10)*1000L;
        long remaining=delay-(System.currentTimeMillis()-data.lastTrustAction());
        if(remaining>0){player.sendMessage(ChatColor.RED+"Wait "+((remaining+999)/1000)+"s before changing your trust list again.");return false;}
        return true;
    }
    private boolean deny(CommandSender s){plugin.messages().send(s,"no-permission");return true;}
    private String value(String s){return s==null?"None":s;}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(c.getName().equalsIgnoreCase("activate"))return args.length==1?List.of("primary","secondary"):List.of();
        if(c.getName().equalsIgnoreCase("trust"))return args.length==1?List.of("add","remove","list","clear"):args.length==2?Bukkit.getOnlinePlayers().stream().map(Player::getName).toList():List.of();
        if(c.getName().equalsIgnoreCase("infuse")){
            if(args.length==1)return List.of("help","gui","effects","controls","bind","get","add","remove","give","reload");
            if(args.length==2&&args[0].equalsIgnoreCase("bind"))return List.of("primary","secondary");
            if(args.length==3&&args[0].equalsIgnoreCase("bind"))return List.of("swap","drop");
            if(args.length==2&&List.of("get","add","remove","give").contains(args[0]))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            if(args.length==3&&List.of("add","remove").contains(args[0]))return List.of("primary","secondary");
            if(args.length==4&&args[0].equalsIgnoreCase("add"))return Arrays.stream(InfuseEffect.values()).map(InfuseEffect::id).toList();
        }return List.of();
    }
}
