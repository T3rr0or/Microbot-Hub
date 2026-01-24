package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.TerrorPkConfig;
import net.runelite.client.plugins.microbot.terrorpk.TerrorPkPlugin;
import net.runelite.client.plugins.microbot.terrorpk.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;

public class MageAction implements CombatAction {
    private final TerrorPkConfig config;
    private final int variant;

    public MageAction(TerrorPkConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        String gearIDs;
        Rs2CombatSpells spell;
        switch (variant) {
            case 1:
                gearIDs = config.gearIDsMagePrimary();
                spell = config.selectedCombatSpellPrimary();
                break;
            case 2:
                gearIDs = config.gearIDsMageSecondary();
                spell = config.selectedCombatSpellSecondary();
                break;
            case 3:
                gearIDs = config.gearIDsMageTertiary();
                spell = config.selectedCombatSpellTertiary();
                break;
            default:
                gearIDs = "";
                spell = Rs2CombatSpells.WIND_STRIKE;
        }
        new PrayOffensiveAction(config, PrayerStyle.MAGE).execute();
        new EquipAction(gearIDs).execute();
        if (TerrorPkPlugin.validTarget()) {
            Rs2Magic.castOn(spell.getMagicAction(), TerrorPkPlugin.getTarget());
        }
    }
}