#!/usr/bin/env python3
"""Generate a huge circular island castle as a Sponge schematic (.schem).

Layout (outside to inside):
  * a round white foundation island with a ledge, a parapet and a grand
    staircase down to a dock at the front (south, +Z)
  * a 6-block-thick curtain wall with an arched inner gallery, battlements
    and 24 round towers topped with teal spires; two bigger towers flank
    the main gate
  * a grass courtyard with a ring path, radial avenues, fountain gardens,
    lamp posts and a ring of smaller towers
  * a three-tier stepped palace: terraces with turrets and pavilions, a
    cross-shaped keep with gabled teal roofs, four corner towers and a big
    central donjon
  * two very tall towers at the back, joined to the curtain wall by arched
    flying bridges

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

# (radius, y of the surface it stands on, height)
TIERS = [(50, 0, 6), (38, 6, 6), (26, 12, 6)]

TALL_TOWER_ANGLES = (240, 300)  # degrees; 270 is straight north (-Z)
TALL_TOWER_DIST = 60
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


def tower(b, cx, cz, y0, r, h, roof_h, *, doors=(), floor_ys=(),
          flare=True, belfry=False):
    """Round hollow tower with windows, floors and a conical roof.

    doors: (angle_deg, y_bottom, height) openings cut through the shell.
    """
    top = y0 + h
    inner = r - 1.3
    n_win = max(4, round(r * 1.3))
    floors = set(range(y0, top, 7)) | set(floor_ys) | {top}
    for x, z, d in disc_points(cx, cz, r + 1.5):
        ang = math.atan2(z - cz, x - cx)
        wpos = (ang / (2 * math.pi) * n_win) % 1
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
    """Rectangular hollow walls in (along, across) coordinates."""
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
                    k = (a - a0) % 4 if on_c else (c - c0) % 4
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
    n_bays = round(2 * math.pi * R_IN / 8)
    n_merlons = 2 * round(2 * math.pi * R_OUT / 4)
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
        u = (frac * n_bays % 1) * 8      # position inside an 8-block bay
        arch_top = 7 + math.sqrt(max(0.0, 9 - (u - 5) ** 2))
        for y in range(1, WALL_H + 1):
            if y == WALL_H:
                blk = WHITE
            elif t < 1.5:                # arcade facing the courtyard
                if u < 2:
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
            if int(frac * n_merlons) % 2 == 0:
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
        tower(b, cx, cz, 1, r, h, roof_h, doors=doors,
              floor_ys=(WALL_H,), flare=False)


def gate(b):
    for x in range(-GATE_HALF_W, GATE_HALF_W + 1):
        top = 7 + math.sqrt(20.25 - x * x)
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
            half_w = 3.5 if abs(a - math.pi / 2) < 1e-6 else 1.5
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

    # lamp posts inside the ring path
    for k in range(48):
        x, z = polar(3.75 + 7.5 * k, 84)
        b.set(x, 1, z, PILLAR)
        b.set(x, 2, z, PILLAR)
        b.set(x, 3, z, LANTERN)

    # ring of smaller towers (two are replaced by the tall towers)
    for k in range(8):
        ang = 22.5 + 45 * k
        if any(angle_diff(math.radians(ang), math.radians(t)) < math.radians(20)
               for t in TALL_TOWER_ANGLES):
            continue
        cx, cz = polar(ang, 64)
        tower(b, cx, cz, 1, 4, 18, 11, doors=[(ang + 180, 1, 4)])


def terrace(b, radius, yb, h, lawn_inner=None):
    n_pil = round(2 * math.pi * radius / 6)
    top = yb + h
    for x, z, d in disc_points(0, 0, radius):
        frac = (math.atan2(z, x) / (2 * math.pi)) % 1
        for y in range(yb + 1, top + 1):
            if y == top:
                if d > radius - 0.5:
                    blk = TRIM
                elif lawn_inner is not None and lawn_inner + 1.5 < d < radius - 2.5:
                    blk = GRASS
                else:
                    blk = WHITE
            elif d > radius - 1.5:
                blk = PILLAR if (frac * n_pil) % 1 < 0.2 else BRICK
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
                b.set(x, yb + k + 1, z, SLAB)


def palace(b):
    for i, (radius, yb, h) in enumerate(TIERS):
        inner = TIERS[i + 1][0] if i + 1 < len(TIERS) else None
        terrace(b, radius, yb, h, lawn_inner=inner)
    for radius, yb, h in TIERS:
        for ang, facing in CARDINALS:
            terrace_stairs(b, radius, yb, h, ang, facing, 4 if ang == 90 else 2)

    # tier 1: turrets between the staircases
    y1 = TIERS[0][1] + TIERS[0][2]
    for k in range(8):
        ang = 22.5 + 45 * k
        cx, cz = polar(ang, 45)
        tower(b, cx, cz, y1, 3, 10, 8, doors=[(ang + 180, y1 + 1, 3)])

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
            tower(b, 14 * sx, 14 * sz, y3, 4, 26, 13)
    tower(b, 0, 0, y3, 10, 48, 28, belfry=True)


def tall_towers(b):
    r = TALL_TOWER_R
    for ang in TALL_TOWER_ANGLES:
        cx, cz = polar(ang, TALL_TOWER_DIST)
        doors = [(ang + 180, 1, 4), (ang, WALL_H + 1, 3)]
        for by in (36, 66):
            doors += [(ang + d, by + 1, 3) for d in (0, 90, 180, 270)]
            balcony(b, cx, cz, by, r)
        tower(b, cx, cz, 1, r, 96, 28, doors=doors,
              floor_ys=(WALL_H, 36, 66), belfry=True)
        bridge(b, ang, TALL_TOWER_DIST + r - 1, R_IN + 1)


def bridge(b, angle_deg, start, end):
    """Arched bridge along a ray from the centre, deck level with the wall walk."""
    a = math.radians(angle_deg)
    ca, sa = math.cos(a), math.sin(a)
    span = end - start
    xs = [start * ca, end * ca]
    zs = [start * sa, end * sa]
    for x in range(int(min(xs)) - 4, int(max(xs)) + 5):
        for z in range(int(min(zs)) - 4, int(max(zs)) + 5):
            along = x * ca + z * sa
            perp = abs(-x * sa + z * ca)
            if not (start <= along <= end) or perp > 2.2:
                continue
            u = 2 * (along - start) / span - 1
            underside = 1 + (WALL_H - 3) * math.sqrt(max(0.0, 1 - u * u))
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
