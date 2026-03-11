package net.runelite.client.plugins.microbot.accountbuilder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.accountbuilder.profile.ProfileManager;
import net.runelite.client.plugins.microbot.accountbuilder.task.Task;
import net.runelite.client.plugins.microbot.accountbuilder.task.TaskExecutor;
import net.runelite.client.plugins.microbot.accountbuilder.task.tasks.quests.AnimalMagnetismTask;
import net.runelite.client.plugins.microbot.accountbuilder.task.tasks.quests.WaterfallQuestTask;
import net.runelite.client.plugins.microbot.accountbuilder.task.tasks.skills.*;
import net.runelite.client.plugins.microbot.accountbuilder.task.tasks.TutorialIslandTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AccountBuilderScript extends Script {

    public static final String version = "1.0.0";

    @Getter
    private TaskExecutor taskExecutor;

    @Getter
    private AccountProfile profile;

    /** Runtime in milliseconds since the script started. */
    private long startTimeMs;

    public boolean run(AccountBuilderConfig config) {
        startTimeMs = System.currentTimeMillis();

        String accountName = resolveAccountName();
        profile = ProfileManager.loadOrCreate(accountName);

        taskExecutor = new TaskExecutor(buildTaskList(config), profile);

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;
            if (!Microbot.isLoggedIn()) return;
            try {
                taskExecutor.tick();
            } catch (Exception ex) {
                log.error("AccountBuilder error: {}", ex.getMessage(), ex);
            }
        }, 0, profile.getTickDelayMs(), TimeUnit.MILLISECONDS);

        return true;
    }

    /** Safely resolve the local player name before login race conditions. */
    private String resolveAccountName() {
        try {
            if (Microbot.getClient().getLocalPlayer() != null) {
                String name = Microbot.getClient().getLocalPlayer().getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return "default";
    }

    private List<Task> buildTaskList(AccountBuilderConfig config) {
        List<Task> tasks = new ArrayList<>();

        // Phase 0 — Tutorial Island (always required)
        tasks.add(new TutorialIslandTask(profile));

        // Phase 1 — Foundation (members-only quests skipped for F2P)
        if (config.isMember()) {
            tasks.add(new WaterfallQuestTask(profile));
            tasks.add(new AnimalMagnetismTask(profile));
        }

        // Phase 2 — Skill training (F2P compatible)
        tasks.add(new TrainAttackTask(30, profile));
        tasks.add(new TrainStrengthTask(40, profile));
        tasks.add(new TrainDefenceTask(40, profile));
        tasks.add(new TrainHitpointsTask(50, profile));
        tasks.add(new TrainRangedTask(40, profile));

        // Phase 3 — Utility Skills (F2P compatible)
        tasks.add(new TrainPrayerTask(43, profile));
        tasks.add(new TrainMagicTask(55, profile));

        return tasks;
    }

    /** Returns elapsed runtime in milliseconds. */
    public long getRuntimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
