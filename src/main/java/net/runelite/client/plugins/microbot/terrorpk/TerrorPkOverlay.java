package net.runelite.client.plugins.microbot.terrorpk;

import net.runelite.api.Actor;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class TerrorPkOverlay extends OverlayPanel {
    private final TerrorPkConfig config;

    @Inject
    TerrorPkOverlay(TerrorPkPlugin plugin, TerrorPkConfig config) {
        super(plugin);
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            // Render target tile on scene
            if (TerrorPkPlugin.getTarget() != null && !TerrorPkPlugin.getTarget().isDead()) {
                WorldPoint targetLoc = TerrorPkPlugin.getTarget().getWorldLocation();
                if (targetLoc != null) {
                    drawTargetTile(graphics, targetLoc);
                }
            }
            
            // Render info panel
            panelComponent.setPreferredSize(new Dimension(220, 200));
            
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("TerrorPK v" + TerrorPkPlugin.version)
                    .color(new Color(255, 215, 0))
                    .build());
            
            panelComponent.getChildren().add(LineComponent.builder().build());
            
            // Target info
            if (TerrorPkPlugin.getTarget() != null && !TerrorPkPlugin.getTarget().isDead()) {
                String targetName = TerrorPkPlugin.getTarget().getName();
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Target:")
                        .right(targetName != null ? targetName : "Unknown")
                        .rightColor(new Color(100, 200, 100))
                        .build());
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Target:")
                        .right("None")
                        .rightColor(new Color(200, 100, 100))
                        .build());
            }
            
            // Active Prayers
            StringBuilder prayerStatus = new StringBuilder();
            if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
                prayerStatus.append("Melee ");
            }
            if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE)) {
                prayerStatus.append("Range ");
            }
            if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC)) {
                prayerStatus.append("Magic ");
            }
            
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Prayers:")
                    .right(prayerStatus.length() > 0 ? prayerStatus.toString().trim() : "None")
                    .rightColor(prayerStatus.length() > 0 ? new Color(100, 200, 100) : new Color(200, 100, 100))
                    .build());
            
            // Auto-pray status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Auto-pray:")
                    .right(config.autoPrayAgainstPlayers() ? "ON" : "OFF")
                    .rightColor(config.autoPrayAgainstPlayers() ? new Color(100, 200, 100) : new Color(200, 100, 100))
                    .build());
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }

    private void drawTargetTile(Graphics2D graphics, WorldPoint point) {
        if (point == null) {
            return;
        }
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null || point.distanceTo(playerLocation) >= 32) {
            return;
        }
        LocalPoint lp = LocalPoint.fromWorld(Microbot.getClient(), point);
        if (lp == null) {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(Microbot.getClient(), lp);
        if (poly != null) {
            Color highlightColor = config.targetTileColor();
            OverlayUtil.renderPolygon(graphics, poly, highlightColor, new Color(0, 0, 0, 50), new BasicStroke(2));
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        Actor deadActor = event.getActor();
        if (deadActor != null && deadActor == TerrorPkPlugin.getTarget()) {
            TerrorPkPlugin.setTarget(null);
        }
    }
}

