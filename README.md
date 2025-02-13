# The Loop
A mod that 'loops' time by using the Motion Capture Mod.
*Motion Capture mod by mt1006. Inspired by Tombino. Original datapack by Penguin Mafia. Fabric version by Luigi. Fixed by VLTNO.*

# Installation
**Requires Motion Capture 1.4 Alpha 6 and Minecraft 1.21.1**
https://modrinth.com/mod/motion-capture/version/1.4-alpha-6-fabric-1.21.1

The mod works in singleplayer and multiplayer.

# Usage
Simply use commands to configure the loop.

`/loop start` - Start the loop.

`/loop stop` - Stop the loop.

`/loop reset` - Reset the loop and go back to the first recording. **This doesn't delete the recordings as of yet but it's being worked on.**

`/loop setLength 6000` - Set the duration / length of the loop. The unit is ticks (6000 ticks is 5 mins).

`/loop status` - Shows the status of the loop in chat.

`/loop maxLoops 0` - Sets the maximum amount of loops. 0 is infinite.

`/loop loopBasedOnTimeOfDay` - Enables/disables looping based on the time of day. **This overrides `setLength`.**

`/loop setTimeOfDay` - Sets the time of day to loop at (same as minecraft so 13000 is night).

`/loop loopOnSleep` - Enables/disables looping when sleeping. **This overrides `loopBasedOnTimeOfDay` and `setLength`.**

## Loop order
`loopOnSleep` > `loopBasedOnTimeOfDay` > `setLength`

# Support
If you need help or encounter an issue, don't hesitate to ask someone in the discord: https://discord.gg/nzDETZhqur
