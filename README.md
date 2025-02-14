# Time Loop
A mod that 'loops' time by using the Motion Capture Mod.
*Motion Capture mod by mt1006. Inspired by Tombino. Original datapack by Penguin Mafia. Fabric version by Luigi & VLTNO.*

# Installation
**Requires Motion Capture 1.4 Alpha 6 and Minecraft 1.21.1**
https://modrinth.com/mod/motion-capture/version/1.4-alpha-6-fabric-1.21.1

The mod works in singleplayer and multiplayer.

# Usage
Simply use commands to configure the loop.

`/loop start` - Start the loop.

`/loop stop` - Stop the loop.

`/loop reset` - Reset the loop and go back to the first recording. **This doesn't delete the recordings as of yet but it's being worked on.**

`/loop status` - Shows the status of the loop in chat.

`/loop maxLoops [0]` - Sets the maximum amount of loops. 0 is infinite.

`/loop setTicks [6000]` - Set the duration / length of the loop in ticks (6000 ticks is 5 mins).

`/loop setTimeOfDay [13000]` - Sets the time of day to loop at (same as minecraft so 13000 is night).

`/loop trackItems [false]` - Sets tracking items during loops.

`/loop loopType [TICK]` - Sets the type of loop.

# LoopType Options
- `TICK` (Loops every `setTicks` ticks)
- `TIME_OF_DAY` (Loops when the time reaches `setTimeOfDay`)
- `SLEEP` (Loops when you sleep)
- `DEATH` (Loops when you die)

# Support
If you need help or encounter an issue, don't hesitate to ask someone in the discord: https://discord.gg/nzDETZhqur
