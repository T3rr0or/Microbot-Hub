package net.runelite.client.plugins.microbot.terrorpk;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.terrorpk.enums.WeaponAnimation;
import net.runelite.client.plugins.microbot.terrorpk.enums.WeaponID;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;

import java.util.concurrent.TimeUnit;

public class TerrorPkScript extends Script {

    private long lastPkAttackTime = 0;
    private String lastPrayedStyle = null;
    private static final long PRAYER_DISABLE_DELAY_MS = 10_000;
    private Player followedPlayer = null;
    private long followEndTime = 0;

    private void prayStyle(String style) {
        boolean shouldChange = !style.equals(lastPrayedStyle)
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

        lastPkAttackTime = System.currentTimeMillis();
    }

    public boolean run(TerrorPkConfig config) {

        Microbot.enableAutoRunOn = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                // Auto-pray against players
                try {
                    if (config.autoPrayAgainstPlayers()) {
                        // Read client-only data on the client thread to avoid thread errors
                        Object[] data = Microbot.getClientThread().invoke(() -> {
                            Player localPlayer = Microbot.getClient().getLocalPlayer();
                            if (localPlayer == null) return new Object[]{0, -1, -1, null};
                            if (!(localPlayer.getInteracting() instanceof Player)) return new Object[]{2, -1, -1, null};
                            Player attacker = (Player) localPlayer.getInteracting();
                            int anim = attacker.getAnimation();
                            int weapon = attacker.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                            String playerName = attacker.getName();
                            return new Object[]{1, anim, weapon, playerName};
                        });

                        if (data == null) return;
                        int state = (int) data[0];
                        if (state == 2) {
                            if (lastPrayedStyle != null && System.currentTimeMillis() - lastPkAttackTime > PRAYER_DISABLE_DELAY_MS) {
                                System.out.println("[AutoPray] No player interaction - disabling prayers after delay");
                                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
                                Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
                                lastPrayedStyle = null;
                                followedPlayer = null;
                                followEndTime = 0;
                            }
                        } else if (state == 1) {
                            int animationId = (int) data[1];
                            int weaponId = (int) data[2];
                            String playerName = (String) data[3];

                            String detectedStyle = null;
                            WeaponAnimation anim = WeaponAnimation.getByAnimationId(animationId);
                            WeaponID weapon = WeaponID.getByObjectId(weaponId);

                            // Debug logging
                            System.out.println("[AutoPray] === Player Combat Detection ===");
                            System.out.println("[AutoPray] Opponent: " + (playerName != null ? playerName : "Unknown"));
                            System.out.println("[AutoPray] Weapon ID: " + weaponId);
                            System.out.println("[AutoPray] Animation ID: " + animationId);
                            
                            if (weapon != null) {
                                detectedStyle = weapon.getAttackType().toLowerCase();
                                System.out.println("[AutoPray] Weapon detected: " + weapon.getItemName() + " (" + weapon.getAttackType() + ")");
                            } else {
                                System.out.println("[AutoPray] Weapon: Unknown (ID " + weaponId + ")");
                            }
                            
                            if (anim != null) {
                                detectedStyle = anim.getAttackType().toLowerCase();
                                System.out.println("[AutoPray] Animation detected: " + anim.getAnimationName() + " (" + anim.getAttackType() + ")");
                            } else if (animationId != -1) {
                                System.out.println("[AutoPray] Animation: Unknown (ID " + animationId + ")");
                            }

                            if (detectedStyle != null) {
                                System.out.println("[AutoPray] Decision: Praying against " + detectedStyle.toUpperCase());
                                prayStyle(detectedStyle);
                            } else {
                                System.out.println("[AutoPray] Decision: No style detected, no prayer change");
                            }
                            System.out.println("[AutoPray] ===================================");
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("[AutoPray] Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
