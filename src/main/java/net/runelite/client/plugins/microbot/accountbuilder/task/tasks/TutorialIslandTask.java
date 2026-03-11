package net.runelite.client.plugins.microbot.accountbuilder.task.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.task.AbstractTask;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.NameGenerator;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.*;
import static net.runelite.client.plugins.microbot.util.settings.Rs2Settings.*;

/**
 * Completes Tutorial Island automatically as the first task in the account-builder pipeline.
 *
 * <p>Adapts {@code TutorialIslandScript}'s automation logic into the {@link AbstractTask} pattern
 * so it runs within AccountBuilderScript's tick loop instead of a separate scheduler.
 * All tutorial sections are driven by varbit 281 — the same approach as the original plugin.
 *
 * <p>Default settings applied (good for fresh account automation):
 * <ul>
 *   <li>Music muted</li>
 *   <li>Roofs hidden</li>
 *   <li>Shift-drop enabled</li>
 *   <li>Level-up notifications disabled</li>
 * </ul>
 *
 * <p>Completion: varbit 281 == 1000.
 */
@Slf4j
public class TutorialIslandTask extends AbstractTask {

    // ── Widget IDs ────────────────────────────────────────────────────────
    private static final int NAME_CREATION_WIDGET      = 558;
    private static final int CHARACTER_CREATION_WIDGET = 679;
    private static final int[] CHARACTER_CREATION_ARROWS = {13,17,21,25,29,33,37,44,48,52,56,60};

    // "Want more bank space?" upsell popup shown when the bank is first opened (group 289)
    private static final int BANK_UPSELL_WIDGET        = 289;
    // Account guide tutorial overlay shown during the banker section (group 310)
    private static final int ACCOUNT_GUIDE_WIDGET      = 310;

    // ── State ─────────────────────────────────────────────────────────────
    private enum Status {
        NAME, CHARACTER, GETTING_STARTED, SURVIVAL_GUIDE, COOKING_GUIDE,
        QUEST_GUIDE, MINING_GUIDE, COMBAT_GUIDE, BANKER_GUIDE, PRAYER_GUIDE,
        MAGE_GUIDE, FINISHED
    }

    // ── Instance state ─────────────────────────────────────────────────
    private boolean initialized       = false;
    private boolean toggledSettings   = false;
    private boolean toggledMusic      = false;
    private boolean hasSelectedGender = false;

    public TutorialIslandTask(AccountProfile profile) {
        super(profile);
    }

    @Override
    public String getName() {
        return "Tutorial Island";
    }

    @Override
    public boolean isComplete() {
        // Microbot.getVarbitPlayerValue() reads from the client's cached varbit array and is
        // safe to call from the scheduler thread (consistent with other AbstractTask subclasses).
        return Microbot.getVarbitPlayerValue(281) == 1000 || isOnMainland();
    }

    @Override
    public void execute() {
        if (!initialized) {
            Rs2Antiban.resetAntibanSettings();
            Rs2AntibanSettings.naturalMouse = true;
            Rs2AntibanSettings.moveMouseRandomly = true;
            Rs2AntibanSettings.simulateMistakes = true;
            initialized = true;
        }

        Status status = calculateStatus();

        if (hasContinue()) {
            clickContinue();
            return;
        }

        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

        switch (status) {
            case NAME:            handleName();          break;
            case CHARACTER:       randomizeCharacter();  break;
            case GETTING_STARTED: gettingStarted();      break;
            case SURVIVAL_GUIDE:  survivalGuide();       break;
            case COOKING_GUIDE:   cookingGuide();        break;
            case QUEST_GUIDE:     questGuide();          break;
            case MINING_GUIDE:    miningGuide();         break;
            case COMBAT_GUIDE:    combatGuide();         break;
            case BANKER_GUIDE:    bankerGuide();         break;
            case PRAYER_GUIDE:    prayerGuide();         break;
            case MAGE_GUIDE:      mageGuide();           break;
            case FINISHED:        /* isComplete() will catch this next tick */ break;
        }
    }

    // ── Timing helper ─────────────────────────────────────────────────────

    /**
     * Sleep for {@code base} ms scaled by {@link AccountProfile#getReactionVariance()},
     * plus up to {@code jitter} ms of additional randomness via {@link Rs2Random#waitEx}.
     */
    private void waitEx(int base, int jitter) {
        Rs2Random.waitEx((int)(base * profile.getReactionVariance()), jitter);
    }

    // ── Status calculation ────────────────────────────────────────────────

    private Status calculateStatus() {
        if (isNameCreationVisible())      return Status.NAME;
        if (isCharacterCreationVisible()) return Status.CHARACTER;

        int v = Microbot.getVarbitPlayerValue(281);
        if (v < 10)   return Status.GETTING_STARTED;
        if (v < 120)  return Status.SURVIVAL_GUIDE;
        if (v < 200)  return Status.COOKING_GUIDE;
        if (v <= 250) return Status.QUEST_GUIDE;
        if (v <= 360) return Status.MINING_GUIDE;
        if (v < 510)  return Status.COMBAT_GUIDE;
        if (v < 540)  return Status.BANKER_GUIDE;
        if (v < 610)  return Status.PRAYER_GUIDE;
        if (v < 1000) return Status.MAGE_GUIDE;
        return Status.FINISHED;
    }

    private boolean isNameCreationVisible() {
        return Rs2Widget.isWidgetVisible(NAME_CREATION_WIDGET, 2);
    }

    private boolean isCharacterCreationVisible() {
        return Rs2Widget.isWidgetVisible(CHARACTER_CREATION_WIDGET, 4);
    }

    /**
     * Returns true when the player is not in Tutorial Island.
     * Tutorial Island is roughly within x:3080-3135, y:3075-3135 plane:0.
     */
    private boolean isOnMainland() {
        WorldPoint loc = Rs2Player.getWorldLocation();
        boolean inTutorialBounds = loc.getX() >= 3080 && loc.getX() <= 3135
                && loc.getY() >= 3075 && loc.getY() <= 3135;
        return !inTutorialBounds;
    }

    // ── Section handlers (adapted from TutorialIslandScript) ─────────────

    private void handleName() {
        Widget nameField = Rs2Widget.getWidget(NAME_CREATION_WIDGET, 12);
        if (nameField == null) return;

        String current = nameField.getText();
        if (current.endsWith("*")) current = current.substring(0, current.length() - 1);

        if (!current.isEmpty()) {
            Rs2Widget.clickWidget(NAME_CREATION_WIDGET, 7);
            waitEx(1200, 300);
            for (int i = 0; i < current.length(); i++) {
                Rs2Keyboard.keyPress(KeyEvent.VK_BACK_SPACE);
                waitEx(600, 100);
            }
            return;
        }

        String name = new NameGenerator(Rs2Random.between(7, 10)).getName();
        Rs2Widget.clickWidget(NAME_CREATION_WIDGET, 7);
        waitEx(1200, 300);
        Rs2Keyboard.typeString(name);
        waitEx(2400, 600);
        Rs2Widget.clickWidget(NAME_CREATION_WIDGET, 18);
        waitEx(4800, 600);

        Widget response = Rs2Widget.getWidget(NAME_CREATION_WIDGET, 13);
        if (response != null) {
            String cleaned = Rs2UiHelper.stripColTags(response.getText());
            if (cleaned.startsWith("Great! The display name " + name + " is available")) {
                Rs2Widget.clickWidget(NAME_CREATION_WIDGET, 19);
                waitEx(4800, 600);
                sleepUntil(() -> !isNameCreationVisible());
            }
        }
    }

    private void randomizeCharacter() {
        if (Rs2Random.diceFractional(0.2)) {
            selectGender();

            if (Rs2Random.diceFractional(0.25)) {
                Widget pronounWidget = Rs2Widget.getWidget(CHARACTER_CREATION_WIDGET, 72);
                if (pronounWidget == null) return;

                Widget currentPronoun = Arrays.stream(pronounWidget.getDynamicChildren())
                        .filter(w -> w.getText().toLowerCase().contains("he/him")
                                  || w.getText().toLowerCase().contains("they/them")
                                  || w.getText().toLowerCase().contains("she/her"))
                        .findFirst().orElse(null);
                Rs2Widget.clickWidget(pronounWidget);
                waitEx(1200, 300);
                sleepUntil(() -> Rs2Widget.isWidgetVisible(CHARACTER_CREATION_WIDGET, 76));

                Widget optionsContainer = Rs2Widget.getWidget(CHARACTER_CREATION_WIDGET, 78);
                if (optionsContainer == null) return;
                Widget[] options = optionsContainer.getDynamicChildren();

                Widget choice = null;
                if (currentPronoun != null) {
                    if (currentPronoun.getText().toLowerCase().contains("he/him")) {
                        choice = Arrays.stream(options)
                                .filter(w -> w.getText().toLowerCase().contains(Rs2Random.diceFractional(0.5) ? "they/them" : "she/her"))
                                .findFirst().orElse(null);
                    } else {
                        choice = Arrays.stream(options)
                                .filter(w -> w.getText().toLowerCase().contains(Rs2Random.diceFractional(0.5) ? "they/them" : "he/him"))
                                .findFirst().orElse(null);
                    }
                }
                if (choice != null) {
                    Rs2Widget.clickWidget(choice);
                    waitEx(1200, 300);
                    sleepUntil(() -> !Rs2Widget.isWidgetVisible(CHARACTER_CREATION_WIDGET, 76));
                }
            }

            Rs2Widget.clickWidget(CHARACTER_CREATION_WIDGET, 74);
            waitEx(1200, 300);
            sleepUntil(() -> !isCharacterCreationVisible());
        }

        int idx = (int)(Math.random() * CHARACTER_CREATION_ARROWS.length);
        int item = CHARACTER_CREATION_ARROWS[idx] + (Math.random() < 0.5 ? 2 : 3);
        Widget widget = Rs2Widget.getWidget(CHARACTER_CREATION_WIDGET, item);
        if (widget == null) return;
        for (int i = 0; i < Rs2Random.between(1, 6); i++) {
            Rs2Widget.clickWidget(widget.getId());
            waitEx(300, 50);
        }
    }

    private void selectGender() {
        if (Rs2Random.diceFractional(0.5) && !hasSelectedGender) {
            Widget maleWidget   = Rs2Widget.getWidget(CHARACTER_CREATION_WIDGET, 68);
            Widget femaleWidget = Rs2Widget.getWidget(CHARACTER_CREATION_WIDGET, 69);
            int selectedColor = 0xaaaaaa;

            boolean hasMaleSelected   = Arrays.stream(maleWidget.getDynamicChildren()).anyMatch(w -> w != null && w.getTextColor() == selectedColor);
            boolean hasFemaleSelected = Arrays.stream(femaleWidget.getDynamicChildren()).anyMatch(w -> w != null && w.getTextColor() == selectedColor);

            if (hasFemaleSelected) {
                Rs2Widget.clickWidget(maleWidget);
                waitEx(1200, 300);
            } else if (hasMaleSelected) {
                Rs2Widget.clickWidget(femaleWidget);
                waitEx(1200, 300);
            }
        }
        hasSelectedGender = true;
    }

    private void gettingStarted() {
        var npc = Rs2Npc.getNpc(NpcID.GIELINOR_GUIDE);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v < 3) {
            if (isInDialogue()) {
                Rs2Keyboard.typeString(Integer.toString(Rs2Random.between(1, 3)));
                return;
            }
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue);
            }
        } else if (v < 8) {
            if (!toggledSettings) {
                Rs2Widget.clickWidget(164, 41);
                toggledSettings = true;
                waitEx(1200, 300);
                return;
            }
            if (!toggledMusic) {
                turnOffMusic();
                toggledMusic = true;
                waitEx(1200, 300);
                return;
            }
            if (!isHideRoofsEnabled()) {
                hideRoofs(false);
                waitEx(1200, 300);
                return;
            }
            if (!isDropShiftSettingEnabled()) {
                enableDropShiftSetting(false);
                waitEx(1200, 300);
                return;
            }
            if (isLevelUpNotificationsEnabled()) {
                disableLevelUpNotifications(true);
                waitEx(1200, 300);
                return;
            }
            Rs2Camera.setZoom(Rs2Random.between(400, 450));
            waitEx(300, 100);
            Rs2Camera.setPitch(280);
            sleepUntil(() -> Rs2Camera.getPitch() > 250);
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue);
            }
        } else {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue);
            }
        }
    }

    private void survivalGuide() {
        var npc = Rs2Npc.getNpc(NpcID.SURVIVAL_EXPERT);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 10 || v == 20 || v == 60) {
            if (!Rs2Npc.hasLineOfSight(npc)) {
                Rs2Walker.walkTo(npc.getWorldLocation(), 4);
                Rs2Player.waitForWalking();
            }
            if (Rs2Npc.interact(npc, "talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue);
            }
        } else if (v < 40) {
            waitEx(1200, 300);
            var widget = Rs2Widget.findWidget("Inventory", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else if (v < 50) {
            fishShrimp();
        } else if (v < 70) {
            var widget = Rs2Widget.findWidget("Skills", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
            if (Rs2Npc.interact(npc, "talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue);
            }
        } else if (v <= 90) {
            if (!Rs2Inventory.hasItem("Bronze Axe") || !Rs2Inventory.hasItem("Tinderbox")) {
                if (Rs2Npc.interact(npc, "talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
                return;
            }
            if (!Rs2Inventory.contains("Raw shrimps")) { fishShrimp(); return; }
            if (!Rs2Inventory.contains("Logs") && (!Rs2GameObject.exists(ObjectID.FIRE_26185) || Rs2Player.getRealSkillLevel(Skill.WOODCUTTING) == 0)) {
                cutTree(); return;
            }
            if (!Rs2GameObject.exists(ObjectID.FIRE_26185)) { lightFire(); return; }
            Rs2Inventory.useItemOnObject(ItemID.RAW_SHRIMPS_2514, ObjectID.FIRE_26185);
        }
    }

    private void cookingGuide() {
        var npc = Rs2Npc.getNpc(NpcID.MASTER_CHEF);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 120) {
            waitEx(1200, 300);
            Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
            Rs2GameObject.interact(ObjectID.GATE_9470, "Open");
            sleepUntil(() -> Microbot.getVarbitPlayerValue(281) != 120);
        } else if (v == 130) {
            Rs2GameObject.interact(ObjectID.DOOR_9709, "Open");
            sleepUntil(() -> Microbot.getVarbitPlayerValue(281) != 130);
        } else if (v == 140) {
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v >= 150 && v < 200) {
            if (!Rs2Inventory.contains("Bread dough") && !Rs2Inventory.contains("Bread")) {
                Rs2Inventory.combine("Bucket of water", "Pot of flour");
                sleepUntil(() -> Rs2Inventory.contains("Dough"), 2000);
            } else if (Rs2Inventory.contains("Bread dough")) {
                Rs2Inventory.interact("Bread dough");
                Rs2GameObject.interact(9736, "Use");
                sleepUntil(() -> Rs2Inventory.contains("Bread"));
            } else if (Rs2Inventory.contains("Bread")) {
                if (Rs2GameObject.interact(9710, "Open")) waitEx(2400, 100);
            }
        }
    }

    private void questGuide() {
        var npc = Rs2Npc.getNpc(NpcID.QUEST_GUIDE);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 200 || v == 210) {
            Rs2Walker.walkTo(new WorldPoint(Rs2Random.between(3083, 3086), Rs2Random.between(3127, 3129), 0));
            Rs2GameObject.interact(9716, "Open");
            waitEx(1200, 300);
        } else if (v == 220 || v == 240) {
            Rs2Npc.interact(npc, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 230) {
            var widget = Rs2Widget.findWidget("Quest List", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else {
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            waitEx(600, 100);
            Rs2GameObject.interact(9726, "Climb-down");
            waitEx(2400, 100);
        }
    }

    private void miningGuide() {
        var npc = Rs2Npc.getNpc(NpcID.MINING_INSTRUCTOR);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 260) {
            Rs2Walker.walkTo(new WorldPoint(Rs2Random.between(3082, 3085), Rs2Random.between(9502, 9505), 0));
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else {
            if (Rs2Inventory.contains("Bronze dagger")) {
                Rs2GameObject.interact(ObjectID.GATE_9718, "Open");
                sleepUntil(() -> Microbot.getVarbitPlayerValue(281) > 360);
                return;
            }
            if (Rs2Inventory.contains("Bronze bar") && Rs2Inventory.contains("Hammer")) {
                Rs2GameObject.interact("Anvil", "Smith");
                sleepUntil(Rs2Widget::isSmithingWidgetOpen);
                Rs2Widget.clickWidget(312, 9);
                waitEx(1200, 300);
                sleepUntil(() -> Rs2Inventory.contains("Bronze dagger") && !Rs2Player.isAnimating(1800));
                return;
            }
            if (Rs2Inventory.contains("Bronze bar") && !Rs2Inventory.contains("Hammer")) {
                if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
                return;
            }
            if (Rs2Inventory.contains("Bronze pickaxe") && (!Rs2Inventory.contains("Copper ore") || !Rs2Inventory.contains("Tin ore"))) {
                List<Integer> rockIds = new ArrayList<>();
                if (!Rs2Inventory.contains("Copper ore")) rockIds.add(ObjectID.COPPER_ROCKS);
                if (!Rs2Inventory.contains("Tin ore"))   rockIds.add(ObjectID.TIN_ROCKS);
                Collections.shuffle(rockIds);
                int rockId = rockIds.get(0);
                Rs2GameObject.interact(rockId, "Mine");
                sleepUntil(() -> rockId == ObjectID.COPPER_ROCKS
                        ? Rs2Inventory.contains("Copper ore") && !Rs2Player.isAnimating(1800)
                        : Rs2Inventory.contains("Tin ore")    && !Rs2Player.isAnimating(1800));
            } else if (Rs2Inventory.contains("Copper ore") && Rs2Inventory.contains("Tin ore")) {
                int[] ores = {ItemID.TIN_ORE, ItemID.COPPER_ORE};
                Collections.shuffle(Arrays.asList(ores));
                Rs2Inventory.useItemOnObject(ores[0], ObjectID.FURNACE_10082);
                sleepUntil(() -> Rs2Inventory.contains("Bronze bar") && !Rs2Player.isAnimating(1800));
            }
        }
    }

    private void combatGuide() {
        var npc = Rs2Npc.getNpc(NpcID.COMBAT_INSTRUCTOR);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v <= 370) {
            Rs2Walker.walkTo(new WorldPoint(Rs2Random.between(3106, 3108), Rs2Random.between(9508, 9510), 0));
            Rs2Player.waitForWalking();
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v <= 410) {
            if (isInDialogue()) { clickContinue(); return; }
            var widget = Rs2Widget.findWidget("Worn Equipment", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
            Rs2Widget.clickWidget(387, 1);
            sleepUntil(() -> Rs2Widget.getWidget(84, 1) != null);
            waitEx(1200, 300);
            Rs2Widget.clickWidget("Bronze dagger");
            waitEx(2400, 300);
            if (Rs2Widget.isWidgetVisible(84, 3)) {
                Widget opts = Rs2Widget.getWidget(84, 3);
                for (Widget child : opts.getDynamicChildren()) {
                    if (child.getActions() != null && Arrays.stream(child.getActions()).anyMatch(a -> a != null && a.equalsIgnoreCase("close"))) {
                        Rs2Widget.clickWidget(child); waitEx(1200, 300); break;
                    }
                }
            }
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 500) {
            Rs2Walker.walkTo(new WorldPoint(3111, 9526, Rs2Player.getWorldLocation().getPlane()));
            Rs2Player.waitForWalking();
            Rs2GameObject.interact("Ladder", "Climb-up");
            sleepUntil(() -> Microbot.getVarbitPlayerValue(281) != 500);
        } else if (v == 480 || v == 490) {
            if (Rs2Player.getInteracting() != null && Rs2Player.getInteracting().getName() != null
                    && Rs2Player.getInteracting().getName().equalsIgnoreCase("giant rat")) return;
            Rs2Inventory.wield("Shortbow");
            waitEx(600, 100);
            Rs2Inventory.wield("Bronze arrow");
            waitEx(600, 100);
            if (Rs2Random.between(1, 5) == 2) Rs2Walker.walkTo(new WorldPoint(3110, 9523, 0), 4);
            Rs2Player.waitForWalking();
            Rs2Npc.attack("Giant rat");
        } else if (v == 470) {
            Rs2Walker.walkTo(npc.getWorldLocation());
            Rs2Player.waitForWalking();
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v >= 420) {
            if (Microbot.getClient().getLocalPlayer().isInteracting() || Rs2Player.isAnimating()) return;
            if (Rs2Equipment.isWearing("Bronze sword")) {
                var combatWidget = Rs2Widget.findWidget("Combat Options", true);
                Rs2Widget.clickWidget(combatWidget);
                waitEx(1200, 300);
                Rs2Walker.walkTo(new WorldPoint(3105, 9517, 0), 3);
                Rs2Player.waitForWalking();
                Rs2Npc.attack("Giant rat");
            } else {
                Rs2Tab.switchTo(InterfaceTab.INVENTORY);
                waitEx(600, 100);
                Rs2Inventory.wield("Bronze sword");
                waitEx(600, 100);
                Rs2Inventory.wield("Wooden shield");
            }
        }
    }

    private void bankerGuide() {
        var npc = Rs2Npc.getNpc(NpcID.ACCOUNT_GUIDE);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 510) {
            Rs2GameObject.interact(ObjectID.BANK_BOOTH_10083);
            sleepUntil(() -> Microbot.getVarbitPlayerValue(281) != 510);
        } else if (v == 520) {
            if (Rs2Widget.isWidgetVisible(BANK_UPSELL_WIDGET, 5)) {
                Widget opts = Rs2Widget.getWidget(BANK_UPSELL_WIDGET, 4);
                for (Widget child : opts.getDynamicChildren()) {
                    if (child.getText() != null && child.getText().equalsIgnoreCase("Want more bank space?")) {
                        Rs2Widget.clickWidget(BANK_UPSELL_WIDGET, 7); waitEx(1200, 300); break;
                    }
                }
            }
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen());
            Rs2GameObject.interact(26815);
            sleepUntil(() -> Microbot.getVarbitPlayerValue(281) != 520);
        } else if (v == 525 || v == 530) {
            if (Rs2Widget.isWidgetVisible(ACCOUNT_GUIDE_WIDGET, 2)) {
                Widget opts = Rs2Widget.getWidget(ACCOUNT_GUIDE_WIDGET, 2);
                for (Widget child : opts.getDynamicChildren()) {
                    if (child.getActions() != null && Arrays.stream(child.getActions()).anyMatch(a -> a != null && a.equalsIgnoreCase("close"))) {
                        Rs2Widget.clickWidget(child); waitEx(1200, 300); break;
                    }
                }
            }
            Rs2Walker.walkTo(npc.getWorldLocation(), 3);
            Rs2Player.waitForWalking();
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 531) {
            var widget = Rs2Widget.findWidget("Account Management", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else if (v == 532) {
            if (Rs2Dialogue.isInDialogue()) { clickContinue(); return; }
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        }
    }

    private void prayerGuide() {
        var npc = Rs2Npc.getNpc(NpcID.BROTHER_BRACE);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 540 || v == 550) {
            Rs2Walker.walkTo(new WorldPoint(3124, 3106, 0));
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 560) {
            var widget = Rs2Widget.findWidget("Prayer", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else if (v == 570) {
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 580) {
            var widget = Rs2Widget.findWidget("Friends list", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else if (v == 600) {
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        }
    }

    private void mageGuide() {
        var npc = Rs2Npc.getNpc(NpcID.MAGIC_INSTRUCTOR);
        int v = Microbot.getVarbitPlayerValue(281);

        if (v == 610 || v == 620) {
            WorldPoint target = (npc != null) ? npc.getWorldLocation() : new WorldPoint(3141, 3088, 0);
            if (Rs2Player.distanceTo(target) > 8) {
                Rs2Walker.walkTo(target, 8);
            } else {
                if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
            }
        } else if (v == 630) {
            var widget = Rs2Widget.findWidget("Magic", true);
            Rs2Widget.clickWidget(widget);
            waitEx(1200, 300);
        } else if (v == 640) {
            if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
        } else if (v == 650) {
            widgetCastWindStrike();
        } else if (v == 670) {
            Rs2Dialogue.clickContinue();
            if (isInDialogue()) {
                if (Rs2Widget.hasWidget("Do you want to go to the mainland?")) {
                    Rs2Keyboard.typeString("1");
                    return;
                }
                if (hasSelectAnOption()) {
                    Widget opts = Rs2Widget.getWidget(219, 1);
                    Widget[] children = opts.getDynamicChildren();
                    for (int i = 0; i < children.length; i++) {
                        String text = children[i].getText();
                        if (text.contains("Yes, send me to the mainland") || text.contains("No, I'm not planning to do that")) {
                            Rs2Keyboard.typeString(String.valueOf(i));
                            break;
                        }
                    }
                }
            } else {
                if (Rs2Npc.interact(npc, "Talk-to")) sleepUntil(Rs2Dialogue::isInDialogue);
            }
        }
    }

    // ── Utility methods ───────────────────────────────────────────────────

    private void lightFire() {
        if (Rs2Player.isStandingOnGameObject()) {
            WorldPoint nearest = Rs2Tile.getNearestWalkableTileWithLineOfSight(Rs2Player.getWorldLocation());
            Rs2Walker.walkFastCanvas(nearest);
            Rs2Player.waitForWalking();
        }
        Rs2Inventory.combine("Logs", "Tinderbox");
        sleepUntil(() -> !Rs2Inventory.hasItem("Logs") && !Rs2Player.isAnimating(2400));
    }

    private void cutTree() {
        Rs2GameObject.interact("Tree", "Chop down");
        sleepUntil(() -> Rs2Inventory.hasItem("Logs") && !Rs2Player.isAnimating(2400));
    }

    private void fishShrimp() {
        Rs2Npc.interact(NpcID.FISHING_SPOT_3317, "Net");
        sleepUntil(() -> Rs2Inventory.contains("Raw shrimps"));
    }

    private boolean widgetCastWindStrike() {
        if (Rs2Player.isAnimating() || Rs2Player.getInteracting() != null) return true;

        Widget windStrike = Rs2Widget.getWidget(218, 8);
        if (windStrike == null) windStrike = Rs2Widget.findWidget("Wind Strike", null, true);
        if (windStrike == null) return false;

        boolean hidden;
        try { hidden = Rs2Widget.isHidden(windStrike.getId()); }
        catch (Exception ignored) { hidden = true; }

        if (hidden) {
            var magicTab = Rs2Widget.findWidget("Magic", true);
            if (magicTab != null) { Rs2Widget.clickWidget(magicTab); waitEx(200, 50); }
            windStrike = Rs2Widget.getWidget(218, 8);
            if (windStrike == null) windStrike = Rs2Widget.findWidget("Wind Strike", null, true);
            if (windStrike == null) return false;
            try { if (Rs2Widget.isHidden(windStrike.getId())) return false; }
            catch (Exception ignored) { return false; }
        }

        Rs2Widget.clickWidget(windStrike);
        waitEx(150, 50);

        Rs2NpcModel chicken = Rs2Npc.getNpcs("chicken").findFirst().orElse(null);
        if (chicken == null) return false;

        if (!Rs2Npc.interact(chicken, "Cast")) Rs2Npc.interact(chicken);
        sleepUntil(() -> Rs2Player.isAnimating() || Microbot.getVarbitPlayerValue(281) != 650, 2_000);
        return true;
    }
}
