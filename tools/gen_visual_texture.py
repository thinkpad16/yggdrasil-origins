"""Paints the visual-parts texture and verifies that the geometry fits inside it.

The idea: every part owns a region, and inside that region each box face
(top, bottom, sides, front, back) is painted with its own lighting, as vanilla textures are.
Without that a part looks like a flat blob.

Run:  python3 tools/gen_visual_texture.py
"""

import os
import struct
import zlib

W, H = 128, 256   # the atlas grew when the breastplate was redrawn at body scale
PX = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]

OUT = 'src/main/resources/assets/yggdrasil_origins/textures/entity/visual_parts.png'
GLOW_OUT = 'src/main/resources/assets/yggdrasil_origins/textures/entity/visual_parts_glow.png'
JAVA = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualParts.java'

# face lighting: the light falls from above and in front
FACE_LIGHT = {'top': 1.22, 'bottom': 0.70, 'right': 0.88, 'front': 1.0, 'left': 0.94, 'back': 0.80}


def rnd(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def mix(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), 255)


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        PX[y][x] = c


# ----------------------------------------------------------------- patterns
# each takes local coordinates inside a face and returns a brightness multiplier

def plain(lx, ly, fw, fh, seed):
    """a plain part with faint constrictions, so it is not one solid blob"""
    if ly % 4 == 3:
        return 0.82
    if ly % 4 == 0:
        return 1.08
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.06


def rings(lx, ly, fw, fh, seed):
    if ly % 3 == 2:
        return 0.78            # groove
    if ly < 2:
        return 1.14            # lighter tip
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.10


def fur(lx, ly, fw, fh, seed):
    r = rnd(lx, ly, seed)
    if r > 0.80:
        return 1.22
    if r < 0.20:
        return 0.76
    return 1.0


def strands(lx, ly, fw, fh, seed):
    if lx % 2 == 0:
        return 0.86
    if ly == fh - 1:
        return 0.72
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.10


def rays(lx, ly, fw, fh, seed):
    if lx % 2 == 1:
        return 1.16            # ray
    if ly >= fh - 1:
        return 0.72            # dark base
    return 0.94


def cracks(lx, ly, fw, fh, seed):
    r = rnd(lx, ly, seed)
    if r > 0.88:
        return 0.68            # crack
    if r < 0.12:
        return 1.18            # chip
    return 1.0 + (r - 0.5) * 0.18


def rivets(lx, ly, fw, fh, seed):
    if lx % 4 == 1 and ly % 4 == 1:
        return 1.30            # rivet
    if lx % 4 == 2 and ly % 4 == 2:
        return 0.80
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.06


def bone(lx, ly, fw, fh, seed):
    """bone: a lengthwise groove, pores and darkening towards the edges"""
    r = rnd(lx, ly, seed)
    edge = 0.86 if (lx == 0 or lx == fw - 1) else 1.0
    if fh > 3 and ly == fh // 2:
        edge *= 0.90           # groove along the bone
    if r > 0.90:
        return 0.80 * edge     # pore
    return (1.0 + (r - 0.5) * 0.10) * edge


def facets(lx, ly, fw, fh, seed):
    """crystal: a light face at the top left, a dark one at the bottom right"""
    t = (lx / max(1, fw - 1) + ly / max(1, fh - 1)) / 2
    return 1.22 - 0.42 * t


def glow(lx, ly, fw, fh, seed):
    """a glowing part: a brighter core and a dimmer rim, otherwise it is a flat blob"""
    if fw < 3 or fh < 3:
        return 1.0
    edge = min(lx, fw - 1 - lx, ly, fh - 1 - ly)
    return 0.80 if edge == 0 else (1.0 if edge == 1 else 1.18)


def halo(lx, ly, fw, fh, seed):
    """halo: a forged rim with notches around it and a lighter side, so it is not a stripe"""
    if ly == 0:
        return 1.26
    if ly >= fh - 1:
        return 0.80
    if lx % 4 == 0:
        return 0.88            # notch
    if lx % 4 == 1:
        return 1.16
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.06


# --- patterns for double-density parts: the drawing is finer than a model pixel

def rings2(lx, ly, fw, fh, seed):
    """horn: deep rings with a lit upper edge"""
    phase = ly % 5
    if phase == 0:
        return 1.20
    if phase == 1:
        return 1.06
    if phase == 3:
        return 0.74
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.08


def feathers2(lx, ly, fw, fh, seed):
    """wing: rows of overlapping feathers - a shaft, a highlight and a dark cut under each row"""
    row = ly // 7
    ry = ly % 7
    col = (lx + (3 if row % 2 else 0)) % 7
    if ry == 6:
        return 0.70            # shadow under the feather cut
    if ry == 5:
        return 0.86
    if col == 0:
        return 0.76            # shaft
    if col == 1:
        return 1.18            # highlight along the shaft
    if col == 6:
        return 0.88            # seam with the neighbouring feather
    return 1.06 - 0.025 * ry + (rnd(lx, ly, seed) - 0.5) * 0.05


def scales2(lx, ly, fw, fh, seed):
    """scales: rows of half-circles, lit on top and shadowed below"""
    row = ly // 4
    sx = (lx + (3 if row % 2 else 0)) % 6
    sy = ly % 4
    if sy == 0:
        return 0.72            # seam between rows
    if sx in (0, 5):
        return 0.82            # scale edge
    if sy == 1 and sx in (2, 3):
        return 1.22            # highlight
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.06


def fur2(lx, ly, fw, fh, seed):
    """fur: strands of varying length, light tips and dark gaps"""
    strand = (lx * 7 + seed * 13) % 5
    length = 2 + strand
    phase = ly % 6
    if phase < length:
        return 1.20 - 0.05 * phase
    r = rnd(lx, ly, seed)
    return 0.70 if r < 0.5 else 0.86


def strands2(lx, ly, fw, fh, seed):
    """beard: strands with the knots of braids"""
    if lx % 4 == 0:
        return 0.78
    if lx % 4 == 1:
        return 1.12
    if ly % 7 == 6:
        return 0.84            # constriction
    return 1.0 + (rnd(lx, ly, seed) - 0.5) * 0.08


def facets2(lx, ly, fw, fh, seed):
    """crystal: crisp faces with a ridge down the middle"""
    t = (lx / max(1, fw - 1) + ly / max(1, fh - 1)) / 2
    edge = abs(lx - fw / 2) < 1 or abs(ly - fh / 2) < 1
    base = 1.26 - 0.46 * t
    return base * (1.12 if edge else 1.0)


def rivets2(lx, ly, fw, fh, seed):
    """metal: brushed bands and rivets with a highlight"""
    if lx % 8 == 2 and ly % 8 == 2:
        return 1.34
    if lx % 8 == 3 and ly % 8 == 3:
        return 0.74
    streak = 1.06 if (ly % 4) < 2 else 0.96
    return streak + (rnd(lx, ly, seed) - 0.5) * 0.05


def rays2(lx, ly, fw, fh, seed):
    """fin: rays with webbing between them"""
    if lx % 4 in (0, 1):
        return 1.18 - 0.03 * (ly % 4)
    if ly >= fh - 2:
        return 0.72
    return 0.90 + (rnd(lx, ly, seed) - 0.5) * 0.06


# ----------------------------------------------------------------- layout
# name: (width, height, depth of the largest box, colour, pattern)
# scale=2 means the model box is built twice as large and shrunk when drawn,
# so twice as many texels land on the same visible size
PARTS = {
    'horns':      (4, 6, 4, (186, 147, 113), rings2),
    'tusks':      (1, 2, 2, (240, 234, 216), bone),
    'antennae':   (1, 5, 1, (109, 109, 132), plain),
    'fins':       (2, 8, 6, (100, 225, 244), rays2),
    'gills':      (1, 3, 3, (126, 213, 238), rays),
    'frill':      (3, 4, 1, (141, 198, 121), rays),
    'embers':     (1, 1, 1, (255, 255, 255), glow),
    'bone_spur':  (2, 4, 2, (238, 232, 214), bone),
    'ribs':       (8, 7, 1, (236, 230, 212), bone),
    'beard':      (14, 8, 2, (197, 138, 91), strands2),
    'wing_arm':   (7, 4, 3, (220, 225, 239), feathers2),
    'wing':       (9, 20, 2, (236, 240, 249), feathers2),
    'shards':     (4, 6, 4, (194, 200, 215), facets2),
    'plates':     (2, 8, 10, (186, 193, 205), rivets2),
    'scarf':      (8, 2, 5, (255, 114, 121), plain),
    'scarf_tail': (3, 7, 1, (255, 111, 118), plain),
    'tendrils':   (2, 7, 2, (111, 106, 143), plain),
    'carapace':   (16, 16, 2, (152, 177, 139), scales2),
    'tail':       (6, 6, 12, (194, 138, 95), fur2),
    'dragon_tail':(6, 6, 12, (142, 185, 121), scales2),
    'dragon_spike': (1, 2, 9, (150, 196, 128), scales2),
    'dragon_tip': (4, 4, 10, (142, 185, 121), scales2),
    'wisps':      (2, 2, 2, (255, 255, 255), glow),
    'streamer':   (2, 14, 2, (226, 240, 255), rays2),
    'vambrace':   (3, 10, 11, (196, 203, 216), rivets2),
    'arm_scales': (2, 12, 8, (143, 185, 121), scales2),
    'halo':       (8, 2, 8, (255, 214, 126), halo),
    'crescent':   (3, 2, 1, (255, 255, 255), glow),
    # appended on purpose: the packer walks this dict in order, so anything added at the end
    # cannot shift a region that already exists
    # two more tails, so no two origins wag the same shape
    'demon_tail': (3, 3, 11, (142, 72, 72), plain),
    'demon_spade':(4, 1, 5, (158, 80, 80), plain),
    'serpent_tail':(5, 5, 12, (134, 192, 106), scales2),
    # a forest head and five path-only details, each with its own motion
    'antlers':    (5, 7, 2, (156, 126, 84), rings2),
    'roots':      (3, 11, 3, (118, 92, 58), strands2),
    'motes':      (2, 2, 2, (255, 255, 255), glow),
    'flame_crown':(2, 3, 2, (255, 255, 255), glow),
    'frost_shard':(2, 4, 2, (198, 238, 255), facets2),
    # four more elements, handed out to the origins that were still bare
    'mane':       (11, 7, 7, (120, 96, 72), strands2),
    'torc':       (9, 2, 8, (214, 184, 96), rings2),
    'claws':      (2, 6, 4, (236, 232, 220), bone),
    'crest':      (2, 5, 9, (150, 196, 128), scales2),
    # the picker's own four: paint, piercing, pauldron, braid
    'markings':   (14, 3, 2, (214, 72, 64), plain),
    'earring':    (2, 4, 1, (226, 198, 112), rings2),
    'pauldron':   (3, 3, 6, (152, 155, 166), rivets2),
    'braid':      (3, 7, 3, (126, 94, 62), strands2),
    # the Cossack sorcerer's forelock
    'forelock':   (3, 3, 3, (122, 96, 62), strands2),
    # one more mark apiece for the origins that wore only a single thing
    'stone_fist': (3, 8, 6, (150, 148, 142), cracks),
    'iron_spine': (3, 4, 2, (164, 168, 178), rivets2),
    'diadem':     (9, 2, 3, (226, 232, 240), rings2),
    'vane':       (3, 8, 2, (198, 224, 240), rays2),
    'sash':       (9, 3, 4, (176, 62, 66), plain),
    'gorget':     (7, 3, 2, (214, 170, 86), rivets2),
    'headband':   (10, 4, 9, (198, 190, 170), plain),
    # two marks for the paths a human can take
    'armband':    (12, 3, 12, (176, 84, 72), plain),
    'pack':       (7, 6, 3, (138, 110, 78), strands2),
    # amber grown through the body rather than strapped onto it
    'amber_spike':(3, 6, 3, (226, 150, 46), facets2),
    # a wreath: the one thing that makes a berehynia read as a berehynia
    'wreath':     (10, 2, 10, (132, 176, 96), strands2),
    'bloom':      (2, 2, 2, (238, 210, 120), plain),
}


def footprint(w, h, d):
    return 2 * (d + w), d + h


def pack():
    """a simple row packer with a one-pixel gap"""
    regions, x, y, row_h = {}, 0, 0, 0
    for name, (w, h, d, _, _) in PARTS.items():
        fw, fh = footprint(w, h, d)
        if x + fw > W:
            x, y, row_h = 0, y + row_h + 1, 0
        regions[name] = (x, y, fw, fh, w, h, d)
        x += fw + 1
        row_h = max(row_h, fh)
    if y + row_h > H:
        raise SystemExit('the texture cannot fit every part')
    return regions


def edge(lx, ly, fw, fh):
    """A bevel along the face edge: darker at the bottom right, lighter at the top left.
    Without it every box in game melts into one uniform blob."""
    if fw < 4 or fh < 4:
        return 1.0
    if lx == fw - 1 or ly == fh - 1:
        return 0.86
    if lx == 0 or ly == 0:
        return 1.10
    return 1.0


def paint_face(x0, y0, fw, fh, base, light, pattern, seed):
    for ly in range(fh):
        for lx in range(fw):
            value = light * pattern(lx, ly, fw, fh, seed) * edge(lx, ly, fw, fh)
            put(x0 + lx, y0 + ly, mix(base, value))


def paint(regions):
    for seed, (name, (u, v, fw, fh, w, h, d)) in enumerate(regions.items()):
        base, pattern = PARTS[name][3], PARTS[name][4]
        iw, ih, idd = int(round(w)), int(round(h)), int(round(d))
        # Fill the whole region first: smaller boxes sharing the region unwrap differently,
        # and without this some of their faces would land on transparent pixels, turning
        # invisible and letting you see the inside of the part.
        # The fill uses a FLATTENED pattern: the corner of a region is not covered by any face
        # of the primary box, so a small box (a tail spike, a braid knot) lands there whole.
        # With the raw pattern it could land on a dark seam row and read as a black hole.
        def flat(lx, ly, bw, bh, sd):
            return min(1.08, max(0.92, pattern(lx, ly, bw, bh, sd)))

        for ly in range(fh):
            for lx in range(fw):
                put(u + lx, v + ly, mix(base, FACE_LIGHT['front'] * flat(lx, ly, fw, fh, seed)))
        # box unwrap: top, bottom, right side, front, left side, back
        paint_face(u + idd, v, iw, idd, base, FACE_LIGHT['top'], pattern, seed)
        paint_face(u + idd + iw, v, iw, idd, base, FACE_LIGHT['bottom'], pattern, seed + 1)
        paint_face(u, v + idd, idd, ih, base, FACE_LIGHT['right'], pattern, seed + 2)
        paint_face(u + idd, v + idd, iw, ih, base, FACE_LIGHT['front'], pattern, seed + 3)
        paint_face(u + idd + iw, v + idd, idd, ih, base, FACE_LIGHT['left'], pattern, seed + 4)
        paint_face(u + idd + iw + iw, v + idd, iw, ih, base, FACE_LIGHT['back'], pattern, seed + 5)


def png(path, buffer=None):
    rows = buffer if buffer is not None else PX
    raw = b''
    for y in range(H):
        raw += b'\x00' + b''.join(bytes(rows[y][x]) for x in range(W))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n'
        + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0))
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# ----------------------------------------------------------------- glow masks
# Only a few elements carry light, and only on part of themselves: a stone set in a
# gorget, the tips of a crest. Painting that into a second sheet is what lets one pixel
# glow while the rest of the part stays ordinary - a whole part tinted bright reads as
# plastic, which is the trap the first pass fell into.

def gem(lx, ly, fw, fh, seed):
    return 1.0 if abs(lx - fw // 2) <= 1 and abs(ly - fh // 2) <= 1 else 0.0


def tips(lx, ly, fw, fh, seed):
    return 0.9 if ly == 0 or ly == 1 and lx % 3 == 0 else 0.0


def cores(lx, ly, fw, fh, seed):
    return 1.0 if lx == fw // 2 or lx == fw // 2 - 1 and ly % 3 else 0.0


def sparks(lx, ly, fw, fh, seed):
    return 1.0 if rnd(lx, ly, seed + 97) > 0.94 else 0.0


def facet_light(lx, ly, fw, fh, seed):
    return 0.85 if (lx + ly) % 5 == 0 else 0.0


GLOW = {
    'gorget': ((255, 206, 112), gem),
    'diadem': ((226, 240, 255), gem),
    'torc': ((255, 226, 150), gem),
    'iron_spine': ((198, 226, 255), tips),
    'crest': ((176, 255, 190), tips),
    'shards': ((190, 236, 255), cores),
    'frost_shard': ((214, 246, 255), facet_light),
}


def paint_glow(regions):
    """A second sheet, transparent but for the pixels that carry light."""
    sheet = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]
    for seed, (name, (u, v, fw, fh, w, h, d)) in enumerate(regions.items()):
        if name not in GLOW:
            continue
        colour, mask = GLOW[name]
        for ly in range(fh):
            for lx in range(fw):
                value = mask(lx, ly, fw, fh, seed)
                if value <= 0.0:
                    continue
                shade = mix(colour, value)
                sheet[v + ly][u + lx] = (shade[0], shade[1], shade[2], 255)
    return sheet


def verify(regions):
    """every box from VisualParts must fit inside its own region"""
    import re
    src = open(JAVA, encoding='utf-8').read()
    skin_end = src.index('public static LayerDefinition propMesh')
    allowed = {(u, v): (fw, fh) for (u, v, fw, fh, *_) in regions.values()}
    cur, bad, ok = None, [], 0
    for t in re.finditer(r'texOffs\((\d+),\s*(\d+)\)|addBox\(([^)]*)\)', src):
        if t.group(1) is not None:
            cur = (int(t.group(1)), int(t.group(2)))
            continue
        if cur is None or t.start() < skin_end:
            continue
        nums = []
        for part in t.group(3).split(','):
            try:
                nums.append(float(part.strip().replace('F', '')))
            except ValueError:
                pass
        if len(nums) < 6:
            continue
        w, h, d = nums[3], nums[4], nums[5]
        need_w, need_h = 2 * (d + w), d + h
        if cur not in allowed:
            bad.append(f'texOffs{cur}: no such region in the texture')
            continue
        rw, rh = allowed[cur]
        if need_w > rw + 0.01 or need_h > rh + 0.01:
            bad.append(f'texOffs{cur}: box {w:g}x{h:g}x{d:g} needs {need_w:.0f}x{need_h:.0f}, '
                       f'but the region is only {rw}x{rh}')
        else:
            ok += 1
    # a region nothing draws from is paint nobody will ever see, and it pushes every
    # region after it along - which is how texOffs quietly go stale
    drawn = {(int(u), int(v)) for u, v in re.findall(r'texOffs\((\d+),\s*(\d+)\)', src)}
    for name, (u, v, *_rest) in regions.items():
        if (u, v) not in drawn:
            bad.append(f'the region {name} at texOffs({u}, {v}) is painted but never drawn')
    print(f'boxes checked: {ok + len(bad)}')
    for b in bad:
        print('  PROBLEM:', b)
    if not bad:
        print('every box fits inside its region, and every region is drawn from')
    return not bad


if __name__ == '__main__':
    import sys
    regions = pack()
    paint(regions)
    png(OUT)
    png(GLOW_OUT, paint_glow(regions))
    print(f'texture {W}x{H}: {os.path.getsize(OUT)} bytes, {len(regions)} regions')
    for name, (u, v, fw, fh, *_) in regions.items():
        print(f'  {name:12} texOffs({u}, {v})  {fw}×{fh}')
    sys.exit(0 if verify(regions) else 1)
