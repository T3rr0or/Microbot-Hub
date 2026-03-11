package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Trains Ranged to the target level by shooting chickens at Lumbridge.
 * Verifies a ranged weapon and ammo are equipped before attacking; banks
 * for a shortbow and bronze arrows if either slot is empty.
 * Eats food from inventory when HP drops below the configured threshold and
 * banks to restock when food runs out.
 */
@Slf4j
public class TrainRangedTask extends AbstractTask {

    private static final WorldPoint CHICKEN_FARM = new WorldPoint(3236, 3295, 0);
    private static final String CHICKEN_NPC = "Chicken";
    private static final int WALK_THRESHOLD = 10;

    private static final String BOW_NAME = "Shortbow";
    private static final String ARROW_NAME = "Bronze arrow";
    private static final int ARROW_WITHDRAW_AMOUNT = 500;

    private enum State { CHECK_GEAR, BANKING, TRAINING }
    private State state = State.CHECK_GEAR;

    private final int targetLevel;
    private final int eatAtPercent;

    public TrainRangedTask(int targetLevel, AccountProfile profile, int eatAtPercent) {
        super(profile);
        this.targetLevel  = targetLevel;
        this.eatAtPercent = eatAtPercent;
    }

    @Override
    public String getName() {
        return "Train Ranged to " + targetLevel;
    }

    @Override
    public boolean isComplete() {
        return Microbot.getClient().getRealSkillLevel(Skill.RANGED) >= targetLevel;
    }

    @Override
    public void execute() {
        maybeIdle();

        switch (state) {
            case CHECK_GEAR:
                if (gearReady()) {
                    state = State.TRAINING;
                } else {
                    state = State.BANKING;
                }
                break;

            case BANKING:
                if (!Rs2Bank.walkToBankAndUseBank()) { sleep(800); return; }
                if (!Rs2Bank.isOpen()) { sleep(600); return; }

                // Withdraw bow if not already equipped
                if (!Rs2Equipment.isWearing(EquipmentInventorySlot.WEAPON)) {
                    if (!Rs2Inventory.hasItem(BOW_NAME)) {
                        if (!Rs2Bank.hasItem(BOW_NAME)) {
                            Microbot.log("No shortbow found in bank — cannot train ranged.");
                            sleep(5000);
                            return;
                        }
                        Rs2Bank.withdrawOne(BOW_NAME);
                        sleep(600);
                    }
                }

                // Withdraw arrows only if ammo slot is empty AND none in inventory
                if (!Rs2Equipment.isWearing(EquipmentInventorySlot.AMMO)
                        && !Rs2Inventory.hasItem(ARROW_NAME)) {
                    if (!Rs2Bank.hasItem(ARROW_NAME)) {
                        Microbot.log("No bronze arrows found in bank — cannot train ranged.");
                        sleep(5000);
                        return;
                    }
                    Rs2Bank.withdrawX(ARROW_NAME, ARROW_WITHDRAW_AMOUNT);
                    sleep(600);
                }

                // Restock food if running low
                if (needsFood()) {
                    String food = findFoodInBank();
                    if (food != null) {
                        Rs2Bank.withdrawX(food, FOOD_WITHDRAW_AMOUNT);
                        sleep(600);
                    }
                }

                Rs2Bank.closeBank();
                sleep(400);

                if (Rs2Inventory.hasItem(BOW_NAME)) {
                    Rs2Inventory.interact(BOW_NAME, "Wield");
                    sleep(400);
                }
                if (Rs2Inventory.hasItem(ARROW_NAME)) {
                    Rs2Inventory.interact(ARROW_NAME, "Wield");
                    sleep(400);
                }

                state = State.CHECK_GEAR;
                break;

            case TRAINING:
                if (!gearReady()) { state = State.BANKING; return; }

                eatIfNeeded(eatAtPercent);

                if (Rs2Inventory.getInventoryFood().isEmpty()) {
                    state = State.BANKING;
                    return;
                }

                if (Rs2Player.getWorldLocation().distanceTo(CHICKEN_FARM) > WALK_THRESHOLD) {
                    Rs2Walker.walkTo(CHICKEN_FARM, 3);
                    sleep(800);
                    return;
                }

                if (Rs2Combat.inCombat()) return;

                boolean attacked = Rs2Npc.attack(CHICKEN_NPC);
                if (!attacked) {
                    Rs2Walker.walkTo(CHICKEN_FARM, 2);
                }
                sleep(600);
                break;
        }
    }

    /**
     * Returns true only when a ranged weapon (bow) AND ammo are equipped.
     * Uses getName().contains("bow") so a melee weapon in the slot doesn't pass
     * the check and accidentally train the wrong skill.
     */
    private boolean gearReady() {
        Rs2ItemModel weapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
        boolean hasRangedWeapon = weapon != null
                && weapon.getName().toLowerCase().contains("bow");
        return hasRangedWeapon && Rs2Equipment.isWearing(EquipmentInventorySlot.AMMO);
    }
}
