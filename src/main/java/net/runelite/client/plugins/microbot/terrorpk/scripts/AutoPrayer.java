package net.runelite.client.plugins.microbot.terrorpk.scripts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.terrorpk.TerrorPkConfig;
import net.runelite.client.plugins.microbot.terrorpk.enums.WeaponAnimation;
import net.runelite.client.plugins.microbot.terrorpk.enums.WeaponID;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AutoPrayer extends Script {

    private long lastPkAttackTime = 0;
    private String lastPrayedStyle = null;
    private static final long PRAYER_DISABLE_DELAY_MS = 10_000;
    private Player followedPlayer = null;  
    private long followEndTime = 0;

    public boolean run(TerrorPkConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.autoPrayAgainstPlayers()) return;

                // Fetch client-only values on the client thread to avoid "must be called on client thread" errors
                int[] result = Microbot.getClientThread().invoke(() -> {
                    Player localPlayer = Microbot.getClient().getLocalPlayer();
                    if (localPlayer == null) {
                        return new int[]{0, -1, -1};
                    }
                    if (!(localPlayer.getInteracting() instanceof Player)) {
                        return new int[]{2, -1, -1};
                    }
                    Player attackerPlayer = (Player) localPlayer.getInteracting();
                    int anim = attackerPlayer.getAnimation();
                    int weapon = attackerPlayer.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                    return new int[]{1, anim, weapon};
                });

                if (result == null) return;
                int state = result[0];
                if (state == 2) {
                    if (lastPrayedStyle != null && System.currentTimeMillis() - lastPkAttackTime > PRAYER_DISABLE_DELAY_MS) {
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_ITEM, false);
                        lastPrayedStyle = null;
                        followedPlayer = null;
                        followEndTime = 0;
                    }
                    return;
                }

                if (state == 1) {
                    int animationId = result[1];
                    int weaponId = result[2];
                    String detectedStyle = null;

                    WeaponAnimation anim = WeaponAnimation.getByAnimationId(animationId);
                    WeaponID weapon = WeaponID.getByObjectId(weaponId);

                    if (weapon != null) {
                        detectedStyle = weapon.getAttackType().toLowerCase();
                    }
                    if (anim != null) {
                        detectedStyle = anim.getAttackType().toLowerCase();
                    }

                    if (detectedStyle != null) {
                        prayStyle(detectedStyle);
                    }
                }

            } catch (Exception ex) {
                log.error("Error in AutoPrayer execution: {}", ex.getMessage(), ex);
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
        return true;
    }

    private void prayStyle(String style) {
        boolean shouldChange = lastPrayedStyle == null || !style.equals(lastPrayedStyle)
            || (style.equals("melee") && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE))
            || (style.equals("magic") && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC))
            || (style.equals("ranged") && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE));

        if (shouldChange) {
            if ("melee".equals(style)) {
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
            } else if ("ranged".equals(style)) {
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
            } else if ("magic".equals(style)) {
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
            }
            lastPrayedStyle = style;
        }

        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_ITEM, false);
        lastPkAttackTime = System.currentTimeMillis();
    }

    public Player getFollowedPlayer() {
        return followedPlayer;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        log.info("AutoPrayer (terrorpk) shutdown complete.");
    }
}
