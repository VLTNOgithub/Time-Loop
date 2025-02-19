# Time Loop
A mod that 'loops' time by using the Motion Capture Mod.
*Motion Capture mod by mt1006. Inspired by Tombino. Original datapack by Penguin Mafia. Fabric version by Luigi & VLTNO.*

# Installation
**Requires Motion Capture 1.4 Alpha 6 and Minecraft 1.21.1**
https://modrinth.com/mod/motion-capture/version/1.4-alpha-6-fabric-1.21.1

The mod works in singleplayer and multiplayer.

# Usage
Simply use commands to configure the loop.

**/loop**
 - `start` - Start the loop.
 - `stop` - Stop the loop.
 - `reset` - Reset the loop and go back to the first recording. **This doesn't delete the recordings as of yet but it's being worked on.**
 - `status` - Shows the status of the loop in chat.
 - **settings**
   - `maxLoops [0]` - Sets the maximum amount of loops. 0 is infinite.
   - `setLength [6000]` - Set the duration / length of the loop in ticks (6000 ticks is 5 mins).
   - `setTimeOfDay [13000]` - Sets the time of day to loop at (same as minecraft so 13000 is night).
   - `loopType [TICK]` - Sets the type of loop.
   - **toggles**
     - `trackTimeOfDay [true]` - Toggles tracking the time of day during loops.
     - `trackItems [false]` - Toggles tracking items during loops.
     - `showLoopInfo [true]` - Toggles a bar at the top of the screen showing the amount of ticks/time left until the next loop.
     - `displayTimeInTicks [false]` - Displays the ticks instead of HH:MM:SS on the Progress bar.

# LoopType Options
 - `TICK` (Loops every `setLength` ticks)
 - `TIME_OF_DAY` (Loops when the time reaches `setTimeOfDay`)
 - `SLEEP` (Loops when you sleep)
 - `DEATH` (Loops when you die)

# Known Bugs
 - Items stop tracking after around 100 loops
 - Loops continue but don't track the player after 100 loops
 - Skins are broken in multiplayer

# Support
If you need help or encounter an issue, don't hesitate to ask someone in the discord: https://discord.gg/nzDETZhqur
