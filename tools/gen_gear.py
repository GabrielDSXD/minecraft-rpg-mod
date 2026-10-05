"""Etapa 3: texturas originais (pixel art procedural), modelos, receitas, tags, loot tables e geração de minérios.
Uso: python tools/gen_gear.py"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "alvorecer")
DATA = os.path.join(ROOT, "data")
NS = "alvorecer"

# ---------------------------------------------------------------- paletas (claro, médio, escuro, destaque)
PAL = {
    "iron": [(222, 222, 226), (170, 170, 176), (108, 108, 116), (240, 240, 245)],
    "lunar": [(222, 234, 255), (160, 180, 222), (88, 102, 146), (150, 210, 255)],
    "aurorite": [(255, 228, 150), (246, 158, 70), (168, 82, 34), (255, 120, 150)],
    "wood": [(150, 112, 62), (108, 78, 40), (70, 50, 26), (176, 136, 80)],
    "shadow": [(150, 110, 210), (88, 58, 140), (44, 26, 76), (200, 140, 255)],
    "solar": [(255, 246, 190), (252, 206, 72), (190, 128, 24), (255, 255, 255)],
    "amethyst": [(222, 180, 255), (166, 112, 222), (100, 60, 156), (240, 220, 255)],
    "leather": [(176, 128, 82), (130, 90, 52), (84, 56, 30), (210, 170, 110)],
    "arcane": [(120, 110, 220), (70, 58, 160), (40, 30, 96), (255, 220, 120)],
}
OUTLINE = (24, 18, 22, 255)


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


class Canvas:
    def __init__(self, w=16, h=16):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h

    def px(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((int(x), int(y)), rgba(c) if len(c) == 3 else c)

    def line(self, x0, y0, x1, y1, c):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.px(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)

    def rect(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.px(x, y, c)

    def outline(self):
        src = self.img.copy()
        sp = src.load()
        for y in range(self.h):
            for x in range(self.w):
                if sp[x, y][3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h and sp[nx, ny][3] and sp[nx, ny] != OUTLINE:
                        self.img.putpixel((x, y), OUTLINE)
                        break
        return self

    def save(self, *path):
        p = os.path.join(ASSETS, "textures", *path)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        self.img.save(p)


# ---------------------------------------------------------------- formas de itens

def diag_blade(c, p, x0, y0, x1, y1):
    """Lâmina diagonal (de baixo-esquerda para cima-direita) com fio claro e costas escuras."""
    n = max(abs(x1 - x0), 1)
    for i in range(n + 1):
        x, y = x0 + i, y0 - i
        c.px(x, y, p[1])
        c.px(x + 1, y, p[0])
        c.px(x, y - 1, p[2] if i % 3 else p[1])
    c.px(x1 + 1, y1 - 1, p[0])


def handle(c, x0, y0, length, wood=PAL["wood"]):
    for i in range(length):
        c.px(x0 + i, y0 - i, wood[1] if i % 2 else wood[0])
        c.px(x0 + i + 1, y0 - i, wood[2])


def sword(p, guard=PAL["iron"], dagger=False, glow=None):
    c = Canvas()
    if dagger:
        diag_blade(c, p, 6, 9, 11, 4)
        c.line(4, 9, 6, 11, guard[1])
        handle(c, 3, 12, 3)
    else:
        diag_blade(c, p, 5, 10, 12, 3)
        c.line(2, 9, 6, 13, guard[1])
        c.px(4, 11, guard[0])
        handle(c, 1, 14, 3)
    if glow:
        for x, y in ((13, 1), (9, 5), (11, 2)):
            c.px(x, y, glow)
    return c.outline()


def twin_daggers(p):
    c = Canvas()
    diag_blade(c, p, 6, 9, 11, 4)
    c.line(4, 9, 6, 11, p[2])
    handle(c, 3, 12, 3, PAL["shadow"])
    for i in range(5):  # segunda adaga espelhada
        c.px(9 - i, 9 - i, p[1])
        c.px(9 - i - 1, 9 - i, p[0])
    c.line(10, 11, 12, 9, p[2])
    c.px(11, 12, PAL["shadow"][1])
    c.px(12, 13, PAL["shadow"][2])
    c.px(12, 3, p[3])
    return c.outline()


def warhammer(p, wood=PAL["wood"], emblem=None):
    c = Canvas()
    handle(c, 2, 14, 8, wood)
    c.rect(8, 2, 13, 6, p[1])
    c.line(8, 2, 13, 2, p[0])
    c.line(8, 2, 8, 6, p[0])
    c.line(13, 3, 13, 6, p[2])
    c.line(9, 6, 13, 6, p[2])
    if emblem:
        c.px(10, 4, emblem)
        c.px(11, 4, emblem)
        c.px(10, 3, emblem)
        c.px(11, 5, emblem)
    return c.outline()


def spear(p, wood=PAL["wood"]):
    c = Canvas()
    for i in range(10):
        c.px(1 + i, 14 - i, wood[0] if i % 2 else wood[1])
    c.line(11, 4, 13, 2, p[1])
    c.line(12, 4, 14, 2, p[0])
    c.line(11, 3, 13, 1, p[0])
    c.px(14, 1, p[0])
    c.line(9, 5, 11, 7, p[2])
    return c.outline()


def pickaxe(p):
    c = Canvas()
    handle(c, 3, 13, 8)
    for i, (x, y) in enumerate([(3, 3), (4, 2), (5, 2), (6, 2), (7, 2), (8, 3), (9, 3), (10, 4), (11, 5), (12, 6), (12, 7), (13, 8), (13, 9)]):
        c.px(x, y, p[0] if i < 6 else p[1])
        c.px(x, y + 1, p[1] if i < 6 else p[2])
    return c.outline()


def axe(p):
    c = Canvas()
    handle(c, 3, 13, 8)
    c.rect(7, 2, 11, 6, p[1])
    c.line(7, 2, 11, 2, p[0])
    c.line(6, 3, 6, 5, p[0])
    c.line(8, 7, 9, 7, p[2])
    c.px(12, 3, p[2])
    return c.outline()


def shovel(p):
    c = Canvas()
    handle(c, 2, 14, 9)
    c.rect(10, 2, 13, 5, p[1])
    c.line(10, 2, 13, 2, p[0])
    c.line(10, 2, 10, 5, p[0])
    c.px(13, 5, p[2])
    return c.outline()


def hoe(p):
    c = Canvas()
    handle(c, 3, 13, 8)
    c.line(8, 2, 12, 2, p[0])
    c.line(8, 3, 12, 3, p[1])
    c.line(12, 4, 12, 5, p[2])
    c.px(7, 3, p[1])
    return c.outline()


def bow(p, pull=-1):
    c = Canvas()
    pts = [(2, 13), (3, 12), (3, 11), (4, 10), (5, 8), (6, 7), (7, 5), (8, 4), (10, 3), (11, 3), (12, 2), (13, 2)]
    for i, (x, y) in enumerate(pts):
        c.px(x, y, p[1] if i % 3 else p[0])
        c.px(x + 1, y + 1, p[2])
    off = [0, 1, 2, 3][pull + 1] if pull >= 0 else 0
    sx, sy = 3 + off, 12 - off
    c.line(sx, sy + 1, 13 + 0, 3 - 0 + 0, (230, 230, 230))
    if pull >= 0:
        for i in range(6 - off):
            c.px(sx + 1 + i, sy - 1 - i, (130, 100, 60))
        c.px(sx + 6 - off, sy - 6 + off, (200, 200, 210))
    return c.outline()


def crossbow(p, state="standby", pull=0):
    c = Canvas()
    c.line(3, 12, 12, 3, PAL["wood"][1])
    c.line(4, 12, 12, 4, PAL["wood"][0])
    for i in range(7):
        c.px(5 + i, 1 + i, p[1])
    c.px(4, 1, p[0])
    c.px(12, 8, p[0])
    if state == "pulling":
        c.line(6 + pull, 3 + pull, 9 - pull, 6 - pull, (230, 230, 230))
    else:
        c.line(5, 2, 11, 8, (230, 230, 230)) if state == "standby" else None
    if state == "arrow":
        c.line(6, 9, 13, 2, (200, 200, 210))
        c.px(13, 2, (240, 240, 240))
    if state == "firework":
        c.line(6, 9, 11, 4, (200, 60, 60))
        c.px(12, 3, (255, 220, 80))
    return c.outline()


def shield_icon(p):
    c = Canvas()
    c.rect(4, 2, 11, 10, p[1])
    c.line(4, 2, 11, 2, p[0])
    c.line(4, 2, 4, 10, p[0])
    for i in range(3):
        c.line(5 + i, 11 + i, 10 - i, 11 + i, p[1] if i < 2 else p[2])
    c.line(7, 4, 8, 4, p[3])
    c.line(7, 4, 7, 9, p[3])
    return c.outline()


def staff(p, gem, wood=PAL["wood"]):
    c = Canvas()
    for i in range(11):
        c.px(1 + i, 14 - i, wood[0] if i % 2 else wood[1])
    c.rect(11, 2, 13, 4, gem[1])
    c.px(11, 2, gem[0])
    c.px(12, 3, gem[3])
    c.px(10, 2, wood[2])
    c.px(13, 5, wood[2])
    c.px(14, 1, gem[0])
    return c.outline()


def wand(p, gem):
    c = Canvas()
    for i in range(7):
        c.px(4 + i, 12 - i, p[1] if i % 2 else p[0])
    c.rect(10, 4, 11, 5, gem[1])
    c.px(11, 3, gem[0])
    c.px(12, 3, gem[3])
    c.px(12, 2, gem[0])
    return c.outline()


def ingot(p):
    c = Canvas()
    for y in range(6, 11):
        c.line(3 + (10 - y), y, 12 - (y - 6) // 2, y, p[1])
    c.line(5, 6, 11, 6, p[0])
    c.line(3, 10, 10, 10, p[2])
    c.px(6, 7, p[3])
    return c.outline()


def raw(p):
    c = Canvas()
    random.seed(7)
    for x, y in [(5, 6), (6, 5), (7, 5), (8, 5), (9, 6), (10, 7), (10, 8), (9, 9), (8, 10), (7, 10), (6, 9), (5, 8), (5, 7), (6, 6), (7, 6), (8, 6), (9, 7), (9, 8), (8, 9), (7, 9), (6, 8), (6, 7), (7, 7), (8, 7), (7, 8), (8, 8)]:
        c.px(x, y, p[random.randint(0, 2)])
    c.px(7, 6, p[3])
    return c.outline()


def shard(p):
    c = Canvas()
    for i in range(6):
        c.px(6 + i // 2, 11 - i, p[1])
        c.px(7 + i // 2, 11 - i, p[0])
    c.px(9, 4, p[3])
    c.px(6, 10, p[2])
    return c.outline()


def gem(p):
    c = Canvas()
    rows = [(7, 8), (6, 9), (5, 10), (5, 10), (6, 9), (7, 8)]
    for i, (a, b) in enumerate(rows):
        c.line(a, 4 + i, b, 4 + i, p[1] if i > 2 else p[0])
    c.px(6, 5, p[3])
    c.px(7, 6, (255, 255, 255))
    c.line(6, 9, 9, 9, p[2])
    return c.outline()


def armor_icon(kind, p, trim):
    c = Canvas()
    if kind == "helmet":
        c.rect(4, 4, 11, 8, p[1])
        c.line(4, 4, 11, 4, p[0])
        c.rect(5, 7, 10, 8, (0, 0, 0, 0))
        c.line(4, 9, 5, 9, p[2])
        c.line(10, 9, 11, 9, p[2])
        c.line(6, 5, 9, 5, trim)
    elif kind == "chestplate":
        c.rect(3, 3, 12, 6, p[1])
        c.rect(5, 6, 10, 12, p[1])
        c.line(3, 3, 12, 3, p[0])
        c.line(7, 3, 8, 3, (0, 0, 0, 0))
        c.line(7, 6, 8, 11, trim)
        c.line(5, 12, 10, 12, p[2])
    elif kind == "leggings":
        c.rect(4, 3, 11, 5, p[1])
        c.rect(4, 6, 6, 12, p[1])
        c.rect(9, 6, 11, 12, p[1])
        c.line(4, 3, 11, 3, trim)
        c.line(4, 12, 6, 12, p[2])
        c.line(9, 12, 11, 12, p[2])
    else:
        c.rect(3, 8, 6, 12, p[1])
        c.rect(9, 8, 12, 12, p[1])
        c.line(3, 8, 6, 8, trim)
        c.line(9, 8, 12, 8, trim)
        c.line(2, 12, 6, 12, p[2])
        c.line(9, 12, 13, 12, p[2])
    return c.outline()


def wings_icon():
    c = Canvas()
    for i in range(10):
        hue = i / 10
        r, g, b = [int(255 * v) for v in hsv(hue)]
        c.line(7 - i // 2, 3 + i, 7, 3 + i, (r, g, b))
        c.line(8, 3 + i, 8 + i // 2, 3 + i, (r, g, b))
    return c.outline()


def hsv(h, s=0.75, v=1.0):
    i = int(h * 6) % 6
    f = h * 6 - int(h * 6)
    p, q, t = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    return [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][i]


def bottle(liquid):
    c = Canvas()
    c.rect(7, 3, 8, 5, (200, 200, 210))
    c.px(7, 2, (140, 100, 60))
    c.px(8, 2, (140, 100, 60))
    c.rect(5, 6, 10, 12, (210, 220, 230))
    c.rect(6, 8, 9, 12, liquid)
    c.px(6, 8, tuple(min(255, v + 60) for v in liquid))
    return c.outline()


def salve():
    c = Canvas()
    c.rect(4, 7, 11, 11, (170, 140, 100))
    c.rect(4, 6, 11, 7, (200, 200, 200))
    c.rect(6, 8, 9, 10, (120, 200, 120))
    return c.outline()


def stew():
    c = Canvas()
    c.line(3, 8, 12, 8, (150, 100, 60))
    c.line(4, 9, 11, 9, (130, 86, 50))
    c.line(5, 10, 10, 10, (110, 70, 40))
    c.line(4, 7, 11, 7, (180, 90, 40))
    c.px(6, 6, (240, 140, 40))
    c.px(9, 6, (120, 180, 80))
    return c.outline()


# ---------------------------------------------------------------- blocos

def stone_base(dark=False, seed=1):
    random.seed(seed)
    c = Canvas()
    base = [(70, 70, 76), (58, 58, 64), (84, 84, 90)] if dark else [(128, 128, 128), (112, 112, 114), (142, 142, 144)]
    for x in range(16):
        for y in range(16):
            c.px(x, y, base[random.choice([0, 0, 1, 2])])
    if dark:
        for y in (4, 9, 13):
            for x in range(16):
                if random.random() < 0.6:
                    c.px(x, y, (50, 50, 56))
    return c


def ore(dark, p, seed):
    c = stone_base(dark, seed)
    random.seed(seed + 3)
    for cx, cy in [(4, 4), (11, 5), (6, 11), (12, 12)]:
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)]:
            if random.random() < 0.85:
                c.px(cx + dx, cy + dy, p[random.randint(0, 1)])
        c.px(cx, cy, p[3])
    return c


def storage(p, crystal=False):
    c = Canvas()
    for x in range(16):
        for y in range(16):
            c.px(x, y, p[1])
    for i in range(16):
        c.px(i, 0, p[0])
        c.px(0, i, p[0])
        c.px(i, 15, p[2])
        c.px(15, i, p[2])
    if crystal:
        for k in range(0, 16, 4):
            c.line(k, 15, min(15, k + 6), 9, p[0])
            c.px(k + 2, 12, p[3])
    else:
        c.rect(3, 3, 12, 12, p[0])
        c.rect(4, 4, 11, 11, p[1])
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            c.px(x, y, p[3])
    return c


def planks(seed=2):
    random.seed(seed)
    c = Canvas()
    w = PAL["wood"]
    for y in range(16):
        for x in range(16):
            c.px(x, y, w[0] if (y // 4) % 2 else w[1])
        if y % 4 == 3:
            c.line(0, y, 15, y, w[2])
    return c


def workbench_top():
    c = planks(5)
    c.rect(2, 2, 13, 13, (120, 88, 48))
    c.line(3, 4, 7, 4, (200, 200, 205))  # régua
    c.rect(9, 3, 12, 6, (90, 90, 96))  # bigorna pequena
    c.line(4, 9, 8, 13, (150, 112, 62))  # martelo
    c.rect(7, 8, 9, 9, (170, 170, 176))
    c.px(11, 11, (140, 80, 200))  # gema
    c.px(12, 10, (220, 180, 255))
    return c


def workbench_side():
    c = planks(6)
    c.rect(4, 4, 11, 11, (80, 58, 30))
    c.rect(5, 5, 10, 10, (246, 158, 70))
    c.rect(6, 6, 9, 9, (255, 228, 150))
    return c


# ---------------------------------------------------------------- texturas de entidade

def armor_layers(name, p, trim, pattern="plate"):
    """Camadas 64x32 de armadura no layout vanilla de humanoides."""
    def fill(img, x0, y0, w, h, col):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                img.putpixel((x, y), rgba(col))

    def shade_box(img, ox, oy, w, h, d):
        """Preenche as 6 faces de uma caixa (w×h×d) a partir do offset de textura."""
        fill(img, ox + d, oy, w, d, p[0])  # topo
        fill(img, ox + d + w, oy, w, d, p[2])  # base
        fill(img, ox, oy + d, d, h, p[2])  # direita
        fill(img, ox + d, oy + d, w, h, p[1])  # frente
        fill(img, ox + d + w, oy + d, d, h, p[2])  # esquerda
        fill(img, ox + 2 * d + w, oy + d, w, h, p[1])  # costas
        for x in range(ox + d, ox + d + w):  # borda e detalhe na frente
            img.putpixel((x, oy + d), rgba(p[0]))
            img.putpixel((x, oy + d + h - 1), rgba(p[2]))
        if pattern == "plate":
            for y in range(oy + d + 2, oy + d + h - 1, 3):
                img.putpixel((ox + d + 1, y), rgba(trim))
                img.putpixel((ox + d + w - 2, y), rgba(trim))
        elif pattern == "cloth":
            for y in range(oy + d + 1, oy + d + h):
                if (y - oy) % 4 == 0:
                    for x in range(ox + d, ox + d + w):
                        img.putpixel((x, y), rgba(trim))
        else:  # couro: costuras
            for y in range(oy + d + 1, oy + d + h, 2):
                img.putpixel((ox + d + w // 2, y), rgba(trim))

    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    shade_box(l1, 0, 0, 8, 8, 8)  # cabeça
    for x in range(8, 16):  # faixa da testa
        l1.putpixel((x, 10), rgba(trim))
    for x in range(9, 15):  # abertura do rosto
        for y in range(11, 16):
            l1.putpixel((x, y), (0, 0, 0, 0))
    shade_box(l1, 16, 16, 8, 12, 4)  # tronco
    shade_box(l1, 40, 16, 4, 12, 4)  # braço
    shade_box(l1, 0, 16, 4, 12, 4)  # perna (botas usam a parte de baixo)
    for x in range(0, 16):  # zera o meio da perna nas botas: mantém só a ponta
        for y in range(20, 27):
            l1.putpixel((x, y), (0, 0, 0, 0))
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    shade_box(l2, 16, 16, 8, 12, 4)  # cintura
    for x in range(16, 40):
        for y in range(16, 26):
            l2.putpixel((x, y), (0, 0, 0, 0))
    shade_box(l2, 0, 16, 4, 12, 4)  # pernas
    base = os.path.join(ASSETS, "textures", "models", "armor")
    os.makedirs(base, exist_ok=True)
    l1.save(os.path.join(base, f"{name}_layer_1.png"))
    l2.save(os.path.join(base, f"{name}_layer_2.png"))


def shield_texture(name, p, emblem):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in range(0, 28):  # placa 12x22x1 em (0,0) e cabo em (26,0)
        for y in range(0, 24):
            img.putpixel((x, y), rgba(p[2]))
    for x in range(1, 13):
        for y in range(1, 23):
            edge = x in (1, 12) or y in (1, 22)
            img.putpixel((x, y), rgba(p[0] if edge else p[1]))
    for y in range(4, 20):
        img.putpixel((6, y), rgba(emblem))
        img.putpixel((7, y), rgba(emblem))
    for x in range(3, 11):
        img.putpixel((x, 8), rgba(emblem))
    for x in range(26, 40):
        for y in range(0, 12):
            img.putpixel((x, y), rgba(PAL["wood"][1]))
    path = os.path.join(ASSETS, "textures", "entity", "shield")
    os.makedirs(path, exist_ok=True)
    img.save(os.path.join(path, f"{name}.png"))


def wings_texture():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for x in range(22, 46):
        for y in range(0, 22):
            h = ((x - 22) / 24 + y / 44) % 1.0
            r, g, b = [int(255 * v) for v in hsv(h, 0.55, 1.0)]
            a = 230 if (x + y) % 5 else 255
            img.putpixel((x, y), (r, g, b, a))
    path = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(path, exist_ok=True)
    img.save(os.path.join(path, "prismatic_wings.png"))


# ---------------------------------------------------------------- JSON

def jdump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


def item_model(name, parent="minecraft:item/generated", extra=None, layer=None):
    obj = {"parent": parent, "textures": {"layer0": f"{NS}:item/{layer or name}"}}
    if extra:
        obj.update(extra)
    jdump(os.path.join(ASSETS, "models", "item", f"{name}.json"), obj)


def block_assets(name, textures=None, model="cube_all"):
    jdump(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    jdump(os.path.join(ASSETS, "models", "block", f"{name}.json"),
          {"parent": f"minecraft:block/{model}", "textures": textures or {"all": f"{NS}:block/{name}"}})
    jdump(os.path.join(ASSETS, "models", "item", f"{name}.json"), {"parent": f"{NS}:block/{name}"})


def shaped(name, pattern, key, result, count=1):
    k = {c: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for c, v in key.items()}
    jdump(os.path.join(DATA, NS, "recipe", f"{name}.json"),
          {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": k, "result": {"id": result, "count": count}})


def shapeless(name, items, result, count=1):
    jdump(os.path.join(DATA, NS, "recipe", f"{name}.json"),
          {"type": "minecraft:crafting_shapeless", "category": "misc",
           "ingredients": [{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in items], "result": {"id": result, "count": count}})


def cooking(name, kind, ingredient, result, xp, time):
    jdump(os.path.join(DATA, NS, "recipe", f"{name}_{kind}.json"),
          {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": ingredient}, "result": {"id": result},
           "experience": xp, "cookingtime": time})


def tag(kind, name, values, ns="minecraft"):
    jdump(os.path.join(DATA, ns, "tags", kind, f"{name}.json"), {"replace": False, "values": values})


def ore_loot(block, drop, lo=1, hi=1):
    jdump(os.path.join(DATA, NS, "loot_table", "blocks", f"{block}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{NS}:{block}",
             "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                 {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": drop, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})


def self_loot(block):
    jdump(os.path.join(DATA, NS, "loot_table", "blocks", f"{block}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def ore_feature(name, targets, size, count, lo, hi, trapezoid=True):
    jdump(os.path.join(DATA, NS, "worldgen", "configured_feature", f"{name}.json"), {
        "type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets}})
    height = {"type": "minecraft:trapezoid" if trapezoid else "minecraft:uniform",
              "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}
    jdump(os.path.join(DATA, NS, "worldgen", "placed_feature", f"{name}.json"), {
        "feature": f"{NS}:{name}",
        "placement": [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                      {"type": "minecraft:height_range", "height": height}, {"type": "minecraft:biome"}]})
    jdump(os.path.join(DATA, NS, "neoforge", "biome_modifier", f"{name}.json"), {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld", "features": f"{NS}:{name}", "step": "underground_ores"})


# ---------------------------------------------------------------- execução

def main():
    L, A, I = PAL["lunar"], PAL["aurorite"], PAL["iron"]
    # itens: armas e ferramentas
    for prefix, p in (("lunar_silver", L), ("aurorite", A)):
        sword(p).save("item", f"{prefix}_sword.png")
        sword(p, dagger=True).save("item", f"{prefix}_dagger.png")
        warhammer(p).save("item", f"{prefix}_warhammer.png")
        spear(p).save("item", f"{prefix}_spear.png")
        pickaxe(p).save("item", f"{prefix}_pickaxe.png")
        axe(p).save("item", f"{prefix}_axe.png")
        shovel(p).save("item", f"{prefix}_shovel.png")
        hoe(p).save("item", f"{prefix}_hoe.png")
        bow(p).save("item", f"{prefix}_bow.png")
        for i in range(3):
            bow(p, i).save("item", f"{prefix}_bow_pulling_{i}.png")
        crossbow(p, "standby").save("item", f"{prefix}_crossbow_standby.png")
        for i in range(3):
            crossbow(p, "pulling", i).save("item", f"{prefix}_crossbow_pulling_{i}.png")
        crossbow(p, "arrow").save("item", f"{prefix}_crossbow_arrow.png")
        crossbow(p, "firework").save("item", f"{prefix}_crossbow_firework.png")
        shield_icon(p).save("item", f"{prefix}_shield.png")
    sword(I, dagger=True).save("item", "iron_dagger.png")
    warhammer(I).save("item", "iron_warhammer.png")
    spear(I).save("item", "iron_spear.png")
    staff(PAL["wood"], PAL["amethyst"]).save("item", "oak_staff.png")
    staff(PAL["arcane"], PAL["solar"], PAL["arcane"]).save("item", "arcane_staff.png")
    wand(PAL["wood"], PAL["amethyst"]).save("item", "amethyst_wand.png")
    wand(PAL["lunar"], A).save("item", "aurorite_wand.png")
    warhammer(PAL["solar"], PAL["wood"], emblem=(255, 255, 255)).save("item", "solar_breaker.png")
    sword(PAL["solar"], guard=A, glow=(255, 255, 230)).save("item", "dawn_blade.png")
    twin_daggers(PAL["shadow"]).save("item", "shadow_daggers.png")
    wings_icon().save("item", "prismatic_wings.png")
    ingot(L).save("item", "lunar_silver_ingot.png")
    raw(L).save("item", "raw_lunar_silver.png")
    shard(A).save("item", "aurorite_shard.png")
    gem(A).save("item", "aurorite_gem.png")
    bottle((70, 120, 255)).save("item", "mana_elixir.png")
    bottle((240, 200, 60)).save("item", "vigor_elixir.png")
    salve().save("item", "healing_salve.png")
    stew().save("item", "adventurer_stew.png")
    sets = {"wanderer": (PAL["leather"], (90, 160, 90), "leather"), "arcane": (PAL["arcane"], (255, 220, 120), "cloth"),
            "lunar_silver": (L, (150, 210, 255), "plate"), "aurorite": (A, (255, 250, 220), "plate")}
    for name, (p, trim, pattern) in sets.items():
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            armor_icon(piece, p, trim).save("item", f"{name}_{piece}.png")
        armor_layers(name, p, trim, pattern)
    shield_texture("lunar_silver_shield", L, L[3])
    shield_texture("aurorite_shield", A, (255, 250, 220))
    wings_texture()

    # blocos
    ore(False, L, 11).save("block", "lunar_silver_ore.png")
    ore(True, L, 12).save("block", "deepslate_lunar_silver_ore.png")
    ore(True, A, 13).save("block", "aurorite_ore.png")
    storage(L).save("block", "lunar_silver_block.png")
    storage(A, crystal=True).save("block", "aurorite_block.png")
    workbench_top().save("block", "profession_workbench_top.png")
    workbench_side().save("block", "profession_workbench_side.png")
    for b in ("lunar_silver_ore", "deepslate_lunar_silver_ore", "aurorite_ore", "lunar_silver_block", "aurorite_block"):
        block_assets(b)
    block_assets("profession_workbench", {"top": f"{NS}:block/profession_workbench_top", "bottom": "minecraft:block/oak_planks",
                                          "side": f"{NS}:block/profession_workbench_side"}, "cube_bottom_top")

    # modelos de item
    handheld = ["iron_dagger", "iron_warhammer", "iron_spear", "solar_breaker", "dawn_blade", "shadow_daggers", "oak_staff",
                "arcane_staff", "amethyst_wand", "aurorite_wand"]
    for prefix in ("lunar_silver", "aurorite"):
        handheld += [f"{prefix}_{t}" for t in ("sword", "dagger", "warhammer", "spear", "pickaxe", "axe", "shovel", "hoe")]
    for n in handheld:
        item_model(n, "minecraft:item/handheld")
    for n in ("lunar_silver_ingot", "raw_lunar_silver", "aurorite_shard", "aurorite_gem", "mana_elixir", "vigor_elixir",
              "healing_salve", "adventurer_stew"):
        item_model(n)
    for name in sets:
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            item_model(f"{name}_{piece}")
    item_model("prismatic_wings", extra={"overrides": [{"predicate": {"broken": 1}, "model": f"{NS}:item/prismatic_wings_broken"}]})
    item_model("prismatic_wings_broken", layer="prismatic_wings")
    for prefix in ("lunar_silver", "aurorite"):
        b = f"{prefix}_bow"
        jdump(os.path.join(ASSETS, "models", "item", f"{b}.json"), {
            "parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{b}"},
            "overrides": [{"predicate": {"pulling": 1}, "model": f"{NS}:item/{b}_pulling_0"},
                          {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"{NS}:item/{b}_pulling_1"},
                          {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"{NS}:item/{b}_pulling_2"}]})
        for i in range(3):
            jdump(os.path.join(ASSETS, "models", "item", f"{b}_pulling_{i}.json"),
                  {"parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{b}_pulling_{i}"}})
        cb = f"{prefix}_crossbow"
        jdump(os.path.join(ASSETS, "models", "item", f"{cb}.json"), {
            "parent": "minecraft:item/crossbow", "textures": {"layer0": f"{NS}:item/{cb}_standby"},
            "overrides": [{"predicate": {"pulling": 1}, "model": f"{NS}:item/{cb}_pulling_0"},
                          {"predicate": {"pulling": 1, "pull": 0.58}, "model": f"{NS}:item/{cb}_pulling_1"},
                          {"predicate": {"pulling": 1, "pull": 1.0}, "model": f"{NS}:item/{cb}_pulling_2"},
                          {"predicate": {"charged": 1}, "model": f"{NS}:item/{cb}_arrow"},
                          {"predicate": {"charged": 1, "firework": 1}, "model": f"{NS}:item/{cb}_firework"}]})
        for s in ("pulling_0", "pulling_1", "pulling_2", "arrow", "firework"):
            jdump(os.path.join(ASSETS, "models", "item", f"{cb}_{s}.json"),
                  {"parent": "minecraft:item/crossbow", "textures": {"layer0": f"{NS}:item/{cb}_{s}"}})
        sh = f"{prefix}_shield"
        jdump(os.path.join(ASSETS, "models", "item", f"{sh}.json"), {
            "parent": "minecraft:item/shield", "textures": {"particle": f"{NS}:item/{sh}"},
            "overrides": [{"predicate": {"blocking": 1}, "model": f"{NS}:item/{sh}_blocking"}]})
        jdump(os.path.join(ASSETS, "models", "item", f"{sh}_blocking.json"),
              {"parent": "minecraft:item/shield_blocking", "textures": {"particle": f"{NS}:item/{sh}"}})

    # receitas de bancada comum (qualquer jogador)
    i = f"{NS}:lunar_silver_ingot"
    stick = "minecraft:stick"
    for prefix, mat in (("lunar_silver", i), ("aurorite", f"{NS}:aurorite_gem")):
        shaped(f"{prefix}_pickaxe", ["MMM", " S ", " S "], {"M": mat, "S": stick}, f"{NS}:{prefix}_pickaxe")
        shaped(f"{prefix}_axe", ["MM", "MS", " S"], {"M": mat, "S": stick}, f"{NS}:{prefix}_axe")
        shaped(f"{prefix}_shovel", ["M", "S", "S"], {"M": mat, "S": stick}, f"{NS}:{prefix}_shovel")
        shaped(f"{prefix}_hoe", ["MM", " S", " S"], {"M": mat, "S": stick}, f"{NS}:{prefix}_hoe")
    shaped("lunar_silver_sword", ["M", "M", "S"], {"M": i, "S": stick}, f"{NS}:lunar_silver_sword")
    shaped("lunar_silver_dagger", ["M", "S"], {"M": i, "S": stick}, f"{NS}:lunar_silver_dagger")
    shaped("lunar_silver_warhammer", ["MMM", "MSM", " S "], {"M": i, "S": stick}, f"{NS}:lunar_silver_warhammer")
    shaped("lunar_silver_spear", ["  M", " S ", "S  "], {"M": i, "S": stick}, f"{NS}:lunar_silver_spear")
    shaped("lunar_silver_bow", [" MS", "M S", " MS"], {"M": i, "S": "minecraft:string"}, f"{NS}:lunar_silver_bow")
    shaped("lunar_silver_crossbow", ["MIM", "SHS", " M "], {"M": i, "I": "minecraft:iron_ingot", "S": "minecraft:string",
                                                            "H": "minecraft:tripwire_hook"}, f"{NS}:lunar_silver_crossbow")
    shaped("lunar_silver_shield", ["PMP", "PPP", " P "], {"P": "#minecraft:planks", "M": i}, f"{NS}:lunar_silver_shield")
    shaped("iron_dagger", ["M", "S"], {"M": "minecraft:iron_ingot", "S": stick}, f"{NS}:iron_dagger")
    shaped("iron_warhammer", ["MMM", "MSM", " S "], {"M": "minecraft:iron_ingot", "S": stick}, f"{NS}:iron_warhammer")
    shaped("iron_spear", ["  M", " S ", "S  "], {"M": "minecraft:iron_ingot", "S": stick}, f"{NS}:iron_spear")
    shaped("oak_staff", ["  A", " S ", "S  "], {"A": "minecraft:amethyst_shard", "S": stick}, f"{NS}:oak_staff")
    shaped("amethyst_wand", [" A", "S "], {"A": "minecraft:amethyst_shard", "S": i}, f"{NS}:amethyst_wand")
    for piece, pat in (("helmet", ["MMM", "M M"]), ("chestplate", ["M M", "MMM", "MMM"]), ("leggings", ["MMM", "M M", "M M"]), ("boots", ["M M", "M M"])):
        shaped(f"lunar_silver_{piece}", pat, {"M": i}, f"{NS}:lunar_silver_{piece}")
        shaped(f"wanderer_{piece}", [r.replace("M", "L") for r in pat] if piece != "boots" else ["L L", "F F"],
               {"L": "minecraft:leather", "F": "minecraft:feather"} if piece == "boots" else {"L": "minecraft:leather"}, f"{NS}:wanderer_{piece}")
    shaped("lunar_silver_block", ["MMM", "MMM", "MMM"], {"M": i}, f"{NS}:lunar_silver_block")
    shapeless("lunar_silver_ingot_from_block", [f"{NS}:lunar_silver_block"], i, 9)
    shaped("aurorite_block", ["MMM", "MMM", "MMM"], {"M": f"{NS}:aurorite_gem"}, f"{NS}:aurorite_block")
    shapeless("aurorite_gem_from_block", [f"{NS}:aurorite_block"], f"{NS}:aurorite_gem", 9)
    shapeless("aurorite_gem", [f"{NS}:aurorite_shard"] * 4, f"{NS}:aurorite_gem")
    shaped("profession_workbench", ["IGI", "PCP", "PPP"], {"I": "minecraft:iron_ingot", "G": "minecraft:amethyst_shard",
                                                           "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, f"{NS}:profession_workbench")
    for kind, time in (("smelting", 200), ("blasting", 100)):
        cooking("lunar_silver_ingot_from_raw", kind, f"{NS}:raw_lunar_silver", i, 0.8, time)
        cooking("lunar_silver_ingot_from_ore", kind, f"{NS}:lunar_silver_ore", i, 0.8, time)
        cooking("lunar_silver_ingot_from_deepslate_ore", kind, f"{NS}:deepslate_lunar_silver_ore", i, 0.8, time)

    # loot tables
    ore_loot("lunar_silver_ore", f"{NS}:raw_lunar_silver")
    ore_loot("deepslate_lunar_silver_ore", f"{NS}:raw_lunar_silver")
    ore_loot("aurorite_ore", f"{NS}:aurorite_shard", 1, 2)
    for b in ("lunar_silver_block", "aurorite_block", "profession_workbench"):
        self_loot(b)

    # tags de bloco
    tag("block", "mineable/pickaxe", [f"{NS}:{b}" for b in ("lunar_silver_ore", "deepslate_lunar_silver_ore", "aurorite_ore",
                                                            "lunar_silver_block", "aurorite_block")])
    tag("block", "mineable/axe", [f"{NS}:quest_board", f"{NS}:profession_workbench"])
    tag("block", "needs_iron_tool", [f"{NS}:lunar_silver_ore", f"{NS}:deepslate_lunar_silver_ore", f"{NS}:lunar_silver_block"])
    tag("block", "needs_diamond_tool", [f"{NS}:aurorite_ore", f"{NS}:aurorite_block"])
    tag("block", "ores", [f"{NS}:lunar_silver_ore", f"{NS}:deepslate_lunar_silver_ore", f"{NS}:aurorite_ore"], ns="c")
    tag("block", "ores/lunar_silver", [f"{NS}:lunar_silver_ore", f"{NS}:deepslate_lunar_silver_ore"], ns="c")
    tag("block", "ores/aurorite", [f"{NS}:aurorite_ore"], ns="c")
    tag("block", "ores_in_ground/stone", [f"{NS}:lunar_silver_ore"], ns="c")
    tag("block", "ores_in_ground/deepslate", [f"{NS}:deepslate_lunar_silver_ore", f"{NS}:aurorite_ore"], ns="c")
    # tags de item (encantamentos, reparo e profissões)
    swords = [f"{NS}:{n}" for n in ("iron_dagger", "iron_warhammer", "iron_spear", "solar_breaker", "dawn_blade", "shadow_daggers")]
    swords += [f"{NS}:{p}_{t}" for p in ("lunar_silver", "aurorite") for t in ("sword", "dagger", "warhammer", "spear")]
    tag("item", "swords", swords)
    tag("item", "pickaxes", [f"{NS}:lunar_silver_pickaxe", f"{NS}:aurorite_pickaxe"])
    tag("item", "axes", [f"{NS}:lunar_silver_axe", f"{NS}:aurorite_axe"])
    tag("item", "shovels", [f"{NS}:lunar_silver_shovel", f"{NS}:aurorite_shovel"])
    tag("item", "hoes", [f"{NS}:lunar_silver_hoe", f"{NS}:aurorite_hoe"])
    for piece, t in (("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")):
        tag("item", t, [f"{NS}:{s}_{piece}" for s in sets])
    tag("item", "enchantable/bow", [f"{NS}:lunar_silver_bow", f"{NS}:aurorite_bow"])
    tag("item", "enchantable/crossbow", [f"{NS}:lunar_silver_crossbow", f"{NS}:aurorite_crossbow"])
    tag("item", "enchantable/durability", [f"{NS}:lunar_silver_shield", f"{NS}:aurorite_shield", f"{NS}:prismatic_wings",
                                           f"{NS}:lunar_silver_bow", f"{NS}:aurorite_bow", f"{NS}:lunar_silver_crossbow", f"{NS}:aurorite_crossbow"])
    tag("item", "ingots", [i], ns="c")
    tag("item", "ingots/lunar_silver", [i], ns="c")
    tag("item", "raw_materials", [f"{NS}:raw_lunar_silver"], ns="c")
    tag("item", "gems", [f"{NS}:aurorite_gem"], ns="c")
    tag("item", "ores", [f"{NS}:lunar_silver_ore", f"{NS}:deepslate_lunar_silver_ore", f"{NS}:aurorite_ore"], ns="c")

    # geração de minérios (frequência ajustável sobrescrevendo estes arquivos por datapack)
    ore_feature("lunar_silver_ore", [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": f"{NS}:lunar_silver_ore"}},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": f"{NS}:deepslate_lunar_silver_ore"}}],
        8, 7, -48, 48)
    ore_feature("aurorite_ore", [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": f"{NS}:aurorite_ore"}}],
        4, 2, -64, -16, trapezoid=False)
    print("assets da etapa 3 gerados")


if __name__ == "__main__":
    main()
