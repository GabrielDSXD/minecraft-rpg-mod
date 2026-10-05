"""Escritor mínimo de NBT (gzip) para modelos de estrutura. Tipos Python: int→Int, str→String, dict→Compound,
float→Double; use Byte/Float/IntList/List para os demais."""
import gzip
import struct


class Byte(int):
    pass


class Float(float):
    pass


class List(list):
    def __init__(self, elem, items):
        super().__init__(items)
        self.elem = elem


END, BYTE, INT, FLOAT, DOUBLE, STRING, LIST, COMPOUND = 0, 1, 3, 5, 6, 8, 9, 10


def tag_type(v):
    if isinstance(v, Byte) or isinstance(v, bool):
        return BYTE
    if isinstance(v, Float):
        return FLOAT
    if isinstance(v, int):
        return INT
    if isinstance(v, float):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, List):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError(type(v))


def _name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def payload(v, t=None):
    t = t or tag_type(v)
    if t == BYTE:
        return struct.pack(">b", int(v))
    if t == INT:
        return struct.pack(">i", v)
    if t == FLOAT:
        return struct.pack(">f", v)
    if t == DOUBLE:
        return struct.pack(">d", v)
    if t == STRING:
        return _name(v)
    if t == LIST:
        elem = v.elem if v else END
        out = bytes([elem]) + struct.pack(">i", len(v))
        for item in v:
            out += payload(item, elem)
        return out
    if t == COMPOUND:
        out = b""
        for k, item in v.items():
            it = tag_type(item)
            out += bytes([it]) + _name(k) + payload(item, it)
        return out + bytes([END])
    raise ValueError(t)


def write(path, root):
    with gzip.open(path, "wb") as f:
        f.write(bytes([COMPOUND]) + _name("") + payload(root, COMPOUND))


class Template:
    """Monta um modelo de estrutura bloco a bloco (coordenadas relativas, y=0 é o chão)."""

    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def entity(self, x, y, z, entity_id, extra=None):
        nbt = {"id": entity_id, "PersistenceRequired": Byte(1)}
        nbt.update(extra or {})
        self.entities.append({"pos": List(DOUBLE, [x + 0.5, float(y), z + 0.5]),
                              "blockPos": List(INT, [x, y, z]), "nbt": nbt})

    def save(self, path, data_version=3955, fill_air=True):
        palette, index, blocks = [], {}, []
        sx, sy, sz = self.size
        for y in range(sy):
            for z in range(sz):
                for x in range(sx):
                    entry = self.blocks.get((x, y, z))
                    if entry is None:
                        if not fill_air:
                            continue
                        entry = ("minecraft:air", (), None)
                    key = entry[:2]
                    if key not in index:
                        index[key] = len(palette)
                        state = {"Name": entry[0]}
                        if entry[1]:
                            state["Properties"] = dict(entry[1])
                        palette.append(state)
                    b = {"pos": List(INT, [x, y, z]), "state": index[key]}
                    if entry[2]:
                        b["nbt"] = entry[2]
                    blocks.append(b)
        write(path, {"DataVersion": data_version, "size": List(INT, list(self.size)),
                     "palette": List(COMPOUND, palette), "blocks": List(COMPOUND, blocks), "entities": List(COMPOUND, self.entities)})
