package de.mcterranova.playerActionAdapter;

import de.mcterranova.playerActionAdapter.pojo.ObjectiveConfig;
import de.mcterranova.playerActionAdapter.pojo.ProfessionConfig;

import java.util.*;

public class ProfessionManager {
    private static final Map<String, ProfessionConfig> professionMap = new HashMap<>();
    private static final Map<String, List<ObjectiveConfig>> objectivesMap = new HashMap<>();

    public static void loadAll() {
        // 1) Professionen laden
        var allProfs = PlayerActionAdapter.professionConfigs;
        for (ProfessionConfig p : allProfs) {
            professionMap.put(p.professionId, p);

            // 2) Objectives
            var objs = p.objectives;
            objectivesMap.put(p.professionId, objs);
        }
    }
    public static List<ObjectiveConfig> getObjectivesForProfession(String profId) {
        return objectivesMap.getOrDefault(profId, Collections.emptyList());
    }
}
