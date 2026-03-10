package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;

/**
 * Trains Prayer to the target level by burying regular bones banked from Lumbridge.
 *
 * <p>Loop: bank → withdraw 28 bones → walk to bury spot → bury all → repeat.
 * Uses Lumbridge bank (closest to a new account start).
 */
@Slf4j
public class TrainPrayerTask extends AbstractTask {

    // Lumbridge bank chest (ground floor)
    private static final WorldPoint LUMBRIDGE_BANK = new WorldPoint(3208, 3220, 2);
    // Open area east of Lumbridge castle to bury without obstacles
    private static final WorldPoint BURY_SPOT = new WorldPoint(3222, 3218, 0);
    private static final int WALK_THRESHOLD = 5;
    private static final int BURY_DELAY_MS = 1800; // game animation ~1.8 s per bone

    private enum State { BANKING, BURYING }
    private State state = State.BANKING;

    private final int targetLevel;

    public TrainPrayerTask(int targetLevel, AccountProfile profile) {
        super(profile);
        this.targetLevel = targetLevel;
    }

    @Override
    public String getName() {
        return "Train Prayer to " + targetLevel;
    }

    @Override
    public boolean isComplete() {
        return Microbot.getClient().getRealSkillLevel(Skill.PRAYER) >= targetLevel;
    }

    @Override
    public void execute() {
        maybeIdle();

        switch (state) {
            case BANKING:
                if (!Rs2Bank.walkToBankAndUseBank()) {
                    sleep(800);
                    return;
                }
                if (!Rs2Bank.isOpen()) {
                    sleep(600);
                    return;
                }

                // Verify bank has bones before trying
                if (!Rs2Bank.hasItem(ItemID.BONES) && !Rs2Bank.hasItem(ItemID.BIG_BONES)) {
                    Microbot.log("No bones in bank — cannot train prayer.");
                    sleep(10_000);
                    return;
                }

                Rs2Bank.depositAll();
                sleep(400);

                // Prefer big bones for better XP; fall back to regular
                if (Rs2Bank.hasItem(ItemID.BIG_BONES)) {
                    Rs2Bank.withdrawAll(ItemID.BIG_BONES);
                } else {
                    Rs2Bank.withdrawAll(ItemID.BONES);
                }
                sleep(600);

                Rs2Bank.closeBank();
                sleep(400);
                state = State.BURYING;
                break;

            case BURYING:
                // If inventory is empty, go back to bank
                if (!Rs2Inventory.contains(ItemID.BONES) && !Rs2Inventory.contains(ItemID.BIG_BONES)) {
                    state = State.BANKING;
                    return;
                }

                // Walk to bury spot if needed
                if (Rs2Player.getWorldLocation().distanceTo(BURY_SPOT) > WALK_THRESHOLD) {
                    Rs2Walker.walkTo(BURY_SPOT, 2);
                    sleep(600);
                    return;
                }

                // Bury one bone per tick
                List<Rs2ItemModel> bones = Rs2Inventory.getBones();
                if (bones != null && !bones.isEmpty()) {
                    Rs2ItemModel bone = bones.get(0);
                    if (Rs2Inventory.interact(bone, "Bury")) {
                        Rs2Player.waitForAnimation(BURY_DELAY_MS);
                        log.debug("Buried bone, prayer {}/{}",
                                Microbot.getClient().getRealSkillLevel(Skill.PRAYER), targetLevel);
                    }
                } else {
                    state = State.BANKING;
                }
                break;
        }
    }
}
