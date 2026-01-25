package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public class DrinkPotionAction implements CombatAction {
    private final String potionName;
    private final int[] potionIds;

    public DrinkPotionAction(String potionName, int... potionIds) {
        this.potionName = potionName;
        this.potionIds = potionIds;
    }

    @Override
    public void execute() {
        // Try by name first (handles all dose variants)
        if (Rs2Inventory.hasItem(potionName)) {
            Rs2Inventory.interact(potionName, "Drink");
            return;
        }

        // Fallback to specific IDs if provided
        if (potionIds != null && potionIds.length > 0) {
            for (int potionId : potionIds) {
                if (Rs2Inventory.hasItem(potionId)) {
                    Rs2Inventory.interact(potionId, "Drink");
                    return;
                }
            }
        }
    }
}
