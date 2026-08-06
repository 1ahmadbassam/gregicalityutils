# GregicalityUtils

This is a helper mod I did through AI (what, you really want me to code while playing Minecraft? I do enough of that already) that attempts to fix bugs or implement features that I find useful while playing Gregicality: Skyblock Edition.

This mod only perfectly makes sense with that specific modpack, and not any instance, but my own one as I heavily tweak the pack every now and then to fix all the dumb problems (might even say it's tied to my specific world too, but that's _probably_ not true).

Note: Gregicality: Skyblock Edition is a horrible pack. Don't play it. It's a half-assed improvement over Gregblock. Unless you wanna spend potential hours of your playtime debugging or editing configs and scripts and even quests.

I don't guarantee you anything works in this mod. This is really just for personal use and some stuff is still untested as I did not reach them in my world yet. You're using AI code, keep that in mind, and keep a lot of backups. This is also updated frequently as I play and discover problems and no guarantees I keep this README up-to-date.

No JAR. Build it yourself. Simply run `gradlew build` with JDK 25 on your PATH, not hard.

## What This Does

Required Dependencies: Gregtech: Nomifactory Edition 1.18.3, Gregicality (Obviously), OpenComputers, GTCE2OC, XNet, OCXNetDriver, ProgressiveAutomation, Extra Utilities 2, and their respective libraries.

Optional Dependencies: Baubles.

### Standalone features (blocks)

* Analog Emitter, which works exactly like the one from Random Things. Didn't want to add an entire mod into my pack just for that one block so that's why it exists.
* Twerk Simulator, which is a block version of Twerk-Sim 2K16 but also works with sugarcane, cactus, nether wart, mushroom (to mushroom trees), and even ender lilies from Extra Utilities 2. Can be considered "cheaty" as it basically breaks farming but I don't care or want to focus on food or farming honestly.
* Sheep Stimulator, which forces sheep to eat grass every second to regen their wool. Best used with grass seeds.

### ProgressiveAutomation Features

* Allow Gregtech axes to be used with the Chopper. Material tier levels 1 to 3 are accepted by wooden+ choppers, whereas tiers 4 and above require an iron+ chopper.

### OpenComputers Features

* For the GTCE driver, this introduces a "getPosition()" method on machine objects to retrieve their absolute X, Y, and Z coordinates. It is also supposed to introduce a "getBridgePosition()" method for the bridge itself but I don't think that ever worked.

* For the XNet driver:
  * First, this allows XNet to access all slots of Gregtech machines regardless if they are input or output. This works if the tile entity is an instance of MetaTileEntityHolder and is using an ItemHandlerProxy. This is used by my old OpenComputers automation script as an LV alternative to AE2, specifically for retrieving non-consumable items after a craft completes.
  * This also introduces a breaking API change to the XNet "getItems()" interface to use a memory-efficient sparse format when returning the data. This is useful for running my old automation script to fit within 1.5MB of OpenComputers' RAM. The API is detailed below.
  
`getItems(pos:table[, side: number][, advanced: boolean])`

Lists all non-empty items in a given inventory using a memory-efficient sparse format.

* **Parameters:**
* `pos`: Position of the inventory.
* `side` *(Optional)*: Side of the block. *(Note: You can omit the side and just pass the `advanced` boolean as the second parameter).*
* `advanced` *(Optional)*: Boolean. If `true`, includes heavy NBT data fields (`name`, `damage`, `maxDamage`, `maxSize`, `hasTag`).


* **Returns:** A table containing:
* `slots`: The total number of slots in the inventory (including empty ones).
* `content`: A sparse list of tables representing only the occupied slots. Each item table contains `slot` (1-indexed), `label`, `size`, and optionally the advanced fields.

### Extra Utilities 2 Features

* Optimizes the following: Flat Transfer Nodes, Analog Crafter, Mechanical User.
* Allows the Mechanical Miner to OreExcavate (break down completely) a mushroom tree.
* Adds a configurable keybind for activating inferior flight rings (chicken ring and squid ring).
* Fix bug with analog crafter side-gating interaction with AE2 buses.
* Analog Crafter new features:
  * Support speed upgrades like the Mechanical Crafter.
  * Allow limiting items in slots to 1 rather than 1 stack.

### GregTech Features

* Optimizes the conveyor cover.
* Backport [ULV recipe overclocking bug](https://github.com/Nomifactory/GregTech/commit/99d9bc954a0357ded01bdf7e42e2ffb36c4bcf68) and [Fix macerators giving wrong number of overclock bonuses](https://github.com/Nomifactory/GregTech/commit/e88c58bb8b2c833a2d480884163e4d703a04baae) while retaining existing method signatures.
* Workbench changes:
  * Optimize the thing to not tick at all while not open (seriously who designed this).
  * If an item is taken from a storage, it is returned to the same storage (relevant when interacting with the storage tab).
  * Highlight missing items in a craft with a red overlay like CEu's workbench (PhantomSlotWidget change). Finnicky, especially with oredicts and NBTs, but good enough. Known bug: items from the oredicts which are actually used for crafting won't be reflected on the interface until you craft at least one item.

### Gregicality Features

* Support GTCE: Nomifactory edition 1.18.3. This version was chosen because implements important bugfixes and the important notifiable inventories optimization while retaining mostly the same API interface and feel. Not everything is tested in this regard.
* Allow rock breakers to check for their required fluids from the bottom and top, not just the three non-front facing side.
* Rock breakers can now generate dirt and coarse dirt if placed near water and witchwater at the same rate as stones.
* Allow armor suites to be worn in Bauble slots. I only meant to add this for the nightvision goggles, battery pack, and jetpacks, but apparently it worked for everything. Don't abuse it.
* Bug fixes:
  * Add the missing overclock button to electric sieves. Also expand the internal sieve inventory for all sieves to 54 slots to successfully run recipes that require 54 distinct item entires (I'm looking at you dirt). Known bug: the GUI is still 24 slots and no I don't intend to fix this. It works correctly with automation (i.e. pipes or conduits) and the lower 30 slots are only used if the first 24 slots are full.
  * Rewrite large multiblock recipe logic completely since the old logic was not working reliably. Apparently it was counting programmed circuits as consumable items or something. Now they work as intended. Furthermore, disable recipe caching for large multiblocks to ensure maximum parallelism when running recipes.
  * Fix running blast furnaces + large multiblocks breaking on world reload (NullPointerException).
  * Fix Central Monitor & Screens not successfully reloading its configuration and caching stale data on world reload, causing it to freeze and require reconfiguration.

## Legal & Credits

* **Minecraft** is a trademark of **Mojang Synergies AB** / **Microsoft Corporation**. This mod is not affiliated with, endorsed by, or associated with Mojang AB or Microsoft.
* **Mods & Dependencies:** All third-party mods, APIs, and libraries mentioned or utilized by this project (including GregTech, Gregicality, OpenComputers, XNet, ProgressiveAutomation, Extra Utilities 2, and Baubles) are the intellectual property of their respective owners and creators. 
* **Assets & Code:** Any assets or original code snippets adapted from other open-source projects remain under their original respective open-source licenses.
* **Template:** [ForgeDevEnv](https://github.com/CleanroomMC/ForgeDevEnv), Ownership of CleanRoomMC.