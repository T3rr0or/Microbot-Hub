package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public class TripleEatAction implements CombatAction {
    private final String firstFoodId;
    private static final int KARAMBWAN_ID = 3144;
    private static final int KARAMBWAN_WILDERNESS_ID = 24595;

    public TripleEatAction(String firstFoodId) {
        this.firstFoodId = firstFoodId;
    }

    @Override
    public void execute() {
        // 1. Eat first food (configured by user)
        if (firstFoodId != null && !firstFoodId.trim().isEmpty()) {
            try {
                int itemId = Integer.parseInt(firstFoodId.trim());
                if (Rs2Inventory.contains(itemId)) {
                    Rs2Inventory.interact(itemId, "Eat");
                }
            } catch (NumberFormatException ignored) {
                // Invalid ID, skip
            }
        }

        // 2. Drink brew (find any brew in inventory)
        // Common brew IDs: Saradomin brew(4)=6685, (3)=6687, (2)=6689, (1)=6691
        int[] brewIds = {6685, 6687, 6689, 6691};
        for (int brewId : brewIds) {
            if (Rs2Inventory.contains(brewId)) {
                Rs2Inventory.interact(brewId, "Drink");
                break;
            }
        }

        // 3. Eat karambwan (normal or wilderness variant)
        if (Rs2Inventory.contains(KARAMBWAN_ID)) {
            Rs2Inventory.interact(KARAMBWAN_ID, "Eat");
        } else if (Rs2Inventory.contains(KARAMBWAN_WILDERNESS_ID)) {
            Rs2Inventory.interact(KARAMBWAN_WILDERNESS_ID, "Eat");
        }
    }
}
