package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;

/**
 * Trains Strength to the target level by fighting cows at Lumbridge.
 * Uses the Aggressive (index 1) attack style, which grants Strength XP.
 */
public class TrainStrengthTask extends AbstractMeleeTask {

    private static final WorldPoint COW_FIELD = new WorldPoint(3255, 3272, 0);

    public TrainStrengthTask(int targetLevel, AccountProfile profile, int eatAtPercent) {
        super(targetLevel, profile, eatAtPercent);
    }

    @Override public String getName()                    { return "Train Strength to " + targetLevel; }
    @Override protected Skill getSkill()                 { return Skill.STRENGTH; }
    @Override protected String getNpcName()              { return "Cow"; }
    @Override protected WorldPoint getTrainingLocation() { return COW_FIELD; }
    @Override protected int getTargetStyleIndex()        { return 1; } // Aggressive / Slash → Strength XP
}
