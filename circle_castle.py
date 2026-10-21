#!/usr/bin/env python3
"""Generate a huge circular island castle as a Sponge schematic (.schem).

Layout (outside to inside):
  * a round white foundation island with a ledge, a parapet and a grand
    staircase down to a dock at the front (south, +Z)
  * a 6-block-thick curtain wall with an arched inner gallery, battlements
    and 24 round towers topped with teal spires; two bigger towers flank
    the main gate, which has carved portals, a portcullis and a gabled
    gatehouse with a rose window and balconies above it
  * a grass courtyard with a ring path, radial avenues, fountain plazas
    and gardens, lamp posts and a ring of smaller towers

Everything is mirror-symmetric left/right (X), and front/back (Z) except the
main gate at the front and the two tall towers at the back.
  * a three-tier stepped palace: terraces with turrets and pavilions, a
    cross-shaped keep with gabled teal roofs, four corner towers and a big
    central donjon
  * two very tall towers at the back (part of the courtyard tower ring),
    joined to the wall towers behind them by arched flying bridges

The schematic origin is the centre of the island at courtyard ground level
(the grass is at y=0). The dock at the foot of the front staircase is at
y=-10, so paste with the origin about 10 blocks above the ocean surface.

Usage:
    pip install mcschematic
    python circle_castle.py                       # -> circle_castle.schem
    python circle_castle.py -o schematics -n my_castle -v JE_1_20_1

Then in game (WorldEdit/FAWE): //schem load circle_castle   //paste -a
"""

import argparse
import math

import mcschematic

# --------------------------------------------------------------------------
# Palette
# --------------------------------------------------------------------------
WHITE = "minecraft:smooth_quartz"
BRICK = "minecraft:quartz_bricks"
PILLAR = "minecraft:quartz_pillar[axis=y]"
TRIM = "minecraft:polished_diorite"
PATH = "minecraft:calcite"
CORE = "minecraft:stone_bricks"
SLAB = "minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]"
GLASS = "minecraft:light_blue_stained_glass"
ROOF = "minecraft:waxed_oxidized_cut_copper"
ROOF_EDGE = "minecraft:prismarine_bricks"
ROOF_CAP = "minecraft:waxed_oxidized_cut_copper_slab[type=bottom,waterlogged=false]"
SPIRE_TIP = "minecraft:end_rod[facing=up]"
CHISELED = "minecraft:chiseled_quartz_block"
BARS_EW = ("minecraft:iron_bars"
           "[east=true,north=false,south=false,waterlogged=false,west=true]")
HANGING_LANTERN = "minecraft:lantern[hanging=true,waterlogged=false]"
GRASS = "minecraft:grass_block[snowy=false]"
DIRT = "minecraft:dirt"
WATER = "minecraft:water[level=0]"
LANTERN = "minecraft:lantern[hanging=false,waterlogged=false]"


def roof_stair(facing):
    return ("minecraft:waxed_oxidized_cut_copper_stairs"
            f"[facing={facing},half=bottom,shape=straight,waterlogged=false]")


def quartz_stair(facing):
    return ("minecraft:quartz_stairs"
            f"[facing={facing},half=bottom,shape=straight,waterlogged=false]")


# --------------------------------------------------------------------------
# Dimensions
# --------------------------------------------------------------------------
R_OUT = 100                 # outer face of the curtain wall
WALL_T = 6                  # curtain wall thickness
R_IN = R_OUT - WALL_T       # inner face of the curtain wall
R_ISLAND = 106              # edge of the island ledge at ground level
WALL_H = 14                 # wall-walk floor height
GALLERY_H = 10              # height of the arched gallery inside the wall
FOUND_BOTTOM = -16          # bottom of the foundation

WALL_TOWER_R = 97           # ring the wall towers sit on
N_WALL_TOWERS = 24
GATE_TOWER_IDS = (5, 6)     # the towers at 82.5 and 97.5 degrees flank the gate
GATE_HALF_W = 4
PLAZA_DIST = 84             # plazas on the ring path, north and south
PLAZA_R = 8

# (radius, y of the surface it stands on, height)
TIERS = [(50, 0, 6), (38, 6, 6), (26, 12, 6)]

MID_TOWER_DIST = 64         # ring of 8 smaller towers in the courtyard
# Two of the courtyard ring towers (north-west and north-east; 270 degrees is
# straight north, -Z) are replaced by very tall towers bridged to the wall
# towers behind them.
TALL_TOWER_ANGLES = (247.5, 292.5)
TALL_TOWER_R = 7

# Stairs climb towards the centre, so they face the centre.
CARDINALS = [  # (angle in degrees, stair facing)
    (0, "west"), (90, "north"), (180, "east"), (270, "south"),
]


# --------------------------------------------------------------------------
# Block buffer
# --------------------------------------------------------------------------
class Builder:
    """Keeps blocks in a dict so later parts can overwrite or carve earlier ones."""

    def __init__(self):
        self.blocks = {}

    def set(self, x, y, z, block):
        self.blocks[(x, y, z)] = block

    def clear(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def to_schematic(self):
        schem = mcschematic.MCSchematic()
        for pos, block in self.blocks.items():
            schem.setBlock(pos, block)
        return schem


def disc_points(cx, cz, r):
    """Yield (x, z, dist) for every column within radius r of (cx, cz)."""
    ir = int(math.ceil(r)) + 1
    for x in range(cx - ir, cx + ir + 1):
        for z in range(cz - ir, cz + ir + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.5:
                yield x, z, d


def polar(angle_deg, dist):
    a = math.radians(angle_deg)
    return round(dist * math.cos(a)), round(dist * math.sin(a))


def angle_diff(a, b):
    """Smallest absolute difference between two angles in radians."""
    return abs((a - b + math.pi) % (2 * math.pi) - math.pi)


# --------------------------------------------------------------------------
# Building blocks
# --------------------------------------------------------------------------
def cone(b, cx, cz, y0, radius, height):
    """Hollow teal spire with a prismarine eave ring and an end-rod tip."""
    for x, z, d in disc_points(cx, cz, radius):
        for i in range(height):
            rr = radius * (1 - i / height) ** 1.15
            if d > rr + 0.5:
                break
            if i == 0:
                b.set(x, y0, z, ROOF_EDGE if d > rr - 1.0 else ROOF)
            elif d > rr - 1.6 or rr < 1.6:
                b.set(x, y0 + i, z, ROOF)
    b.set(cx, y0 + height, cz, ROOF)
    b.set(cx, y0 + height + 1, cz, SPIRE_TIP)
    b.set(cx, y0 + height + 2, cz, SPIRE_TIP)


def tower(b, cx, cz, y0, r, h, roof_h, *, face=90, doors=(), floor_ys=(),
          flare=True, belfry=False):
    """Round hollow tower with windows, floors and a conical roof.

    face: angle (degrees) a window column is centred on; the window count is
          a multiple of 4 so the pattern is mirror-symmetric about `face`.
    doors: (angle_deg, y_bottom, height) openings cut through the shell.
    """
    top = y0 + h
    inner = r - 1.3
    n_win = 4 * max(1, round(r * 1.3 / 4))
    floors = set(range(y0, top, 7)) | set(floor_ys) | {top}
    for x, z, d in disc_points(cx, cz, r + 1.5):
        ang = math.atan2(z - cz, x - cx) - math.radians(face)
        wpos = (ang / (2 * math.pi) * n_win + 0.5) % 1
        arc_from_win = abs(wpos - 0.5) * 2 * math.pi * r / n_win
        is_win = arc_from_win < 0.6
        is_pilaster = r >= 5 and (wpos < 0.07 or wpos > 0.93)
        for y in range(y0, top + 1):
            if d <= inner:
                if y in floors:
                    b.set(x, y, z, WHITE)
                else:
                    b.clear(x, y, z)
            elif d <= r + 0.5:
                rel = (y - y0) % 7
                if belfry and top - 7 < y < top - 1 and arc_from_win < 1.6:
                    b.clear(x, y, z)
                elif is_win and rel in (2, 3, 4) and y0 < y < top - 2:
                    b.set(x, y, z, GLASS)
                elif is_pilaster and y < top - 1:
                    b.set(x, y, z, PILLAR)
                else:
                    b.set(x, y, z, BRICK)
            elif y >= top - 1 or (flare and y <= y0 + 1):
                b.set(x, y, z, TRIM)
    cone(b, cx, cz, top + 1, r + 2, roof_h)

    for door_ang, dy, dh in doors:
        a = math.radians(door_ang)
        for x, z, d in disc_points(cx, cz, r + 1.5):
            if d <= inner:
                continue
            ang = math.atan2(z - cz, x - cx)
            if angle_diff(ang, a) * r <= 1.6:
                for y in range(dy, dy + dh):
                    b.clear(x, y, z)


def balcony(b, cx, cz, y, r):
    for x, z, d in disc_points(cx, cz, r + 2.5):
        if d > r + 0.5:
            b.set(x, y, z, WHITE)
            if d > r + 1.5:
                b.set(x, y + 1, z, SLAB)


def _walls(put, a0, a1, c0, c1, y0, wall_h):
    """Rectangular hollow walls in (along, across) coordinates.

    Pilasters and windows are spaced out from the middle of each wall so both
    halves match; a0 + a1 and c0 + c1 must be even.
    """
    a_mid, c_mid = (a0 + a1) // 2, (c0 + c1) // 2
    for a in range(a0, a1 + 1):
        for c in range(c0, c1 + 1):
            put(a, y0, c, WHITE)
            on_a, on_c = a in (a0, a1), c in (c0, c1)
            for y in range(y0 + 1, y0 + wall_h + 1):
                if not (on_a or on_c):
                    put(a, y, c, None)
                elif on_a and on_c:
                    put(a, y, c, PILLAR)
                elif y == y0 + wall_h:
                    put(a, y, c, TRIM)
                else:
                    k = abs(a - a_mid) % 4 if on_c else abs(c - c_mid) % 4
                    if k == 2 and y0 + 3 <= y <= y0 + wall_h - 3:
                        put(a, y, c, GLASS)
                    elif k == 0:
                        put(a, y, c, PILLAR)
                    else:
                        put(a, y, c, BRICK)


def hall(b, x0, x1, z0, z1, y0, wall_h, axis="x"):
    """Rectangular hall with a gabled teal roof whose ridge runs along `axis`."""
    if axis == "x":
        a0, a1, c0, c1 = x0, x1, z0, z1
        low_face, high_face = "south", "north"

        def put(a, y, c, blk):
            b.clear(a, y, c) if blk is None else b.set(a, y, c, blk)
    else:
        a0, a1, c0, c1 = z0, z1, x0, x1
        low_face, high_face = "east", "west"

        def put(a, y, c, blk):
            b.clear(c, y, a) if blk is None else b.set(c, y, a, blk)

    _walls(put, a0, a1, c0, c1, y0, wall_h)

    # doors in the middle of both gable ends
    mid = (c0 + c1) // 2
    for c in range(mid - 1, mid + 2):
        for y in range(y0 + 1, y0 + 5):
            put(a0, y, c, None)
            put(a1, y, c, None)

    k = 0
    while True:
        cl, ch = c0 - 1 + k, c1 + 1 - k
        y = y0 + wall_h + k
        if cl > ch:
            break
        for a in range(a0 - 1, a1 + 2):
            if cl == ch:
                put(a, y, cl, ROOF)
                continue
            put(a, y, cl, roof_stair(low_face))
            put(a, y, ch, roof_stair(high_face))
            if k > 0 and a in (a0, a1):
                for c in range(cl + 1, ch):
                    put(a, y, c, BRICK)
            if ch == cl + 1:
                put(a, y + 1, cl, ROOF_CAP)
                put(a, y + 1, ch, ROOF_CAP)
        k += 1


def pavilion(b, cx, cz, half, y0, wall_h):
    """Square pavilion with a pyramid roof."""
    x0, x1, z0, z1 = cx - half, cx + half, cz - half, cz + half

    def put(a, y, c, blk):
        b.clear(a, y, c) if blk is None else b.set(a, y, c, blk)

    _walls(put, x0, x1, z0, z1, y0, wall_h)
    for y in range(y0 + 1, y0 + 5):
        for d in (-1, 0, 1):
            for x, z in ((cx + d, z0), (cx + d, z1), (x0, cz + d), (x1, cz + d)):
                b.clear(x, y, z)

    k = 0
    while True:
        lo_x, hi_x, lo_z, hi_z = x0 - 1 + k, x1 + 1 - k, z0 - 1 + k, z1 + 1 - k
        y = y0 + wall_h + k
        if lo_x >= hi_x:
            b.set(lo_x, y, lo_z, ROOF)
            b.set(lo_x, y + 1, lo_z, SPIRE_TIP)
            break
        for x in range(lo_x, hi_x + 1):
            for z in range(lo_z, hi_z + 1):
                edge_x, edge_z = x in (lo_x, hi_x), z in (lo_z, hi_z)
                if edge_x and edge_z:
                    b.set(x, y, z, ROOF)
                elif x == lo_x:
                    b.set(x, y, z, roof_stair("east"))
                elif x == hi_x:
                    b.set(x, y, z, roof_stair("west"))
                elif z == lo_z:
                    b.set(x, y, z, roof_stair("south"))
                elif z == hi_z:
                    b.set(x, y, z, roof_stair("north"))
        k += 1


# --------------------------------------------------------------------------
# Castle parts
# --------------------------------------------------------------------------
def foundation(b):
    for y in range(FOUND_BOTTOM, 1):
        r = R_ISLAND + (-y) // 3  # battered (sloped) island edge
        for x, z, d in disc_points(0, 0, r):
            if y == 0:
                blk = GRASS if d < R_IN - 0.5 else WHITE
            elif y == -1 and d < R_IN - 0.5:
                blk = DIRT
            elif d > r - 0.5 and y in (-1, -6):
                blk = TRIM
            elif d > r - 2:
                blk = BRICK
            else:
                blk = CORE
            b.set(x, y, z, blk)

    # parapet along the island edge, open at the front staircase
    for x, z, d in disc_points(0, 0, R_ISLAND):
        if d > R_ISLAND - 0.5 and not (abs(x) <= 6 and z > 0):
            b.set(x, 1, z, SLAB)

    # grand staircase from the front ledge down to the dock
    steps = 9
    for k in range(1, steps + 1):
        y, z = -k, R_ISLAND + k
        for x in range(-6, 7):
            b.set(x, y, z, quartz_stair("north"))
            for yy in range(FOUND_BOTTOM, y):
                b.set(x, yy, z, BRICK)
        for x in (-7, 7):
            for yy in range(FOUND_BOTTOM, y + 2):
                b.set(x, yy, z, BRICK)
            if k % 4 == 1:
                lamp_post(b, x, y + 1, z)
            else:
                b.set(x, y + 2, z, SLAB)

    dock_y = -steps - 1
    z_start = R_ISLAND + steps + 1
    for z in range(z_start, z_start + 7):
        for x in range(-10, 11):
            b.set(x, dock_y, z, WHITE)
            for yy in range(FOUND_BOTTOM, dock_y):
                b.set(x, yy, z, BRICK)
            if abs(x) == 10 or z == z_start + 6:
                b.set(x, dock_y + 1, z, SLAB)
    for x in (-10, 10):
        for z in (z_start, z_start + 6):
            b.set(x, dock_y + 1, z, PILLAR)
            b.set(x, dock_y + 2, z, LANTERN)


def curtain_wall(b):
    # Whole numbers of bays and merlons per tower-to-tower segment, with a
    # pillar/merlon centred on angle 0, keep the ring mirror-symmetric and
    # put an arch (hidden by the tower) behind every tower.
    n_bays = 3 * N_WALL_TOWERS
    n_merlons = 14 * N_WALL_TOWERS
    for x, z, d in disc_points(0, 0, R_OUT + 1):
        if d < R_IN - 0.5:
            continue
        frac = (math.atan2(z, x) / (2 * math.pi)) % 1
        if d > R_OUT + 0.5:
            # plinth at the bottom and corbel band under the battlements
            for y in (1, 2, WALL_H - 1, WALL_H):
                b.set(x, y, z, TRIM)
            continue

        t = d - R_IN                     # -0.5 (inner face) .. 6.5 (outer face)
        u = (frac * n_bays % 1) * 8      # 0..8 across a bay, pillar at 0/8
        arch_top = 7 + math.sqrt(max(0.0, 9 - (u - 4) ** 2))
        for y in range(1, WALL_H + 1):
            if y == WALL_H:
                blk = WHITE
            elif t < 1.5:                # arcade facing the courtyard
                if u < 1 or u > 7:
                    blk = PILLAR if y <= 7 else BRICK
                elif y <= arch_top:
                    blk = None
                elif y <= arch_top + 1:
                    blk = TRIM
                else:
                    blk = BRICK
            elif t < 3.5:                # gallery corridor behind the arches
                blk = None if y <= GALLERY_H else BRICK
            else:
                blk = BRICK
            if blk is None:
                b.clear(x, y, z)
            else:
                b.set(x, y, z, blk)

        if t >= 5.5:
            b.set(x, WALL_H + 1, z, BRICK)
            if math.floor(frac * n_merlons + 0.5) % 2 == 0:
                b.set(x, WALL_H + 2, z, BRICK)
        elif t < 0.5:
            b.set(x, WALL_H + 1, z, SLAB)


def wall_towers(b):
    for k in range(N_WALL_TOWERS):
        ang = 7.5 + 360 / N_WALL_TOWERS * k
        cx, cz = polar(ang, WALL_TOWER_R)
        gate = k in GATE_TOWER_IDS
        r, h, roof_h = (7, 34, 20) if gate else (6, 28, 16)
        doors = [
            (ang + 180, 1, 4),               # into the courtyard
            (ang + 90, 1, 4), (ang - 90, 1, 4),            # gallery
            (ang + 90, WALL_H + 1, 3), (ang - 90, WALL_H + 1, 3),  # wall walk
        ]
        if ang in TALL_TOWER_ANGLES:
            doors.append((ang + 180, WALL_H + 1, 3))      # bridge landing
        tower(b, cx, cz, 1, r, h, roof_h, face=ang, doors=doors,
              floor_ys=(WALL_H,), flare=False)


def gate_arch_top(x):
    return 7 + math.sqrt(20.25 - x * x)


def gate(b):
    for x in range(-GATE_HALF_W, GATE_HALF_W + 1):
        top = gate_arch_top(x)
        for z in range(R_IN - 3, R_ISLAND):
            d = math.hypot(x, z)
            if not (R_IN - 1 <= d <= R_OUT + 1.6):
                continue
            for y in range(1, WALL_H):
                if y <= top:
                    b.clear(x, y, z)
                elif y <= top + 1 and (d < R_IN + 0.5 or d > R_OUT - 0.5):
                    b.set(x, y, z, TRIM)
            b.set(x, 0, z, PATH)


def entrance(b):
    """Gatehouse over the main gate with carved portals, portcullis and lights."""
    z_in, z_out = R_IN - 2, R_OUT + 2       # gatehouse spans z_in..z_out

    # Portal frames projecting two blocks from both faces of the wall:
    # quartz-pillar piers, a diorite arch ring and a chiseled keystone.
    for z in (z_in, z_in + 1, z_out - 1, z_out):
        for x in range(-GATE_HALF_W - 2, GATE_HALF_W + 3):
            b.set(x, 0, z, PATH)
            for y in range(1, WALL_H):
                if abs(x) > GATE_HALF_W:
                    b.set(x, y, z, PILLAR)
                elif y <= gate_arch_top(x):
                    b.clear(x, y, z)
                elif y <= gate_arch_top(x) + 1:
                    b.set(x, y, z, CHISELED if x == 0 else TRIM)
                else:
                    b.set(x, y, z, BRICK)
        for x in (-GATE_HALF_W - 2, GATE_HALF_W + 2):
            b.set(x, WALL_H, z, TRIM)

    # pinnacles on the outer piers
    for z in (z_in, z_out):
        for x in (-GATE_HALF_W - 2, GATE_HALF_W + 2):
            for y in range(WALL_H, WALL_H + 3):
                b.set(x, y, z, PILLAR)
            b.set(x, WALL_H + 3, z, ROOF)
            b.set(x, WALL_H + 4, z, SPIRE_TIP)

    # gatehouse hall on top of the gate, open to the gate towers' wall-walk doors
    half = GATE_HALF_W + 1
    hall(b, -half, half, z_in, z_out, WALL_H, 8, axis="z")
    for x in (-half, half):
        for z in range(WALL_TOWER_R - 2, WALL_TOWER_R + 2):
            for y in range(WALL_H + 1, WALL_H + 4):
                b.clear(x, y, z)

    # rose windows in both gables
    rose_y = WALL_H + 8 + 3
    for z in (z_in, z_out):
        for x in range(-2, 3):
            for y in range(rose_y - 2, rose_y + 3):
                if b.blocks.get((x, y, z)) != BRICK:
                    continue
                dd = x * x + (y - rose_y) ** 2
                if dd <= 2:
                    b.set(x, y, z, GLASS)
                elif dd <= 5:
                    b.set(x, y, z, CHISELED)

    # balconies in front of both gable doors, on upside-down stair corbels
    for z_edge, step, corbel_facing in ((z_out, 1, "north"), (z_in, -1, "south")):
        for i in (1, 2):
            z = z_edge + step * i
            for x in range(-3, 4):
                b.set(x, WALL_H, z, WHITE)
                if abs(x) == 3 or i == 2:
                    b.set(x, WALL_H + 1, z, SLAB)
                if i == 1:
                    b.set(x, WALL_H - 1, z,
                          "minecraft:quartz_stairs[facing=" + corbel_facing +
                          ",half=top,shape=straight,waterlogged=false]")
        b.set(0, WALL_H - 1, z_edge + step * 2, HANGING_LANTERN)

    # raised portcullis and lanterns along the gate passage
    for x in range(-GATE_HALF_W, GATE_HALF_W + 1):
        top = math.floor(gate_arch_top(x))
        for y in (top - 1, top):
            b.set(x, y, WALL_TOWER_R + 1, BARS_EW)
    for z in (z_in, WALL_TOWER_R - 2, z_out - 1):
        b.set(0, math.floor(gate_arch_top(0)), z, HANGING_LANTERN)

    # paved approach across the ledge, lamp posts and obelisks at the stairs
    for z in range(z_out + 1, R_ISLAND + 1):
        for x in range(-GATE_HALF_W - 1, GATE_HALF_W + 2):
            b.set(x, 0, z, TRIM if abs(x) > GATE_HALF_W else PATH)
    for x in (-9, 9):
        lamp_post(b, x, 0, z_out + 1, height=3)
    for x in (-7, 7):
        b.set(x, 1, R_ISLAND, BRICK)
        for y in range(2, 5):
            b.set(x, y, R_ISLAND, PILLAR)
        b.set(x, 5, R_ISLAND, CHISELED)
        b.set(x, 6, R_ISLAND, ROOF)
        b.set(x, 7, R_ISLAND, SPIRE_TIP)


def grounds(b):
    r_tier1 = TIERS[0][0]
    avenues = [math.radians(a) for a in range(0, 360, 45)]
    for x, z, d in disc_points(0, 0, R_IN - 1):
        if d < r_tier1 - 0.5:
            continue
        on_path = 85.5 <= d <= 89.5 or d <= r_tier1 + 3.5
        for a in avenues:
            along = x * math.cos(a) + z * math.sin(a)
            perp = abs(-x * math.sin(a) + z * math.cos(a))
            north_south = abs(math.sin(a)) > 0.99
            half_w = 3.5 if north_south else 1.5
            if along > 0 and perp <= half_w:
                on_path = True
        if on_path:
            b.set(x, 0, z, PATH)

    # fountain gardens between the avenues
    for k in range(16):
        cx, cz = polar(11.25 + 22.5 * k, 76)
        for x, z, d in disc_points(cx, cz, 7):
            if d > 6.5:
                b.set(x, 1, z, SLAB)
            elif d <= 1.5:
                b.set(x, 0, z, WATER)
            elif d <= 2.5:
                b.set(x, 0, z, WHITE)
                b.set(x, 1, z, SLAB)
            elif 4.0 < d <= 5.0:
                b.set(x, 1, z, "minecraft:flowering_azalea_leaves"
                               "[distance=7,persistent=true,waterlogged=false]")
        b.set(cx, 0, cz, WHITE)
        b.set(cx, 1, cz, PILLAR)
        b.set(cx, 2, cz, LANTERN)

    # lamp posts inside the ring path (none inside the plazas)
    for k in range(48):
        x, z = polar(3.75 + 7.5 * k, 84)
        if abs(x) < PLAZA_R + 2:
            continue
        lamp_post(b, x, 0, z)

    # plazas where the north and south avenues cross the ring path
    for sz in (1, -1):
        plaza(b, 0, PLAZA_DIST * sz)

    # ring of smaller towers (two are replaced by the tall towers)
    for k in range(8):
        ang = 22.5 + 45 * k
        if ang in TALL_TOWER_ANGLES:
            continue
        cx, cz = polar(ang, MID_TOWER_DIST)
        tower(b, cx, cz, 1, 4, 18, 11, face=ang, doors=[(ang + 180, 1, 4)])


def lamp_post(b, x, y, z, height=2):
    for i in range(1, height + 1):
        b.set(x, y + i, z, PILLAR)
    b.set(x, y + height + 1, z, LANTERN)


def plaza(b, cx, cz):
    """Round paved plaza with a fountain, an obelisk and four lamp posts."""
    for x, z, d in disc_points(cx, cz, PLAZA_R):
        for y in range(1, 5):
            b.clear(x, y, z)
        if d > PLAZA_R - 0.5 or 4.5 < d <= 5.5:
            b.set(x, 0, z, TRIM)
        elif d <= 2.5:
            b.set(x, 0, z, WATER)
        elif d <= 3.5:
            b.set(x, 0, z, WHITE)
            b.set(x, 1, z, SLAB)
        else:
            b.set(x, 0, z, PATH)
    b.set(cx, 0, cz, WHITE)
    for y in range(1, 4):
        b.set(cx, y, cz, PILLAR)
    b.set(cx, 4, cz, CHISELED)
    b.set(cx, 5, cz, LANTERN)
    for dx in (-5, 5):
        for dz in (-5, 5):
            lamp_post(b, cx + dx, 0, cz + dz)


def terrace(b, radius, yb, h, lawn_inner=None):
    n_pil = 8 * round(2 * math.pi * radius / 48)   # ~6 apart, 8-fold symmetric
    pil_gap = 2 * math.pi * radius / n_pil
    top = yb + h
    for x, z, d in disc_points(0, 0, radius):
        s = (math.atan2(z, x) / (2 * math.pi)) % 1 * n_pil
        is_pilaster = abs(s - round(s)) * pil_gap < 0.7
        for y in range(yb + 1, top + 1):
            if y == top:
                if d > radius - 0.5:
                    blk = TRIM
                elif lawn_inner is not None and lawn_inner + 1.5 < d < radius - 2.5:
                    blk = GRASS
                else:
                    blk = WHITE
            elif d > radius - 1.5:
                blk = PILLAR if is_pilaster else BRICK
            else:
                blk = CORE
            b.set(x, y, z, blk)
        if d > radius - 0.5:
            b.set(x, top + 1, z, SLAB)


def terrace_stairs(b, radius, yb, h, angle_deg, facing, half_w):
    a = math.radians(angle_deg)
    ca, sa = round(math.cos(a)), round(math.sin(a))

    def pos(along, lateral):
        return along * ca - lateral * sa, along * sa + lateral * ca

    for lat in range(-half_w - 1, half_w + 2):
        for along in range(radius - 3, radius + 2):   # keep the landing clear
            x, z = pos(along, lat)
            b.clear(x, yb + h + 1, z)
            if abs(lat) <= half_w:
                b.set(x, yb + h, z, WHITE)
        for k in range(1, h + 1):
            x, z = pos(radius + 1 + h - k, lat)
            if abs(lat) <= half_w:
                b.set(x, yb + k, z, quartz_stair(facing))
                for y in range(yb + 1, yb + k):
                    b.set(x, y, z, WHITE)
            else:
                for y in range(yb + 1, yb + k + 1):
                    b.set(x, y, z, BRICK)
                if k == 1:   # lantern posts at the foot of the stairs
                    lamp_post(b, x, yb + 1, z, height=1)
                else:
                    b.set(x, yb + k + 1, z, SLAB)


def palace(b):
    for i, (radius, yb, h) in enumerate(TIERS):
        inner = TIERS[i + 1][0] if i + 1 < len(TIERS) else None
        terrace(b, radius, yb, h, lawn_inner=inner)
    for radius, yb, h in TIERS:
        for ang, facing in CARDINALS:
            terrace_stairs(b, radius, yb, h, ang, facing,
                           4 if ang in (90, 270) else 2)

    # tier 1: turrets between the staircases
    y1 = TIERS[0][1] + TIERS[0][2]
    for k in range(8):
        ang = 22.5 + 45 * k
        cx, cz = polar(ang, 45)
        tower(b, cx, cz, y1, 3, 10, 8, face=ang, doors=[(ang + 180, y1 + 1, 3)])

    # tier 2: pavilions on the diagonals
    y2 = TIERS[1][1] + TIERS[1][2]
    for sx in (-1, 1):
        for sz in (-1, 1):
            pavilion(b, 23 * sx, 23 * sz, 4, y2, 8)

    # tier 3: cross-shaped keep, corner towers and the central donjon
    y3 = TIERS[2][1] + TIERS[2][2]
    hall(b, -20, 20, -7, 7, y3, 16, axis="x")
    hall(b, -7, 7, -20, 20, y3, 16, axis="z")
    for sx in (-1, 1):
        for sz in (-1, 1):
            tower(b, 14 * sx, 14 * sz, y3, 4, 26, 13,
                  face=math.degrees(math.atan2(sz, sx)))
    tower(b, 0, 0, y3, 10, 48, 28, belfry=True)


def tall_towers(b):
    r = TALL_TOWER_R
    for ang in TALL_TOWER_ANGLES:
        cx, cz = polar(ang, MID_TOWER_DIST)
        doors = [(ang + 180, 1, 4), (ang, WALL_H + 1, 3)]
        for by in (36, 66):
            doors += [(ang + d, by + 1, 3) for d in (0, 90, 180, 270)]
            balcony(b, cx, cz, by, r)
        tower(b, cx, cz, 1, r, 96, 28, face=ang, doors=doors,
              floor_ys=(WALL_H, 36, 66), belfry=True)
        # land on the wall tower behind; spring high there to keep its
        # courtyard door clear
        bridge(b, ang, MID_TOWER_DIST + r - 1, WALL_TOWER_R - 6,
               end_spring=WALL_H - 5)


def bridge(b, angle_deg, start, end, end_spring=1):
    """Arched bridge along a ray from the centre, deck level with the wall walk."""
    a = math.radians(angle_deg)
    ca, sa = math.cos(a), math.sin(a)
    span = end - start
    xs = [start * ca, end * ca]
    zs = [start * sa, end * sa]
    for x in range(int(min(xs)) - 4, int(max(xs)) + 5):
        for z in range(int(min(zs)) - 4, int(max(zs)) + 5):
            along = round(x * ca + z * sa, 6)
            perp = round(abs(-x * sa + z * ca), 6)
            if not (start <= along <= end) or perp > 2.2:
                continue
            t = (along - start) / span
            u = 2 * t - 1
            spring = 1 + (end_spring - 1) * t
            underside = spring + (WALL_H - 3 - spring) * math.sqrt(max(0.0, 1 - u * u))
            for y in range(math.ceil(underside), WALL_H):
                b.set(x, y, z, BRICK if perp > 1.2 else WHITE)
            b.set(x, math.ceil(underside), z, TRIM)
            b.set(x, WALL_H, z, WHITE)
            for y in range(WALL_H + 1, WALL_H + 4):
                b.clear(x, y, z)
            if perp > 1.2:
                b.set(x, WALL_H + 1, z, SLAB)


def build_castle():
    b = Builder()
    foundation(b)
    curtain_wall(b)
    wall_towers(b)
    gate(b)
    grounds(b)
    entrance(b)
    palace(b)
    tall_towers(b)
    return b


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("-o", "--output-dir", default=".",
                        help="folder to write the schematic into")
    parser.add_argument("-n", "--name", default="circle_castle",
                        help="schematic file name (without .schem)")
    parser.add_argument("-v", "--version", default="JE_1_20_1",
                        help="mcschematic.Version member, e.g. JE_1_20_1")
    args = parser.parse_args()

    print("Building castle...")
    b = build_castle()
    print(f"  {len(b.blocks):,} blocks")
    xs = [p[0] for p in b.blocks]
    ys = [p[1] for p in b.blocks]
    zs = [p[2] for p in b.blocks]
    print(f"  size {max(xs) - min(xs) + 1} x {max(ys) - min(ys) + 1} x "
          f"{max(zs) - min(zs) + 1} (X x Y x Z)")

    print("Writing schematic...")
    schem = b.to_schematic()
    schem.save(args.output_dir, args.name, getattr(mcschematic.Version, args.version))
    print(f"Saved {args.output_dir}/{args.name}.schem")


if __name__ == "__main__":
    main()
