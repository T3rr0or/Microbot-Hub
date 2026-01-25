package net.runelite.client.plugins.microbot.terrorpk;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.terrorpk.actions.*;
import net.runelite.client.plugins.microbot.terrorpk.handlers.MenuHandler;
import net.runelite.client.plugins.microbot.terrorpk.handlers.PostActionHandler;
import net.runelite.client.plugins.microbot.terrorpk.handlers.RelayHandler;
import net.runelite.client.plugins.microbot.terrorpk.handlers.TankHandler;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;

import static net.runelite.client.plugins.microbot.Microbot.isLoggedIn;

@Slf4j
@PluginDescriptor(
        name = "<html>[<font color=#000000>💀</font>] " + "TerrorPk",
        description = "Combat utility for TerrorPk: swaps, specs, prayers, and hotkeys.",
        tags = {"combat", "hotkeys", "microbot"},
        authors = {"Terrpr"},
        version = TerrorPkPlugin.version,
        minClientVersion = "2.0.7",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
public class TerrorPkPlugin extends Plugin implements KeyListener {
    final static String version = "1.0.0";

    @Inject
    private TerrorPkConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TerrorPkOverlay overlay;
    @Inject
    private TerrorPkScript script;
    @Inject
    private MenuHandler menuHandler;
    @Inject
    private PostActionHandler postActionHandler;
    @Inject
    private TankHandler tankHandler;
    @Inject
    private EventBus eventBus;
    @Inject
    private ClientThread clientThread;
    @Inject
    private RelayHandler relayHandler;

    private boolean hotkeyConsumed = false;

    @Setter
    @Getter
    private static Actor target;

    @Provides
    TerrorPkConfig provideConfig(ConfigManager m) {
        return m.getConfig(TerrorPkConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(this);
        overlayManager.add(overlay);
        eventBus.register(menuHandler);
        eventBus.register(tankHandler);
        eventBus.register(postActionHandler);
        eventBus.register(overlayManager);
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        Microbot.enableAutoRunOn = false;
        keyManager.unregisterKeyListener(this);
        overlayManager.remove(overlay);
        eventBus.unregister(menuHandler);
        eventBus.unregister(postActionHandler);
        eventBus.unregister(tankHandler);
        eventBus.unregister(overlayManager);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (hotkeyConsumed) {
            e.consume();
            hotkeyConsumed = false;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!isLoggedIn()) return;
        if (!Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) return;
        hotkeyConsumed = false;
        if (processKey(e, config.protectFromMagic(), () -> relayHandler.action(config.protectFromMagic(), new DefensivePrayerAction(Rs2PrayerEnum.PROTECT_MAGIC))))
            return;
        if (processKey(e, config.protectFromMissles(), () -> relayHandler.action(config.protectFromMissles(), new DefensivePrayerAction(Rs2PrayerEnum.PROTECT_RANGE))))
            return;
        if (processKey(e, config.protectFromMelee(), () -> relayHandler.action(config.protectFromMelee(), new DefensivePrayerAction(Rs2PrayerEnum.PROTECT_MELEE))))
            return;
        if (processKey(e, config.hotkeyMeleePrimary(), () -> relayHandler.action(config.hotkeyMeleePrimary(), new MeleeAction(config, 1))))
            return;
        if (processKey(e, config.hotkeyMeleeSecondary(), () -> relayHandler.action(config.hotkeyMeleeSecondary(), new MeleeAction(config, 2))))
            return;
        if (processKey(e, config.hotkeyMeleeTertiary(), () -> relayHandler.action(config.hotkeyMeleeTertiary(), new MeleeAction(config, 3))))
            return;
        if (processKey(e, config.hotkeyRangePrimary(), () -> relayHandler.action(config.hotkeyRangePrimary(), new RangeAction(config, 1))))
            return;
        if (processKey(e, config.hotkeyRangeSecondary(), () -> relayHandler.action(config.hotkeyRangeSecondary(), new RangeAction(config, 2))))
            return;
        if (processKey(e, config.hotkeyRangeTertiary(), () -> relayHandler.action(config.hotkeyRangeTertiary(), new RangeAction(config, 3))))
            return;
        if (processKey(e, config.hotkeyMagePrimary(), () -> relayHandler.action(config.hotkeyMagePrimary(), new MageAction(config, 1))))
            return;
        if (processKey(e, config.hotkeyMageSecondary(), () -> relayHandler.action(config.hotkeyMageSecondary(), new MageAction(config, 2))))
            return;
        if (processKey(e, config.hotkeyMageTertiary(), () -> relayHandler.action(config.hotkeyMageTertiary(), new MageAction(config, 3))))
            return;
        if (processKey(e, config.hotkeySpecialAttackPrimary(), () -> relayHandler.action(config.hotkeySpecialAttackPrimary(), new SpecAction(config, 1))))
            return;
        if (processKey(e, config.hotkeySpecialAttackSecondary(), () -> relayHandler.action(config.hotkeySpecialAttackSecondary(), new SpecAction(config, 2))))
            return;
        if (processKey(e, config.hotkeySpecialAttackTertiary(), () -> relayHandler.action(config.hotkeySpecialAttackTertiary(), new SpecAction(config, 3))))
            return;
        if (processKey(e, config.hotkeyTank(), () -> relayHandler.action(config.hotkeyTank(), new TankAction(config))))
            return;
        if (processKey(e, config.singleEatHotkey(), () -> relayHandler.action(config.singleEatHotkey(), new EatAction(config.singleEatFood(), 1))))
            return;
        if (processKey(e, config.doubleEatHotkey(), () -> relayHandler.action(config.doubleEatHotkey(), new DoubleEatAction(config.doubleEatFirstFood()))))
            return;
        if (processKey(e, config.tripleEatHotkey(), () -> relayHandler.action(config.tripleEatHotkey(), new TripleEatAction(config.tripleEatFirstFood()))))
            return;
        // Potion hotkeys
        if (processKey(e, config.restoreHotkey(), () -> relayHandler.action(config.restoreHotkey(), new DrinkPotionAction("super restore", 3024, 3026, 3028, 3030))))
            return;
        if (processKey(e, config.brewHotkey(), () -> relayHandler.action(config.brewHotkey(), new DrinkPotionAction("saradomin brew", 6685, 6687, 6689, 6691))))
            return;
        if (processKey(e, config.combatPotionHotkey(), () -> relayHandler.action(config.combatPotionHotkey(), new DrinkPotionAction("super combat", 12695, 12697, 12699, 12701))))
            return;
        if (processKey(e, config.rangePotionHotkey(), () -> relayHandler.action(config.rangePotionHotkey(), new DrinkPotionAction("ranging potion", 2444, 2446, 2448, 2450))))
            return;
        if (processKey(e, config.clearTargetHotkey(), () -> clearTarget()))
            return;
        if (processKey(e, config.walkUnderHotkey(), () -> relayHandler.action(config.walkUnderHotkey(), new WalkUnderAction())))
            ;
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private boolean processKey(KeyEvent e, net.runelite.client.config.Keybind key, Runnable action) {
        if (key.matches(e)) {
            e.consume();
            hotkeyConsumed = true;
            Microbot.getClientThread().runOnSeperateThread(() -> {
                if (isLoggedIn()) action.run();
                return null;
            });
            return true;
        }
        return false;
    }

    public static boolean validTarget() {
        return target != null && target.getLocalLocation() != null && target.getLocalLocation().isInScene();
    }

    public static boolean isNPC() {
        return target instanceof NPC;
    }

    public static boolean isPlayer() {
        return target instanceof Player;
    }

    public static void clearTarget() {
        target = null;
    }

}
