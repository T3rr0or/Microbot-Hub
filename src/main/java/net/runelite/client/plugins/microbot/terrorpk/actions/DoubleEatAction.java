package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public class DoubleEatAction implements CombatAction {
    private final String firstFoodId;
    private static final int KARAMBWAN_ID = 3144;
    private static final int KARAMBWAN_WILDERNESS_ID = 24595;

    public DoubleEatAction(String firstFoodId) {
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

        // 2. Eat karambwan (normal or wilderness variant)
        if (Rs2Inventory.contains(KARAMBWAN_ID)) {
            Rs2Inventory.interact(KARAMBWAN_ID, "Eat");
        } else if (Rs2Inventory.contains(KARAMBWAN_WILDERNESS_ID)) {
            Rs2Inventory.interact(KARAMBWAN_WILDERNESS_ID, "Eat");
        }
    }
}
