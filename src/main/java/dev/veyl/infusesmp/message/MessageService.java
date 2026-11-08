package dev.veyl.infusesmp.message;

import dev.veyl.infusesmp.InfuseSMP;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class MessageService {
    private final InfuseSMP plugin;
    private YamlConfiguration messages;
    private final MiniMessage mini=MiniMessage.miniMessage();
    public MessageService(InfuseSMP plugin){this.plugin=plugin;reload();}
    public void reload(){messages=YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(),"messages.yml"));}
    public void send(CommandSender sender,String key){send(sender,key,Map.of());}
    public void send(CommandSender sender,String key,Map<String,String> placeholders){
        String prefix=messages.getString("prefix","");
        String message=messages.getString(key);
        if(message==null||message.isBlank())return;
        for(Map.Entry<String,String> entry:placeholders.entrySet())
            message=message.replace("{"+entry.getKey()+"}",entry.getValue());
        sender.sendMessage(mini.deserialize(prefix+message));
    }
    public void sendList(CommandSender sender,String key){
        var lines=messages.getStringList(key);boolean first=true;
        for(String line:lines){sender.sendMessage(mini.deserialize((first?messages.getString("prefix",""):"")+line));first=false;}
    }
    public String raw(String key,String fallback){return messages.getString(key,fallback);}
}
