package org.openjfx.app.core;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationManager {

   private static final Map<EntityType, List<EntityType>> threat = new HashMap<>();
   static{
    threat.put(EntityType.RABBIT, Arrays.asList(EntityType.WOLF, EntityType.BEAR));
    threat.put(EntityType.FISH, Arrays.asList(EntityType.BEAR));
    threat.put(EntityType.FRUIT, Arrays.asList(EntityType.BEAR, EntityType.RABBIT));
    threat.put(EntityType.GRASS, Arrays.asList(EntityType.RABBIT, EntityType.ELEPHANT));
    threat.put(EntityType.ALGAE, Arrays.asList(EntityType.FISH));
   }

    public static boolean isScaredOf(EntityType subject, EntityType target) {
        return false;
    }

    public static boolean isPrey(EntityType subject, EntityType target) {
        if (threat.containsKey(subject) ){
            for (EntityType animal : threat.get(subject)){
                if (animal == target){
                    return true;
                }
            }

        }
        return false;
        
    }

    public static boolean isEdiblePlant(EntityType subject, EntityType target) {
        return false;
    }

    
}