package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class EatAction implements CombatAction {
    private final String foodCfg;
    private final int count;

    public EatAction(String foodCfg, int count) {
        this.foodCfg = foodCfg;
        this.count = count;
    }

    @Override
    public void execute() {
        if (foodCfg == null || foodCfg.trim().isEmpty())
            return;
        
        String[] foodItems = foodCfg.split("\\s*,\\s*");
        
        for (int i = 0; i < count; i++) {
            boolean ate = false;
            
            // Try each food in the configured list
            for (String food : foodItems) {
                food = food.trim();
                if (food.isEmpty())
                    continue;
                
                try {
                    // Try parsing as item ID first
                    int itemId = Integer.parseInt(food);
                    if (Rs2Inventory.contains(itemId)) {
                        Rs2Inventory.interact(itemId, "Eat");
                        ate = true;
                        // Wait for inventory to change to confirm we ate
                        Rs2Inventory.waitForInventoryChanges(1000);
                        break;
                    }
                } catch (NumberFormatException e) {
                    // Not a number, try as item name
                    if (Rs2Inventory.contains(food)) {
                        Rs2Inventory.interact(food, "Eat");
                        ate = true;
                        // Wait for inventory to change to confirm we ate
                        Rs2Inventory.waitForInventoryChanges(1000);
                        break;
                    }
                }
            }
            
            // If we couldn't eat from the configured list, stop trying
            if (!ate) {
                break;
            }
            
            // Add small delay between eats (except for the last one)
            if (i < count - 1) {
                sleep(50);
            }
        }
    }
}
