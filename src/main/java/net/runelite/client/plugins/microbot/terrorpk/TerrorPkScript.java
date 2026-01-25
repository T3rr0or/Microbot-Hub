package net.runelite.client.plugins.microbot.terrorpk;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.terrorpk.enums.WeaponID;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;

import java.util.concurrent.TimeUnit;

public class TerrorPkScript extends Script {

    private long lastPkAttackTime = 0;
    private String lastDetectedStyle = null;
    private static final long PRAYER_DISABLE_DELAY_MS = 10_000;

    private void prayStyle(String style, TerrorPkConfig config) {
        // Apply delay if configured
        int minDelay = config.autoPrayDelayMin();
        int maxDelay = config.autoPrayDelayMax();
        if (minDelay > 0 || maxDelay > 0) {
            int delay = Rs2Random.between(minDelay, Math.max(minDelay, maxDelay));
            System.out.println("[AutoPray] Switching to " + style.toUpperCase() + " prayer (delay: " + delay + "ms)");
            sleep(delay);
        }
        
        // Just enable the correct prayer, don't disable others
        if ("melee".equals(style)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        } else if ("ranged".equals(style)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
        } else if ("magic".equals(style)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
        }
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
                        // Quick check if we have a target before doing expensive client thread invoke
                        if (TerrorPkPlugin.getTarget() == null || !TerrorPkPlugin.validTarget()) {
                            // No target - skip processing this tick
                            return;
                        }
                        
                        // Use the current target from TerrorPkPlugin instead of interacting player
                        Object[] data = Microbot.getClientThread().invoke(() -> {
                            Player target = (Player) TerrorPkPlugin.getTarget();
                            if (target == null) return new Object[]{2, -1, -1, null};
                            if (!TerrorPkPlugin.validTarget()) return new Object[]{2, -1, -1, null};
                            int anim = target.getAnimation();
                            int weapon = target.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                            String playerName = target.getName();
                            return new Object[]{1, anim, weapon, playerName};
                        });

                        if (data == null) return;
                        int state = (int) data[0];
                        if (state == 2) {
                            // No target - skip
                            return;
                        } else if (state == 1) {
                            int animationId = (int) data[1];
                            int weaponId = (int) data[2];
                            String playerName = (String) data[3];

                            String detectedStyle = null;
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
                                // Log unknown weapon
                                if (weaponId != -1) {
                                    System.out.println("[AutoPray] Weapon: Unknown (ID " + weaponId + ")");
                                }
                            }

                            if (detectedStyle != null) {
                                System.out.println("[AutoPray] Decision: Praying against " + detectedStyle.toUpperCase());
                                // Only switch if the style has actually changed
                                if (!detectedStyle.equals(lastDetectedStyle)) {
                                    lastDetectedStyle = detectedStyle;
                                    prayStyle(detectedStyle, config);
                                }
                            } else {
                                System.out.println("[AutoPray] Decision: No style detected, no prayer change");
                                lastDetectedStyle = null;
                            }
                            System.out.println("[AutoPray] ===================================");
                        }
                    } else {
                        // Auto-pray is disabled - turn off all prayers
                        System.out.println("[AutoPray] Auto-pray disabled - disabling all prayers");
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
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
