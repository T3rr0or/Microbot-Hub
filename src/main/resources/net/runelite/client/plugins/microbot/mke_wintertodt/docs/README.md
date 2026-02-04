# MKE AI-Wintertodt

Advanced Wintertodt bot with intelligent automation and human-like behavior patterns.

![Wintertodt Bot](assets/card.png)

## Features

- **🤖 Fully Automated**: Start anywhere, handles navigation, gearing, and complete Wintertodt gameplay
- **⚙️ Auto Gear Optimization**: Finds and equips best available warm gear automatically  
- **💊 Smart Healing**: Free potions (crafted in-game) or food from bank
- **🏆 Reward Collection**: Automatic reward cart looting when profitable
- **🛌 Break System**: AFK and logout breaks with natural timing patterns
- **🎭 Human-like Behavior**: Randomized timing, mouse movements, and camera adjustments

## Requirements

- **Membership** + **Firemaking 50+**
- **4+ warm clothing pieces** in bank (Pyromancer, Santa outfit, Hunter gear, etc.)
- **Tools in bank**: Axe, Knife, Hammer, Tinderbox

## Quick Start

1. Choose healing method in config (Potions recommended)
2. Start plugin anywhere 
3. Let it run - no manual intervention needed

## Configuration

### Healing Methods
- **Potions**: FREE, crafted from crate materials (recommended)
- **Food**: Withdrawn from bank

### Key Settings
- **Healing amount**: Items per trip (default: 2)
- **Brazier location**: Preferred brazier (default: South East)  
- **Break system**: Enable for natural AFK/logout patterns
- **Advanced options**: Humanized timing, camera/mouse movements, antiban overlay

### Break System Details
The custom break handler is smart about timing:
- **Safe Break Locations**: Only starts breaks when banking or making potions
- **Smart Timing**: When timer reaches zero, waits for safe opportunity (doesn't break mid-action)
- **Compatibility**: Works alongside Microbot's built-in break handler
- **Force Break**: If no safe spot found within 10 minutes, forces a break by walking to safe location

## Tips

- Default settings work well for most players
- Potions are more efficient than food
- Enable break system for better anti-detection
