package dev.veyl.infusesmp.data;

import dev.veyl.infusesmp.InfuseSMP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerDataStore {
    private final InfuseSMP plugin;
    private final Map<UUID,PlayerData> players=new HashMap<>();
    private final File file;
    public PlayerDataStore(InfuseSMP plugin){this.plugin=plugin;this.file=new File(plugin.getDataFolder(),"players.yml");}
    public PlayerData get(UUID uuid){return players.computeIfAbsent(uuid,k->new PlayerData());}
    public void load(){
        if(!file.exists())return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root=yaml.getConfigurationSection("players"); if(root==null)return;
        for(String key:root.getKeys(false)){
            UUID uuid;try{uuid=UUID.fromString(key);}catch(IllegalArgumentException ex){continue;}
            ConfigurationSection s=root.getConfigurationSection(key);if(s==null)continue;
            PlayerData d=new PlayerData();d.primary(s.getString("primary"),s.getBoolean("primary-augmented"));
            d.secondary(s.getString("secondary"),s.getBoolean("secondary-augmented"));
            for(String value:s.getStringList("trusted"))try{d.trusted().add(UUID.fromString(value));}catch(IllegalArgumentException ignored){}
            ConfigurationSection c=s.getConfigurationSection("cooldowns");
            if(c!=null)for(String id:c.getKeys(false))d.cooldowns().put(id,c.getLong(id));
            for(String line:s.getStringList("history"))d.addHistory(line);
            players.put(uuid,d);
        }
    }
    public void save(){
        YamlConfiguration yaml=new YamlConfiguration();
        for(Map.Entry<UUID,PlayerData> e:players.entrySet()){
            String p="players."+e.getKey();PlayerData d=e.getValue();
            yaml.set(p+".primary",d.primary());yaml.set(p+".secondary",d.secondary());
            yaml.set(p+".primary-augmented",d.primaryAugmented());yaml.set(p+".secondary-augmented",d.secondaryAugmented());
            yaml.set(p+".trusted",d.trusted().stream().map(UUID::toString).toList());
            yaml.set(p+".cooldowns",d.cooldowns());yaml.set(p+".history",new ArrayList<>(d.history()));
        }
        try{if(!plugin.getDataFolder().exists())plugin.getDataFolder().mkdirs();yaml.save(file);}
        catch(IOException ex){plugin.getLogger().severe("Could not save player data: "+ex.getMessage());}
    }
}
