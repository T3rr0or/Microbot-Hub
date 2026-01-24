package net.runelite.client.plugins.microbot.terrorpk.actions;

import net.runelite.client.plugins.microbot.terrorpk.TerrorPkConfig;
import net.runelite.client.plugins.microbot.terrorpk.enums.PrayerStyle;
import net.runelite.client.plugins.microbot.terrorpk.interfaces.CombatAction;

public class MeleeAction implements CombatAction {
    private final TerrorPkConfig config;
    private final int variant;

    public MeleeAction(TerrorPkConfig config, int variant) {
        this.config = config;
        this.variant = variant;
    }

    @Override
    public void execute() {
        new PrayOffensiveAction(config, PrayerStyle.MELEE).execute();
        String gearIDs;
        boolean attackTarget;
        boolean useVengeance;
        switch (variant) {
            case 1:
                gearIDs = config.gearIDsMeleePrimary();
                attackTarget = config.attackTargetMeleePrimary();
                useVengeance = config.useVengeanceMeleePrimary();
                break;
            case 2:
                gearIDs = config.gearIDsMeleeSecondary();
                attackTarget = config.attackTargetMeleeSecondary();
                useVengeance = config.useVengeanceMeleeSecondary();
                break;
            case 3:
                gearIDs = config.gearIDsMeleeTertiary();
                attackTarget = config.attackTargetMeleeTertiary();
                useVengeance = config.useVengeanceMeleeTertiary();
                break;
            default:
                gearIDs = "";
                attackTarget = false;
                useVengeance = false;
        }
        new EquipAction(gearIDs).execute();
        new VengeanceAction(useVengeance).execute();
        new AttackAction(attackTarget).execute();
    }
}