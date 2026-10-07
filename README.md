# Orbital Strikes

A **Quilt 1.19.2** mod that recreates Wemmbu's fishing-rod orbital strike cannon. Cast the rod, reel it back in,
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
