"""Gera estruturas NBT simples (gzip) sem dependências externas. Uso: python tools/gen_structures.py"""
import gzip
import os
import struct

DATA_VERSION = 3955  # Minecraft 1.21.1
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "alvorecer", "structure")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def _name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(tag_type, value):
    if tag_type == TAG_INT:
        return struct.pack(">i", value)
    if tag_type == TAG_STRING:
        return _name(value)
    if tag_type == TAG_COMPOUND:
        out = b""
        for k, (t, v) in value.items():
            out += bytes([t]) + _name(k) + _payload(t, v)
        return out + bytes([TAG_END])
    if tag_type == TAG_LIST:
        elem_type, items = value
        out = bytes([elem_type if items else TAG_END]) + struct.pack(">i", len(items))
        for it in items:
            out += _payload(elem_type, it)
        return out
    raise ValueError(tag_type)


def structure(name, size, block_at):
    """block_at(x, y, z) -> nome do bloco; a paleta é montada automaticamente."""
    palette, blocks = [], []
    for y in range(size[1]):
        for z in range(size[2]):
            for x in range(size[0]):
                b = block_at(x, y, z)
                if b not in palette:
                    palette.append(b)
                blocks.append({"pos": (TAG_LIST, (TAG_INT, [x, y, z])), "state": (TAG_INT, palette.index(b))})
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": (TAG_LIST, (TAG_INT, list(size))),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, b)} for b in palette])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, blocks)),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    os.makedirs(OUT, exist_ok=True)
    with gzip.open(os.path.join(OUT, name + ".nbt"), "wb") as f:
        f.write(bytes([TAG_COMPOUND]) + _name("") + _payload(TAG_COMPOUND, root))


if __name__ == "__main__":
    # Arena de testes: piso de pedra 7x7 e 4 blocos de ar acima.
    structure("arena", (7, 5, 7), lambda x, y, z: "minecraft:stone" if y == 0 else "minecraft:air")
    print("estruturas geradas")
