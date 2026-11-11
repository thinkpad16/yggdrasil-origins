"""Checks that every visual element sits on the body the way it should.

A part may not vanish inside the skin, and it may not drift off the body when a player
resizes it. Both ends of the size range a player can reach are checked, because the size
is applied around the joint and the far end of a part travels when it is scaled.
"""
import re

LAYER = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualPartsLayer.java'
PARTS = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualParts.java'

def grow(args, tail):
    """How far a box is inflated on every side.

    A deformation is part of the shape: a spike written with -0.3 stands a third of a pixel
    less proud than its numbers say, which is the difference between clearing the second
    skin layer and disappearing inside it.
    """
    if tail is not None:
        return float(tail)
    rest = args.split(',')[6:]
    for piece in rest:
        piece = piece.strip()
        if piece.startswith('new CubeDeformation('):
            return float(piece[len('new CubeDeformation('):].rstrip(')Ff'))
    return 0.0


src = open(PARTS, encoding='utf-8').read()
consts = dict(re.findall(r'String (\w+) = "([\w_]+)";', src))

# which mesh a part is defined in decides its density: the detail mesh is drawn at half scale
bounds = {}
for name in ('skinMesh', 'propMesh', 'bodyMesh', 'detailMesh'):
    bounds[name] = src.index('LayerDefinition ' + name)
order = sorted(bounds.items(), key=lambda kv: kv[1])

def mesh_of(index):
    found = order[0][0]
    for name, at in order:
        if at <= index:
            found = name
    return found

geometry = {}
for m in re.finditer(
        r'for \(String name : new String\[\] \{([^}]*)\}\) \{(.*?)\n        \}', src, re.S):
    names = [consts[n.strip()] for n in m.group(1).split(',') if n.strip() in consts]
    body = m.group(2)
    pose_match = re.search(r'PartPose\.(?:(\w+)\(([^)]*)\)|ZERO)', body)
    raw = (pose_match.group(2) or '').strip() if pose_match else ''
    pose = [0.0, 0.0, 0.0] if not raw else [float(v.strip().rstrip('Ff')) for v in raw.split(',')]
    boxes = []
    for b in re.finditer(r'addBox\(([^)]*)\)(?:,\s*new CubeDeformation\(([-\d.]+)F\))?', body):
        nums = [float(q.strip().rstrip('Ff')) for q in b.group(1).split(',')[:6]]
        if len(nums) == 6:
            boxes.append(nums + [grow(b.group(1), b.group(2))])
    for name in names:
        geometry.setdefault(name, []).append(
            {'pose': pose[:3], 'boxes': boxes, 'mesh': mesh_of(m.start())})

for m in re.finditer(
        r'addOrReplaceChild\((\w+),(.*?)PartPose\.(?:(\w+)\(([^)]*)\)|ZERO)\)', src, re.S):
    if m.group(1) == 'name':
        continue
    key = consts.get(m.group(1), m.group(1))
    raw = (m.group(4) or '').strip()
    pose = [0.0, 0.0, 0.0] if not raw else [
        float(v.strip().rstrip('Ff')) for v in raw.split(',')]
    boxes = []
    for b in re.finditer(r'addBox\(([^)]*)\)(?:,\s*new CubeDeformation\(([-\d.]+)F\))?', m.group(2)):
        nums = [float(p.strip().rstrip('Ff')) for p in b.group(1).split(',')[:6]]
        if len(nums) == 6:
            boxes.append(nums + [grow(b.group(1), b.group(2))])
    geometry.setdefault(key, []).append({
        'pose': pose[:3], 'boxes': boxes, 'mesh': mesh_of(m.start())})

layer = open(LAYER, encoding='utf-8').read()

# VisualPart -> the model parts its case draws, and whether it draws them at double density
cases = {}
for m in re.finditer(r'\n            case ([A-Z_, ]+) ->(.*?)(?=\n            case |\n            default)',
                     layer, re.S):
    body = m.group(2)
    names = [consts[n] for n in re.findall(r'VisualParts\.(\w+)', body) if n in consts]
    dense = 'detail(' in body
    for part in (p.strip() for p in m.group(1).split(',')):
        if not part:
            continue
        got = cases.setdefault(part, {'names': [], 'dense': dense})
        got['names'].extend(names)
        got['dense'] = got['dense'] or dense

# the arm pieces are drawn by the layer's arm branch rather than by a case of their own
cases['VAMBRACE'] = {'names': ['vambrace', 'vambrace_left'], 'dense': True}
cases['ARM_SCALES'] = {'names': ['arm_scales', 'arm_scales_left'], 'dense': True}
cases['CLAWS'] = {'names': ['claw_right', 'claw_left'], 'dense': True}
cases['STONE_FISTS'] = {'names': ['stone_fist_right', 'stone_fist_left'], 'dense': True}
cases['ARMBAND'] = {'names': ['armband_right', 'armband_left'], 'dense': True}
cases['AMBER_SPURS'] = {'names': ['spikes_arm_right', 'spikes_arm_left'], 'dense': True}

anchors = {}
for m in re.finditer(r'\n    ([A-Z_]+)\(Anchor\.(\w+),', open(
        'src/main/java/org/origins_yggdrasil/yggdrasil_origins/visual/VisualPart.java',
        encoding='utf-8').read()):
    anchors[m.group(1)] = m.group(2)

# the body a part may rest on, in its anchor's own pixels. A shoulder piece sits on the arm,
# so the body anchor counts the arms too - otherwise every pauldron reads as floating.
SKIN = {
    'HEAD': [(-4.0, 4.0, -8.0, 0.0, -4.0, 4.0)],
    'BODY': [(-4.0, 4.0, 0.0, 12.0, -2.0, 2.0),
             (-8.0, -4.0, 0.0, 12.0, -2.0, 2.0),
             (4.0, 8.0, 0.0, 12.0, -2.0, 2.0)],
    'ARM': [(-3.0, 1.0, -2.0, 10.0, -2.0, 2.0)],
}
# parts that are meant to float free: a halo that touched the head would be a hat
FREE = {'HALO', 'CRESCENT', 'EMBERS', 'WISPS', 'MOTES', 'FLAME_CROWN', 'FROST_SHARDS',
        'STREAMERS', 'WINGS'}

part_src = open('src/main/java/org/origins_yggdrasil/yggdrasil_origins/visual/VisualPart.java',
                encoding='utf-8').read()
style_src = open('src/main/java/org/origins_yggdrasil/yggdrasil_origins/visual/VisualStyle.java',
                 encoding='utf-8').read()
MAX_SHIFT = float(re.search(r'MAX_SHIFT = ([\d.]+)F', style_src).group(1))
HUG_SHIFT = float(re.search(r'TIGHT\.contains\(this\) \? ([\d.]+)F : VisualStyle\.MAX_SHIFT',
                            part_src).group(1))
MIN_SCALE = float(re.search(r'MIN_SCALE = ([\d.]+)F', style_src).group(1))
MAX_SCALE = float(re.search(r'MAX_SCALE = ([\d.]+)F', style_src).group(1))

# elements with no room to spare: their size floor and their nudge bound are both smaller
hug_block = re.search(r'HUGGING =\s*java\.util\.EnumSet\.of\(([^;]*?)\);', part_src, re.S)
HUGGING = ({n.strip() for n in hug_block.group(1).replace('\n', ' ').split(',')}
           if hug_block else set())
TIGHT = HUGGING | set(re.findall(r'set\.add\((\w+)\);', part_src))
flush_block = re.search(r'FLUSH =\s*java\.util\.EnumSet\.of\(([^;]*?)\);', part_src, re.S)
FLUSH = ({n.strip() for n in flush_block.group(1).replace('\n', ' ').split(',')}
         if flush_block else set())
FLUSH_SHIFT = float(re.search(r'FLUSH\.contains\(this\)\) return ([\d.]+)F', part_src).group(1))
band_block = re.search(r'ENCIRCLING =\s*java\.util\.EnumSet\.of\(([^;]*?)\);', part_src, re.S)
# a band grows only in height, so its own axes are measured that way too
ENCIRCLING = ({n.strip() for n in band_block.group(1).replace('\n', ' ').split(',')}
              if band_block else set())

# The skin has a second layer over it - hat, jacket, sleeves - and anything sitting inside
# that layer fights it for the same pixels and flickers. These are how far clear a part has
# to reach on each anchor.
OVERLAY = {'HEAD': 0.55, 'BODY': 0.30, 'ARM': 0.30}


def _tables(name):
    """Reads one of the layer's variant tables as part -> list of rows of floats."""
    out = {}
    block = layer.split(name + ' =', 1)[1].split(');', 1)[0]
    for m in re.finditer(r'VisualPart\.(\w+), new float\[\]\[\] \{(.*?)\}\)', block, re.S):
        rows = []
        for row in re.finditer(r'\{([^{}]*)\}', m.group(2)):
            rows.append([float(v.strip().rstrip('Ff')) for v in row.group(1).split(',')])
        out[m.group(1)] = rows
    return out


# A variant may change a part's proportions, so each one is measured too. Rotations are left
# out on purpose: turning a part about its own joint cannot move it away from the body.
SHAPES = _tables('VARIANT_SHAPES')
STRETCH = _tables('VARIANT_STRETCH')


def variant_scales(part):
    """Per-axis multipliers each variant of this part applies, variant 0 first."""
    shapes = SHAPES.get(part, [])
    stretch = STRETCH.get(part, [])
    count = max(len(shapes), len(stretch), 1)
    out = []
    for i in range(count):
        flat = shapes[i][0] if i < len(shapes) and shapes[i] else 1.0
        axes = stretch[i] if i < len(stretch) and len(stretch[i]) == 3 else [1.0, 1.0, 1.0]
        out.append([flat * axes[0], flat * axes[1], flat * axes[2]])
    return out


def box_corners(part_name, dense):
    """Every box of a model part, in anchor pixels.

    A left-hand piece is written in its own arm's space, which is the right arm's mirrored.
    Measuring it against the one arm box below would read as a piece hanging in the air, so
    it is mirrored back first.
    """
    mirror = part_name.endswith('_left')
    out = []
    for piece in geometry.get(part_name, []):
        ox, oy, oz = piece['pose']
        for x, y, z, w, h, d, blow in piece['boxes']:
            lo = (ox + x - blow, oy + y - blow, oz + z - blow)
            hi = (ox + x + w + blow, oy + y + h + blow, oz + z + d + blow)
            joint = (ox, oy, oz)
            if dense:
                lo = tuple(v / 2.0 for v in lo)
                hi = tuple(v / 2.0 for v in hi)
                joint = tuple(v / 2.0 for v in joint)
            if mirror:
                lo, hi = (-hi[0], lo[1], lo[2]), (-lo[0], hi[1], hi[2])
                joint = (-joint[0], joint[1], joint[2])
            out.append((lo, hi, joint))
    return out


def outside(lo, hi, skins):
    """How far this box reaches past every piece of skin it overlaps."""
    worst = 99.0
    for x0, x1, y0, y1, z0, z1 in skins:
        worst = min(worst, max(x0 - lo[0], hi[0] - x1,
                               y0 - lo[1], hi[1] - y1,
                               z0 - lo[2], hi[2] - z1))
    return worst


def gap(lo, hi, skins):
    """How far this box floats away from the nearest skin: zero while it still touches."""
    best = 99.0
    for x0, x1, y0, y1, z0, z1 in skins:
        dx = max(x0 - hi[0], lo[0] - x1, 0.0)
        dy = max(y0 - hi[1], lo[1] - y1, 0.0)
        dz = max(z0 - hi[2], lo[2] - z1, 0.0)
        best = min(best, max(dx, dy, dz))
    return best


def inside(lo, hi, skins):
    """True when this box is completely swallowed by one piece of the body."""
    for x0, x1, y0, y1, z0, z1 in skins:
        if (lo[0] >= x0 - 0.05 and hi[0] <= x1 + 0.05
                and lo[1] >= y0 - 0.05 and hi[1] <= y1 + 0.05
                and lo[2] >= z0 - 0.05 and hi[2] <= z1 + 0.05):
            return True
    return False


problems = []
buried = []
checked = 0
for part, anchor in sorted(anchors.items()):
    drawn = cases.get(part)
    if not drawn or not drawn['names']:
        problems.append(f'{part}: no geometry found for it in the layer')
        continue
    skin = SKIN[anchor]
    boxes = []
    for name in dict.fromkeys(drawn['names']):
        boxes.extend(box_corners(name, drawn['dense']))
    if not boxes:
        problems.append(f'{part}: its model parts have no boxes')
        continue
    checked += 1
    # a player may also nudge an element; the worst corner of that range still has to sit
    # on the body, otherwise the controls let anyone detach their own marks
    shift = FLUSH_SHIFT if part in FLUSH else (HUG_SHIFT if part in TIGHT else MAX_SHIFT)
    if part not in FREE:
        for axis in range(3):
            for way in (-shift, shift):
                moved = 99.0
                reach = -99.0
                for lo, hi, joint in boxes:
                    slo = list(lo)
                    shi = list(hi)
                    slo[axis] += way
                    shi[axis] += way
                    moved = min(moved, gap(tuple(slo), tuple(shi), skin))
                    reach = max(reach, outside(tuple(slo), tuple(shi), skin))
                if moved > 1.6:
                    problems.append(f'{part}: nudged {way:+.2f} on {"xyz"[axis]} it floats '
                                    f'{moved:.2f}px off the body')
                if reach < 0.2:
                    problems.append(f'{part}: nudged {way:+.2f} on {"xyz"[axis]} it sinks '
                                    f'into the skin')

    smallest = 0.85 if part in HUGGING else MIN_SCALE
    trials = [(scale, shape, index)
             for scale in (smallest, 1.0, MAX_SCALE)
             for index, shape in enumerate(variant_scales(part))]
    for scale, shape, variant in trials:
        best_out = -99.0
        best_gap = 99.0
        axes = ((1.0, scale, 1.0) if part in ENCIRCLING else (scale, scale, scale))
        for lo, hi, joint in boxes:
            # the same transform the layer uses: scale around the model part's own joint
            slo = tuple(j + (v - j) * a * axis for v, j, a, axis in zip(lo, joint, axes, shape))
            shi = tuple(j + (v - j) * a * axis for v, j, a, axis in zip(hi, joint, axes, shape))
            lo2 = tuple(min(a, b) for a, b in zip(slo, shi))
            hi2 = tuple(max(a, b) for a, b in zip(slo, shi))
            best_out = max(best_out, outside(lo2, hi2, skin))
            best_gap = min(best_gap, gap(lo2, hi2, skin))
        # free-floating elements are placed by their own orbit each frame, so where their
        # boxes sit in the model file says nothing about whether they are visible
        if scale == 1.0 and variant == 0 and part not in FREE:
            # a single piece hidden inside the body is usually a mistake: it is paid for in
            # triangles and never seen. A root inside the hip is the exception, so these are
            # listed rather than failed.
            sunk = sum(1 for lo, hi, j in boxes if inside(lo, hi, skin))
            if sunk:
                buried.append(f'{part}: {sunk} of {len(boxes)} pieces sit inside the body')
        where = f'at size {scale:.0%}, shape {variant + 1}'
        clear = OVERLAY[anchor]
        # an element that is worn has to reach into the skin, not hover a fraction above it:
        # a hair's gap is what reads in game as a plate floating off a shoulder
        if scale == 1.0 and variant == 0 and part not in FREE and best_gap > 0.0:
            problems.append(
                f'{part}: {where} it does not touch the body '
                f'(nearest piece floats {best_gap:.2f}px off it)')
        if scale == 1.0 and variant == 0 and part not in FREE and best_out < clear:
            problems.append(
                f'{part}: reaches only {best_out:+.2f}px past the skin - the second skin '
                f'layer needs {clear:.2f}px, so it will flicker')
        if best_out < 0.25 and part not in FREE:
            problems.append(
                f'{part}: {where} it sinks into the skin '
                f'(reaches only {best_out:+.2f}px past it)')
        if part not in FREE and best_gap > 1.6:
            problems.append(f'{part}: {where} it floats {best_gap:.2f}px off the body')

# The two variant tables hold different things: poses are {scale, pitch, yaw, roll} and
# stretches are {x, y, z}. A row in the wrong table scales an element to nothing.
for table_name, width in (('VARIANT_SHAPES', 4), ('VARIANT_STRETCH', 3)):
    block = layer.split(table_name + ' =', 1)[1].split(');', 1)[0]
    for m in re.finditer(r'VisualPart\.(\w+), new float\[\]\[\] \{(.*?)\}\)', block, re.S):
        for row in re.finditer(r'\{([^{}]*)\}', m.group(2)):
            if len(row.group(1).split(',')) != width:
                problems.append(f'{m.group(1)}: a row in {table_name} has '
                                f'{len(row.group(1).split(","))} numbers, not {width}')

# ----------------------------------------------------------- seen, and seen alone

HIDDEN_BY = dict(re.findall(r'^    ([A-Z_]+)\(Anchor\.\w+, (?:true|false), Hide\.(\w+)',
                            part_src, re.M))
# armour is a shell a whole pixel off the skin. An element that armour does not take off
# has to stand clear of that shell, or putting a helmet on swallows it.
ARMOUR = 1.0

for part, anchor in sorted(anchors.items()):
    drawn = cases.get(part)
    if not drawn or part in FREE:
        continue
    skin = SKIN[anchor]
    clear = OVERLAY[anchor]
    pieces = []
    for name in dict.fromkeys(drawn['names']):
        for lo, hi, _ in box_corners(name, drawn['dense']):
            pieces.append((name, outside(lo, hi, skin)))
    if not pieces:
        continue
    for name, reach in pieces:
        if reach < clear:
            problems.append(f'{part}: the piece {name} stands only {reach:+.2f}px proud - '
                            f'the second skin layer needs {clear:.2f}px, so it is never seen')
    if HIDDEN_BY.get(part) == 'NONE' and max(r for _, r in pieces) < ARMOUR:
        problems.append(f'{part}: armour does not take it off, yet it stands only '
                        f'{max(r for _, r in pieces):+.2f}px proud - armour would swallow it')


# ------------------------------------------------- two marks of one origin in one place

def _origin_parts():
    import glob
    import json
    out = {}
    for path in glob.glob('src/main/resources/data/yggdrasil_origins/origins/*.json'):
        data = json.load(open(path, encoding='utf-8'))
        looks = data.get('visual') or {}
        worn = [p for p in looks.get('parts', []) if p not in looks.get('optional', [])]
        if worn:
            out[path.split('/')[-1][:-5]] = worn
    return out


def _ghost_parts():
    """Elements named in the data that this build no longer has."""
    import glob
    import json
    known = {name.lower() for name in anchors}
    for path in sorted(glob.glob('src/main/resources/data/yggdrasil_origins/origins/*.json')):
        looks = (json.load(open(path, encoding='utf-8')).get('visual') or {})
        origin = path.split('/')[-1][:-5]
        named = list(looks.get('parts', [])) + list(looks.get('optional', []))
        for look in looks.get('looks', []):
            named += list(look.get('shapes', {}))
        for name in sorted(set(named)):
            if name not in known:
                problems.append(f'{origin}: names the element "{name}", which no longer exists')


_ghost_parts()


def _in_body_space(part):
    """Every box of an element in the body's own pixels, arms moved onto their shoulder."""
    drawn = cases.get(part)
    if not drawn:
        return []
    out = []
    for name in dict.fromkeys(drawn['names']):
        for lo, hi, _ in box_corners(name, drawn['dense']):
            if anchors[part] == 'ARM':
                # box_corners has already mirrored the left arm onto the right one
                lo = (lo[0] - 5.0, lo[1] + 2.0, lo[2])
                hi = (hi[0] - 5.0, hi[1] + 2.0, hi[2])
            out.append((name, lo, hi))
    return out


def _shared(a, b):
    space = 1.0
    for i in range(3):
        span = min(a[1][i], b[1][i]) - max(a[0][i], b[0][i])
        if span <= 0.0:
            return 0.0
        space *= span
    return space


import itertools
reported = set()
for origin, worn in sorted(_origin_parts().items()):
    for one, two in itertools.combinations(worn, 2):
        first, second = one.upper(), two.upper()
        if first not in anchors or second not in anchors:
            continue
        if first in FREE or second in FREE or (first, second) in reported:
            continue
        worst = 0.0
        for _, la, ha in _in_body_space(first):
            for _, lb, hb in _in_body_space(second):
                worst = max(worst, _shared((la, ha), (lb, hb)))
        if worst > 1.0:
            reported.add((first, second))
            problems.append(f'{origin}: {first} and {second} share {worst:.1f} cubic px - '
                            f'two of its own marks are inside each other')


# A variant may not resize a part by scaling the pose: that moves it away from its joint
# and the part floats off the body. ModelPart's own scale does the same job in place.
stray = []
for name in ('renderHead', 'renderBody'):
    block = layer.split('private void ' + name + '(', 1)[1].split('\n    private ', 1)[0]
    for m in re.finditer(r'pose\.scale\(', block):
        line = block[:m.start()].count('\n')
        stray.append(f'{name}: pose.scale near line {line} - use setScale so the piece '
                     f'grows from its joint')
problems.extend(stray)

print(f'elements checked: {checked}')
if buried:
    print('pieces hidden inside the body (check these are meant to be):')
    for line in buried:
        print('  ' + line)
if problems:
    print('PROBLEMS:')
    for line in problems:
        print('  ' + line)
else:
    print('every element stays on the body and clear of the skin at every size')
