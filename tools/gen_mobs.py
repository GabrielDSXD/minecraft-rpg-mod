"""Etapa 4: texturas das criaturas/NPCs (UV iguais às de MobModels.java), itens de drop, ovos, loot tables, spawns e tags.
Uso: python tools/gen_mobs.py"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "alvorecer")
DATA = os.path.join(ROOT, "data")
NS = "alvorecer"
random.seed(4)


def rgba(c, a=255):
    return (int(c[0]), int(c[1]), int(c[2]), a if len(c) == 3 else c[3])


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def jitter(c, amount=10):
    return tuple(max(0, min(255, v + random.randint(-amount, amount))) for v in c[:3])


class Tex:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def px(self, x, y, c, a=255):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((int(x), int(y)), rgba(c, a))

    def rect(self, x0, y0, w, h, c, noise=8, a=255):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                self.px(x, y, jitter(c, noise) if noise else c, a)

    def box(self, u, v, w, h, d, color, top=None, bottom=None, front=None, noise=8, a=255):
        """Pinta as 6 faces de uma caixa (layout UV do Minecraft); front/top/bottom podem ser cores."""
        self.rect(u + d, v, w, d, top or shade(color, 1.1), noise, a)
        self.rect(u + d + w, v, w, d, bottom or shade(color, 0.8), noise, a)
        self.rect(u, v + d, d, h, shade(color, 0.9), noise, a)
        self.rect(u + d, v + d, w, h, front or color, noise, a)
        self.rect(u + d + w, v + d, d, h, shade(color, 0.9), noise, a)
        self.rect(u + 2 * d + w, v + d, w, h, shade(color, 0.85), noise, a)

    @staticmethod
    def front(u, v, d):
        return u + d, v + d

    def save(self, name):
        p = os.path.join(ASSETS, "textures", "entity", name + ".png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        self.img.save(p)


def humanoid(skin, tunic, sleeves, pants, boots, face, hat=None):
    """Textura 64x32 de humanoide (cabeça, corpo, braços, pernas) com rosto desenhado por função."""
    t = Tex(64, 32)
    t.box(0, 0, 8, 8, 8, skin)
    face(t, 8, 8)
    if hat:
        hat(t)
    t.box(16, 16, 8, 12, 4, tunic)
    t.box(40, 16, 4, 12, 4, sleeves)
    t.rect(44, 28, 4, 4, skin, 4)  # mãos
    t.rect(40, 28, 4, 4, shade(skin, 0.9), 4)
    t.rect(48, 28, 8, 4, shade(skin, 0.85), 4)
    t.box(0, 16, 4, 12, 4, pants)
    for x in range(0, 16):  # botas
        for y in range(29, 32):
            t.px(x, y, jitter(boots, 6))
    return t


def eyes(t, fx, fy, color, pupil=(20, 20, 20), y=3):
    t.px(fx + 1, fy + y, color)
    t.px(fx + 2, fy + y, pupil)
    t.px(fx + 5, fy + y, pupil)
    t.px(fx + 6, fy + y, color)


def goblin():
    skin = (96, 142, 62)

    def face(t, fx, fy):
        t.px(fx + 1, fy + 3, (250, 220, 60))
        t.px(fx + 2, fy + 3, (40, 30, 10))
        t.px(fx + 5, fy + 3, (40, 30, 10))
        t.px(fx + 6, fy + 3, (250, 220, 60))
        for x in range(2, 6):
            t.px(fx + x, fy + 6, (60, 30, 20))
        t.px(fx + 2, fy + 7, (240, 240, 220))
        t.px(fx + 5, fy + 7, (240, 240, 220))
        for x in range(1, 7):
            t.px(fx + x, fy + 2, shade(skin, 0.7))

    t = humanoid(skin, (112, 80, 46), (112, 80, 46), (74, 54, 34), skin, face)
    for x in range(20, 28):  # cinto
        t.px(x, 27, (50, 34, 20))
    t.px(23, 27, (220, 190, 60))
    t.rect(24, 0, 4, 3, skin, 6)  # nariz
    t.rect(56, 16, 8, 3, skin, 6)  # orelhas
    t.rect(56, 20, 8, 3, skin, 6)
    t.rect(44, 20, 4, 6, skin, 6)  # braços nus abaixo da manga
    t.save("goblin")


def skeleton_warrior():
    bone = (222, 216, 196)
    t = Tex(64, 32)
    t.box(0, 0, 8, 8, 8, bone, noise=6)
    fx, fy = 8, 8
    for x, y in ((1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4)):
        t.px(fx + x, fy + y, (25, 25, 30))
    t.px(fx + 2, fy + 4, (120, 220, 255))
    t.px(fx + 5, fy + 4, (120, 220, 255))
    for x in range(1, 7):
        t.px(fx + x, fy + 6, (40, 40, 40) if x % 2 else bone)
    # elmo enferrujado na camada de chapéu (frente aberta)
    steel = (128, 126, 122)
    t.box(32, 0, 8, 8, 8, steel, noise=14)
    for x in range(41, 47):
        for y in range(11, 16):
            t.px(x, y, (0, 0, 0), 0)
    for _ in range(25):
        t.px(random.randint(32, 63), random.randint(0, 15), (140, 80, 40))
    # tronco: costelas e tabardo vermelho
    t.box(16, 16, 8, 12, 4, (50, 46, 44), noise=4)
    for y in (21, 23, 25):
        for x in range(20, 28):
            t.px(x, y, bone)
    t.rect(22, 26, 4, 6, (150, 30, 30), 10)
    t.box(40, 16, 2, 12, 2, bone, noise=6)
    t.box(0, 16, 2, 12, 2, bone, noise=6)
    t.save("skeleton_warrior")


def npc(name, skin, tunic, sleeves, pants, boots, beard, hat_color=None, trim=(220, 184, 90)):
    def face(t, fx, fy):
        eyes(t, fx, fy, (240, 240, 240))
        for x in range(1, 7):
            t.px(fx + x, fy + 2, shade(skin, 0.6) if beard is None else beard)
        if beard is not None:
            for x in range(0, 8):
                for y in range(5, 8):
                    t.px(fx + x, fy + y, jitter(beard, 8))
            t.px(fx + 3, fy + 5, (120, 60, 50))
            t.px(fx + 4, fy + 5, (120, 60, 50))
        else:
            for x in range(3, 5):
                t.px(fx + x, fy + 6, (130, 70, 60))

    def hat(t):
        if hat_color:
            t.box(32, 0, 8, 8, 8, hat_color, noise=8)
            for x in range(41, 47):
                for y in range(11, 16):
                    t.px(x, y, (0, 0, 0), 0)

    t = humanoid(skin, tunic, sleeves, pants, boots, face, hat)
    for y in range(20, 32):  # faixa central da roupa
        t.px(23, y, trim)
        t.px(24, y, trim)
    t.save("npc_" + name)


def arcane_wolf():
    fur, belly, glow = (52, 58, 118), (90, 98, 160), (140, 210, 255)
    t = Tex(64, 32)
    t.box(0, 0, 6, 6, 10, fur, top=shade(fur, 1.15), bottom=belly)
    for x in range(10, 16):  # listras rúnicas no dorso
        t.px(x, 3, glow)
        t.px(x, 7, glow)
    t.box(32, 0, 6, 6, 5, fur)
    fx, fy = 37, 5
    t.px(fx + 1, fy + 2, glow)
    t.px(fx + 4, fy + 2, glow)
    t.box(32, 11, 3, 3, 3, belly)
    t.px(35, 14, (20, 20, 30))
    t.rect(54, 0, 6, 6, fur, 6)
    t.box(0, 16, 2, 8, 2, shade(fur, 0.8))
    t.box(8, 16, 2, 7, 2, fur)
    t.rect(10, 23, 2, 3, glow, 0)
    t.save("arcane_wolf")
    e = Tex(64, 32)  # brilho: olhos e runas
    for x, y in ((fx + 1, fy + 2), (fx + 4, fy + 2)):
        e.px(x, y, glow)
    for x in range(10, 16):
        e.px(x, 3, glow)
        e.px(x, 7, glow)
    e.rect(10, 23, 2, 3, glow, 0)
    e.save("arcane_wolf_eyes")


def elemental(name, core, crack, shard, eye):
    t = Tex(64, 32)
    t.box(0, 0, 8, 8, 8, core, noise=30)
    for _ in range(40):
        t.px(random.randint(0, 31), random.randint(0, 15), crack)
    fx, fy = 8, 8
    for x in (1, 2, 5, 6):
        t.px(fx + x, fy + 3, eye)
    t.box(32, 0, 4, 4, 4, shard, noise=20)
    t.box(0, 16, 4, 6, 4, shade(core, 0.9), noise=30)
    t.box(16, 16, 2, 4, 2, shade(core, 0.8), noise=30)
    t.save("elemental_" + name)


def stone_sentinel():
    stone, moss, rune = (128, 126, 120), (78, 112, 52), (246, 158, 70)
    t = Tex(128, 64)
    t.box(0, 0, 16, 20, 10, stone, noise=14)
    t.box(64, 0, 8, 8, 8, shade(stone, 1.05), noise=14)
    t.box(64, 16, 6, 24, 6, shade(stone, 0.95), noise=14)
    t.box(0, 40, 6, 11, 6, shade(stone, 0.9), noise=14)
    for y in range(0, 64, 5):  # juntas das pedras
        for x in range(0, 128):
            if t.img.getpixel((x, y))[3] and random.random() < 0.7:
                t.px(x, y, shade(stone, 0.6))
    for _ in range(140):  # musgo
        x, y = random.randint(0, 127), random.randint(0, 63)
        if t.img.getpixel((x, y))[3]:
            t.px(x, y, jitter(moss, 12))
    g = Tex(128, 64)
    fx, fy = 10, 10  # runa no peito (frente do tronco começa em u=10, v=10)
    pts = [(7, 4), (8, 4), (7, 5), (8, 5), (6, 6), (9, 6), (5, 8), (10, 8), (7, 9), (8, 9), (7, 11), (8, 11), (7, 12), (8, 12)]
    for x, y in pts:
        t.px(fx + x, fy + y, rune)
        g.px(fx + x, fy + y, rune)
    hx, hy = 72, 8  # olho da cabeça
    for x in range(1, 7):
        t.px(hx + x, hy + 3, rune)
        g.px(hx + x, hy + 3, rune)
    t.save("stone_sentinel")
    g.save("stone_sentinel_glow")


def fairy():
    t = Tex(32, 32)
    t.box(0, 0, 2, 4, 2, (240, 150, 210), noise=6)
    t.box(8, 0, 3, 3, 3, (250, 220, 200), top=(255, 230, 120), noise=4)
    t.px(12, 4, (60, 120, 220))
    t.px(13, 4, (60, 120, 220))
    for u in (0, 10):
        for x in range(u, u + 10):
            for y in range(13, 19):
                c = (190, 240, 255) if (x + y) % 3 else (255, 210, 245)
                t.px(x, y, c, 150)
    t.save("fairy")


def wyrmling():
    scale, belly, membrane, horn = (168, 54, 42), (230, 170, 90), (220, 110, 60), (230, 220, 190)
    t = Tex(64, 64)
    t.box(0, 0, 6, 5, 12, scale, bottom=belly, noise=14)
    for x in range(12, 18):  # crista
        t.px(x, 6, shade(scale, 0.6))
    t.box(0, 17, 3, 3, 4, scale, noise=12)
    t.box(14, 17, 5, 4, 5, scale, noise=12)
    t.px(14 + 1, 17 + 5 + 1, (255, 220, 40))  # olhos nas laterais da cabeça
    t.px(14 + 5 + 5 + 3, 17 + 5 + 1, (255, 220, 40))
    t.px(19 + 1, 22 + 1, (30, 20, 20))
    t.px(19 + 3, 22 + 1, (30, 20, 20))
    t.box(34, 17, 4, 1, 5, belly, noise=8)
    t.box(52, 17, 1, 2, 1, horn, noise=4)
    t.box(56, 17, 1, 2, 1, horn, noise=4)
    for v in (30, 38):
        for x in range(8, 32):
            for y in range(v, v + 8):
                c = membrane if (x - 8) % 4 else shade(scale, 0.6)
                t.px(x, y, jitter(c, 8))
    t.box(36, 0, 3, 3, 8, scale, noise=12)
    t.box(36, 11, 2, 2, 6, shade(scale, 0.9), noise=12)
    t.box(52, 24, 2, 6, 2, shade(scale, 0.8), noise=10)
    t.save("wyrmling")


# ---------------------------------------------------------------- itens de drop e cetro

def item(name, pixels):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in pixels.items():
        img.putpixel((x, y), rgba(c))
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3]:
                    img.putpixel((x, y), (24, 18, 22, 255))
                    break
    p = os.path.join(ASSETS, "textures", "item", name + ".png")
    img.save(p)


def blob(cx, cy, r, color, noise=12):
    out = {}
    for x in range(16):
        for y in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                out[(x, y)] = jitter(color, noise)
    return out


def items():
    ear = {(x, y): (96, 142, 62) for x in range(4, 12) for y in range(6, 10) if y - 6 <= (x - 4) // 2}
    ear.update({(x, 8): (70, 110, 46) for x in range(6, 11)})
    item("goblin_ear", ear)
    fang = {(7 + (y > 8) * 0, y): (240, 236, 220) for y in range(3, 13)}
    fang.update({(8, y): (200, 196, 180) for y in range(3, 11)})
    fang.update({(6, 3): (140, 210, 255), (9, 3): (140, 210, 255), (7, 2): (140, 210, 255)})
    item("arcane_fang", fang)
    core = blob(8, 8, 4, (246, 130, 60), 30)
    core.update(blob(8, 8, 2, (255, 230, 150), 10))
    item("elemental_core", core)
    heart = blob(8, 8, 5, (120, 118, 112), 14)
    for x, y in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 9), (8, 9), (6, 8), (9, 8)):
        heart[(x, y)] = (246, 158, 70)
    item("sentinel_heart", heart)
    dust = {}
    for _ in range(26):
        dust[(random.randint(4, 11), random.randint(5, 12))] = random.choice([(255, 230, 250), (200, 240, 255), (255, 250, 180)])
    item("fairy_dust", dust)
    sc = {}
    for x in range(4, 12):
        for y in range(4, 13):
            if abs(x - 7.5) + abs(y - 8) <= 5:
                sc[(x, y)] = jitter((168, 54, 42), 14) if (x + y) % 3 else (220, 120, 70)
    item("wyrmling_scale", sc)
    scepter = {(3 + i, 13 - i): ((150, 112, 62) if i % 2 else (108, 78, 40)) for i in range(9)}
    scepter.update(blob(12, 3, 2, (190, 240, 120), 10))
    scepter[(12, 3)] = (255, 255, 255)
    item("bestiary_scepter", scepter)


# ---------------------------------------------------------------- dados

def jdump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


def loot(entity, entries):
    pools = []
    for name, lo, hi, chance, looting in entries:
        funcs = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        if looting:
            funcs.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                          "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
        pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": name, "functions": funcs}]}
        if chance < 1:
            pool["conditions"] = [{"condition": "minecraft:killed_by_player"},
                                  {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                   "unenchanted_chance": chance, "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.02, "per_level_above_first": 0.02}}]
        pools.append(pool)
    jdump(os.path.join(DATA, NS, "loot_table", "entities", entity + ".json"), {"type": "minecraft:entity", "pools": pools})


def spawns(name, entity, biomes, weight, lo, hi):
    jdump(os.path.join(DATA, NS, "neoforge", "biome_modifier", f"spawn_{name}.json"), {
        "type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": {"type": f"{NS}:{entity}", "weight": weight, "minCount": lo, "maxCount": hi}})


def main():
    goblin()
    skeleton_warrior()
    arcane_wolf()
    elemental("fire", (232, 96, 34), (120, 30, 10), (255, 200, 80), (255, 255, 210))
    elemental("frost", (120, 190, 240), (40, 90, 150), (220, 245, 255), (255, 255, 255))
    stone_sentinel()
    fairy()
    wyrmling()
    npc("guild_master", (214, 170, 130), (110, 76, 40), (40, 52, 110), (90, 64, 36), (30, 24, 20), (110, 70, 40))
    npc("merchant", (190, 140, 100), (46, 108, 58), (46, 108, 58), (120, 100, 70), (60, 44, 30), None, hat_color=(46, 108, 58))
    npc("sage", (226, 196, 170), (70, 52, 130), (70, 52, 130), (70, 52, 130), (40, 30, 70), (235, 235, 235), hat_color=(70, 52, 130),
        trim=(250, 230, 120))
    items()
    for n in ("goblin_ear", "arcane_fang", "elemental_core", "sentinel_heart", "fairy_dust", "wyrmling_scale"):
        jdump(os.path.join(ASSETS, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{n}"}})
    jdump(os.path.join(ASSETS, "models", "item", "bestiary_scepter.json"),
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/bestiary_scepter"}})
    for e in ("goblin", "skeleton_warrior", "arcane_wolf", "elemental", "stone_sentinel", "fairy", "wyrmling", "guild_master", "merchant", "sage"):
        jdump(os.path.join(ASSETS, "models", "item", f"{e}_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})

    loot("goblin", [(f"{NS}:goblin_ear", 1, 2, 1, True), ("minecraft:gold_nugget", 0, 3, 1, True)])
    loot("skeleton_warrior", [("minecraft:bone", 1, 3, 1, True), (f"{NS}:raw_lunar_silver", 1, 1, 0.15, False)])
    loot("arcane_wolf", [(f"{NS}:arcane_fang", 0, 1, 1, True), ("minecraft:leather", 0, 2, 1, True)])
    loot("elemental", [(f"{NS}:elemental_core", 1, 1, 0.5, False), ("minecraft:blaze_powder", 0, 2, 1, True)])
    loot("stone_sentinel", [(f"{NS}:sentinel_heart", 1, 1, 1, False), ("minecraft:cobblestone", 4, 10, 1, False),
                            (f"{NS}:aurorite_shard", 1, 2, 0.3, False)])
    loot("fairy", [(f"{NS}:fairy_dust", 1, 2, 1, True)])
    loot("wyrmling", [(f"{NS}:wyrmling_scale", 1, 3, 1, True), (f"{NS}:aurorite_gem", 1, 1, 0.1, False)])
    for npc_name in ("guild_master", "merchant", "sage"):
        jdump(os.path.join(DATA, NS, "loot_table", "entities", npc_name + ".json"), {"type": "minecraft:entity", "pools": []})

    spawns("goblin", "goblin", ["minecraft:plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:savanna", "minecraft:dark_forest"], 40, 2, 4)
    spawns("skeleton_warrior", "skeleton_warrior", "#minecraft:is_overworld", 20, 1, 2)
    spawns("arcane_wolf", "arcane_wolf", "#minecraft:is_taiga", 35, 2, 3)
    spawns("elemental", "elemental", ["minecraft:desert", "minecraft:badlands", "minecraft:snowy_plains", "minecraft:ice_spikes",
                                      "minecraft:snowy_taiga", "minecraft:frozen_peaks"], 25, 1, 1)
    spawns("stone_sentinel", "stone_sentinel", "#minecraft:is_mountain", 6, 1, 1)
    spawns("fairy", "fairy", ["minecraft:flower_forest", "minecraft:meadow", "minecraft:cherry_grove", "minecraft:sunflower_plains"], 12, 2, 3)
    spawns("wyrmling", "wyrmling", ["minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:badlands", "minecraft:windswept_hills"], 10, 1, 1)

    def tag(kind, name, values):
        jdump(os.path.join(DATA, "minecraft", "tags", kind, name + ".json"), {"replace": False, "values": values})

    tag("entity_type", "undead", [f"{NS}:skeleton_warrior"])
    tag("entity_type", "skeletons", [f"{NS}:skeleton_warrior"])
    tag("entity_type", "sensitive_to_smite", [f"{NS}:skeleton_warrior"])
    tag("entity_type", "fall_damage_immune", [f"{NS}:fairy", f"{NS}:wyrmling", f"{NS}:elemental"])
    print("assets da etapa 4 gerados")


if __name__ == "__main__":
    main()
