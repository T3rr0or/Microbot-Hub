package net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;

/**
 * Trains Defence to the target level by fighting barbarians at Barbarian Village.
 *
 * <p>Uses attack style index 3 (sword "Defensive" = Defence XP).
 * If the equipped weapon has only 3 styles (e.g. scimitar), {@link AbstractMeleeTask}
 * automatically falls back to index 2 ("Block"), which also grants Defence XP.
 */
public class TrainDefenceTask extends AbstractMeleeTask {

    private static final WorldPoint BARBARIAN_VILLAGE = new WorldPoint(3082, 3422, 0);

    public TrainDefenceTask(int targetLevel, AccountProfile profile) {
        super(targetLevel, profile);
    }

    @Override public String getName()                    { return "Train Defence to " + targetLevel; }
    @Override protected Skill getSkill()                 { return Skill.DEFENCE; }
    @Override protected String getNpcName()              { return "Barbarian"; }
    @Override protected WorldPoint getTrainingLocation() { return BARBARIAN_VILLAGE; }
    @Override protected int getTargetStyleIndex()        { return 3; } // Defensive (sword) / Block fallback (scimitar)
}
