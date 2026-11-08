package dev.veyl.infusesmp.ritual;

import dev.veyl.infusesmp.InfuseSMP;
import dev.veyl.infusesmp.effect.EffectService;
import dev.veyl.infusesmp.effect.InfuseEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class RitualService implements Listener {
    private final InfuseSMP plugin;
    private final EffectService effects;
    private Location stand;
    private InfuseEffect effect;
    private BossBar bar;
    private BukkitTask task;
    private EnderCrystal crystal;
    private long startedAt;
    private long durationTicks;

    public RitualService(InfuseSMP plugin,EffectService effects){this.plugin=plugin;this.effects=effects;}

    public boolean isActive(){return task!=null&&!task.isCancelled();}
    public boolean isAt(Location location){
        return isActive()&&stand!=null&&location!=null&&stand.getWorld().equals(location.getWorld())
                &&stand.getBlockX()==location.getBlockX()&&stand.getBlockY()==location.getBlockY()&&stand.getBlockZ()==location.getBlockZ();
    }
    public boolean start(Player player,InfuseEffect effect,Location location){
        if(isActive()||location==null||location.getBlock().getType()!=Material.BREWING_STAND)return false;
        this.stand=location.clone();this.effect=effect;
        long defaultSeconds=effect.id().equals("ender")?plugin.getConfig().getLong("rituals.ender-duration-seconds",3600):plugin.getConfig().getLong("rituals.duration-seconds",600);
        long seconds=Math.max(1,defaultSeconds);
        this.durationTicks=seconds*20L;this.startedAt=System.currentTimeMillis();
        this.bar=Bukkit.createBossBar("InfuseSMP S3 • "+effect.displayName()+" ritual",BarColor.PURPLE,BarStyle.SOLID);
        for(Player online:Bukkit.getOnlinePlayers())bar.addPlayer(online);
        if(plugin.getConfig().getBoolean("rituals.beacon",true)){
            Location beam=stand.clone().add(.5,-100,.5);crystal=stand.getWorld().spawn(beam,EnderCrystal.class);
            crystal.setShowingBottom(false);crystal.setInvulnerable(true);crystal.setInvisible(true);crystal.setPersistent(false);
            crystal.setBeamTarget(stand.clone().add(.5,500,.5));
        }
        Bukkit.broadcastMessage("§d"+player.getName()+" started an augmented "+effect.displayName()+" ritual at "
                +stand.getBlockX()+", "+stand.getBlockY()+", "+stand.getBlockZ()+".");
        this.task=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            if(stand.getBlock().getType()!=Material.BREWING_STAND){
                Bukkit.broadcastMessage("§cThe "+effect.displayName()+" ritual was interrupted because its brewing stand was destroyed.");
                stop();return;
            }
            long elapsed=(System.currentTimeMillis()-startedAt)/50L;
            double progress=Math.max(0.0,1.0-(double)elapsed/durationTicks);
            bar.setProgress(progress);
            if(progress<=0){
                Location drop=stand.clone().add(.5,1,.5);
                drop.getWorld().dropItemNaturally(drop,effects.createEffectItem(effect,true));
                Bukkit.broadcastMessage("§aThe augmented "+effect.displayName()+" ritual is complete!");
                stop();
            }
        },20L,20L);
        return true;
    }
    public void stop(){
        if(task!=null){task.cancel();task=null;}
        if(bar!=null){bar.removeAll();bar=null;}
        if(crystal!=null){crystal.remove();crystal=null;}
        stand=null;effect=null;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event){if(isActive()&&bar!=null)bar.addPlayer(event.getPlayer());}
    @EventHandler(ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event){
        if(isAt(event.getBlock().getLocation())&&plugin.getConfig().getBoolean("rituals.protect-brewing-stand",true)){event.setCancelled(true);event.getPlayer().sendMessage("§cThis brewing stand is protected by an active Infuse ritual.");}
    }
    @EventHandler(ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event){
        if(event.getClickedBlock()!=null&&isAt(event.getClickedBlock().getLocation())){
            event.setCancelled(true);event.getPlayer().sendMessage("§eAn augmented ritual is already running here.");
        }
    }
    @EventHandler(ignoreCancelled=true)
    public void onBlockExplode(BlockExplodeEvent event){
        if(isAt(event.getBlock().getLocation())&&plugin.getConfig().getBoolean("rituals.protect-brewing-stand",true))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event){
        if(stand!=null&&event.blockList().stream().anyMatch(b->isAt(b.getLocation())))event.blockList().removeIf(b->isAt(b.getLocation()));
    }
}
