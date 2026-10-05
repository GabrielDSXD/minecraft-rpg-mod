"""Etapa 6: masmorras (moldes das salas, estrutura, saque, texturas e modelos dos blocos de mecanismo).
Uso: python tools/gen_dungeon.py"""
import os
import random

from PIL import Image

from gen_world import ASSETS, NS, data, jdump
from nbt import Template

random.seed(66)
W, H = 15, 9  # sala 15×9×15 (paredes incluídas); y=0 piso, y=8 teto

BRICKS = ["minecraft:stone_bricks"] * 5 + ["minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks"]
DEEP = ["minecraft:deepslate_bricks"] * 4 + ["minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles"]


def chest(t, x, y, z, loot, facing="north"):
    t.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
          {"LootTable": f"{NS}:{loot}", "LootTableSeed": random.randint(1, 2 ** 31 - 1)})


def shell(floor=BRICKS, wall=BRICKS):
    t = Template(W, H, W)
    for x in range(W):
        for z in range(W):
            t.set(x, 0, z, random.choice(floor))
            t.set(x, H - 1, z, random.choice(wall))
            for y in range(1, H - 1):
                if x in (0, W - 1) or z in (0, W - 1):
                    t.set(x, y, z, random.choice(wall))
    for x, z in ((2, 2), (12, 2), (2, 12), (12, 12), (7, 2), (7, 12), (2, 7), (12, 7)):
        t.set(x, H - 2, z, "minecraft:lantern" if (x + z) % 2 == 0 else "minecraft:soul_lantern", {"hanging": "true", "waterlogged": "false"})
    return t


def seal(t, kind):
    t.set(7, 0, 7, f"{NS}:dungeon_seal", {"kind": kind, "phase": "idle"})


def cobwebs(t, n):
    for _ in range(n):
        t.set(random.randint(1, 13), random.randint(5, 7), random.randint(1, 13), "minecraft:cobweb")


def room_start():
    t = shell()
    for y in range(1, H - 1):
        t.set(7, y, 6, "minecraft:chiseled_stone_bricks" if y % 3 == 0 else "minecraft:stone_bricks")
    t.set(7, H - 1, 7, "minecraft:air")  # abertura do poço
    chest(t, 2, 1, 12, "chests/dungeon_room", "east")
    t.set(12, 1, 12, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    cobwebs(t, 4)
    return t


def room_combat():
    t = shell(floor=BRICKS + ["minecraft:polished_andesite"])
    seal(t, "combat")
    for x, z in ((4, 4), (10, 4), (4, 10), (10, 10)):  # cobertura
        for y in range(1, 4):
            t.set(x, y, z, random.choice(BRICKS))
    for x in range(5, 10):
        for z in (5, 9):
            t.set(x, 0, z, "minecraft:polished_deepslate")
            t.set(z, 0, x, "minecraft:polished_deepslate")
    t.set(12, 1, 2, "minecraft:skeleton_skull", {"rotation": "6"})
    cobwebs(t, 5)
    return t


def room_trap():
    t = shell()
    for x in range(1, W - 1):
        for z in range(1, W - 1):
            t.set(x, 0, z, f"{NS}:spike_trap", {"stage": "0"})
    for _ in range(7):  # teias que prendem quem não olha onde pisa
        t.set(random.randint(3, 11), 1, random.randint(3, 11), "minecraft:cobweb")
    chest(t, 12, 1, 12, "chests/dungeon_room", "west")
    t.set(12, 0, 12, "minecraft:stone_bricks")
    return t


def room_puzzle():
    t = shell(floor=["minecraft:smooth_stone"] * 3 + BRICKS)
    seal(t, "puzzle")
    for x in (2, 5, 9, 12):  # glifos na parede norte e runas alinhadas na mesma coluna X
        t.set(x, 4, 0, f"{NS}:rune_glyph", {"rune": "0", "target": "0"})
        t.set(x, 0, 3, "minecraft:chiseled_stone_bricks")
        t.set(x, 1, 3, f"{NS}:dungeon_rune", {"rune": "0", "target": "0"})
    for x in range(1, W - 1):
        t.set(x, 5, 1, "minecraft:stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(7, 1, 11, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    cobwebs(t, 3)
    return t


def room_champion():
    t = shell(floor=DEEP, wall=DEEP)
    seal(t, "champion")
    for x, z in ((3, 3), (11, 3), (3, 11), (11, 11)):
        for y in range(1, H - 1):
            t.set(x, y, z, "minecraft:polished_deepslate" if y % 2 else "minecraft:chiseled_deepslate")
        t.set(x, 4, z, "minecraft:shroomlight")
    for x in range(4, 11):
        for z in range(4, 11):
            if (x - 7) ** 2 + (z - 7) ** 2 <= 9 and (x, z) != (7, 7):
                t.set(x, 0, z, "minecraft:gilded_blackstone" if (x + z) % 3 == 0 else "minecraft:polished_blackstone")
    return t


def room_treasure():
    t = shell(floor=["minecraft:polished_deepslate"], wall=DEEP)
    t.fill(5, 0, 5, 9, 0, 9, "minecraft:gold_block")
    t.set(7, 1, 7, f"{NS}:dungeon_vault")
    chest(t, 2, 1, 7, "chests/dungeon_treasure", "east")
    chest(t, 12, 1, 7, "chests/dungeon_treasure", "west")
    for x, z in ((4, 4), (10, 4), (4, 10), (10, 10)):
        t.set(x, 1, z, "minecraft:candle", {"candles": "4", "lit": "true", "waterlogged": "false"})
    return t


def entrance():
    t = Template(7, 7, 7)
    moss = ["minecraft:mossy_stone_bricks", "minecraft:mossy_cobblestone", "minecraft:cracked_stone_bricks", "minecraft:stone_bricks"]
    for x in range(7):
        for z in range(7):
            if (x, z) != (3, 3):
                t.set(x, 0, z, random.choice(moss))
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        for y in range(1, 5):
            t.set(x, y, z, random.choice(moss))
    for i in range(7):
        for (x, z) in ((i, 0), (i, 6), (0, i), (6, i)):
            if random.random() < 0.75:
                t.set(x, 5, z, random.choice(moss))
    for x in range(2, 5):
        for z in range(2, 5):
            if (x, z) != (3, 3):
                t.set(x, 1, z, "minecraft:stone_brick_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    t.set(3, 1, 3, "minecraft:air")
    for x, z in ((0, 3), (6, 3)):
        t.set(x, 1, z, "minecraft:soul_lantern", {"hanging": "false", "waterlogged": "false"})
    return t


ROOMS = {"dungeon_start": room_start, "dungeon_combat": room_combat, "dungeon_trap": room_trap, "dungeon_puzzle": room_puzzle,
         "dungeon_champion": room_champion, "dungeon_treasure": room_treasure, "dungeon_entrance": entrance}

LAND = ["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_hill", "#minecraft:is_savanna", "#minecraft:is_badlands",
        "#minecraft:is_mountain", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:desert", "minecraft:snowy_plains", "minecraft:meadow"]
STRUCTURES = {"dungeon": (LAND, 26, 10, 7311047), "aether_dungeon": ([f"#{NS}:is_aether"], 24, 9, 7311048)}


def structures():
    for name, fn in ROOMS.items():
        path = data(NS, "structure", name + ".nbt")
        fn().save(path)
    for name, (biomes, spacing, sep, salt) in STRUCTURES.items():
        jdump(data(NS, "tags", "worldgen", "biome", "has_structure", name + ".json"), {"values": biomes})
        jdump(data(NS, "worldgen", "structure", name + ".json"), {
            "type": f"{NS}:dungeon", "biomes": f"#{NS}:has_structure/{name}", "step": "underground_structures",
            "spawn_overrides": {}, "terrain_adaptation": "none"})
        jdump(data(NS, "worldgen", "structure_set", name + ".json"), {
            "structures": [{"structure": f"{NS}:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})


def loot_tables():
    def entry(n, w, lo, hi):
        return {"type": "minecraft:item", "name": n, "weight": w,
                "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}

    def pool(rolls, entries, empty=0):
        e = [entry(*x) for x in entries]
        if empty:
            e.append({"type": "minecraft:empty", "weight": empty})
        return {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": e}

    def table(name, pools, kind="minecraft:chest"):
        jdump(data(NS, "loot_table", name + ".json"), {"type": kind, "pools": pools})

    basics = [(f"{NS}:lunar_silver_ingot", 15, 2, 5), (f"{NS}:aurorite_shard", 12, 1, 3), (f"{NS}:mana_elixir", 10, 1, 2),
              (f"{NS}:vigor_elixir", 10, 1, 2), (f"{NS}:healing_salve", 10, 1, 3), ("minecraft:emerald", 10, 2, 6),
              ("minecraft:diamond", 5, 1, 2), ("minecraft:golden_apple", 3, 1, 1)]
    gear = [(f"{NS}:aurorite_sword", 4, 1, 1), (f"{NS}:aurorite_bow", 3, 1, 1), (f"{NS}:aurorite_warhammer", 3, 1, 1),
            (f"{NS}:aurorite_dagger", 3, 1, 1), (f"{NS}:aurorite_wand", 3, 1, 1), (f"{NS}:arcane_staff", 2, 1, 1),
            (f"{NS}:arcane_chestplate", 2, 1, 1), (f"{NS}:aurorite_chestplate", 2, 1, 1), (f"{NS}:shadow_daggers", 1, 1, 1)]
    sigil = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:dawn_sigil_fragment"}]}
    table("dungeon/vault", [sigil, pool((2, 4), basics + [(f"{NS}:aurorite_gem", 6, 1, 1)]), pool((1, 1), gear, empty=12)])
    table("dungeon/vault_aether", [sigil, pool((3, 5), basics + [(f"{NS}:aurorite_gem", 12, 1, 2), (f"{NS}:elemental_core", 6, 1, 2),
                                                                 (f"{NS}:fairy_dust", 8, 2, 5)]),
                                   pool((1, 1), gear + [(f"{NS}:prismatic_wings", 1, 1, 1), (f"{NS}:dawn_key", 3, 1, 1)], empty=8)])
    table("chests/dungeon_room", [pool((2, 4), [("minecraft:bread", 10, 1, 3), ("minecraft:arrow", 10, 4, 12), ("minecraft:torch", 8, 4, 10),
                                                ("minecraft:iron_ingot", 8, 1, 3), (f"{NS}:raw_lunar_silver", 6, 1, 3),
                                                (f"{NS}:healing_salve", 5, 1, 2), ("minecraft:bone", 6, 2, 5)])])
    table("chests/dungeon_treasure", [pool((3, 6), basics + [("minecraft:gold_ingot", 12, 2, 6), (f"{NS}:aurorite_gem", 4, 1, 1)])])


# ---------------------------------------------------------------- texturas

SYMBOLS = [  # sol, lua, estrela, olho (8×8, centralizados)
    ["...##...", ".#....#.", "..####..", "#.#..#.#", "#.#..#.#", "..####..", ".#....#.", "...##..."],
    ["..####..", ".##.....", "##......", "##......", "##......", "##......", ".##.....", "..####.."],
    ["...#....", "...#....", "#..#..#.", ".#####..", "..###...", ".#####..", "#..#..#.", "...#...."],
    ["........", "..####..", ".#....#.", "#..##..#", "#..##..#", ".#....#.", "..####..", "........"],
]


def noise_tex(base, var):
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            j = random.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))
    return img


def bevel(img, light, dark):
    for i in range(16):
        img.putpixel((i, 0), light)
        img.putpixel((0, i), light)
        img.putpixel((i, 15), dark)
        img.putpixel((15, i), dark)
    return img


def symbol(img, idx, color):
    for y, row in enumerate(SYMBOLS[idx]):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((4 + x, 4 + y), color)
    return img


def save_block(name, img):
    os.makedirs(os.path.join(ASSETS, "textures", "block"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures", "block", name + ".png"))


def model(name, parent, textures, extra=None):
    m = {"parent": parent, "textures": textures}
    m.update(extra or {})
    jdump(os.path.join(ASSETS, "models", "block", name + ".json"), m)


def item_model(name, parent):
    jdump(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": parent})


def textures():
    B = f"{NS}:block/"
    dark = (62, 60, 66)
    side = bevel(noise_tex(dark, 6), (86, 84, 90, 255), (40, 38, 44, 255))
    save_block("dungeon_stone", side)
    # Selo: círculo rúnico cuja cor indica a fase
    for phase, col in (("idle", (190, 40, 40)), ("active", (255, 140, 30)), ("cleared", (90, 170, 255))):
        img = noise_tex(dark, 5)
        for x in range(16):
            for y in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if 5.5 <= d <= 6.6 or d <= 1.6 or (abs(x - y) <= 0 or abs(x + y - 15) <= 0) and 2 < d < 5.5:
                    img.putpixel((x, y), col + (255,))
        save_block(f"dungeon_seal_{phase}", img)
        model(f"dungeon_seal_{phase}", "minecraft:block/cube_bottom_top",
              {"top": B + f"dungeon_seal_{phase}", "bottom": B + "dungeon_stone", "side": B + "dungeon_stone"})
    jdump(os.path.join(ASSETS, "blockstates", "dungeon_seal.json"), {"variants": {
        f"phase={p}": {"model": f"{NS}:block/dungeon_seal_{p}"} for p in ("idle", "active", "cleared")}})
    item_model("dungeon_seal", B + "dungeon_seal_idle")
    # Portão Selado: grade dourada brilhante, vazada
    door = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if x % 5 in (1, 2) or y in (0, 1, 7, 8, 14, 15):
                g = 200 + random.randint(-20, 30)
                door.putpixel((x, y), (255, min(255, g), 90, 255))
    save_block("sealed_door", door)
    model("sealed_door", "minecraft:block/cube_all", {"all": B + "sealed_door"}, {"render_type": "minecraft:cutout"})
    jdump(os.path.join(ASSETS, "blockstates", "sealed_door.json"), {"variants": {"": {"model": f"{NS}:block/sealed_door"}}})
    item_model("sealed_door", B + "sealed_door")
    # Runas (símbolo atual, ciano) e glifos (símbolo-alvo, dourado)
    for i in range(4):
        save_block(f"dungeon_rune_{i}", symbol(bevel(noise_tex((88, 90, 96), 6), (120, 122, 128, 255), (50, 52, 58, 255)), i, (80, 230, 255, 255)))
        model(f"dungeon_rune_{i}", "minecraft:block/cube_all", {"all": B + f"dungeon_rune_{i}"})
        save_block(f"rune_glyph_{i}", symbol(bevel(noise_tex((40, 34, 30), 4), (70, 60, 50, 255), (25, 20, 18, 255)), i, (255, 200, 60, 255)))
        model(f"rune_glyph_{i}", "minecraft:block/cube_all", {"all": B + f"rune_glyph_{i}"})
    jdump(os.path.join(ASSETS, "blockstates", "dungeon_rune.json"), {"variants": {
        f"rune={i}": {"model": f"{NS}:block/dungeon_rune_{i}"} for i in range(4)}})
    jdump(os.path.join(ASSETS, "blockstates", "rune_glyph.json"), {"variants": {
        f"target={i}": {"model": f"{NS}:block/rune_glyph_{i}"} for i in range(4)}})
    item_model("dungeon_rune", B + "dungeon_rune_0")
    item_model("rune_glyph", B + "rune_glyph_0")
    # Placa de Espinhos: furos (armada), furos em brasa (aviso), espinhos erguidos
    for stage, glow in ((0, (20, 18, 22)), (1, (255, 70, 30))):
        img = bevel(noise_tex((120, 118, 112), 8), (150, 148, 140, 255), (80, 78, 72, 255))
        for cx in (3, 8, 12):
            for cy in (3, 8, 12):
                for dx in (0, 1):
                    for dy in (0, 1):
                        img.putpixel((cx + dx, cy + dy), glow + (255,))
        save_block(f"spike_trap_top_{stage}", img)
        model(f"spike_trap_{stage}", "minecraft:block/cube_bottom_top",
              {"top": B + f"spike_trap_top_{stage}", "bottom": B + "dungeon_stone", "side": B + "dungeon_stone"})
    spike = noise_tex((200, 200, 205), 15)
    save_block("spike", spike)
    elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
        "up": {"texture": "#top"}, "down": {"texture": "#side"}, "north": {"texture": "#side"}, "south": {"texture": "#side"},
        "east": {"texture": "#side"}, "west": {"texture": "#side"}}}]
    for cx in (3, 8, 12):
        for cy in (3, 8, 12):
            elements.append({"from": [cx, 16, cy], "to": [cx + 2, 25, cy + 2], "faces": {
                f: {"texture": "#spike"} for f in ("up", "north", "south", "east", "west")}})
    model("spike_trap_2", "minecraft:block/block", {"top": B + "spike_trap_top_1", "side": B + "dungeon_stone",
                                                    "spike": B + "spike", "particle": B + "dungeon_stone"}, {"elements": elements})
    jdump(os.path.join(ASSETS, "blockstates", "spike_trap.json"), {"variants": {
        f"stage={i}": {"model": f"{NS}:block/spike_trap_{i}"} for i in range(3)}})
    item_model("spike_trap", B + "spike_trap_0")
    # Relicário
    top = bevel(noise_tex((214, 170, 60), 12), (255, 220, 120, 255), (140, 100, 30, 255))
    front = top.copy()
    for x in range(6, 10):
        for y in range(5, 11):
            front.putpixel((x, y), (60, 30, 20, 255) if (x in (7, 8) and 6 <= y <= 9) else (255, 230, 150, 255))
    save_block("dungeon_vault_top", top)
    save_block("dungeon_vault_front", front)
    model("dungeon_vault", "minecraft:block/cube_all", {"all": B + "dungeon_vault_front"})
    jdump(os.path.join(ASSETS, "blockstates", "dungeon_vault.json"), {"variants": {"": {"model": f"{NS}:block/dungeon_vault"}}})
    item_model("dungeon_vault", B + "dungeon_vault")
    # Fragmento do Sigilo
    frag = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pts = [(7, 2), (11, 6), (9, 13), (4, 11), (3, 6)]
    for x in range(16):
        for y in range(16):
            inside = all((pts[(i + 1) % 5][0] - pts[i][0]) * (y - pts[i][1]) - (pts[(i + 1) % 5][1] - pts[i][1]) * (x - pts[i][0]) >= 0
                         for i in range(5))
            if inside:
                frag.putpixel((x, y), (255, 170 + random.randint(0, 60), 60, 255))
    for i in range(5):
        frag.putpixel((6 + i % 2, 5 + i), (255, 245, 210, 255))
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    frag.save(os.path.join(ASSETS, "textures", "item", "dawn_sigil_fragment.png"))
    jdump(os.path.join(ASSETS, "models", "item", "dawn_sigil_fragment.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/dawn_sigil_fragment"}})
    jdump(data(NS, "advancement", "dungeon.json"), {"parent": f"{NS}:root", "display": {
        "icon": {"id": f"{NS}:dawn_sigil_fragment"}, "title": {"translate": "advancement.alvorecer.dungeon"},
        "description": {"translate": "advancement.alvorecer.dungeon.desc"}, "frame": "challenge", "show_toast": True,
        "announce_to_chat": True, "hidden": False}, "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def main():
    structures()
    loot_tables()
    textures()
    print("dados da etapa 6 gerados")


if __name__ == "__main__":
    main()
