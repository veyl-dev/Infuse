package dev.veyl.infusesmp.effect;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public enum InfuseEffect {
    EMERALD("emerald","Emerald",Material.EMERALD,414001,30,PotionEffectType.LUCK,Color.fromRGB(13,252,0)),
    FEATHER("feather","Feather",Material.FEATHER,414002,40,PotionEffectType.SLOW_FALLING,Color.fromRGB(190,163,202)),
    FIRE("fire","Fire",Material.BLAZE_POWDER,414003,90,PotionEffectType.FIRE_RESISTANCE,Color.fromRGB(252,120,3)),
    FROST("frost","Frost",Material.PACKED_ICE,414004,60,null,Color.fromRGB(122,227,252)),
    THUNDER("thunder","Thunder",Material.TRIDENT,414005,90,PotionEffectType.CONDUIT_POWER,Color.fromRGB(252,237,0)),
    HASTE("haste","Haste",Material.GOLDEN_PICKAXE,414006,60,PotionEffectType.HASTE,Color.fromRGB(185,108,0)),
    HEART("heart","Heart",Material.GHAST_TEAR,414007,120,PotionEffectType.HEALTH_BOOST,Color.fromRGB(252,0,70)),
    INVISIBILITY("invisibility","Invisibility",Material.FERMENTED_SPIDER_EYE,414008,90,PotionEffectType.INVISIBILITY,Color.fromRGB(126,0,252)),
    OCEAN("ocean","Ocean",Material.HEART_OF_THE_SEA,414009,90,PotionEffectType.DOLPHINS_GRACE,Color.fromRGB(0,90,252)),
    REGENERATION("regeneration","Regeneration",Material.GHAST_TEAR,414010,60,PotionEffectType.REGENERATION,Color.fromRGB(252,0,221)),
    SPEED("speed","Speed",Material.SUGAR,414011,20,PotionEffectType.SPEED,Color.fromRGB(209,164,75)),
    STRENGTH("strength","Strength",Material.BLAZE_ROD,414012,60,PotionEffectType.STRENGTH,Color.fromRGB(158,32,32)),
    ENDER("ender","Ender",Material.ENDER_EYE,414013,30,PotionEffectType.DARKNESS,Color.fromRGB(131,17,119));

    private final String id,displayName;
    private final Material icon;
    private final int modelData,cooldownSeconds;
    private final PotionEffectType passive;
    private final Color color;
    InfuseEffect(String id,String displayName,Material icon,int modelData,int cooldownSeconds,PotionEffectType passive,Color color){
        this.id=id;this.displayName=displayName;this.icon=icon;this.modelData=modelData;
        this.cooldownSeconds=cooldownSeconds;this.passive=passive;this.color=color;
    }
    public String id(){return id;} public String displayName(){return displayName;}
    public Material icon(){return icon;} public int modelData(){return modelData;}
    public int cooldownSeconds(){return cooldownSeconds;} public PotionEffectType passive(){return passive;}
    public Color color(){return color;}
    public static InfuseEffect fromId(String id){
        if(id==null)return null;
        for(InfuseEffect effect:values())if(effect.id.equalsIgnoreCase(id))return effect;
        return null;
    }
}
