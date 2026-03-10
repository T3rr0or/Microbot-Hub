package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.quests;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/**
 * Automates Waterfall Quest.
 *
 * <p>Quest overview:
 * <ol>
 *   <li>Talk to Almera north of Baxtorian Falls — she asks you to find her son Hudon.</li>
 *   <li>Row the raft east of her house to the island and speak with Hudon.</li>
 *   <li>Use the rope on the rock, then the dead tree, to cross to the dungeon entrance.</li>
 *   <li>Climb down the rocks into the dungeon; loot the Giant Dwarf's chest for Book on Baxtorian.</li>
 *   <li>Go to Golrie in Tree Gnome Village dungeon to get Glarial's Pebble.</li>
 *   <li>Enter Glarial's Tomb (Taradin's farm), collect Glarial's Amulet and Glarial's Urn.</li>
 *   <li>Return to Baxtorian Falls, use Glarial's Amulet to enter the dungeon.</li>
 *   <li>Activate the 6 elements (3 water altars, 3 chalices) and use the urn + amulet on the altar.</li>
 * </ol>
 *
 * <p>Completion check: Varbit 222 == 7.
 * Source: OSRS Wiki — https://oldschool.runescape.wiki/w/Waterfall_Quest#Varbit
 * TODO: verify varbit value against live client before enabling in production.
 *
 * <p>Required items (brought from bank before quest):
 * <ul>
 *   <li>1 Rope</li>
 *   <li>6 Air runes, 6 Earth runes, 6 Water runes</li>
 *   <li>Glarial's Amulet and Glarial's Urn (obtained mid-quest)</li>
 * </ul>
 */
@Slf4j
public class WaterfallQuestTask extends AbstractTask {

    // ── Varbit ──────────────────────────────────────────────────────────────
    /**
     * Varbit ID tracking Waterfall Quest progress.
     * Source: OSRS Wiki https://oldschool.runescape.wiki/w/Waterfall_Quest#Varbit
     * TODO: verify against live client.
     */
    private static final int WATERFALL_QUEST_VARBIT = 222;
    private static final int COMPLETE_VALUE = 7;

    // ── Varbit progress thresholds ────────────────────────────────────────
    private static final int VARBIT_NOT_STARTED      = 0;
    private static final int VARBIT_TALKED_ALMERA    = 1;
    private static final int VARBIT_AT_ISLAND        = 2;
    private static final int VARBIT_RETURNED         = 3;
    private static final int VARBIT_GOT_PEBBLE       = 4;
    private static final int VARBIT_GOT_TOMB_ITEMS   = 5;
    private static final int VARBIT_INSIDE_CAVE      = 6;

    // ── WorldPoints ───────────────────────────────────────────────────────
    // TODO: verify all coordinates against live client
    private static final WorldPoint ALMERA_HOUSE    = new WorldPoint(2521, 3522, 0);
    private static final WorldPoint RAFT_LOCATION   = new WorldPoint(2508, 3520, 0);
    private static final WorldPoint ISLAND_HUDON    = new WorldPoint(2510, 3494, 0);
    private static final WorldPoint ROPE_ROCK       = new WorldPoint(2512, 3468, 0);
    private static final WorldPoint DEAD_TREE       = new WorldPoint(2512, 3465, 0);
    private static final WorldPoint DUNGEON_ENTRANCE= new WorldPoint(2511, 3462, 0);
    /** Golrie is imprisoned in the Tree Gnome Village dungeon. */
    private static final WorldPoint GOLRIE_LOCATION = new WorldPoint(2455, 9895, 0);
    /** Glarial's Tombstone in Taradin's farm. */
    private static final WorldPoint GLARIALS_TOMBSTONE = new WorldPoint(2530, 3444, 0);
    /** Inside the waterfall dungeon for the final altar. */
    private static final WorldPoint DUNGEON_ALTAR   = new WorldPoint(2604, 9897, 0);

    // ── Game Object IDs ───────────────────────────────────────────────────
    // TODO: verify all object IDs against live client
    private static final int RAFT_OBJECT_ID        = 2416;
    private static final int ROPE_ROCK_OBJECT_ID   = 2419;
    private static final int DEAD_TREE_OBJECT_ID   = 2418;
    private static final int ROCKS_OBJECT_ID       = 2415;  // climb-down into dungeon
    private static final int GOLRIE_CHEST_ID       = 2416;  // chest containing book (TODO: verify)
    private static final int GLARIALS_TOMB_ID      = 2406;  // tombstone entrance
    private static final int BAXTORIAN_ALTAR_ID    = 2436;  // final quest altar (TODO: verify)

    // ── NPC names ─────────────────────────────────────────────────────────
    private static final String NPC_ALMERA  = "Almera";
    private static final String NPC_HUDON   = "Hudon";
    private static final String NPC_GOLRIE  = "Golrie";

    // ── Item IDs ──────────────────────────────────────────────────────────
    private static final int GLARIALS_PEBBLE  = ItemID.GLARIALS_PEBBLE;
    private static final int GLARIALS_AMULET  = ItemID.GLARIALS_AMULET;
    private static final int GLARIALS_URN     = ItemID.GLARIALS_URN;
    private static final int BOOK_ON_BAXTORIAN = ItemID.BOOK_ON_BAXTORIAN;
    private static final int ROPE             = ItemID.ROPE;

    // ── State ─────────────────────────────────────────────────────────────
    private enum State {
        GATHER_SUPPLIES,
        WALK_TO_ALMERA,
        TALK_TO_ALMERA,
        WALK_TO_RAFT,
        ROW_TO_ISLAND,
        TALK_TO_HUDON,
        CROSS_ROPE_ROCK,
        CROSS_DEAD_TREE,
        ENTER_DUNGEON,
        LOOT_CHEST,
        WALK_TO_GOLRIE,
        TALK_TO_GOLRIE,
        WALK_TO_GLARIALS_TOMB,
        ENTER_GLARIALS_TOMB,
        LOOT_TOMB,
        RETURN_TO_FALLS,
        ENTER_WATERFALL_DUNGEON,
        COMPLETE_ALTAR,
        DONE
    }

    private State state = State.GATHER_SUPPLIES;

    public WaterfallQuestTask(AccountProfile profile) {
        super(profile);
    }

    @Override
    public String getName() {
        return "Waterfall Quest";
    }

    @Override
    public boolean isComplete() {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getVarbitValue(WATERFALL_QUEST_VARBIT) >= COMPLETE_VALUE
        ).orElse(false);
    }

    @Override
    public void execute() {
        maybeIdle();

        // Sync state to varbit to recover from interruptions
        int varbit = Microbot.getClientThread()
                .runOnClientThreadOptional(() -> Microbot.getClient().getVarbitValue(WATERFALL_QUEST_VARBIT))
                .orElse(0);

        if (varbit >= COMPLETE_VALUE) return;
        if (varbit >= VARBIT_INSIDE_CAVE  && state.ordinal() < State.COMPLETE_ALTAR.ordinal())  state = State.COMPLETE_ALTAR;
        if (varbit >= VARBIT_GOT_TOMB_ITEMS && state.ordinal() < State.RETURN_TO_FALLS.ordinal()) state = State.RETURN_TO_FALLS;
        if (varbit >= VARBIT_GOT_PEBBLE   && state.ordinal() < State.WALK_TO_GLARIALS_TOMB.ordinal()) state = State.WALK_TO_GLARIALS_TOMB;
        if (varbit >= VARBIT_RETURNED     && state.ordinal() < State.WALK_TO_GOLRIE.ordinal())  state = State.WALK_TO_GOLRIE;
        if (varbit >= VARBIT_AT_ISLAND    && state.ordinal() < State.TALK_TO_HUDON.ordinal())   state = State.TALK_TO_HUDON;
        if (varbit >= VARBIT_TALKED_ALMERA && state.ordinal() < State.WALK_TO_RAFT.ordinal())   state = State.WALK_TO_RAFT;

        log.debug("WaterfallQuest state={} varbit={}", state, varbit);

        switch (state) {

            case GATHER_SUPPLIES:
                // Check for rope; other items are obtained mid-quest
                if (!Rs2Inventory.contains(ROPE)) {
                    // TODO: bank for rope, 6 air runes, 6 earth runes, 6 water runes
                    Microbot.log("WaterfallQuest: Need rope in inventory — please bank for supplies.");
                    sleep(5000);
                    return;
                }
                state = State.WALK_TO_ALMERA;
                break;

            case WALK_TO_ALMERA:
                if (Rs2Player.getWorldLocation().distanceTo(ALMERA_HOUSE) <= 4) {
                    state = State.TALK_TO_ALMERA;
                } else {
                    Rs2Walker.walkTo(ALMERA_HOUSE, 4);
                    sleep(800);
                }
                break;

            case TALK_TO_ALMERA:
                // TODO: dialog auto-continue — click through Almera's dialog
                if (Rs2Npc.interact(NPC_ALMERA, "Talk-to")) {
                    sleep(600);
                    // Advance dialog options: "I'll look for your son." → confirm
                    // Rs2Widget.clickWidget(DIALOG_CONTINUE_WIDGET_ID);
                    Microbot.log("WaterfallQuest: Talk to Almera, select 'Hudon... I'll look for your son.'");
                    // State will auto-advance via varbit sync on next tick
                }
                sleep(600);
                break;

            case WALK_TO_RAFT:
                if (Rs2Player.getWorldLocation().distanceTo(RAFT_LOCATION) <= 4) {
                    state = State.ROW_TO_ISLAND;
                } else {
                    Rs2Walker.walkTo(RAFT_LOCATION, 4);
                    sleep(800);
                }
                break;

            case ROW_TO_ISLAND:
                // TODO: verify RAFT_OBJECT_ID against live client
                if (Rs2GameObject.interact(RAFT_OBJECT_ID, "Row")) {
                    sleep(3000); // wait for rowing animation
                    state = State.TALK_TO_HUDON;
                } else {
                    Microbot.log("WaterfallQuest: Could not interact with raft (ID " + RAFT_OBJECT_ID + ")");
                    sleep(1000);
                }
                break;

            case TALK_TO_HUDON:
                if (Rs2Player.getWorldLocation().distanceTo(ISLAND_HUDON) > 5) {
                    Rs2Walker.walkTo(ISLAND_HUDON, 3);
                    sleep(800);
                    return;
                }
                // TODO: dialog — Hudon's conversation is brief
                if (Rs2Npc.interact(NPC_HUDON, "Talk-to")) {
                    sleep(1200);
                    state = State.CROSS_ROPE_ROCK;
                }
                break;

            case CROSS_ROPE_ROCK:
                if (Rs2Player.getWorldLocation().distanceTo(ROPE_ROCK) > 5) {
                    Rs2Walker.walkTo(ROPE_ROCK, 3);
                    sleep(800);
                    return;
                }
                // Use rope on rock
                // TODO: verify ROPE_ROCK_OBJECT_ID
                if (Rs2Inventory.contains(ROPE) && Rs2GameObject.interact(ROPE_ROCK_OBJECT_ID, "Use-rope")) {
                    sleep(2000);
                    state = State.CROSS_DEAD_TREE;
                } else {
                    Rs2GameObject.interact(ROPE_ROCK_OBJECT_ID, "Cross");
                    sleep(2000);
                    state = State.CROSS_DEAD_TREE;
                }
                break;

            case CROSS_DEAD_TREE:
                if (Rs2Player.getWorldLocation().distanceTo(DEAD_TREE) > 5) {
                    Rs2Walker.walkTo(DEAD_TREE, 3);
                    sleep(800);
                    return;
                }
                // TODO: verify DEAD_TREE_OBJECT_ID
                if (Rs2GameObject.interact(DEAD_TREE_OBJECT_ID, "Swing-on")) {
                    sleep(2000);
                    state = State.ENTER_DUNGEON;
                }
                break;

            case ENTER_DUNGEON:
                if (Rs2Player.getWorldLocation().distanceTo(DUNGEON_ENTRANCE) > 5) {
                    Rs2Walker.walkTo(DUNGEON_ENTRANCE, 3);
                    sleep(800);
                    return;
                }
                // TODO: verify ROCKS_OBJECT_ID
                if (Rs2GameObject.interact(ROCKS_OBJECT_ID, "Climb-down")) {
                    sleep(2000);
                    state = State.LOOT_CHEST;
                }
                break;

            case LOOT_CHEST:
                // TODO: search dungeon chest for Book on Baxtorian
                // TODO: verify GOLRIE_CHEST_ID (Giant Dwarf's chest)
                if (Rs2Inventory.contains(BOOK_ON_BAXTORIAN)) {
                    state = State.WALK_TO_GOLRIE;
                    return;
                }
                if (Rs2GameObject.interact(GOLRIE_CHEST_ID, "Search")) {
                    sleep(1500);
                }
                break;

            case WALK_TO_GOLRIE:
                // Golrie is in the Tree Gnome Village dungeon (requires navigating via trapdoor)
                // TODO: implement full navigation to Golrie including entering the dungeon
                if (Rs2Inventory.contains(GLARIALS_PEBBLE)) {
                    state = State.WALK_TO_GLARIALS_TOMB;
                    return;
                }
                Microbot.log("WaterfallQuest: Walking to Golrie in Tree Gnome Village...");
                Rs2Walker.walkTo(GOLRIE_LOCATION, 3);
                sleep(800);
                break;

            case TALK_TO_GOLRIE:
                // TODO: dialog — ask Golrie for Glarial's pebble
                if (Rs2Inventory.contains(GLARIALS_PEBBLE)) {
                    state = State.WALK_TO_GLARIALS_TOMB;
                    return;
                }
                if (Rs2Npc.interact(NPC_GOLRIE, "Talk-to")) {
                    sleep(1200);
                }
                break;

            case WALK_TO_GLARIALS_TOMB:
                if (Rs2Inventory.contains(GLARIALS_AMULET) && Rs2Inventory.contains(GLARIALS_URN)) {
                    state = State.RETURN_TO_FALLS;
                    return;
                }
                if (Rs2Player.getWorldLocation().distanceTo(GLARIALS_TOMBSTONE) <= 5) {
                    state = State.ENTER_GLARIALS_TOMB;
                } else {
                    Rs2Walker.walkTo(GLARIALS_TOMBSTONE, 4);
                    sleep(800);
                }
                break;

            case ENTER_GLARIALS_TOMB:
                // Use Glarial's Pebble on tombstone
                // TODO: verify GLARIALS_TOMB_ID
                if (Rs2Inventory.contains(GLARIALS_PEBBLE)) {
                    Rs2Inventory.interact(GLARIALS_PEBBLE, "Use");
                    Rs2GameObject.interact(GLARIALS_TOMB_ID, "Open");
                    sleep(1500);
                    state = State.LOOT_TOMB;
                }
                break;

            case LOOT_TOMB:
                // Search the tomb for Glarial's amulet and urn
                // TODO: interact with correct tomb chest/container
                if (Rs2Inventory.contains(GLARIALS_AMULET) && Rs2Inventory.contains(GLARIALS_URN)) {
                    state = State.RETURN_TO_FALLS;
                } else {
                    Microbot.log("WaterfallQuest: Searching tomb for Glarial's amulet and urn...");
                    // TODO: Rs2GameObject.interact(TOMB_CHEST_ID, "Search");
                    sleep(1000);
                }
                break;

            case RETURN_TO_FALLS:
                if (Rs2Player.getWorldLocation().distanceTo(RAFT_LOCATION) <= 5) {
                    state = State.ENTER_WATERFALL_DUNGEON;
                } else {
                    Rs2Walker.walkTo(RAFT_LOCATION, 4);
                    sleep(800);
                }
                break;

            case ENTER_WATERFALL_DUNGEON:
                // Wear Glarial's Amulet to enter
                if (Rs2Inventory.contains(GLARIALS_AMULET)) {
                    Rs2Inventory.interact(GLARIALS_AMULET, "Wear");
                    sleep(400);
                }
                // TODO: dive into waterfall — exact object ID needed
                // Rs2GameObject.interact(WATERFALL_DIVING_ID, "Dive-in");
                Microbot.log("WaterfallQuest: Entering waterfall dungeon...");
                sleep(1000);
                state = State.COMPLETE_ALTAR;
                break;

            case COMPLETE_ALTAR:
                // Walk to altar, use Glarial's Urn + Amulet on it
                if (Rs2Player.getWorldLocation().distanceTo(DUNGEON_ALTAR) > 6) {
                    Rs2Walker.walkTo(DUNGEON_ALTAR, 4);
                    sleep(800);
                    return;
                }
                // TODO: activate 6 elements (3 water + 3 earth runes on pillars) before using urn
                // TODO: verify BAXTORIAN_ALTAR_ID
                if (Rs2Inventory.contains(GLARIALS_URN)) {
                    Rs2Inventory.interact(GLARIALS_URN, "Use");
                    Rs2GameObject.interact(BAXTORIAN_ALTAR_ID, "Use");
                    sleep(1500);
                }
                Microbot.log("WaterfallQuest: Completing altar ritual...");
                state = State.DONE;
                break;

            case DONE:
                Microbot.log("WaterfallQuest: Sequence complete — verifying via varbit.");
                sleep(2000);
                break;
        }
    }
}
