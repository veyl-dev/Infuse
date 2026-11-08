package dev.veyl.infusesmp.effect;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public enum InfuseEffect {
    EMERALD("emerald", "Emerald", Material.EMERALD, 414001, 30, PotionEffectType.LUCK),
    FEATHER("feather", "Feather", Material.FEATHER, 414002, 40, PotionEffectType.SLOW_FALLING),
    FIRE("fire", "Fire", Material.BLAZE_POWDER, 414003, 90, PotionEffectType.FIRE_RESISTANCE),
    FROST("frost", "Frost", Material.PACKED_ICE, 414004, 60, null),
    HASTE("haste", "Haste", Material.GOLDEN_PICKAXE, 414006, 60, PotionEffectType.HASTE),
    HEART("heart", "Heart", Material.GHAST_TEAR, 414007, 120, PotionEffectType.HEALTH_BOOST),
    INVISIBILITY("invisibility", "Invisibility", Material.FERMENTED_SPIDER_EYE, 414008, 90, PotionEffectType.INVISIBILITY),
    OCEAN("ocean", "Ocean", Material.HEART_OF_THE_SEA, 414009, 90, PotionEffectType.DOLPHINS_GRACE),
    REGENERATION("regeneration", "Regeneration", Material.GHAST_TEAR, 414010, 60, PotionEffectType.REGENERATION),
    SPEED("speed", "Speed", Material.SUGAR, 414011, 20, PotionEffectType.SPEED),
    STRENGTH("strength", "Strength", Material.BLAZE_ROD, 414012, 60, PotionEffectType.STRENGTH),
    THUNDER("thunder", "Thunder", Material.TRIDENT, 414005, 90, PotionEffectType.CONDUIT_POWER),
    ENDER("ender", "Ender", Material.ENDER_EYE, 414013, 30, PotionEffectType.DARKNESS);

    private final String id, displayName;
    private final Material icon;
    private final int modelData, cooldownSeconds;
    private final PotionEffectType passive;
    InfuseEffect(String id, String displayName, Material icon, int modelData, int cooldownSeconds, PotionEffectType passive) {
        this.id=id; this.displayName=displayName; this.icon=icon; this.modelData=modelData;
        this.cooldownSeconds=cooldownSeconds; this.passive=passive;
    }
    public String id(){return id;}
    public String displayName(){return displayName;}
    public Material icon(){return icon;}
    public int modelData(){return modelData;}
    public int cooldownSeconds(){return cooldownSeconds;}
    public PotionEffectType passive(){return passive;}
    public static InfuseEffect fromId(String id) {
        if (id == null) return null;
        for (InfuseEffect effect : values()) if (effect.id.equalsIgnoreCase(id)) return effect;
        return null;
    }
}
