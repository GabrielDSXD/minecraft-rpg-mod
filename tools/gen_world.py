"""Etapa 5: Reinos do Éter (dimensão, biomas, minérios), Portal do Alvorecer (texturas/modelos/receitas) e estruturas.
Uso: python tools/gen_world.py"""
import json
import math
import os
import random

from PIL import Image

from nbt import Byte, Float, List, Template, DOUBLE, INT, COMPOUND, FLOAT

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "alvorecer")
DATA = os.path.join(ROOT, "data")
NS = "alvorecer"
random.seed(11)


def jdump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


def data(*parts):
    return os.path.join(DATA, *parts)


# ---------------------------------------------------------------- dimensão e biomas

AETHER_BIOMES = ["aether_meadows", "luminous_forest", "crystal_highlands", "ruined_expanse"]


def spawn(entity, weight, lo, hi):
    return {"type": entity, "weight": weight, "minCount": lo, "maxCount": hi}


# Temperatura >= 0.6: as ilhas chegam a y~250 e o jogo esfria 0.05 a cada 40 blocos acima de y=80 (abaixo de 0.15 neva)
def biome(name, temp, downfall, sky, fog, grass, foliage, vegetal, ores, monsters, creatures, particle=None, local=None, decoration=None):
    features = [[] for _ in range(11)]
    features[2] = local or []
    features[6] = ores
    features[7] = decoration or []
    features[9] = vegetal
    features[10] = ["minecraft:freeze_top_layer"]
    effects = {"sky_color": sky, "fog_color": fog, "water_color": 0x4FC3F7, "water_fog_color": 0x0B3D5C,
               "grass_color": grass, "foliage_color": foliage}
    if particle:
        effects["particle"] = {"options": {"type": particle[0]}, "probability": particle[1]}
    jdump(data(NS, "worldgen", "biome", name + ".json"), {
        "has_precipitation": True, "temperature": temp, "downfall": downfall, "effects": effects,
        "spawners": {"monster": monsters, "creature": creatures, "ambient": [], "axolotls": [], "underground_water_creature": [],
                     "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {}, "carvers": {}, "features": features})


def dimension():
    jdump(data(NS, "dimension_type", "aether_realms.json"), {
        "ultrawarm": False, "natural": True, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.1, "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7},
        "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False,
        "has_raids": False, "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": f"{NS}:aether"})
    params = {
        "aether_meadows": {"temperature": [-0.2, 0.4], "humidity": [-1.0, 0.1]},
        "luminous_forest": {"temperature": [-0.2, 0.4], "humidity": [0.1, 1.0]},
        "crystal_highlands": {"temperature": [-1.0, -0.2], "humidity": [-1.0, 1.0]},
        "ruined_expanse": {"temperature": [0.4, 1.0], "humidity": [-1.0, 1.0]},
    }
    jdump(data(NS, "dimension", "aether_realms.json"), {
        "type": f"{NS}:aether_realms",
        "generator": {"type": "minecraft:noise", "settings": "minecraft:floating_islands",
                      "biome_source": {"type": "minecraft:multi_noise", "biomes": [
                          {"biome": f"{NS}:{b}", "parameters": {**p, "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0,
                                                                "depth": 0.0, "offset": 0.0}} for b, p in params.items()]}}})
    ores = [f"{NS}:aether_lunar_silver_ore", f"{NS}:aether_aurorite_ore", "minecraft:ore_iron_upper", "minecraft:ore_coal_upper"]
    sky = [(f"{NS}:fairy", 12, 2, 3)]
    mon = lambda *xs: [spawn(*x) for x in xs]
    biome("aether_meadows", 0.6, 0.5, 0x9FD8FF, 0xFFE9C2, 0x8EE07A, 0x6CC060,
          ["minecraft:flower_meadow", "minecraft:patch_grass_plain", "minecraft:trees_meadow"], ores,
          mon(("alvorecer:arcane_wolf", 40, 2, 3), ("alvorecer:goblin", 30, 2, 4)), mon(*sky), ("minecraft:end_rod", 0.0015))
    biome("luminous_forest", 0.6, 0.8, 0xB7A6FF, 0xF2D7FF, 0x7BD9A6, 0x9F7BE0,
          ["minecraft:trees_cherry", "minecraft:flower_cherry", "minecraft:patch_grass_forest"], ores,
          mon(("alvorecer:arcane_wolf", 50, 2, 3), ("alvorecer:elemental", 20, 1, 1)), mon(*sky), ("minecraft:end_rod", 0.005),
          decoration=["minecraft:glow_lichen"])
    biome("crystal_highlands", 0.6, 0.3, 0xA8E6FF, 0xE0F4FF, 0x9ADBC8, 0x7FC9B0,
          ["minecraft:patch_grass_plain"], ores + [f"{NS}:aether_aurorite_ore_rich"],
          mon(("alvorecer:elemental", 40, 1, 1), ("alvorecer:stone_sentinel", 10, 1, 1), ("alvorecer:wyrmling", 15, 1, 1)), [],
          ("minecraft:white_ash", 0.01), local=["minecraft:amethyst_geode"])
    biome("ruined_expanse", 0.9, 0.2, 0xFFD39A, 0xFFE2B8, 0xC9C27A, 0xB0A85E,
          ["minecraft:trees_plains", "minecraft:patch_dead_bush"], ores,
          mon(("alvorecer:skeleton_warrior", 50, 1, 2), ("alvorecer:wyrmling", 20, 1, 1), ("alvorecer:goblin", 30, 2, 4)), [])
    for name, block, size, count, lo, hi in (("aether_lunar_silver_ore", "lunar_silver_ore", 9, 10, 0, 160),
                                              ("aether_aurorite_ore", "aurorite_ore", 5, 4, 0, 120),
                                              ("aether_aurorite_ore_rich", "aurorite_ore", 5, 6, 0, 120)):
        jdump(data(NS, "worldgen", "configured_feature", name + ".json"), {"type": "minecraft:ore", "config": {
            "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": {"Name": f"{NS}:{block}"}}]}})
        jdump(data(NS, "worldgen", "placed_feature", name + ".json"), {"feature": f"{NS}:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo},
                                                          "max_inclusive": {"absolute": hi}}}, {"type": "minecraft:biome"}]})
    jdump(data(NS, "tags", "worldgen", "biome", "is_aether.json"), {"values": [f"{NS}:{b}" for b in AETHER_BIOMES]})


# ---------------------------------------------------------------- estruturas

def chest(t, x, y, z, loot, facing="north"):
    t.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
          {"LootTable": f"{NS}:chests/{loot}", "LootTableSeed": random.randint(1, 2 ** 31 - 1)})


def goblin_camp():
    t = Template(13, 6, 13)
    for x in range(13):
        for z in range(13):
            if (x - 6) ** 2 + (z - 6) ** 2 <= 36:
                t.set(x, 0, z, random.choice(["minecraft:coarse_dirt", "minecraft:dirt_path", "minecraft:coarse_dirt", "minecraft:gravel"]))
    for (x0, z0, wool) in ((1, 1, "minecraft:green_wool"), (8, 7, "minecraft:brown_wool")):
        for i in range(4):  # barraca em A
            for dz in range(4):
                t.set(x0 + i, 1 + min(i, 3 - i), z0 + dz, wool)
            t.set(x0 + 1, 3, z0 + i, wool)
            t.set(x0 + 2, 3, z0 + i, wool)
    t.set(6, 1, 6, "minecraft:campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    for x, z in ((4, 6), (8, 6), (6, 4), (6, 8)):
        t.set(x, 1, z, "minecraft:oak_log", {"axis": "x" if z == 6 else "z"})
    t.set(10, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    t.set(2, 1, 9, "minecraft:oak_fence")
    t.set(2, 2, 9, "minecraft:skeleton_skull", {"rotation": "4"})
    chest(t, 3, 1, 9, "goblin_camp", "east")
    for x, z in ((5, 3), (4, 8), (8, 4)):
        t.entity(x, 1, z, f"{NS}:goblin")
    return t


def watch_tower():
    t = Template(7, 17, 7)
    bricks = ["minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks"]
    for y in range(0, 14):
        for x in range(7):
            for z in range(7):
                edge = x in (0, 6) or z in (0, 6)
                if y == 0 or (edge and not (x in (0, 6) and z in (0, 6)) or edge and y < 14):
                    t.set(x, y, z, random.choice(bricks))
    for y in (5, 9, 13):  # pisos
        t.fill(1, y, 1, 5, y, 5, "minecraft:spruce_planks")
        t.set(5, y, 1, "minecraft:air")
    for y in range(1, 14):  # escada
        t.set(5, y, 1, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    t.fill(3, 1, 0, 3, 2, 0, "minecraft:air")  # porta
    for y in (3, 7, 11):  # janelas
        for x, z in ((3, 6), (0, 3), (6, 3)):
            t.set(x, y, z, "minecraft:air")
    for x in range(7):  # ameias
        for z in range(7):
            if (x in (0, 6) or z in (0, 6)) and (x + z) % 2 == 0:
                t.set(x, 14, z, random.choice(bricks))
    chest(t, 2, 14, 2, "watch_tower")
    t.set(1, 6, 4, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    t.entity(3, 14, 4, f"{NS}:skeleton_warrior")
    t.entity(4, 10, 3, f"{NS}:skeleton_warrior")
    t.entity(2, 1, 3, f"{NS}:skeleton_warrior")
    return t


def dawn_shrine():
    t = Template(15, 9, 15)
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - 7, z - 7)
            if d <= 7.2:
                t.set(x, 0, z, f"{NS}:dawnstone" if d > 5.5 or (x + z) % 4 == 0 else "minecraft:smooth_quartz")
    for x, z in ((2, 2), (12, 2), (2, 12), (12, 12)):
        t.fill(x, 1, z, x, 5, z, f"{NS}:dawnstone")
        t.set(x, 6, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    for x in range(2, 13):
        for z in (2, 12):
            t.set(x, 6, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
    t.fill(6, 1, 6, 8, 1, 8, f"{NS}:dawnstone")
    chest(t, 7, 2, 7, "shrine", "south")
    t.set(4, 1, 10, f"{NS}:quest_board")
    t.set(10, 1, 10, f"{NS}:profession_workbench")
    t.entity(7, 1, 4, f"{NS}:sage", {"Rotation": List(FLOAT, [Float(0), Float(0)])})
    t.entity(4, 1, 7, f"{NS}:merchant")
    t.entity(10, 1, 7, f"{NS}:guild_master")
    return t


def aether_ruins():
    t = Template(11, 8, 11)
    for x in range(11):
        for z in range(11):
            if random.random() < 0.7:
                t.set(x, 0, z, random.choice(["minecraft:smooth_quartz", f"{NS}:dawnstone", "minecraft:chiseled_quartz_block"]))
    for x, z, h in ((1, 1, 6), (9, 1, 3), (1, 9, 4), (9, 9, 7), (5, 1, 2)):
        for y in range(1, h + 1):
            t.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
        if h > 4:
            t.set(x, h + 1, z, f"{NS}:dawnstone")
    for _ in range(12):
        t.set(random.randint(0, 10), 1, random.randint(0, 10), random.choice(["minecraft:quartz_stairs", "minecraft:smooth_quartz_slab"]))
    chest(t, 5, 1, 5, "aether_ruins")
    t.entity(3, 1, 6, f"{NS}:skeleton_warrior")
    t.entity(7, 1, 4, f"{NS}:skeleton_warrior")
    return t


def sky_arena():
    t = Template(27, 11, 27)
    c = 13
    for x in range(27):
        for z in range(27):
            d = math.hypot(x - c, z - c)
            if d <= 13:
                t.set(x, 0, z, f"{NS}:dawnstone" if d > 11.5 or int(d) % 4 == 0 else "minecraft:smooth_quartz")
            if 12.5 <= d <= 13:
                t.set(x, 1, z, "minecraft:quartz_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for i in range(8):  # pilares que servem de cobertura contra a descarga do chefe
        a = math.tau * i / 8
        px, pz = round(c + math.cos(a) * 8), round(c + math.sin(a) * 8)
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(1, 8):
                    t.set(px + dx, y, pz + dz, "minecraft:quartz_pillar" if y < 7 else f"{NS}:dawnstone", {"axis": "y"} if y < 7 else None)
    t.fill(c - 1, 1, c - 1, c + 1, 1, c + 1, f"{NS}:dawnstone")
    t.set(c, 2, c, f"{NS}:dawn_altar")  # Sigilo do Primeiro Alvorecer desperta o Guardião aqui
    t.entity(c + 4, 1, c, f"{NS}:stone_sentinel")
    t.entity(c - 4, 1, c, f"{NS}:stone_sentinel")
    return t


STRUCTURES = {
    # nome: (função, tipo de configuração, biomas, espaçamento, separação, salt, ajuste de terreno, deslocamento y)
    "goblin_camp": (goblin_camp, "camp", ["minecraft:plains", "minecraft:forest", "minecraft:savanna", "minecraft:birch_forest", "minecraft:meadow"], 28, 10, 7311042, "beard_thin", -1),
    "watch_tower": (watch_tower, "tower", ["#minecraft:is_forest", "minecraft:plains", "minecraft:savanna", "#minecraft:is_taiga", "minecraft:windswept_hills"], 40, 16, 7311043, "beard_box", -1),
    "dawn_shrine": (dawn_shrine, "shrine", ["minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains", "minecraft:flower_forest", "minecraft:cherry_grove"], 36, 14, 7311044, "beard_thin", -1),
    "aether_ruins": (aether_ruins, "ruins", [f"#{NS}:is_aether"], 20, 8, 7311045, "none", -1),
    "sky_arena": (sky_arena, "arena", [f"#{NS}:is_aether"], 48, 20, 7311046, "none", -1),
}


def structures():
    for name, (fn, kind, biomes, spacing, sep, salt, adapt, yoff) in STRUCTURES.items():
        path = data(NS, "structure", name + ".nbt")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        jdump(data(NS, "tags", "worldgen", "biome", "has_structure", name + ".json"), {"values": biomes})
        jdump(data(NS, "worldgen", "structure", name + ".json"), {
            "type": f"{NS}:template", "biomes": f"#{NS}:has_structure/{name}", "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": adapt, "template": f"{NS}:{name}", "kind": kind, "y_offset": yoff})
        jdump(data(NS, "worldgen", "structure_set", name + ".json"), {
            "structures": [{"structure": f"{NS}:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})


def loot_tables():
    def table(name, pools):
        jdump(data(NS, "loot_table", "chests", name + ".json"), {"type": "minecraft:chest", "pools": pools})

    def pool(rolls, entries):
        return {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
            {"type": "minecraft:item", "name": n, "weight": w, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
            for n, w, lo, hi in entries]}

    table("goblin_camp", [pool((3, 6), [("minecraft:gold_nugget", 20, 2, 8), ("minecraft:bread", 15, 1, 4), ("minecraft:arrow", 15, 4, 12),
                                        (f"{NS}:raw_lunar_silver", 10, 1, 3), ("minecraft:emerald", 8, 1, 2), (f"{NS}:iron_dagger", 4, 1, 1),
                                        (f"{NS}:goblin_ear", 10, 1, 3)])])
    table("watch_tower", [pool((3, 6), [("minecraft:iron_ingot", 15, 1, 4), (f"{NS}:lunar_silver_ingot", 12, 1, 3), ("minecraft:arrow", 15, 6, 16),
                                        ("minecraft:bone", 10, 2, 6), (f"{NS}:mana_elixir", 8, 1, 2), (f"{NS}:lunar_silver_sword", 3, 1, 1),
                                        (f"{NS}:wanderer_boots", 4, 1, 1), (f"{NS}:iron_spear", 5, 1, 1)])])
    table("shrine", [pool((2, 5), [(f"{NS}:mana_elixir", 15, 1, 2), (f"{NS}:vigor_elixir", 15, 1, 2), (f"{NS}:healing_salve", 15, 1, 3),
                                   (f"{NS}:fairy_dust", 10, 2, 5), (f"{NS}:aurorite_shard", 6, 1, 2), (f"{NS}:dawnstone", 10, 4, 8),
                                   ("minecraft:golden_apple", 2, 1, 1)])])
    table("aether_ruins", [pool((3, 6), [(f"{NS}:aurorite_shard", 15, 1, 3), (f"{NS}:aurorite_gem", 6, 1, 1), (f"{NS}:elemental_core", 6, 1, 1),
                                         (f"{NS}:lunar_silver_ingot", 12, 2, 4), (f"{NS}:fairy_dust", 10, 2, 4), (f"{NS}:dawnstone", 10, 4, 10),
                                         ("minecraft:diamond", 3, 1, 2), (f"{NS}:dawn_key", 2, 1, 1)])])


# ---------------------------------------------------------------- texturas, modelos e receitas

def textures():
    d = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            base = (232, 204, 140) if (x // 4 + y // 4) % 2 else (222, 190, 120)
            j = random.randint(-10, 10)
            d.putpixel((x, y), (base[0] + j, base[1] + j, base[2] + j // 2, 255))
    for i in range(16):
        d.putpixel((i, 0), (250, 236, 190, 255))
        d.putpixel((i, 15), (170, 130, 70, 255))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8), (6, 7), (9, 8), (7, 6), (8, 9)):
        d.putpixel((x, y), (255, 168, 60, 255))
    os.makedirs(os.path.join(ASSETS, "textures", "block"), exist_ok=True)
    d.save(os.path.join(ASSETS, "textures", "block", "dawnstone.png"))
    frames = 16
    p = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        for x in range(16):
            for y in range(16):
                v = (math.sin((x + f) * 0.6) + math.cos((y - f) * 0.5) + math.sin((x + y + 2 * f) * 0.3)) / 3
                r, g, b = 255, int(190 + 50 * v), int(110 + 90 * v)
                p.putpixel((x, f * 16 + y), (r, max(0, min(255, g)), max(0, min(255, b)), 190))
    p.save(os.path.join(ASSETS, "textures", "block", "dawn_portal.png"))
    with open(os.path.join(ASSETS, "textures", "block", "dawn_portal.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)
    k = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    gold, gem = (236, 190, 70, 255), (246, 158, 70, 255)
    for i in range(7):
        k.putpixel((3 + i, 12 - i), gold)
    for x, y in ((10, 3), (11, 4), (12, 5), (11, 2), (12, 3), (13, 4), (11, 3), (12, 4)):
        k.putpixel((x, y), gem)
    k.putpixel((12, 4), (255, 240, 200, 255))
    k.putpixel((4, 12), gold)
    k.putpixel((5, 13), gold)
    k.putpixel((3, 10), gold)
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    k.save(os.path.join(ASSETS, "textures", "item", "dawn_key.png"))

    jdump(os.path.join(ASSETS, "blockstates", "dawnstone.json"), {"variants": {"": {"model": f"{NS}:block/dawnstone"}}})
    jdump(os.path.join(ASSETS, "models", "block", "dawnstone.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/dawnstone"}})
    jdump(os.path.join(ASSETS, "models", "item", "dawnstone.json"), {"parent": f"{NS}:block/dawnstone"})
    jdump(os.path.join(ASSETS, "blockstates", "dawn_portal.json"), {"variants": {
        "axis=x": {"model": f"{NS}:block/dawn_portal_ns"}, "axis=z": {"model": f"{NS}:block/dawn_portal_ew"}}})
    for m in ("ns", "ew"):
        jdump(os.path.join(ASSETS, "models", "block", f"dawn_portal_{m}.json"), {
            "parent": f"minecraft:block/nether_portal_{m}", "render_type": "minecraft:translucent",
            "textures": {"particle": f"{NS}:block/dawn_portal", "portal": f"{NS}:block/dawn_portal"}})
    jdump(os.path.join(ASSETS, "models", "item", "dawn_key.json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/dawn_key"}})
    jdump(data(NS, "recipe", "dawnstone.json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SSS", "SGS", "SSS"],
                                                 "key": {"S": {"item": "minecraft:stone_bricks"}, "G": {"item": f"{NS}:aurorite_gem"}},
                                                 "result": {"id": f"{NS}:dawnstone", "count": 8}})
    jdump(data(NS, "recipe", "dawn_key.json"), {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" G ", " I ", " F "],
                                                "key": {"G": {"item": f"{NS}:aurorite_gem"}, "I": {"item": "minecraft:gold_ingot"},
                                                        "F": {"item": f"{NS}:fairy_dust"}}, "result": {"id": f"{NS}:dawn_key", "count": 1}})
    jdump(data(NS, "loot_table", "blocks", "dawnstone.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:dawnstone"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tag_path = data("minecraft", "tags", "block", "mineable", "pickaxe.json")
    with open(tag_path, encoding="utf-8") as fh:
        tag = json.load(fh)
    if f"{NS}:dawnstone" not in tag["values"]:
        tag["values"].append(f"{NS}:dawnstone")
    jdump(tag_path, tag)


def main():
    dimension()
    structures()
    loot_tables()
    textures()
    print("dados da etapa 5 gerados")


if __name__ == "__main__":
    main()
