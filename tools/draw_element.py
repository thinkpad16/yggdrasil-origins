"""Draws an element onto a plain player silhouette, front and side, as a PNG.

The dev client is not always available, and a numeric check cannot show whether a mark
reads well. This projects the model boxes - including the rotation each piece is posed
with - over the body, so placement can be judged without starting the game.
"""
import math
import re
import struct
import sys
import zlib

LAYER = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualPartsLayer.java'
PARTS = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualParts.java'

src = open(PARTS, encoding='utf-8').read()
layer = open(LAYER, encoding='utf-8').read()
consts = dict(re.findall(r'String (\w+) = "([\w_]+)";', src))

mesh_at = {name: src.index('LayerDefinition ' + name)
           for name in ('skinMesh', 'propMesh', 'bodyMesh', 'detailMesh')}
order = sorted(mesh_at.items(), key=lambda kv: kv[1])


def mesh_of(index):
    found = order[0][0]
    for name, at in order:
        if at <= index:
            found = name
    return found


# which PartDefinition variable holds which part, so a chain can be followed up to the root
holder = {}
for m in re.finditer(r'PartDefinition (\w+) = (\w+)\.addOrReplaceChild\((\w+),', src):
    holder[m.group(1)] = consts.get(m.group(3), m.group(3))

pieces = {}
for m in re.finditer(
        r'(?:(\w+)\.)?addOrReplaceChild\((\w+),(.*?)PartPose\.(?:(\w+)\(([^)]*)\)|ZERO)\)',
        src, re.S):
    if m.group(2) == 'name':
        continue
    parent = holder.get(m.group(1))
    key = consts.get(m.group(2), m.group(2))
    raw = (m.group(5) or '').strip()
    pose = [0.0] * 6
    if raw:
        values = [float(v.strip().rstrip('Ff')) for v in raw.split(',')]
        pose[:len(values)] = values
    boxes = []
    for b in re.finditer(r'addBox\(([^)]*)\)', m.group(3)):
        nums = [float(q.strip().rstrip('Ff')) for q in b.group(1).split(',')[:6]]
        if len(nums) == 6:
            boxes.append(nums)
    pieces.setdefault(key, []).append({'pose': pose, 'boxes': boxes, 'parent': parent,
                                       'dense': mesh_of(m.start()) == 'detailMesh'})

cases = {}
for m in re.finditer(r'\n            case ([A-Z_, ]+) ->(.*?)(?=\n            case |\n            default)',
                     layer, re.S):
    names = [consts[n] for n in re.findall(r'VisualParts\.(\w+)', m.group(2)) if n in consts]
    for part in (p.strip() for p in m.group(1).split(',')):
        if part:
            cases.setdefault(part, []).extend(names)
cases['VAMBRACE'] = ['vambrace', 'vambrace_left']
cases['ARM_SCALES'] = ['arm_scales', 'arm_scales_left']
cases['CLAWS'] = ['claw_right', 'claw_left']
cases['STONE_FISTS'] = ['stone_fist_right', 'stone_fist_left']

anchors = dict(re.findall(r'\n    ([A-Z_]+)\(Anchor\.(\w+),', open(
    'src/main/java/org/origins_yggdrasil/yggdrasil_origins/visual/VisualPart.java',
    encoding='utf-8').read()))

# the body, in world pixels with the head's own origin at the neck
BODY = [
    ('head', -4, 4, -8, 0, -4, 4),
    ('torso', -4, 4, 0, 12, -2, 2),
    ('arm_r', -8, -4, 0, 12, -2, 2),
    ('arm_l', 4, 8, 0, 12, -2, 2),
    ('leg_r', -4, 0, 12, 24, -2, 2),
    ('leg_l', 0, 4, 12, 24, -2, 2),
]
ANCHOR_SHIFT = {'HEAD': (0, 0), 'BODY': (0, 0), 'ARM': (-6, 0)}


def place(point, pose):
    """Applies one joint's rotation and offset to a point."""
    px, py, pz = point
    ox, oy, oz, rx, ry, rz = pose
    px, py, pz = (px, py * math.cos(rx) - pz * math.sin(rx),
                  py * math.sin(rx) + pz * math.cos(rx))
    px, py, pz = (px * math.cos(ry) + pz * math.sin(ry), py,
                  -px * math.sin(ry) + pz * math.cos(ry))
    px, py, pz = (px * math.cos(rz) - py * math.sin(rz),
                  px * math.sin(rz) + py * math.cos(rz), pz)
    return px + ox, py + oy, pz + oz


def chain(piece):
    """Every joint from this piece up to the root it hangs from, innermost first."""
    out = [piece['pose']]
    parent = piece.get('parent')
    guard = 0
    while parent and guard < 8:
        up = pieces.get(parent, [])
        if not up:
            break
        out.append(up[0]['pose'])
        parent = up[0].get('parent')
        guard += 1
    return out


def corners(box, joints, dense):
    x, y, z, w, h, d = box
    out = []
    for cx in (x, x + w):
        for cy in (y, y + h):
            for cz in (z, z + d):
                point = (cx, cy, cz)
                # up the chain: a tail tip sits wherever its parents put it
                for pose in joints:
                    point = place(point, pose)
                if dense:
                    point = tuple(v / 2.0 for v in point)
                out.append(point)
    return out


SCALE = 9
W, H = 2 * 26 * SCALE, 46 * SCALE


def blank():
    return [[(16, 18, 26)] * W for _ in range(H)]


def put_rect(img, x0, y0, x1, y1, colour):
    for py in range(max(0, int(y0)), min(H, int(y1) + 1)):
        for px in range(max(0, int(x0)), min(W, int(x1) + 1)):
            img[py][px] = colour


def draw(img, lo, hi, colour, view, pane):
    # view 0: from the front (x, y); view 1: from the side (z, y)
    ax, bx = (lo[0], hi[0]) if view == 0 else (lo[2], hi[2])
    ox = pane * 26 * SCALE + 13 * SCALE
    oy = 16 * SCALE
    put_rect(img, ox + ax * SCALE, oy + lo[1] * SCALE,
             ox + bx * SCALE, oy + hi[1] * SCALE, colour)


def fill_shape(img, points, colour, view, pane):
    """Fills the outline of a turned box rather than the box that would contain it."""
    ox = pane * 26 * SCALE + 13 * SCALE
    oy = 16 * SCALE
    flat = [(ox + (p[0] if view == 0 else p[2]) * SCALE, oy + p[1] * SCALE) for p in points]
    hull = convex_hull(flat)
    if len(hull) < 3:
        return
    top = min(int(p[1]) for p in hull)
    bottom = max(int(p[1]) for p in hull)
    for py in range(max(0, top), min(H, bottom + 1)):
        spans = []
        for i in range(len(hull)):
            x0, y0 = hull[i]
            x1, y1 = hull[(i + 1) % len(hull)]
            if (y0 <= py < y1) or (y1 <= py < y0):
                spans.append(x0 + (x1 - x0) * (py - y0) / (y1 - y0))
        if len(spans) >= 2:
            for px in range(max(0, int(min(spans))), min(W, int(max(spans)) + 1)):
                img[py][px] = colour


def convex_hull(points):
    pts = sorted(set(points))
    if len(pts) <= 2:
        return pts

    def half(seq):
        out = []
        for p in seq:
            while len(out) >= 2:
                (ax, ay), (bx, by) = out[-2], out[-1]
                if (bx - ax) * (p[1] - ay) - (by - ay) * (p[0] - ax) > 0:
                    break
                out.pop()
            out.append(p)
        return out[:-1]

    return half(pts) + half(reversed(pts))


def png(path, img):
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in img)
    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))
    open(path, 'wb').write(
        b'\x89PNG\r\n\x1a\n'
        + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 2, 0, 0, 0))
        + chunk(b'IDAT', zlib.compress(raw))
        + chunk(b'IEND', b''))


def render_names(anchor, names, path, dense=None):
    """Draws an explicit set of model parts - one variant of an element on its own."""
    img = blank()
    for view in (0, 1):
        for _, x0, x1, y0, y1, z0, z1 in BODY:
            draw(img, (x0, y0, z0), (x1, y1, z1), (62, 66, 80), view, view)
    for name in names:
        for piece in pieces.get(name, []):
            use = piece['dense'] if dense is None else dense
            for box in piece['boxes']:
                pts = corners(box, chain(piece), use)
                shift = ANCHOR_SHIFT[anchor]
                if anchor == 'ARM':
                    shift = (6, 0) if name.endswith('_left') else (-6, 0)
                moved = [(p[0] + shift[0], p[1], p[2]) for p in pts]
                for view in (0, 1):
                    fill_shape(img, moved, (120, 230, 140), view, view)
    png(path, img)


def render(part, path):
    img = blank()
    for view in (0, 1):
        for _, x0, x1, y0, y1, z0, z1 in BODY:
            draw(img, (x0, y0, z0), (x1, y1, z1), (62, 66, 80), view, view)
    for name in dict.fromkeys(cases.get(part, [])):
        # an arm piece is drawn under its own arm, so each side moves to the arm it belongs to
        shift = ANCHOR_SHIFT[anchors[part]]
        if anchors[part] == 'ARM':
            shift = (6, 0) if name.endswith('_left') else (-6, 0)
        for piece in pieces.get(name, []):
            for box in piece['boxes']:
                pts = corners(box, chain(piece), piece['dense'])
                lo = [min(p[i] for p in pts) for i in range(3)]
                hi = [max(p[i] for p in pts) for i in range(3)]
                moved = [(p[0] + shift[0], p[1], p[2]) for p in pts]
                for view in (0, 1):
                    fill_shape(img, moved, (120, 230, 140), view, view)
    png(path, img)


if __name__ == '__main__':
    out = sys.argv[1]
    for part in sys.argv[2:]:
        if '/' in part:
            anchor, names, label = part.split('/')
            render_names(anchor, names.split(','), f'{out}/{label}.png')
            print('drew', label)
        else:
            render(part, f'{out}/{part.lower()}.png')
            print('drew', part)
