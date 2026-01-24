package net.runelite.client.plugins.microbot.terrorpk.actions;


import net.runelite.client.plugins.microbot.terrorpk.TerrorPkPlugin;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import static net.runelite.client.plugins.microbot.util.player.Rs2Player.getPlayer;

public class WalkUnderAction implements CombatAction {
    @Override
    public void execute() {
        if (TerrorPkPlugin.validTarget() && TerrorPkPlugin.getTarget().getLocalLocation().isInScene())
            Rs2Player.walkUnder(getPlayer(TerrorPkPlugin.getTarget().getName()));
    }
}