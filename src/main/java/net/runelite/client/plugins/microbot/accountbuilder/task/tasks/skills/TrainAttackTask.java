package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;

/**
 * Trains Attack to the target level by fighting chickens at Lumbridge farm.
 * Uses the Accurate (index 0) attack style, which grants Attack XP.
 */
public class TrainAttackTask extends AbstractMeleeTask {

    private static final WorldPoint CHICKEN_FARM = new WorldPoint(3236, 3295, 0);

    public TrainAttackTask(int targetLevel, AccountProfile profile, int eatAtPercent) {
        super(targetLevel, profile, eatAtPercent);
    }

    @Override public String getName()                    { return "Train Attack to " + targetLevel; }
    @Override protected Skill getSkill()                 { return Skill.ATTACK; }
    @Override protected String getNpcName()              { return "Chicken"; }
    @Override protected WorldPoint getTrainingLocation() { return CHICKEN_FARM; }
    @Override protected int getTargetStyleIndex()        { return 0; } // Accurate / Chop → Attack XP
}
