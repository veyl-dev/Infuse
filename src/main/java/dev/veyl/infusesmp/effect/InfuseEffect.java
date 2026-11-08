package dev.veyl.infusesmp.effect;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public enum InfuseEffect {
    EMERALD("emerald", "Emerald", Material.EMERALD, 414101, 30, PotionEffectType.LUCK),
    FEATHER("feather", "Feather", Material.FEATHER, 414102, 40, PotionEffectType.SLOW_FALLING),
    FIRE("fire", "Fire", Material.BLAZE_POWDER, 414103, 90, PotionEffectType.FIRE_RESISTANCE),
    FROST("frost", "Frost", Material.PACKED_ICE, 414104, 60, null),
    HASTE("haste", "Haste", Material.GOLDEN_PICKAXE, 414105, 60, PotionEffectType.HASTE),
    HEART("heart", "Heart", Material.GHAST_TEAR, 414106, 120, PotionEffectType.HEALTH_BOOST),
    INVISIBILITY("invisibility", "Invisibility", Material.FERMENTED_SPIDER_EYE, 414107, 90, PotionEffectType.INVISIBILITY),
    OCEAN("ocean", "Ocean", Material.HEART_OF_THE_SEA, 414108, 90, PotionEffectType.DOLPHINS_GRACE),
    REGENERATION("regeneration", "Regeneration", Material.GHAST_TEAR, 414109, 60, PotionEffectType.REGENERATION),
    SPEED("speed", "Speed", Material.SUGAR, 414110, 20, PotionEffectType.SPEED),
    STRENGTH("strength", "Strength", Material.BLAZE_ROD, 414111, 60, PotionEffectType.STRENGTH),
    THUNDER("thunder", "Thunder", Material.TRIDENT, 414112, 90, PotionEffectType.CONDUIT_POWER),
    ENDER("ender", "Ender", Material.ENDER_EYE, 414113, 30, PotionEffectType.DARKNESS);

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
