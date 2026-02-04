package net.runelite.client.plugins.microbot.valetotems;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("valeTotems")
@ConfigInformation(
    "<html>" +
    "🤖 <b>VALE TOTEM BOT - SETUP GUIDE</b><br /><br />" +
    
    "🔴 <b>REQUIREMENTS:</b><br />" +
    "✅ Children of the Sun quest completed (Varlamore access)<br />" +
    "✅ Vale Totems miniquest completed<br />" +
    "✅ Fletching level 20+<br />" +
    "✅ Agility 45+ highly recommended (untested below 45)<br />" +
    "✅ Knife or Fletching knife in bank<br />" +
    "✅ logs in bank<br /><br />" +
    
    "📊 <b>FLETCHING REQUIREMENTS:</b><br />" +
    "• <b>Oak:</b> Shortbow (20) | Longbow (25)<br />" +
    "• <b>Willow:</b> Shortbow (35) | Longbow (40)<br />" +
    "• <b>Maple:</b> Shortbow (50) | Longbow (55)<br />" +
    "• <b>Yew:</b> Shortbow (65) | Longbow (70)<br />" +
    "• <b>Magic:</b> Shortbow (80) | Longbow (85)<br /><br />" +
    
    "🎯 <b>FEATURES:</b><br />" +
    "• Automatically plays Vale Totem minigame<br />" +
    "• <b>Log Basket support:</b> Takes longer route with basket, shorter without<br />" +
    "• Automatically walks over ent trails for extra points and XP<br />" +
    "• Fletches while walking for efficiency<br />" +
    "• Auto banking and item management<br /><br />" +
    
    "⚠️ <b>CURRENT LIMITATIONS:</b><br />" +
    "• Only withdraws logs from bank (not pre-fletched items)<br /><br />" +
    
    "🖥️ <b>CRITICAL REQUIREMENT:</b><br />" +
    "• <b>GPU Plugin</b> OR <b>117 HD Plugin</b> must be enabled<br />" +
    "• <b>Draw Distance:</b> Minimum 40<br />" +
    "• <i>Note: Bot clicks far distances to fletch while walking</i><br /><br />" +
    
    "🏁 <b>QUICK START:</b><br />" +
    "• Start anywhere with empty inventory (recommended)<br />" +
    "• Ensure desired logs and knife/fletching knife are in bank<br />" +
    "• <b>Equip desired gear before starting</b> (bot doesn't wield/unwield)<br />" +
    "• <b>Recommended:</b> Graceful gear for reduced run energy drain<br />" +
    "• Bot handles all navigation and banking automatically!<br /><br />" +

    "⚡ <b>STAMINA SUPPORT:</b><br />" +
    "• Enable 'Use Stamina Potions' to automatically drink stamina potions<br />" +
    "• Bot will withdraw potions from bank and drink when run energy is low<br />" +
    "• Stamina potions are prioritized over energy potions"
)
public interface ValeTotemConfig extends Config {
    
    @ConfigItem(
            keyName = "logType",
            name = "Log Type",
            description = "Select which logs to use for fletching",
            position = 0
    )
    default LogType logType() {
        return LogType.YEW;
    }

    @ConfigItem(
            keyName = "bowType",
            name = "Bow Type",
            description = "Select whether to fletch shortbows or longbows",
            position = 1
    )
    default BowType bowType() {
        return BowType.LONGBOW;
    }

    @ConfigItem(
            keyName = "collectOfferingsFrequency",
            name = "Offering Collection Frequency",
            description = "How often to collect offerings (every X rounds) - +- 1",
            position = 3
    )
    default int collectOfferingsFrequency() {
        return 5;
    }

    @ConfigSection(
            name = "Stamina",
            description = "Stamina potion settings",
            position = 10
    )
    String staminaSection = "stamina";

    @ConfigItem(
            keyName = "useStaminaPotions",
            name = "Use Stamina Potions",
            description = "Automatically drink stamina potions when run energy is low",
            position = 11,
            section = staminaSection
    )
    default boolean useStaminaPotions() {
        return false;
    }

    @ConfigItem(
            keyName = "staminaThreshold",
            name = "Drink At Energy %",
            description = "Drink stamina potion when run energy falls below this percentage",
            position = 12,
            section = staminaSection
    )
    @Range(min = 10, max = 90)
    default int staminaThreshold() {
        return 50;
    }

    enum LogType {
        OAK("Oak Logs", 20),
        WILLOW("Willow Logs", 35),
        MAPLE("Maple Logs", 50),
        YEW("Yew Logs", 65),
        MAGIC("Magic Logs", 80);

        private final String displayName;
        private final int requiredLevel;

        LogType(String displayName, int requiredLevel) {
            this.displayName = displayName;
            this.requiredLevel = requiredLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        @Override
        public String toString() {
            return displayName + " (" + requiredLevel + " Fletching)";
        }
    }

    enum BowType {
        SHORTBOW("Shortbow"),
        LONGBOW("Longbow");

        private final String displayName;

        BowType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
