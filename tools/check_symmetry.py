import re

SRC = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualParts.java'
src = open(SRC, encoding='utf-8').read()
consts = dict(re.findall(r'String (\w+) = "([\w_]+)";', src))

# addOrReplaceChild(NAME, ...builder..., PartPose....) blocks
parts = {}
for m in re.finditer(r'addOrReplaceChild\((\w+),(.*?)PartPose\.(\w+)\(([^)]*)\)\)', src, re.S):
    name = consts.get(m.group(1), m.group(1))
    body = m.group(2)
    pose = [float(v.strip().rstrip('Ff')) for v in m.group(4).split(',')]
    boxes = []
    for b in re.finditer(r'addBox\(([^)]*)\)', body):
        nums = [float(p.strip().rstrip('Ff')) for p in b.group(1).split(',')[:6]]
        if len(nums) == 6:
            boxes.append(nums)
    parts[name] = {'pose': pose, 'boxes': boxes, 'mirror': '.mirror()' in body}

def close(a, b, eps=0.001):
    return abs(a - b) <= eps

# pieces that float free rather than sit on the body: they are placed by their own drift
# each frame, so writing them as a mirrored pair would say nothing
FREE_PAIRS = {'wisp_right'}

issues = []
for name in sorted(parts):
    if not name.endswith('_right') or name in FREE_PAIRS:
        continue
    twin = name[:-6] + '_left'
    if twin not in parts:
        continue
    right, left = parts[name], parts[twin]
    rp, lp = right['pose'], left['pose']
    if not close(rp[0], -lp[0]):
        issues.append(f'{name}: pivot x {rp[0]:g} vs {lp[0]:g} - not mirrored')
    for axis, index in (('y', 1), ('z', 2)):
        if len(rp) > index and len(lp) > index and not close(rp[index], lp[index]):
            issues.append(f'{name}: pivot {axis} {rp[index]:g} vs {lp[index]:g}')
    if len(rp) == 6 and len(lp) == 6:
        if not close(rp[3], lp[3]):
            issues.append(f'{name}: xRot {rp[3]:g} vs {lp[3]:g} - these must match')
        if not close(rp[4], -lp[4]):
            issues.append(f'{name}: yRot {rp[4]:g} vs {lp[4]:g} - these must be mirrored')
        if not close(rp[5], -lp[5]):
            issues.append(f'{name}: zRot {rp[5]:g} vs {lp[5]:g} - these must be mirrored')
    if len(right['boxes']) != len(left['boxes']):
        issues.append(f'{name}: {len(right["boxes"])} boxes vs {len(left["boxes"])}')
        continue
    for i, (rb, lb) in enumerate(zip(right['boxes'], left['boxes'])):
        # mirror along x: [x, x+w] -> [-(x+w), -x]
        if not close(rb[0], -(lb[0] + lb[3])) or not close(rb[3], lb[3]):
            issues.append(f'{name}: box {i} along x {rb[0]:g}..{rb[0] + rb[3]:g} '
                          f'vs {lb[0]:g}..{lb[0] + lb[3]:g}')
        for axis, index in (('y', 1), ('z', 2), ('h', 4), ('d', 5)):
            if not close(rb[index], lb[index]):
                issues.append(f'{name}: box {i} {axis} {rb[index]:g} vs {lb[index]:g}')
    if right['mirror'] == left['mirror']:
        issues.append(f'{name}: both sides {"with" if right["mirror"] else "without"} .mirror() - '
                      f'the texture is not mirrored')

print('pairs checked:', sum(1 for n in parts if n.endswith('_right') and n[:-6] + '_left' in parts))
print('mismatches found:', len(issues))
for i in issues:
    print('  ', i)
