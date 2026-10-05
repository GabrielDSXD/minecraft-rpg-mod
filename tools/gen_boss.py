"""Etapa 7: Guardião do Primeiro Alvorecer (texturas com UV de GuardianModel.java), Coração, Sigilo, Essência, Altar,
saque, receitas e conquista. Uso: python tools/gen_boss.py (depois rode gen_world.py para o altar na Arena Celeste)."""
import os
import random

from PIL import Image

from gen_mobs import ASSETS, NS, Tex, jitter, shade
from gen_world import data, jdump

random.seed(77)
GOLD, STONE, GLOW, CORE = (214, 178, 96), (232, 222, 196), (255, 170, 60), (255, 236, 170)


def guardian():
    t = Tex(128, 128)
    t.box(0, 0, 20, 24, 12, STONE, noise=12)          # tronco
    t.box(104, 0, 6, 6, 2, GOLD, noise=6)             # núcleo
    t.box(64, 0, 10, 10, 10, shade(STONE, 1.05), noise=10)  # cabeça
    t.box(64, 24, 12, 2, 2, GOLD, noise=6)            # coroa
    t.box(0, 104, 18, 2, 1, GOLD, noise=6)            # auréola
    t.box(40, 104, 2, 14, 1, GOLD, noise=6)
    t.box(0, 36, 8, 30, 8, shade(STONE, 0.95), noise=12)   # braço
    t.box(64, 30, 10, 9, 10, GOLD, noise=10)          # punho
    t.box(32, 36, 8, 22, 8, shade(STONE, 0.9), noise=12)   # perna
    # Faixas douradas nas placas de pedra
    for y in range(0, 74, 6):
        for x in range(0, 64):
            if t.img.getpixel((x, y))[3] and random.random() < 0.8:
                t.px(x, y, jitter(GOLD, 8))
    g = Tex(128, 128)   # brilho normal
    c = Tex(128, 128)   # brilho durante a carga (mais forte)

    def glow(x, y, col=GLOW):
        t.px(x, y, col)
        g.px(x, y, col)
        c.px(x, y, CORE)

    fx, fy = 12, 12  # frente do tronco: runas em V
    for i in range(10):
        glow(fx + 3 + i // 2, fy + 2 + i)
        glow(fx + 16 - i // 2, fy + 2 + i)
    for x in range(106, 112):  # frente do núcleo
        for y in range(2, 8):
            glow(x, y, CORE)
    hx, hy = 74, 10  # olhos
    for x in (2, 3, 6, 7):
        glow(hx + x, hy + 4, CORE)
    for u, v, w, h in ((0, 104, 38, 3), (40, 104, 6, 15)):  # auréola inteira brilha
        for x in range(u, u + w):
            for y in range(v, v + h):
                if t.img.getpixel((x, y))[3]:
                    glow(x, y, GLOW if (x + y) % 3 else CORE)
    for y in range(44, 72, 3):  # veios nos braços
        glow(10 + (y % 5), y)
    for x in range(0, 128):  # na carga tudo o que é dourado acende
        for y in range(0, 128):
            p = t.img.getpixel((x, y))
            if p[3] and p[0] > 190 and p[2] < 140 and not c.img.getpixel((x, y))[3]:
                c.px(x, y, (255, 200, 90), 200)
    t.save("dawn_guardian")
    g.save("dawn_guardian_glow")
    c.save("dawn_guardian_charge")


def item_tex(name, draw):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(img)
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
    jdump(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def heart(img):
    for x in range(16):
        for y in range(16):
            u, v = (x - 7.5) / 6.5, (8 - y) / 6.5
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                d = (u * u + (v - 0.2) ** 2) ** 0.5
                img.putpixel((x, y), (255, int(140 + 100 * max(0, 1 - d)), int(40 + 150 * max(0, 1 - d * 1.6)), 255))
    for x, y in ((5, 4), (6, 4), (5, 5)):
        img.putpixel((x, y), (255, 250, 230, 255))


def sigil(img):
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 <= d <= 7.2:
                img.putpixel((x, y), (226, 180, 80, 255))
            elif d < 5.2 and (abs(x - 7.5) < 1 or abs(y - 7.5) < 1 or abs(abs(x - 7.5) - abs(y - 7.5)) < 0.8):
                img.putpixel((x, y), (255, 210, 120, 255))
    img.putpixel((7, 7), (255, 255, 230, 255))
    img.putpixel((8, 8), (255, 255, 230, 255))


def essence(img):
    for x in range(16):
        for y in range(16):
            d = abs(x - 7.5) + abs(y - 8) * 0.7
            if d < 5:
                img.putpixel((x, y), (255, int(200 + 11 * (5 - d)) if d < 5 else 0, int(120 + 25 * (5 - d)), int(150 + 20 * (5 - d))))


def altar():
    top = Image.new("RGBA", (16, 16))
    side = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            j = random.randint(-8, 8)
            side.putpixel((x, y), (232 + j // 2, 214 + j, 170 + j, 255))
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            col = (255, 190, 80) if 4.5 <= d <= 5.6 or d < 1.6 else (226 + j, 200 + j, 150 + j)
            top.putpixel((x, y), col + (255,))
    for x in range(16):
        side.putpixel((x, 0), (255, 200, 90, 255))
        side.putpixel((x, 15), (170, 130, 60, 255))
        if x % 4 == 1:
            for y in range(5, 11):
                side.putpixel((x, y), (255, 176, 60, 255))
    base = os.path.join(ASSETS, "textures", "block")
    top.save(os.path.join(base, "dawn_altar_top.png"))
    side.save(os.path.join(base, "dawn_altar_side.png"))
    jdump(os.path.join(ASSETS, "models", "block", "dawn_altar.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/dawn_altar_top", "bottom": f"{NS}:block/dawnstone", "side": f"{NS}:block/dawn_altar_side"}})
    jdump(os.path.join(ASSETS, "blockstates", "dawn_altar.json"), {"variants": {"": {"model": f"{NS}:block/dawn_altar"}}})
    jdump(os.path.join(ASSETS, "models", "item", "dawn_altar.json"), {"parent": f"{NS}:block/dawn_altar"})


def data_files():
    jdump(os.path.join(ASSETS, "models", "item", "dawn_guardian_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})

    def item(name, lo, hi, chance=1.0):
        e = {"type": "minecraft:item", "name": name, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
        pool = {"rolls": 1, "entries": [e]}
        if chance < 1:
            pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        return pool

    jdump(data(NS, "loot_table", "entities", "dawn_guardian.json"), {"type": "minecraft:entity", "pools": [
        item(f"{NS}:dawn_essence", 4, 8), item(f"{NS}:aurorite_gem", 2, 4), item(f"{NS}:sentinel_heart", 1, 2),
        item("minecraft:diamond", 2, 5), item(f"{NS}:dawn_key", 1, 1, 0.5)]})
    jdump(data(NS, "recipe", "dawn_sigil.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
                                                  "pattern": [" F ", "FGF", " F "],
                                                  "key": {"F": {"item": f"{NS}:dawn_sigil_fragment"}, "G": {"item": f"{NS}:aurorite_gem"}},
                                                  "result": {"id": f"{NS}:dawn_sigil", "count": 1}})
    jdump(data(NS, "recipe", "dawn_sigil_from_essence.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
                                                               "pattern": ["EFE", "EGE", "EFE"],
                                                               "key": {"E": {"item": f"{NS}:dawn_essence"}, "F": {"item": f"{NS}:dawn_sigil_fragment"},
                                                                       "G": {"item": f"{NS}:aurorite_gem"}},
                                                               "result": {"id": f"{NS}:dawn_sigil", "count": 1}})
    jdump(data(NS, "advancement", "guardian.json"), {"parent": f"{NS}:dungeon", "display": {
        "icon": {"id": f"{NS}:heart_of_dawn"}, "title": {"translate": "advancement.alvorecer.guardian"},
        "description": {"translate": "advancement.alvorecer.guardian.desc"}, "frame": "challenge", "show_toast": True,
        "announce_to_chat": True, "hidden": False}, "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def main():
    guardian()
    item_tex("heart_of_dawn", heart)
    item_tex("dawn_sigil", sigil)
    item_tex("dawn_essence", essence)
    altar()
    data_files()
    print("dados da etapa 7 gerados")


if __name__ == "__main__":
    main()
