package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.quests;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Automates Animal Magnetism quest, which rewards Ava's device for free Ranged ammo recovery.
 *
 * <p>Prerequisites (must be met before this task starts):
 * <ul>
 *   <li>18 Slayer</li>
 *   <li>19 Crafting</li>
 *   <li>30 Ranged</li>
 *   <li>35 Woodcutting</li>
 *   <li>Completed Ernest the Chicken and Priest in Peril quests</li>
 * </ul>
 *
 * <p>Quest overview:
 * <ol>
 *   <li>Talk to Ava at Draynor Manor (upstairs).</li>
 *   <li>Collect an undead chicken from the undead chicken farm south of Draynor Manor.</li>
 *   <li>Talk to Alice at her farm north of Port Phasmatys; convince her husband to sell a magnet.</li>
 *   <li>Collect 20 polished buttons from undead cows west of Port Phasmatys.</li>
 *   <li>Talk to the Ghost Innkeeper in Port Phasmatys ectophial area for a translation.</li>
 *   <li>Return to Ava to create the device.</li>
 * </ol>
 *
 * <p>Completion check: Varbit 274 == 4.
 * Source: OSRS Wiki — https://oldschool.runescape.wiki/w/Animal_Magnetism#Varbit
 * TODO: verify varbit value against live client before enabling in production.
 */
@Slf4j
public class AnimalMagnetismTask extends AbstractTask {

    // ── Varbit ───────────────────────────────────────────────────────────
    /**
     * Varbit tracking Animal Magnetism quest progress.
     * Source: OSRS Wiki https://oldschool.runescape.wiki/w/Animal_Magnetism#Varbit
     * TODO: verify against live client.
     */
    private static final int ANIMAL_MAGNETISM_VARBIT = 274;
    private static final int COMPLETE_VALUE = 4;

    // ── Varbit progress thresholds ────────────────────────────────────────
    private static final int VARBIT_NOT_STARTED            = 0;
    private static final int VARBIT_TALKED_AVA             = 1;
    private static final int VARBIT_GOT_UNDEAD_CHICKEN     = 2;
    private static final int VARBIT_GOT_MAGNET             = 3;

    // ── WorldPoints ───────────────────────────────────────────────────────
    // TODO: verify all coordinates against live client
    /** Ava is on the 2nd floor (index 1) of Draynor Manor. */
    private static final WorldPoint AVA_LOCATION          = new WorldPoint(3096, 3358, 1);
    /** Undead chickens roam south of Draynor Manor. */
    private static final WorldPoint UNDEAD_CHICKEN_FARM   = new WorldPoint(3085, 3263, 0);
    /** Alice's farm north of Port Phasmatys. */
    private static final WorldPoint ALICE_FARM            = new WorldPoint(3578, 3503, 0);
    /** Undead cows for polished buttons west of Port Phasmatys. */
    private static final WorldPoint UNDEAD_COW_FIELD      = new WorldPoint(3480, 3474, 0);
    /** Ghost Innkeeper in Port Phasmatys. */
    private static final WorldPoint GHOST_INNKEEPER       = new WorldPoint(3679, 3490, 0);

    // ── NPC names ─────────────────────────────────────────────────────────
    private static final String NPC_AVA             = "Ava";
    private static final String NPC_ALICE           = "Alice";
    private static final String NPC_ALICE_HUSBAND   = "Alice's husband";
    private static final String NPC_UNDEAD_CHICKEN  = "Undead chicken";
    private static final String NPC_UNDEAD_COW      = "Undead cow";
    private static final String NPC_GHOST_INNKEEPER = "Ghost innkeeper";

    // ── Item IDs ──────────────────────────────────────────────────────────
    private static final int UNDEAD_CHICKEN    = ItemID.UNDEAD_CHICKEN;
    private static final int BAR_MAGNET        = ItemID.BAR_MAGNET;
    private static final int POLISHED_BUTTONS  = ItemID.POLISHED_BUTTONS;
    private static final int GHOSTSPEAK_AMULET = ItemID.GHOSTSPEAK_AMULET;
    private static final int ECTOPHIAL         = ItemID.ECTOPHIAL;

    private static final int POLISHED_BUTTONS_NEEDED = 20;

    // ── State ─────────────────────────────────────────────────────────────
    private enum State {
        CHECK_PREREQUISITES,
        WALK_TO_AVA,
        TALK_TO_AVA,
        GET_UNDEAD_CHICKEN,
        WALK_TO_ALICE,
        TALK_TO_ALICE,
        TALK_TO_ALICES_HUSBAND,
        GET_POLISHED_BUTTONS,
        TALK_TO_GHOST_INNKEEPER,
        RETURN_TO_AVA,
        DONE
    }

    private State state = State.CHECK_PREREQUISITES;

    public AnimalMagnetismTask(AccountProfile profile) {
        super(profile);
    }

    @Override
    public String getName() {
        return "Animal Magnetism";
    }

    @Override
    public boolean isComplete() {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getVarbitValue(ANIMAL_MAGNETISM_VARBIT) >= COMPLETE_VALUE
        ).orElse(false);
    }

    @Override
    public void execute() {
        maybeIdle();

        // Sync state to varbit
        int varbit = Microbot.getClientThread()
                .runOnClientThreadOptional(() -> Microbot.getClient().getVarbitValue(ANIMAL_MAGNETISM_VARBIT))
                .orElse(0);

        if (varbit >= COMPLETE_VALUE) return;
        if (varbit >= VARBIT_GOT_MAGNET          && state.ordinal() < State.GET_POLISHED_BUTTONS.ordinal()) state = State.GET_POLISHED_BUTTONS;
        if (varbit >= VARBIT_GOT_UNDEAD_CHICKEN  && state.ordinal() < State.WALK_TO_ALICE.ordinal())        state = State.WALK_TO_ALICE;
        if (varbit >= VARBIT_TALKED_AVA          && state.ordinal() < State.GET_UNDEAD_CHICKEN.ordinal())   state = State.GET_UNDEAD_CHICKEN;

        log.debug("AnimalMagnetism state={} varbit={}", state, varbit);

        switch (state) {

            case CHECK_PREREQUISITES:
                if (!prerequisitesMet()) {
                    Microbot.log("AnimalMagnetism: Prerequisites not met — need 18 Slayer, 19 Crafting, 30 Ranged, 35 WC.");
                    sleep(10_000);
                    return;
                }
                // Need Ghostspeak Amulet for this quest
                if (!Rs2Inventory.contains(GHOSTSPEAK_AMULET) && !Rs2Inventory.contains(ECTOPHIAL)) {
                    Microbot.log("AnimalMagnetism: Need Ghostspeak Amulet or Ectophial in inventory.");
                    sleep(5000);
                    return;
                }
                state = State.WALK_TO_AVA;
                break;

            case WALK_TO_AVA:
                if (Rs2Player.getWorldLocation().distanceTo(AVA_LOCATION) <= 4) {
                    state = State.TALK_TO_AVA;
                } else {
                    Rs2Walker.walkTo(AVA_LOCATION, 4);
                    sleep(800);
                }
                break;

            case TALK_TO_AVA:
                // TODO: dialog — select "I'd like to speak to you about your research."
                if (Rs2Npc.interact(NPC_AVA, "Talk-to")) {
                    Microbot.log("AnimalMagnetism: Talk to Ava, follow dialog prompts.");
                    sleep(1200);
                    // State advances via varbit sync
                }
                sleep(600);
                break;

            case GET_UNDEAD_CHICKEN:
                if (Rs2Inventory.contains(UNDEAD_CHICKEN)) {
                    state = State.WALK_TO_ALICE;
                    return;
                }
                if (Rs2Player.getWorldLocation().distanceTo(UNDEAD_CHICKEN_FARM) > 8) {
                    Rs2Walker.walkTo(UNDEAD_CHICKEN_FARM, 5);
                    sleep(800);
                    return;
                }
                // TODO: pick up undead chicken — might require "Catch" or "Talk-to" interaction
                Rs2Npc.interact(NPC_UNDEAD_CHICKEN, "Catch");
                sleep(1000);
                break;

            case WALK_TO_ALICE:
                if (Rs2Player.getWorldLocation().distanceTo(ALICE_FARM) <= 5) {
                    state = State.TALK_TO_ALICE;
                } else {
                    Rs2Walker.walkTo(ALICE_FARM, 4);
                    sleep(800);
                }
                break;

            case TALK_TO_ALICE:
                // TODO: dialog — obtain magnet information and directions
                if (Rs2Inventory.contains(BAR_MAGNET)) {
                    state = State.GET_POLISHED_BUTTONS;
                    return;
                }
                if (Rs2Npc.interact(NPC_ALICE, "Talk-to")) {
                    sleep(1200);
                    state = State.TALK_TO_ALICES_HUSBAND;
                }
                break;

            case TALK_TO_ALICES_HUSBAND:
                // TODO: dialog — convince husband to sell the magnet (pays 200 gp)
                if (Rs2Inventory.contains(BAR_MAGNET)) {
                    state = State.GET_POLISHED_BUTTONS;
                    return;
                }
                if (Rs2Npc.interact(NPC_ALICE_HUSBAND, "Talk-to")) {
                    sleep(1200);
                }
                break;

            case GET_POLISHED_BUTTONS:
                int buttonCount = Rs2Inventory.count(POLISHED_BUTTONS);
                if (buttonCount >= POLISHED_BUTTONS_NEEDED) {
                    state = State.TALK_TO_GHOST_INNKEEPER;
                    return;
                }
                if (Rs2Player.getWorldLocation().distanceTo(UNDEAD_COW_FIELD) > 8) {
                    Rs2Walker.walkTo(UNDEAD_COW_FIELD, 5);
                    sleep(800);
                    return;
                }
                // Undead cows drop polished buttons — just attack them
                Rs2Npc.attack(NPC_UNDEAD_COW);
                sleep(800);
                break;

            case TALK_TO_GHOST_INNKEEPER:
                if (Rs2Player.getWorldLocation().distanceTo(GHOST_INNKEEPER) > 6) {
                    Rs2Walker.walkTo(GHOST_INNKEEPER, 4);
                    sleep(800);
                    return;
                }
                // TODO: need Ghostspeak Amulet equipped to talk to ghost
                Rs2Npc.interact(NPC_GHOST_INNKEEPER, "Talk-to");
                sleep(1200);
                state = State.RETURN_TO_AVA;
                break;

            case RETURN_TO_AVA:
                if (Rs2Player.getWorldLocation().distanceTo(AVA_LOCATION) <= 4) {
                    // TODO: dialog — hand over items to Ava to complete the quest
                    if (Rs2Npc.interact(NPC_AVA, "Talk-to")) {
                        Microbot.log("AnimalMagnetism: Returning all items to Ava to complete quest.");
                        sleep(1200);
                        state = State.DONE;
                    }
                } else {
                    Rs2Walker.walkTo(AVA_LOCATION, 4);
                    sleep(800);
                }
                break;

            case DONE:
                Microbot.log("AnimalMagnetism: Sequence complete — verifying via varbit.");
                sleep(2000);
                break;
        }
    }

    /** Returns true when all skill prerequisites are met. */
    private boolean prerequisitesMet() {
        return Microbot.getClient().getRealSkillLevel(Skill.SLAYER) >= 18
                && Microbot.getClient().getRealSkillLevel(Skill.CRAFTING) >= 19
                && Microbot.getClient().getRealSkillLevel(Skill.RANGED) >= 30
                && Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING) >= 35;
    }
}
