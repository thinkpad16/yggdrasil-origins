import re
from PIL import Image

SRC = 'src/client/java/org/origins_yggdrasil/yggdrasil_origins/client/render/VisualParts.java'
TEX = 'src/main/resources/assets/yggdrasil_origins/textures/entity/visual_parts.png'
src = open(SRC, encoding='utf-8').read()
im = Image.open(TEX).convert('RGBA')
W, H = im.size

# texOffs(u, v) ... addBox(x, y, z, w, h, d, ...)
# skinMesh parts are drawn against the player skin, not against our atlas
skin_start = src.index('public static LayerDefinition skinMesh()')
skin_end = src.index('public static LayerDefinition propMesh()')
tokens = []
for m in re.finditer(r'texOffs\((\d+),\s*(\d+)\)|addBox\(([^)]*)\)', src):
    if skin_start <= m.start() < skin_end:
        continue
    tokens.append((m.group(1) or '', m.group(2) or '', m.group(3) or ''))
cur = None
boxes = []
for u, v, args in tokens:
    if u:
        cur = (int(u), int(v))
        continue
    nums = [float(p.strip().rstrip('Ff')) for p in args.split(',')[:6]]
    if len(nums) < 6 or cur is None:
        continue
    boxes.append((cur, nums[3], nums[4], nums[5]))   # (u,v), w, h, d

def rects(u, v, w, h, d):
    """The six faces exactly as Minecraft lays them out, in fractional coordinates."""
    return [
        ('down',  u + d,         v,     w, d),
        ('up',    u + d + w,     v,     w, d),
        ('east',  u,             v + d, d, h),
        ('north', u + d,         v + d, w, h),
        ('west',  u + d + w,     v + d, d, h),
        ('south', u + d + w + d, v + d, w, h),
    ]

holes = []
for (u, v), w, h, d in boxes:
    for name, x, y, fw, fh in rects(u, v, w, h, d):
        x0, y0 = int(x), int(y)
        x1, y1 = min(W, int(x + fw + 0.999)), min(H, int(y + fh + 0.999))
        empty = 0
        for py in range(y0, y1):
            for px in range(x0, x1):
                if im.getpixel((px, py))[3] == 0:
                    empty += 1
        if empty:
            holes.append((u, v, w, h, d, name, empty, (x1 - x0) * (y1 - y0)))

print('boxes checked:', len(boxes))
print('faces with transparent pixels:', len(holes))
seen = set()
for u, v, w, h, d, name, empty, total in holes:
    key = (u, v, w, h, d)
    if key in seen:
        continue
    seen.add(key)
    print(f'  texOffs({u},{v}) {w:g}x{h:g}x{d:g}  face {name}: {empty} of {total} empty')
print('unique boxes with holes:', len(seen))
