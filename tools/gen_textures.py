"""Gera as texturas originais do mod (pixel art procedural). Uso: python tools/gen_textures.py"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "alvorecer", "textures")


def save(img, *path):
    p = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def outline(img, color=(20, 14, 10, 255)):
    """Contorno escuro de 1 px em volta dos pixels opacos (estilo vanilla)."""
    w, h = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0 and src[nx, ny][:3] != color[:3]:
                    dst[x, y] = color
                    break
    return out


def book(cover, trim, emblem):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 1, 13, 14], fill=cover)
    d.rectangle([12, 2, 13, 13], fill=(232, 222, 196, 255))   # páginas
    d.line([12, 2, 12, 13], fill=(196, 182, 150, 255))
    d.rectangle([2, 1, 3, 14], fill=tuple(max(0, c - 40) for c in cover[:3]) + (255,))  # lombada
    d.line([4, 1, 11, 1], fill=trim)
    d.line([4, 14, 11, 14], fill=trim)
    emblem(d)
    return outline(img)


def sun(d):
    cx, cy = 8, 7
    for a in range(0, 360, 45):
        x = cx + round(3 * math.cos(math.radians(a)))
        y = cy + round(3 * math.sin(math.radians(a)))
        d.point((x, y), fill=(255, 196, 64, 255))
    d.ellipse([6, 5, 10, 9], fill=(255, 222, 96, 255))
    d.point((7, 6), fill=(255, 250, 200, 255))


def cycle(d):
    c = (210, 220, 230, 255)
    d.arc([5, 4, 11, 10], 200, 520, fill=c)
    d.point((10, 4), fill=c)
    d.point((11, 5), fill=c)
    d.point((6, 10), fill=c)
    d.point((5, 9), fill=c)


def icon(draw_fn):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(img))
    return outline(img)


ICONS = {
    "determination": lambda d: (d.polygon([(9, 2), (15, 4), (14, 10), (9, 15), (4, 10), (3, 4)], fill=(242, 201, 76, 255)),
                                d.polygon([(9, 4), (13, 5), (12, 10), (9, 13)], fill=(255, 232, 140, 255))),
    "stoneskin": lambda d: (d.rectangle([3, 3, 14, 14], fill=(138, 138, 138, 255)), d.line([5, 6, 8, 9, 7, 12], fill=(90, 90, 90, 255)),
                            d.line([11, 4, 12, 8, 10, 11], fill=(90, 90, 90, 255)), d.point((5, 4), fill=(190, 190, 190, 255))),
    "evasion": lambda d: [d.arc([2 + i * 2, 4 + i, 15 - i, 13 - i], 180, 360, fill=(155, 231, 196, 255)) for i in range(3)],
    "orc_fury": lambda d: (d.polygon([(9, 2), (13, 8), (11, 8), (14, 15), (6, 9), (8, 9), (4, 4)], fill=(179, 58, 58, 255)),
                           d.polygon([(9, 6), (11, 9), (9, 12)], fill=(255, 140, 90, 255))),
    "barbarian_rage": lambda d: [d.line([4 + i * 4, 3, 2 + i * 4, 15], fill=(217, 83, 79, 255), width=2) for i in range(3)],
    "undying": lambda d: (d.ellipse([4, 2, 14, 11], fill=(200, 200, 190, 255)), d.rectangle([6, 10, 12, 14], fill=(200, 200, 190, 255)),
                          d.rectangle([6, 5, 7, 7], fill=(122, 0, 0, 255)), d.rectangle([11, 5, 12, 7], fill=(122, 0, 0, 255)),
                          d.line([7, 13, 11, 13], fill=(122, 0, 0, 255))),
    "poison_blades": lambda d: (d.line([3, 15, 13, 5], fill=(200, 210, 220, 255), width=2), d.line([3, 11, 7, 15], fill=(110, 80, 40, 255), width=2),
                                d.ellipse([12, 10, 15, 14], fill=(78, 154, 6, 255)), d.point((13, 9), fill=(78, 154, 6, 255))),
    "bear_form": lambda d: (d.ellipse([5, 8, 13, 15], fill=(107, 68, 35, 255)),
                            *[d.ellipse([3 + i * 3, 3 + (1 if i in (0, 3) else 0), 5 + i * 3, 6 + (1 if i in (0, 3) else 0)], fill=(107, 68, 35, 255)) for i in range(4)]),
    "holy_aura": lambda d: (d.ellipse([2, 2, 15, 15], outline=(255, 243, 176, 255), width=2), d.ellipse([7, 7, 10, 10], fill=(255, 243, 176, 255))),
    "dawn_avatar": lambda d: ([d.line([9, 9, 9 + round(7 * math.cos(math.radians(a))), 9 + round(7 * math.sin(math.radians(a)))], fill=(255, 209, 102, 255)) for a in range(0, 360, 30)],
                              d.ellipse([5, 5, 12, 12], fill=(255, 230, 150, 255))),
    "stun": lambda d: [d.polygon([(x, y - 3), (x + 1, y - 1), (x + 3, y), (x + 1, y + 1), (x, y + 3), (x - 1, y + 1), (x - 3, y), (x - 1, y - 1)],
                                 fill=(255, 255, 102, 255)) for x, y in ((5, 6), (12, 5), (9, 12))],
    "root": lambda d: (d.line([9, 2, 9, 15], fill=(46, 125, 50, 255), width=2), d.line([9, 7, 4, 12, 3, 15], fill=(46, 125, 50, 255)),
                       d.line([9, 9, 14, 13, 15, 15], fill=(46, 125, 50, 255)), d.ellipse([10, 3, 14, 6], fill=(102, 187, 106, 255))),
    "charm": lambda d: (d.ellipse([3, 4, 9, 10], fill=(244, 143, 177, 255)), d.ellipse([8, 4, 14, 10], fill=(244, 143, 177, 255)),
                        d.polygon([(3, 8), (14, 8), (9, 15)], fill=(244, 143, 177, 255)), d.point((5, 6), fill=(255, 220, 230, 255))),
}

if __name__ == "__main__":
    save(book((59, 31, 74, 255), (224, 184, 90, 255), sun), "item", "codex.png")
    save(book((36, 92, 60, 255), (200, 200, 210, 255), cycle), "item", "respec_tome.png")
    for name, fn in ICONS.items():
        save(icon(fn), "mob_effect", name + ".png")
    print("texturas geradas")
