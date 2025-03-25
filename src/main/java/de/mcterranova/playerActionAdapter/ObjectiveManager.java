package de.mcterranova.playerActionAdapter;

import de.mcterranova.playerActionAdapter.database.SettlementProfessionRelationDAO;
import de.mcterranova.playerActionAdapter.pojo.ObjectiveConfig;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class ObjectiveManager {

    // ---------------------------------------------
    // Beispielhafte Sets für Sammelbegriffe
    // ---------------------------------------------
    private static final Set<String> ANY_LOGS = Set.of(
            "OAK_LOG", "BIRCH_LOG", "ACACIA_LOG", "SPRUCE_LOG",
            "DARK_OAK_LOG", "JUNGLE_LOG", "MANGROVE_LOG",
            "CRIMSON_STEM", "WARPED_STEM", "PALE_OAK_LOG", "CHERRY_LOG"
    );

    private static final Set<String> ANY_STONES = Set.of(
            "STONE", "GRANITE", "DIORITE", "ANDESITE", "BLACKSTONE", "DEEPSLATE", "TUFF", "CALCITE"
    );

    private static final Set<String> ANY_FISHES = Set.of(
            "COD", "SALMON", "TROPICAL_FISH", "PUFFERFISH"
    );

    private static final Set<String> ANY_CROPS = Set.of(
            "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART"
    );

    private static final Set<EntityType> HOSTILE_MOBS = Set.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
            EntityType.SPIDER, EntityType.ENDERMAN, EntityType.BLAZE,
            EntityType.WITCH, EntityType.WITHER, EntityType.SLIME,
            EntityType.PHANTOM, EntityType.DROWNED, EntityType.HUSK,
            EntityType.STRAY, EntityType.VEX, EntityType.PILLAGER, EntityType.EVOKER,
            EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.RAVAGER, EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN, EntityType.SHULKER, EntityType.HOGLIN, EntityType.PIGLIN,
            EntityType.ZOGLIN, EntityType.PIGLIN_BRUTE, EntityType.WITHER_SKELETON,
            EntityType.GHAST, EntityType.MAGMA_CUBE, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_VILLAGER,
            EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.CAVE_SPIDER, EntityType.ENDER_DRAGON
    );

    private static final Set<EntityType> ANY_ANIMALS = Set.of(
            EntityType.COW, EntityType.SHEEP, EntityType.PIG,
            EntityType.CHICKEN, EntityType.HORSE, EntityType.RABBIT,
            EntityType.DONKEY, EntityType.MULE
            // etc.
    );

    /**
     * Wird vom Listener aufgerufen, wenn eine Aktion passiert (z.B. DESTROY, KILL, FISH, etc.)
     * @param player        Der Spieler, der die Aktion ausführt
     * @param action        Aktion (DESTROY / KILL / FISH / ...), wie in config definiert
     * @param actualObject  z.B. "COAL_ORE", "ZOMBIE", "OAK_LOG", "DIAMOND_SWORD", ...
     * @param amount        Wie viel wurde zerstört/gefarmt/gefischt/...
     */
    public static void handleEvent(Player player, String action, String actualObject, long amount) {
        // Settlement ermitteln
        UUID settlementId = NationsHelper.getMembersTown(player.getUniqueId());
        if (settlementId == null) {
            return;
        }
        // Liste aller Objectives für die aktive Profession
        List<ObjectiveConfig> objectives = ProfessionManager.getObjectivesForProfession(SettlementProfessionRelationDAO.getActiveProfessionID(settlementId.toString()));

        // Filter: Gleiche Action? Und Objekt passt (exakt oder per Sammelbegriff)?
        objectives.stream()
                .filter(o -> o.action.equalsIgnoreCase(action))
                .filter(o -> matchesObject(o.object, actualObject))
                .forEach(o -> {
                    long oldVal = NationsHelper.getObjectiveProgress(o.objectiveId, settlementId);
                    NationsHelper.setObjectiveProgress(settlementId, o.objectiveId, oldVal + amount);
                });
    }

    // -------------------------------------------------------------------------
    // matchesObject: Prüft, ob "COAL_ORE" zu "COAL_ORE" (exakt) oder "ANY_ORE" (Kategorie) passt
    // -------------------------------------------------------------------------
    private static boolean matchesObject(String configObject, String actualObject) {
        // 1) Exakte Übereinstimmung
        if (configObject.equalsIgnoreCase(actualObject)) {
            return true;
        }

        // 2) Sammelbegriffe (Strings)
        switch (configObject.toUpperCase()) {
            case "ANY_LOG":
                return ANY_LOGS.contains(actualObject.toUpperCase());

            case "ANY_STONE":
                return ANY_STONES.contains(actualObject.toUpperCase());

            case "ANY_FISH":
                return ANY_FISHES.contains(actualObject.toUpperCase());

            case "ANY_CROP":
                return ANY_CROPS.contains(actualObject.toUpperCase());

            // case "ANY_ORE":
            //     return ANY_ORES.contains(actualObject.toUpperCase());
        }

        // 3) Sammelbegriffe (Mobs)
        switch (configObject.toUpperCase()) {
            case "HOSTILE_MOB" -> {
                EntityType eTypeHostile = parseEntityType(actualObject);
                if (eTypeHostile != null) {
                    return HOSTILE_MOBS.contains(eTypeHostile);
                }
            }
            case "ANY_ANIMAL" -> {
                EntityType eTypeAnimal = parseEntityType(actualObject);
                if (eTypeAnimal != null) {
                    return ANY_ANIMALS.contains(eTypeAnimal);
                }
            }
        }

        // Nichts passt => false
        return false;
    }

    private static EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
