package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Trains Magic to the target level by casting Wind Strike on chickens at Lumbridge.
 *
 * <p>Uses a bank loop to replenish air runes and mind runes when stocks run low.
 * Also eats food when HP drops below the configured threshold and banks to restock
 * when food runs out (chickens deal minimal damage, but safety is still ensured).
 */
@Slf4j
public class TrainMagicTask extends AbstractTask {

    private static final WorldPoint CHICKEN_FARM = new WorldPoint(3236, 3295, 0);
    private static final String CHICKEN_NPC = "Chicken";
    private static final int WALK_THRESHOLD = 10;

    private static final int RUNE_LOW_THRESHOLD = 50;
    private static final int RUNE_WITHDRAW_AMOUNT = 500;

    private enum State { CHECK_RUNES, BANKING, TRAINING }
    private State state = State.CHECK_RUNES;

    private final int targetLevel;
    private final int eatAtPercent;

    public TrainMagicTask(int targetLevel, AccountProfile profile, int eatAtPercent) {
        super(profile);
        this.targetLevel  = targetLevel;
        this.eatAtPercent = eatAtPercent;
    }

    @Override
    public String getName() {
        return "Train Magic to " + targetLevel;
    }

    @Override
    public boolean isComplete() {
        return Microbot.getClient().getRealSkillLevel(Skill.MAGIC) >= targetLevel;
    }

    @Override
    public void execute() {
        maybeIdle();

        switch (state) {
            case CHECK_RUNES:
                state = runesOk() ? State.TRAINING : State.BANKING;
                break;

            case BANKING:
                if (!Rs2Bank.walkToBankAndUseBank()) { sleep(800); return; }
                if (!Rs2Bank.isOpen()) { sleep(600); return; }

                if (!Rs2Bank.hasItem(ItemID.AIR_RUNE) || !Rs2Bank.hasItem(ItemID.MIND_RUNE)) {
                    Microbot.log("No air/mind runes in bank — cannot train magic.");
                    sleep(10_000);
                    return;
                }

                if (Rs2Inventory.count(ItemID.AIR_RUNE) < RUNE_LOW_THRESHOLD) {
                    Rs2Bank.withdrawX(ItemID.AIR_RUNE, RUNE_WITHDRAW_AMOUNT);
                    sleep(500);
                }
                if (Rs2Inventory.count(ItemID.MIND_RUNE) < RUNE_LOW_THRESHOLD) {
                    Rs2Bank.withdrawX(ItemID.MIND_RUNE, RUNE_WITHDRAW_AMOUNT);
                    sleep(500);
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
                state = State.CHECK_RUNES;
                break;

            case TRAINING:
                if (!runesOk()) { state = State.BANKING; return; }

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

                Rs2NpcModel chicken = Rs2Npc.getNpc(CHICKEN_NPC);
                if (chicken == null) {
                    Rs2Walker.walkTo(CHICKEN_FARM, 2);
                    sleep(600);
                    return;
                }

                // Rs2CombatSpells.WIND_STRIKE.getMagicAction() verified present in
                // BradleyCombatPlugin MageAction.java and AIOMagicConfig.java.
                Rs2Magic.castOn(Rs2CombatSpells.WIND_STRIKE.getMagicAction(), chicken);
                log.debug("Cast Wind Strike, magic {}/{}", Microbot.getClient().getRealSkillLevel(Skill.MAGIC), targetLevel);
                sleep(600);
                break;
        }
    }

    private boolean runesOk() {
        return Rs2Inventory.count(ItemID.AIR_RUNE) >= RUNE_LOW_THRESHOLD
                && Rs2Inventory.count(ItemID.MIND_RUNE) >= RUNE_LOW_THRESHOLD;
    }
}
