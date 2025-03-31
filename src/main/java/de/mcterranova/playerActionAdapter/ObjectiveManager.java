package de.mcterranova.playerActionAdapter;

import de.mcterranova.playerActionAdapter.database.SettlementProfessionRelationDAO;
import de.mcterranova.playerActionAdapter.pojo.ObjectiveConfig;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class ObjectiveManager {

    // ------------------------------------------------------------------------
    // Example sets for grouped blocks/items/entities
    // ------------------------------------------------------------------------
    private static final Set<String> ANY_LOGS = Set.of(
            "OAK_LOG", "BIRCH_LOG", "ACACIA_LOG", "SPRUCE_LOG",
            "DARK_OAK_LOG", "JUNGLE_LOG", "MANGROVE_LOG",
            "CRIMSON_STEM", "WARPED_STEM", "PALE_OAK_LOG", "CHERRY_LOG"
    );

    private static final Set<String> ANY_STONES = Set.of(
            "STONE", "GRANITE", "DIORITE", "ANDESITE", "BLACKSTONE",
            "DEEPSLATE", "TUFF", "CALCITE", "SANDSTONE", "RED_SANDSTONE"
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
            // etc. Add more if desired
    );

    private static final Set<String> IRON_TOOL = Set.of(
            "IRON_PICKAXE", "IRON_AXE", "IRON_SHOVEL", "IRON_HOE", "IRON_SWORD"
    );

    // For “DIAMOND_TOOL” references in config
    private static final Set<String> DIAMOND_TOOL = Set.of(
            "DIAMOND_PICKAXE", "DIAMOND_AXE", "DIAMOND_SHOVEL",
            "DIAMOND_HOE", "DIAMOND_SWORD"
    );

    // For “NETHERITE_TOOL” references in config
    private static final Set<String> NETHERITE_TOOL = Set.of(
            "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL",
            "NETHERITE_HOE", "NETHERITE_SWORD"
    );

    private static final Set<String> ANY_TOOL = Set.of(
            "WOODEN_PICKAXE", "WOODEN_AXE", "WOODEN_SHOVEL", "WOODEN_HOE", "WOODEN_SWORD",
            "STONE_PICKAXE", "STONE_AXE", "STONE_SHOVEL", "STONE_HOE", "STONE_SWORD",
            "GOLDEN_PICKAXE", "GOLDEN_AXE", "GOLDEN_SHOVEL", "GOLDEN_HOE", "GOLDEN_SWORD",
            "IRON_PICKAXE", "IRON_AXE", "IRON_SHOVEL", "IRON_HOE", "IRON_SWORD",
            "DIAMOND_PICKAXE", "DIAMOND_AXE", "DIAMOND_SHOVEL", "DIAMOND_HOE", "DIAMOND_SWORD",
            "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL", "NETHERITE_HOE", "NETHERITE_SWORD"
    );

    // ------------------------------------------------------------------------
    // Called by listeners to register an action that a player did
    // ------------------------------------------------------------------------
    public static void handleEvent(Player player, String action, String actualObject, long amount) {
        // Identify which settlement the player is in (hypothetical method)
        UUID settlementId = NationsHelper.getMembersTown(player.getUniqueId());
        if (settlementId == null) {
            return; // not in any settlement => skip
        }

        // Get the active profession for that settlement from your DB
        String professionId = SettlementProfessionRelationDAO.getActiveProfessionID(settlementId.toString());
        // Then get the list of objective config entries for that profession
        List<ObjectiveConfig> objectives = ProfessionManager.getObjectivesForProfession(professionId);

        // Filter: correct action & object matches => increment progress
        objectives.stream()
                .filter(o -> o.action.equalsIgnoreCase(action))
                .filter(o -> matchesObject(o.object, actualObject))
                .forEach(o -> {
                    long oldVal = NationsHelper.getObjectiveProgress(o.objectiveId, settlementId);
                    NationsHelper.setObjectiveProgress(settlementId, o.objectiveId, oldVal + amount);
                });
    }

    // ------------------------------------------------------------------------
    // Matches "actualObject" (e.g. "COAL_ORE", "ZOMBIE", "POTION_FIRE_RESISTANCE")
    // against "object" in config (e.g. "ANY_ORE", "HOSTILE_MOB", "ANY_POTION", etc.)
    // ------------------------------------------------------------------------
    private static boolean matchesObject(String configObject, String actualObject) {
        // 1) Direct string match
        if (configObject.equalsIgnoreCase(actualObject)) {
            return true;
        }

        // 2) String-based categories
        switch (configObject.toUpperCase()) {
            case "ANY_LOG" -> {
                return ANY_LOGS.contains(actualObject.toUpperCase());
            }
            case "ANY_STONE" -> {
                return ANY_STONES.contains(actualObject.toUpperCase());
            }
            case "ANY_FISH" -> {
                return ANY_FISHES.contains(actualObject.toUpperCase());
            }
            case "ANY_CROP" -> {
                return ANY_CROPS.contains(actualObject.toUpperCase());
            }
            case "IRON_TOOL" -> {
                return IRON_TOOL.contains(actualObject.toUpperCase());
            }
            case "DIAMOND_TOOL" -> {
                return DIAMOND_TOOL.contains(actualObject.toUpperCase());
            }
            case "NETHERITE_TOOL" -> {
                return NETHERITE_TOOL.contains(actualObject.toUpperCase());
            }
            case "ANY_POTION" -> {
                // e.g. "POTION_FIRE_RESISTANCE", "SPLASH_POTION_NIGHT_VISION", etc.
                // We'll just check if the word "POTION" is in the name.
                return actualObject.toUpperCase().contains("POTION");
            }

            case "ANY_ITEM" -> {
                // This means anything matches
                return true;
            }
        }

        // 3) Entity-based categories
        switch (configObject.toUpperCase()) {
            case "HOSTILE_MOB" -> {
                EntityType eType = parseEntityType(actualObject);
                return eType != null && HOSTILE_MOBS.contains(eType);
            }
            case "ANY_ANIMAL" -> {
                EntityType eType = parseEntityType(actualObject);
                return eType != null && ANY_ANIMALS.contains(eType);
            }
        }

        // No match => false
        return false;
    }

    private static EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // not an entity
        }
    }
}
