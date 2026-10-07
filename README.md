# Orbital Strikes

A **Quilt 1.19.2** mod with a backported 1.21 [Mace](#mace-backported-from-121) and a recreation of Wemmbu's fishing-rod orbital strike cannon. Cast the rod, reel it back in,
and the strike comes down wherever the bobber was. A coloured targeting beam marks the spot for 1.5 seconds first.

| Cannon | What it does |
| --- | --- |
| **Nuke** | Drops about 400 TNT from 60 blocks up in 10 rings (out to a 30-block radius). Everything lands and goes off together. |
| **Stab** | Drills a narrow TNT shaft straight down until it hits bedrock or the bottom of the world. |
| **Wither** | Drops 5 waves of wither skulls in rings (some are the blue, block-breaking kind), then summons a Wither in the middle with its normal spawn blast. |
| **Dog** | Drops 3 waves of wolves (about 120 in total) from the sky. They're tamed to you, have random collar colours, include some puppies, and get Resistance V so they survive the landing. |

## Getting the cannons

* **Creative:** look in the *Combat* tab.
* **Crafting:**
  * Nuke: 8 TNT around a fishing rod.
  * Stab: 2 TNT stacked on top of a fishing rod (one column).
  * Wither: 3 wither skeleton skulls on the top row, a fishing rod in the middle, a nether star below it.
  * Dog: 8 bones around a fishing rod.
* **Wemmbu style:** rename a plain fishing rod in an anvil to `Nuke`, `Stab`, `Wither` or `Dog`. It works the same way.
* **Command (operators):** `/orbitalstrike <nuke|stab|wither|dog> [x y z]`. Without coordinates it targets the block you're looking at, up to 256 blocks away.

Reeling in sets a 2-second cooldown. The cannons have no durability. Range is the normal fishing-rod range (the bobber
breaks at 32 blocks).

## Mace (backported from 1.21)

The mace and its enchantments, rebuilt for 1.19.2.

* **Stats:** 6 attack damage, 0.6 attack speed, 500 durability. Repair it with blaze rods (breeze rods don't exist in 1.19.2).
* **Smash attack:** hit something after falling more than 1.5 blocks. You get +4 damage per block for the first 3 blocks,
  +2 per block for the next 5, and +1 per block after that. The smash cancels your fall damage. It also knocks back
  every mob within 3.5 blocks of the target, and twice as hard if you fell more than 5 blocks. Smashes don't work while
  gliding with an elytra.
* **Enchantments:**
  * **Density I–V:** +0.5 smash damage per block fallen, per level.
  * **Breach I–IV:** each level makes the target's armor 15% less effective.
  * **Wind Burst I–III:** a smash launches you back up into the air. Falling back down only hurts for the distance
    below where you were launched, so you can chain smashes.
  * The mace also takes Smite, Bane of Arthropods, Fire Aspect, Unbreaking and Mending. Density, Breach, Smite and
    Bane of Arthropods can't be combined.
  * Density and Breach come up at the enchanting table. Wind Burst is a treasure enchantment, so it only comes from
    enchanted books (librarian trades or `/give`).
* **Crafting:**
  * Heavy Core: 8 iron blocks around a netherite ingot (1.19.2 has no trial chambers to find one in).
  * Mace: Heavy Core on top of a blaze rod.
* **Commands:** `/give @s orbital_strikes:mace`, `/enchant @s orbital_strikes:wind_burst 3`.

1.19.2 has no mace sounds, so the smash borrows vanilla sounds (an anvil thud for heavy hits).

## Requirements

* Minecraft 1.19.2
* [Quilt Loader](https://quiltmc.org/)
* [Quilted Fabric API (QFAPI/QSL)](https://modrinth.com/mod/qsl)

The mod has to be installed on the server. Clients need it too, for the items and textures.

## Building

```sh
./gradlew build
```

The jar ends up in `build/libs/`. Every push also builds the jar on GitHub Actions; download it from the
**orbital-strikes** artifact on the workflow run.
