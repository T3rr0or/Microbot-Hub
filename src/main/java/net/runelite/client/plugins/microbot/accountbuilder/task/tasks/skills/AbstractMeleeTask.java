package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Common base class for all melee skill training tasks.
 *
 * <p>Handles:
 * <ul>
 *   <li><b>HP safety</b> — eats food when HP drops below the configured threshold;
 *       transitions to BANKING when inventory food is exhausted.</li>
 *   <li><b>Gear acquisition</b> — checks the bank for the best available weapon from a
 *       priority list (rune scimitar → bronze sword) and wields it.</li>
 *   <li><b>Food restocking</b> — withdraws food during every bank visit when supply
 *       drops below {@link #MIN_FOOD}.</li>
 *   <li><b>Attack style auto-swap</b> — switches the combat tab to the style that gives
 *       XP in {@link #getSkill()}. For defence, tries the 4th style (sword "Defensive")
 *       and falls back to the 3rd (scimitar "Block"), both of which grant Defence XP.</li>
 *   <li><b>Combat loop</b> — walks to the training location and attacks the target NPC.</li>
 * </ul>
 *
 * <p>Subclasses only need to provide: skill, NPC name, training location, and the
 * desired {@link VarPlayer#ATTACK_STYLE} varbit index.
 */
@Slf4j
public abstract class AbstractMeleeTask extends AbstractTask {

    /**
     * Weapon priority list, best to worst.
     * The first item found in the bank is withdrawn.
     */
    private static final String[] WEAPON_PRIORITY = {
        "Rune scimitar",   "Rune longsword",
        "Adamant scimitar","Adamant longsword",
        "Mithril scimitar","Mithril longsword",
        "Steel scimitar",  "Steel longsword",
        "Iron scimitar",   "Iron longsword",   "Iron sword",
        "Bronze scimitar", "Bronze longsword", "Bronze sword"
    };

    /** Maps style index 0-3 to the corresponding combat-tab widget. */
    private static final WidgetInfo[] STYLE_WIDGETS = {
        WidgetInfo.COMBAT_STYLE_ONE,   // 0 – Accurate / Chop   → Attack XP
        WidgetInfo.COMBAT_STYLE_TWO,   // 1 – Aggressive / Slash → Strength XP
        WidgetInfo.COMBAT_STYLE_THREE, // 2 – Block (scimitar)   → Defence XP
        WidgetInfo.COMBAT_STYLE_FOUR   // 3 – Defensive (sword)  → Defence XP
    };

    private enum State { CHECK_GEAR, BANKING, TRAINING }
    private State state = State.CHECK_GEAR;

    protected final int targetLevel;
    private final int eatAtPercent;

    protected AbstractMeleeTask(int targetLevel, AccountProfile profile, int eatAtPercent) {
        super(profile);
        this.targetLevel  = targetLevel;
        this.eatAtPercent = eatAtPercent;
    }

    // ── Subclass contract ─────────────────────────────────────────────────

    /** The skill whose level determines task completion. */
    protected abstract Skill getSkill();

    /** Name of the NPC to attack for XP. */
    protected abstract String getNpcName();

    /** Center of the training area. */
    protected abstract WorldPoint getTrainingLocation();

    /**
     * The target {@link VarPlayer#ATTACK_STYLE} varbit value.
     * <ul>
     *   <li>0 — Accurate   → Attack XP</li>
     *   <li>1 — Aggressive → Strength XP</li>
     *   <li>3 — Defensive  → Defence XP (falls back to 2 on scimitars)</li>
     * </ul>
     */
    protected abstract int getTargetStyleIndex();

    // ── Task contract ─────────────────────────────────────────────────────

    @Override
    public boolean isComplete() {
        return Microbot.getClient().getRealSkillLevel(getSkill()) >= targetLevel;
    }

    @Override
    public void execute() {
        maybeIdle();

        switch (state) {
            case CHECK_GEAR:
                state = gearReady() ? State.TRAINING : State.BANKING;
                break;

            case BANKING:
                if (!Rs2Bank.walkToBankAndUseBank()) { sleep(800); return; }
                if (!Rs2Bank.isOpen()) { sleep(600); return; }

                // Ensure a melee weapon is available
                if (!hasMeleeWeaponEquipped()) {
                    String weapon = findWeaponInBank();
                    if (weapon == null) {
                        Microbot.log(getName() + ": no usable weapon found in bank — waiting.");
                        sleep(10_000);
                        return;
                    }
                    if (!Rs2Inventory.hasItem(weapon)) {
                        Rs2Bank.withdrawOne(weapon);
                        sleep(600);
                    }
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

                // Wield highest-priority weapon that landed in inventory
                for (String weapon : WEAPON_PRIORITY) {
                    if (Rs2Inventory.hasItem(weapon)) {
                        Rs2Inventory.interact(weapon, "Wield");
                        sleep(400);
                        break;
                    }
                }

                state = State.CHECK_GEAR;
                break;

            case TRAINING:
                if (!gearReady()) { state = State.BANKING; return; }

                // Eat before engaging — avoid dying mid-fight
                eatIfNeeded(eatAtPercent);

                // No food left: head to bank before HP becomes critical
                if (Rs2Inventory.getInventoryFood().isEmpty()) {
                    state = State.BANKING;
                    return;
                }

                ensureAttackStyle();

                WorldPoint loc = getTrainingLocation();
                if (Rs2Player.getWorldLocation().distanceTo(loc) > 10) {
                    Rs2Walker.walkTo(loc, 3);
                    sleep(800);
                    return;
                }

                if (Rs2Combat.inCombat()) return;

                if (!Rs2Npc.attack(getNpcName())) {
                    Rs2Walker.walkTo(loc, 2);
                }
                sleep(600);
                break;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private boolean hasMeleeWeaponEquipped() {
        Rs2ItemModel w = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
        if (w == null) return false;
        String name = w.getName().toLowerCase();
        return !name.contains("bow") && !name.contains("staff") && !name.contains("wand");
    }

    private boolean gearReady() {
        return hasMeleeWeaponEquipped();
    }

    /** Returns the name of the best weapon found in the bank, or {@code null}. */
    private String findWeaponInBank() {
        for (String weapon : WEAPON_PRIORITY) {
            if (Rs2Bank.hasItem(weapon)) return weapon;
        }
        return null;
    }

    /**
     * Switches to the combat options tab and selects the correct attack style.
     *
     * <p>Defence style: tries index 3 (sword "Defensive") first. If the varbit does
     * not change — meaning the equipped weapon has only 3 styles (e.g. scimitar) —
     * falls back to index 2 ("Block"), which also grants Defence XP.
     */
    private void ensureAttackStyle() {
        int current = Microbot.getVarbitPlayerValue(VarPlayer.ATTACK_STYLE);
        int target  = getTargetStyleIndex();

        // Defence XP is granted by both index 3 (sword Defensive) and index 2 (scimitar Block)
        boolean correct = current == target || (target == 3 && current == 2);
        if (correct) return;

        if (Rs2Tab.getCurrentTab() != InterfaceTab.COMBAT) {
            Rs2Tab.switchToCombatOptionsTab();
            sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.COMBAT, 2000);
        }

        Rs2Combat.setAttackStyle(STYLE_WIDGETS[target]);
        sleep(400);

        // Scimitar fallback: if the 4th style widget didn't register, try the 3rd
        if (target == 3 && Microbot.getVarbitPlayerValue(VarPlayer.ATTACK_STYLE) != 3) {
            Rs2Combat.setAttackStyle(STYLE_WIDGETS[2]);
            sleep(400);
        }
    }
}
