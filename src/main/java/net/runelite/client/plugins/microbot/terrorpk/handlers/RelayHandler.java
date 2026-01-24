package net.runelite.client.plugins.microbot.terrorpk.handlers;

import net.runelite.client.config.Keybind;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.Relay;

public class RelayHandler implements Relay {
    @Override
    public void action(Keybind key, CombatAction action) {
        if (key != null)
            action.execute();
    }
}