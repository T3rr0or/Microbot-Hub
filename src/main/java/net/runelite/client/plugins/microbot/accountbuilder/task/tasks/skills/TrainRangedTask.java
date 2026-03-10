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
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Trains Ranged to the target level by shooting chickens at Lumbridge.
 * Verifies a ranged weapon and ammo are equipped before attacking; banks
 * for a shortbow and bronze arrows if either slot is empty.
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

    public TrainRangedTask(int targetLevel, AccountProfile profile) {
        super(profile);
        this.targetLevel = targetLevel;
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
                if (!Rs2Bank.walkToBankAndUseBank()) {
                    sleep(800);
                    return;
                }
                if (!Rs2Bank.isOpen()) {
                    sleep(600);
                    return;
                }

                // Equip bow from bank if we don't have one
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

                Rs2Bank.closeBank();
                sleep(400);

                // Wield from inventory
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
                // Re-check gear each iteration in case arrows run out
                if (!gearReady()) {
                    state = State.BANKING;
                    return;
                }

                if (Rs2Player.getWorldLocation().distanceTo(CHICKEN_FARM) > WALK_THRESHOLD) {
                    Rs2Walker.walkTo(CHICKEN_FARM, 3);
                    sleep(800);
                    return;
                }

                if (Rs2Combat.inCombat()) {
                    return;
                }

                boolean attacked = Rs2Npc.attack(CHICKEN_NPC);
                if (!attacked) {
                    Rs2Walker.walkTo(CHICKEN_FARM, 2);
                }

                sleep(600);
                break;
        }
    }

    private boolean gearReady() {
        return Rs2Equipment.isWearing(EquipmentInventorySlot.WEAPON)
                && Rs2Equipment.isWearing(EquipmentInventorySlot.AMMO);
    }
}
