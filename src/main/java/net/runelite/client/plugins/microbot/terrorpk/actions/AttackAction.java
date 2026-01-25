package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.api.Actor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.terrorpk.TerrorPkPlugin;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;

public class AttackAction implements CombatAction {
    private final boolean shouldAttack;

    public AttackAction(boolean shouldAttack) {
        this.shouldAttack = shouldAttack;
    }

    @Override
    public void execute() {
        if (!shouldAttack) return;
        if (!TerrorPkPlugin.validTarget()) return;

        // Run attack on the client thread to ensure it executes reliably
        Microbot.getClientThread().invoke(() -> {
            Actor target = TerrorPkPlugin.getTarget();
            if (target instanceof Rs2PlayerModel) {
                Rs2Player.attack((Rs2PlayerModel) target);
            }
            return null;
        });
    }
}