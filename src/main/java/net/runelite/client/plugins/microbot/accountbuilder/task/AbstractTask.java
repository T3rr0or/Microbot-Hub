package net.runelite.client.plugins.microbot.accountbuilder.task;

import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;

/**
 * Base class for all account-builder tasks.
 * Provides profile-aware sleep helpers so tasks never hard-code delays.
 */
public abstract class AbstractTask implements Task {

    protected final AccountProfile profile;

    protected AbstractTask(AccountProfile profile) {
        this.profile = profile;
    }

    /** Sleep for {@code ms} milliseconds scaled by this account's reaction variance. */
    protected void sleep(long ms) {
        long actual = (long) (ms * profile.getReactionVariance());
        try {
            Thread.sleep(actual);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Sleep for a raw number of milliseconds (no variance applied). */
    protected void sleepRaw(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Block until {@code condition} returns true or the timeout elapses.
     * Polls every 100 ms. Default timeout: 10 seconds.
     */
    protected void sleepUntil(java.util.function.BooleanSupplier condition) {
        sleepUntil(condition, 10_000);
    }

    /**
     * Block until {@code condition} returns true or {@code timeoutMs} elapses.
     * Polls every 100 ms.
     */
    protected void sleepUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            sleepRaw(100);
        }
    }

    /** Optionally insert a random idle pause based on the account profile. */
    protected void maybeIdle() {
        if (Math.random() < profile.getIdleChance()) {
            long idleMs = (long) (Math.random() * profile.getIdleMaxMs());
            sleepRaw(idleMs);
        }
    }
}
