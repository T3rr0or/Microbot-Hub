package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.TerrorPkConfig;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;

public class TankAction implements CombatAction {
    private final TerrorPkConfig config;

    public TankAction(TerrorPkConfig config) {
        this.config = config;
    }

    @Override
    public void execute() {
        new EquipAction(config.gearIDsTank()).execute();
    }
}