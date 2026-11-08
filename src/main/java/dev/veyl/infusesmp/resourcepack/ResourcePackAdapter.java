package dev.veyl.infusesmp.resourcepack;

import dev.veyl.infusesmp.InfuseSMP;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public final class ResourcePackAdapter {
    private static final String RESOURCE = "resourcepack/InfuseSMP-S3-Original.zip.b64";
    private ResourcePackAdapter() {}

    public static Path extract(InfuseSMP plugin) throws IOException {
        Path output=plugin.getDataFolder().toPath().resolve("resourcepack/InfuseSMP-S3-"+safeVersion()+".zip");
        if(Files.exists(output))return output;
        Map<String,byte[]> entries=new LinkedHashMap<>();
        try(InputStream input=plugin.getResource(RESOURCE)){
            if(input==null)throw new FileNotFoundException("Bundled S3 resource pack not found");
            byte[] zip=Base64.getMimeDecoder().decode(input.readAllBytes());
            try(ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(zip))){
                ZipEntry entry;
                while((entry=zin.getNextEntry())!=null){
                    if(!entry.isDirectory())entries.put(entry.getName(),zin.readAllBytes());
                }
            }
        }
        int[] version=parseVersion(Bukkit.getBukkitVersion());
        int format=packFormat(version);
        entries.put("pack.mcmeta",metadata(format).getBytes(StandardCharsets.UTF_8));
        if(isBefore(version,1,21,4)){
            entries.remove("assets/minecraft/items/potion.json");
            entries.remove("assets/minecraft/items/wind_charge.json");
            entries.remove("assets/minecraft/models/wind_charge_base.json");
            entries.remove("assets/minecraft/models/wind_charge_wind.json");
            entries.put("assets/minecraft/models/item/potion.json",legacyPotionModel().getBytes(StandardCharsets.UTF_8));
        }else{
            entries.put("assets/minecraft/items/potion.json",modernPotionModel().getBytes(StandardCharsets.UTF_8));
            if(isBefore(version,1,21,6)){
                entries.remove("assets/minecraft/items/wind_charge.json");
                entries.remove("assets/minecraft/models/wind_charge_base.json");
                entries.remove("assets/minecraft/models/wind_charge_wind.json");
            }
        }
        Files.createDirectories(output.getParent());
        try(ZipOutputStream zout=new ZipOutputStream(Files.newOutputStream(output))){
            for(Map.Entry<String,byte[]> entry:entries.entrySet()){
                zout.putNextEntry(new ZipEntry(entry.getKey()));
                zout.write(entry.getValue());
                zout.closeEntry();
            }
        }
        plugin.getLogger().info("Prepared S3 resource pack for Minecraft "+Bukkit.getBukkitVersion()+" (pack format "+format+").");
        return output;
    }

    private static int[] parseVersion(String value){
        Matcher m=Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(value);
        if(!m.find())return new int[]{1,21,11};
        return new int[]{Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),m.group(3)==null?0:Integer.parseInt(m.group(3))};
    }
    private static boolean isBefore(int[] v,int major,int minor,int patch){
        if(v[0]!=major)return v[0]<major;
        if(v[1]!=minor)return v[1]<minor;
        return v[2]<patch;
    }
    private static int packFormat(int[] v){
        if(v[0]>=26){
            if(v[1]>=3)return 971;
            if(v[1]==2)return 88;
            return 84;
        }
        if(v[1]!=21)return 64;
        if(v[2]<=1)return 34;
        if(v[2]<=3)return 42;
        if(v[2]==4)return 46;
        if(v[2]==5)return 55;
        if(v[2]==6)return 63;
        if(v[2]<=8)return 64;
        if(v[2]<=10)return 69;
        return 75;
    }
    private static String metadata(int format){
        String description="Infuse SMP Season 3 - original textures and sounds";
        if(format<=64)return "{\n  \"pack\": {\n    \"pack_format\": "+format+",\n    \"description\": \""+description+"\"\n  }\n}";
        int major=format,minor=0;
        if(format==971){major=97;minor=1;}
        return "{\n  \"pack\": {\n    \"min_format\": ["+major+", "+minor+"],\n    \"max_format\": ["+major+", "+minor+"],\n    \"description\": \""+description+"\"\n  }\n}";
    }
    private static String modernPotionModel(){
        StringBuilder json=new StringBuilder("{\"model\":{\"type\":\"range_dispatch\",\"property\":\"minecraft:custom_model_data\",\"index\":0,\"scale\":1,\"entries\":[");
        List<Integer> ids=new ArrayList<>();
        for(int i=414001;i<=414013;i++)ids.add(i);
        for(int i=414101;i<=414113;i++)ids.add(i);
        for(int i=0;i<ids.size();i++){
            if(i>0)json.append(",");
            json.append("{\"threshold\":").append(ids.get(i)).append(",\"model\":{\"type\":\"model\",\"model\":\"minecraft:item/potion_custom\",\"tints\":[{\"type\":\"potion\",\"default\":16253176}]}}");
        }
        json.append("],\"fallback\":{\"type\":\"model\",\"model\":\"minecraft:item/potion\",\"tints\":[{\"type\":\"potion\",\"default\":16253176}]}}}");
        return json.toString();
    }
    private static String legacyPotionModel(){
        StringBuilder json=new StringBuilder("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/potion\"},\"overrides\":[");
        List<Integer> ids=new ArrayList<>();
        for(int i=414001;i<=414013;i++)ids.add(i);
        for(int i=414101;i<=414113;i++)ids.add(i);
        for(int i=0;i<ids.size();i++){
            if(i>0)json.append(",");
            json.append("{\"predicate\":{\"custom_model_data\":").append(ids.get(i)).append("},\"model\":\"minecraft:item/potion_custom\"}");
        }
        return json.append("]}").toString();
    }
    private static String safeVersion(){return Bukkit.getBukkitVersion().replaceAll("[^A-Za-z0-9._-]","_");}
}
