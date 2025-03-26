package de.mcterranova.playerActionAdapter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class GrindListener implements Listener {

    // ------------------------------------------------------------------------
    // 1) BLOCK BREAK & HARVEST
    // ------------------------------------------------------------------------
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();

        // If crop is not fully grown => skip HARVEST
        if (isCrop(mat) && !isFullyGrown(block)) {
            return;
        }

        // Always count destroyed block => "DESTROY"
        ObjectiveManager.handleEvent(player, "DESTROY", mat.name(), 1);

        // If it's a harvestable crop => also "HARVEST"
        if (isCrop(mat)) {
            ObjectiveManager.handleEvent(player, "HARVEST", mat.name(), 1);
        }
    }

    private boolean isCrop(Material mat) {
        return switch (mat) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
    }

    private boolean isFullyGrown(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // 2) CRAFTING
    // ------------------------------------------------------------------------
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Material mat = event.getRecipe().getResult().getType();
        int amountCrafted = event.getRecipe().getResult().getAmount();

        // If shift-click, figure out how many will be crafted in total
        if (event.isShiftClick()) {
            amountCrafted = computeShiftClickAmount(event);
        }

        ObjectiveManager.handleEvent(player, "CRAFT", mat.name(), amountCrafted);
    }

    private int computeShiftClickAmount(CraftItemEvent event) {
        int perCraft = event.getRecipe().getResult().getAmount();
        ItemStack[] matrix = event.getInventory().getMatrix().clone();
        int maxCrafts = Integer.MAX_VALUE;

        for (ItemStack slot : matrix) {
            if (slot == null || slot.getType().isAir()) continue;
            // Very simplistic: assume each craft consumes 1 from each slot
            int craftsFromSlot = slot.getAmount() / 1;
            if (craftsFromSlot < maxCrafts) {
                maxCrafts = craftsFromSlot;
            }
        }
        int totalItems = maxCrafts * perCraft;
        return (totalItems > 0) ? totalItems : perCraft;
    }

    // ------------------------------------------------------------------------
    // 3) FURNACE EXTRACTION => SMELT
    // ------------------------------------------------------------------------
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItemType();
        int amount = event.getItemAmount();

        ObjectiveManager.handleEvent(player, "SMELT", mat.name(), amount);
    }

    // ------------------------------------------------------------------------
    // 4) FISHING => FISH
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        switch (event.getState()) {
            case CAUGHT_FISH, CAUGHT_ENTITY -> {
                Entity caught = event.getCaught();
                if (caught instanceof Item itemEntity) {
                    ItemStack caughtStack = itemEntity.getItemStack();
                    Material caughtMat = caughtStack.getType();
                    int amount = caughtStack.getAmount();
                    ObjectiveManager.handleEvent(player, "FISH", caughtMat.name(), amount);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------------
    // 5) ENTITY DEATH => KILL
    // ------------------------------------------------------------------------
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        EntityType type = event.getEntityType();

        ObjectiveManager.handleEvent(player, "KILL", type.name(), 1);
    }

    // ------------------------------------------------------------------------
    // 6) ENCHANTING => ENCHANT
    // ------------------------------------------------------------------------
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Material enchantedMat = event.getItem().getType();
        ObjectiveManager.handleEvent(player, "ENCHANT", enchantedMat.name(), 1);
    }

    // ------------------------------------------------------------------------
    // 7) BREEDING => BREED
    // ------------------------------------------------------------------------
    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        // Only if the breeder is a Player
        if (event.getBreeder() instanceof Player player) {
            EntityType babyType = event.getEntityType();
            // For "ANY_ANIMAL" objectives, we pass the baby’s actual type
            ObjectiveManager.handleEvent(player, "BREED", babyType.name(), 1);
        }
    }

    // ------------------------------------------------------------------------
    // 8) BREW ("picking potions out" => BREW)
    //
    //   - We catch InventoryClickEvent in a Brewing Stand (BrewerInventory).
    //   - If the click is in one of the top 3 "potion result" slots (0..2),
    //     we treat that as finishing the brew. The player physically
    //     takes out the potions => we credit them for "BREW".
    //
    //   - Since PotionData is deprecated in 1.20.6, we do a simplified
    //     approach for identifying the potion:
    //       * If your config uses "ANY_POTION", we just say "POTION".
    //       * If your config has more detail (e.g. "POTION_FIRE_RESISTANCE"),
    //         we attempt to detect an effect name from the custom effects
    //         or from scanning its display name (fallback).
    // ------------------------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Must be a player
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Check if top inventory is a BrewerInventory => Brewing Stand
        if (!(event.getView().getTopInventory() instanceof BrewerInventory)) {
            return;
        }

        // The top 3 potion-result slots in a brewing stand are raw slots 0..2
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot > 2) {
            return; // Not one of the three result slots
        }

        // The item the player is about to remove
        ItemStack potionItem = event.getCurrentItem();
        if (potionItem == null || potionItem.getType() == Material.AIR) {
            return;
        }

        // We'll handle how many potions they pick up (SHIFT-click, etc.)
        int amount = potionItem.getAmount();

        // Build a string to pass to ObjectiveManager, e.g. "POTION_FIRE_RESISTANCE"
        String potionKey = identifyPotionKey(potionItem);

        // Now record as "BREW"
        ObjectiveManager.handleEvent(player, "BREW", potionKey, amount);
    }

    /**
     * Attempt to build a string like:
     *   "POTION_FIRE_RESISTANCE"
     *   "SPLASH_POTION_NIGHT_VISION"
     *   "LINGERING_POTION_SLOW_FALLING"
     *
     * If we can't reliably detect the base effect (since PotionData is deprecated),
     * we do either "POTION_{something from customEffects}" or just "POTION" as fallback.
     */
     private String identifyPotionKey(ItemStack potion) {
        // e.g. "POTION", "SPLASH_POTION", "LINGERING_POTION"
        String baseName = potion.getType().name();

        if (!(potion.getItemMeta() instanceof PotionMeta pm)) {
            return baseName;
        }

        // 1) If truly custom effects exist, we can pick the first effect’s name
        if (pm.hasCustomEffects()) {
            PotionEffect first = pm.getCustomEffects().get(0);
            if (first != null) {
                // e.g. "FIRE_RESISTANCE", "JUMP", etc.
                return baseName + "_" + first.getType().getName().toUpperCase();
            }
        }

        // 2) For base potions, check the default lore lines
        //    If the user/server hasn't overridden them, it might say "Fire Resistance (3:00)"
        //    in English. We'll look for "FIRE RESISTANCE", "NIGHT VISION", etc.
        if (pm.lore() != null) {
            for (Component c : pm.lore()) {
                String line = PlainTextComponentSerializer.plainText().serialize(c).toUpperCase();

                System.out.println(line);

                if (line.contains("FIRE RESISTANCE")) {
                    return baseName + "_FIRE_RESISTANCE";
                } else if (line.contains("NIGHT VISION")) {
                    return baseName + "_NIGHT_VISION";
                } else if (line.contains("INVISIBILITY")) {
                    return baseName + "_INVISIBILITY";
                } else if (line.contains("LEVITATION")) {
                    return baseName + "_LEVITATION";
                }
                // ... add more if needed
            }
        }

        // 3) If no lore or it didn't match anything, we have no reliable way
        //    => just return "POTION" or "SPLASH_POTION" etc.
        return baseName;
    }
}
