package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;

/**
 * Trains Hitpoints to the target level by fighting barbarians at Barbarian Village.
 *
 * <p>Hitpoints XP is earned alongside every combat style, so the Aggressive (index 1)
 * style is used here for efficient damage output. By the time this task runs, the
 * Strength target has already been reached, so gaining additional Strength XP is harmless.
 */
public class TrainHitpointsTask extends AbstractMeleeTask {

    private static final WorldPoint BARBARIAN_VILLAGE = new WorldPoint(3082, 3422, 0);

    public TrainHitpointsTask(int targetLevel, AccountProfile profile) {
        super(targetLevel, profile);
    }

    @Override public String getName()                    { return "Train Hitpoints to " + targetLevel; }
    @Override protected Skill getSkill()                 { return Skill.HITPOINTS; }
    @Override protected String getNpcName()              { return "Barbarian"; }
    @Override protected WorldPoint getTrainingLocation() { return BARBARIAN_VILLAGE; }
    @Override protected int getTargetStyleIndex()        { return 1; } // Aggressive → Strength XP + Hitpoints XP
}
