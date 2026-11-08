package dev.veyl.infusesmp.data;

import java.util.*;

public final class PlayerData {
    private String primary, secondary;
    private boolean primaryAugmented, secondaryAugmented;
    private final Set<UUID> trusted = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Deque<String> history = new ArrayDeque<>();
    private int chargePrimary, chargeSecondary;
    public String primary(){return primary;} public String secondary(){return secondary;}
    public boolean primaryAugmented(){return primaryAugmented;} public boolean secondaryAugmented(){return secondaryAugmented;}
    public Set<UUID> trusted(){return trusted;} public Map<String,Long> cooldowns(){return cooldowns;}
    public Deque<String> history(){return history;}
    public int chargePrimary(){return chargePrimary;} public int chargeSecondary(){return chargeSecondary;}
    public void primary(String id, boolean aug){primary=id;primaryAugmented=aug;}
    public void secondary(String id, boolean aug){secondary=id;secondaryAugmented=aug;}
    public void charge(boolean primarySlot,int value){if(primarySlot)chargePrimary=value;else chargeSecondary=value;}
    public void swap(){
        String id=primary;primary=secondary;secondary=id;
        boolean aug=primaryAugmented;primaryAugmented=secondaryAugmented;secondaryAugmented=aug;
        int charge=chargePrimary;chargePrimary=chargeSecondary;chargeSecondary=charge;
    }
    public void addHistory(String line){
        history.addFirst(System.currentTimeMillis()+"|"+line);
        while(history.size()>30)history.removeLast();
    }
}
