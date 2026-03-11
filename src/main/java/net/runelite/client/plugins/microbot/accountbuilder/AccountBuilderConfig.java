package net.runelite.client.plugins.microbot.accountbuilder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("accountbuilder")
public interface AccountBuilderConfig extends Config {

    // ── Sections ──────────────────────────────────────────────────────────

    @ConfigSection(
            name = "Skill Targets",
            description = "Set the target level for each skill. Use 1 to skip a skill entirely.",
            position = 0
    )
    String skillTargets = "skillTargets";

    @ConfigSection(
            name = "General",
            description = "General plugin settings",
            position = 1
    )
    String general = "general";

    // ── Skill target levels ───────────────────────────────────────────────

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetAttack",
            name = "Attack Target Level",
            description = "Train Attack to this level (1 = skip)",
            section = skillTargets,
            position = 0
    )
    default int targetAttack() { return 30; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetStrength",
            name = "Strength Target Level",
            description = "Train Strength to this level (1 = skip)",
            section = skillTargets,
            position = 1
    )
    default int targetStrength() { return 40; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetDefence",
            name = "Defence Target Level",
            description = "Train Defence to this level (1 = skip)",
            section = skillTargets,
            position = 2
    )
    default int targetDefence() { return 40; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetHitpoints",
            name = "Hitpoints Target Level",
            description = "Train Hitpoints to this level (1 = skip)",
            section = skillTargets,
            position = 3
    )
    default int targetHitpoints() { return 50; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetRanged",
            name = "Ranged Target Level",
            description = "Train Ranged to this level (1 = skip)",
            section = skillTargets,
            position = 4
    )
    default int targetRanged() { return 40; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetPrayer",
            name = "Prayer Target Level",
            description = "Train Prayer to this level (1 = skip)",
            section = skillTargets,
            position = 5
    )
    default int targetPrayer() { return 43; }

    @Range(min = 1, max = 99)
    @ConfigItem(
            keyName = "targetMagic",
            name = "Magic Target Level",
            description = "Train Magic to this level (1 = skip)",
            section = skillTargets,
            position = 6
    )
    default int targetMagic() { return 55; }

    // ── General settings ──────────────────────────────────────────────────

    @ConfigItem(
            keyName = "isMember",
            name = "Members Account",
            description = "Uncheck for F2P — skips members-only quests (Waterfall Quest, Animal Magnetism)",
            section = general,
            position = 0
    )
    default boolean isMember() { return true; }

    @ConfigItem(
            keyName = "enableBreaks",
            name = "Enable AFK Breaks",
            description = "Take randomized AFK breaks between tasks",
            section = general,
            position = 1
    )
    default boolean enableBreaks() { return true; }

    @ConfigItem(
            keyName = "logLevel",
            name = "Log Level",
            description = "How much to log to in-game chat",
            section = general,
            position = 2
    )
    default LogLevel logLevel() { return LogLevel.INFO; }
}
