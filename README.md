# Time Loop
[![Static Badge](https://img.shields.io/badge/GitHub-Release-green?logo=Github)](https://github.com/VLTNOgithub/Time-Loop/releases/latest)
[![Static Badge](https://img.shields.io/badge/Modrinth-Release-green?logo=Modrinth)](https://modrinth.com/mod/timeloop/version/latest)
\
A mod that 'loops' time by using the Motion Capture Mod.
*Motion Capture mod by mt1006. Inspired by [this Tombino video](https://www.youtube.com/watch?v=i602-oh0a0c). Original datapack by Penguin Mafia. Fabric version by Luigi & VLTNO.*

# Installation
**Requires Motion Capture 1.4 Alpha 8 and Minecraft 1.21.1**

[Mocap 1.4 Alpha 8 - Fabric](https://modrinth.com/mod/motion-capture/version/1.4-alpha-8-fabric-1.21.1)

[Mocap 1.4 Alpha 8 - NeoForge](https://modrinth.com/mod/motion-capture/version/1.4-alpha-8-neoforge-1.21.1)

The mod works in singleplayer and multiplayer.

# Usage
Simply use commands to configure the loop.

**/loop**
 - `start` - Start the loop.
 - `stop` - Stop the loop.
 - `reset` - Reset the loop and go back to the first recording. **This doesn't delete the recordings as of yet but it's being worked on.**
 - `status` - Shows the status of the loop in chat.
 - **settings**
   - `setLoopType [TICK]` - Sets the type of loop.
   - `setLength [6000]` - Set the duration / length of the loop in ticks (6000 ticks is 5 mins).
   - `maxLoops [0]` - Set the maximum amount of loops. 0 is infinite.
   - `setTimeOfDay [13000]` - Sets the time of day to loop at (same as minecraft so 13000 is night).
   - `modifyPlayer [target_player] [nickname] [skin]` - Changes a looped player's nickname and skin.
   - **toggles**
     - `trackTimeOfDay [true]` - Toggles tracking the time of day during loops.
     - `trackItems [false]` - Toggles tracking items during loops.
     - `showLoopInfo [true]` - Toggles a bar at the top of the screen showing the amount of ticks/time left until the next loop.
     - `displayTimeInTicks [false]` - Displays the ticks instead of HH:MM:SS on the info bar.
     - `trackChat [false]` - Toggles tracking chat during loops.
     - `hurtLoopedPlayers [false]` - Toggles being able to hit looped players.

# LoopType Options
 - `TICK` (Loops every `setLength` ticks)
 - `TIME_OF_DAY` (Loops when the time reaches `setTimeOfDay`)
 - `SLEEP` (Loops when you sleep)
 - `DEATH` (Loops when you die)

# Known Issues
 - You can see spectators' held item and worn armour
 - If the loop starts before a player joins, they cannot see the loops
 - Loops don't work through dimensions
 - Items sometimes do not loop.

### Issue Links
https://github.com/mt1006/mc-mocap-mod/issues/53
https://github.com/mt1006/mc-mocap-mod/issues/56
https://github.com/mt1006/mc-mocap-mod/issues/57

# Support
If you need help or encounter an issue, don't hesitate to ask someone in the discord: https://discord.gg/nzDETZhqur
