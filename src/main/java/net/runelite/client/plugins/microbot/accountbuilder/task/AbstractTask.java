package net.runelite.client.plugins.microbot.accountbuilder.task;

import net.runelite.client.plugins.microbot.accountbuilder.profile.AccountProfile;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

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

    // ── Food / HP helpers (used by all combat tasks) ──────────────────────

    /**
     * F2P food ordered best to worst.
     * Used by combat tasks when banking to restock food.
     */
    protected static final String[] FOOD_PRIORITY = {
        "Monkfish", "Lobster", "Swordfish", "Tuna",
        "Salmon",   "Trout",   "Pike",      "Sardine",
        "Herring",  "Anchovies","Shrimps"
    };

    /** Bank when fewer than this many food items remain in inventory. */
    protected static final int MIN_FOOD = 5;

    /** How many food items to withdraw per bank trip. */
    protected static final int FOOD_WITHDRAW_AMOUNT = 10;

    /**
     * Eats the best available food in inventory when HP is below {@code eatAtPercent}.
     * Delegates to {@link Rs2Player#eatAt(int)} which handles item detection automatically.
     *
     * @return {@code true} if food was eaten
     */
    protected boolean eatIfNeeded(int eatAtPercent) {
        return Rs2Player.eatAt(eatAtPercent);
    }

    /** Returns {@code true} if the inventory has fewer than {@link #MIN_FOOD} food items. */
    protected boolean needsFood() {
        return Rs2Inventory.getInventoryFood().size() < MIN_FOOD;
    }

    /**
     * Returns the name of the highest-priority food found in the open bank,
     * or {@code null} if no known food is stocked.
     */
    protected String findFoodInBank() {
        for (String food : FOOD_PRIORITY) {
            if (Rs2Bank.hasItem(food)) return food;
        }
        return null;
    }
}
