# BetterFurnacesReforged ![https://www.curseforge.com/minecraft/mc-mods/better-furnaces-reforged/files](https://cf.way2muchnoise.eu/395653.svg) ![https://beta.curseforge.com/minecraft/mc-mods/better-furnaces-reforged/files](https://cf.way2muchnoise.eu/versions/For%20MC_395653_all.svg)
## Resume

This mod project plans to rewrite the [old Better Furnaces mod](https://www.9minecraft.net/better-furnaces-mod/) for the new versions, adapting it to the new Minecraft concepts, but without leaving its old look.

![](https://i.imgur.com/iQMxtUE.jpg)

## Mod Additions
### Furnaces (each can be upgraded to the next tier and have upgrades like Liquid Fuel, Fuel Efficiency, and Processing)
- Copper Furnace (1.14x speed)
- Iron Furnace (1.33x speed)
- Steel Furnace (1.6x speed)
- Gold Furnace (2x speed)
- Amethyst Furnace (3x speed)
- Diamond Furnace (4x speed)
- Platinum Furnace (8x speed)
- Netherhot Furnace (25x speed)
- Extreme Furnace (50x speed)
- Ultimate Furnace (200x speed)

*Each of these speed tiers can be configured in ```<world_dir>/serverconfig/betterfurnacesreforged-server.toml archive```, and if using Fabric or Quilt only with [ForgeConfigApiPort mod installed](https://github.com/Fuzss/forgeconfigapiport)*

### Forges (like their furnace counterparts, but they can smelt 3 separate items at a time, and can also be upgraded)
- Copper Forge (1.14x3 speed)
- Iron Forge (1.33x3 speed)
- Gold Forge (2x3 speed)
- Diamond Forge (4x3 speed)
- Netherhot Forge (25x3 speed)
- Extreme Forge (50x3 speed)
- Ultimate Forge (200x speed)

### Upgrades (advanced upgrades are unbreakable, while regular upgrades that double something can break after usage)
- (Advanced) Fuel Efficiency - doubles the amount that each fuel can smelt
- (Advanced) Ore Processing - doubles the amount that each smelted item yields
- Auto Input/Output - items automatically enter or exit nearby inventories
- Redstone Signal - controls furnace or forge operation based on redstone signals
- Factory - combines the Auto Input/Output and Redstone Signal upgrades
- Color - change the color of the furnace (right click to configure)
- Blasting/Smoking - change the furnace type to blast furnace or smoker for their specific bonuses
- Fuel Liquid - use liquid fuel like lava buckets instead of solid fuel
- Energy - uses Energy instead of fuel
- XP Tank - turns resulting experience points into liquid experience points in a created tank
- Generator - instead of smelting items, this furnace will generate Energy from water evaporation
- Ultimate Ore Processing - quadruples infinitely the amount that each smelted ore type yields
- Tier upgrades - furnaces can be upgraded in-place to the next tier without having to break and replace it

## Objective and Thanks

I have no financial interests in this, the fun and knowledge acquired to do it are the most important.  Parts of code from other open source mods were used for certain elements.  
The Iron Furnaces mod code base was completely used to make this version
