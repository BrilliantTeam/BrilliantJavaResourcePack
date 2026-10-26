import json, math, sys
from pathlib import Path

PACK = Path(sys.argv[1])
OUT = Path(sys.argv[2])
ROOTS = [PACK / "26.3", PACK / "26.1", PACK]
MULT = 0.25 / 14
MIN_HEIGHT = 0.02
GENERATED_HEAD = {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}
SKIP_SUFFIXES = ("_helmet", "_head", "_skull")


def find(kind, rid):
    ns, path = rid.split(":", 1) if ":" in rid else ("minecraft", rid)
    for r in ROOTS:
        p = r / "assets" / ns / kind / (path + ".json")
        if p.exists():
            return json.loads(p.read_text(encoding="utf-8"))
    return None


def models_of(node):
    t = node.get("type", "").removeprefix("minecraft:")
    if t == "model":
        return [node["model"]]
    if t == "composite":
        return [m for sub in node["models"] for m in models_of(sub)]
    for key in ("on_false", "fallback"):
        if key in node:
            return models_of(node[key])
    for c in node.get("cases", []):
        return models_of(c["model"])
    return []


def resolve(model_id):
    elements, head, chain = None, None, model_id
    while chain:
        m = find("models", chain)
        if m is None:
            break
        if elements is None and "elements" in m:
            elements = m["elements"]
        if head is None and "head" in m.get("display", {}):
            head = m["display"]["head"]
        chain = m.get("parent", "").removeprefix("minecraft:")
        if chain in ("item/generated", "item/handheld", "builtin/generated"):
            head = head or GENERATED_HEAD
            elements = elements or [{"from": [0, 0, 7.5], "to": [16, 16, 8.5]}]
            break
    return elements or [], head or {}


def rot(v, axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v
    if axis == "x":
        return [x, y * c - z * s, y * s + z * c]
    if axis == "y":
        return [x * c + z * s, y, -x * s + z * c]
    return [x * c - y * s, x * s + y * c, z]


def top_above_head(model_id):
    elements, head = resolve(model_id.removeprefix("minecraft:"))
    if not elements:
        return None
    sc = [max(-4, min(4, v)) for v in head.get("scale", [1, 1, 1])]
    rx, ry, rz = head.get("rotation", [0, 0, 0])
    tr = [max(-80, min(80, v)) / 16 for v in head.get("translation", [0, 0, 0])]
    ymax = -1e9
    for e in elements:
        f, t = e["from"], e["to"]
        r = e.get("rotation")
        for cx in (f[0], t[0]):
            for cy in (f[1], t[1]):
                for cz in (f[2], t[2]):
                    v = [cx, cy, cz]
                    if r and r.get("angle"):
                        o = r.get("origin", [8, 8, 8])
                        v = [a - b for a, b in zip(v, o)]
                        v = rot(v, r["axis"], r["angle"])
                        v = [a + b for a, b in zip(v, o)]
                    v = [(a / 16 - 0.5) * s for a, s in zip(v, sc)]
                    v = rot(rot(rot(v, "z", rz), "y", ry), "x", rx)
                    ymax = max(ymax, v[1] + tr[1])
    return 0.625 * ymax - 0.25


def item_files():
    seen = {}
    for r in reversed(ROOTS):
        for p in (r / "assets" / "minecraft" / "items").glob("*.json"):
            seen[p.stem] = p
    return sorted(seen.items())


def main():
    rules = []
    for mat, path in item_files():
        if mat.endswith(SKIP_SUFFIXES):
            continue
        root = json.loads(path.read_text(encoding="utf-8"))["model"]
        if root.get("type", "").removeprefix("minecraft:") != "range_dispatch" or root.get("property", "").removeprefix("minecraft:") != "custom_model_data":
            continue
        entries = sorted(root.get("entries", []), key=lambda e: e["threshold"])
        for i, e in enumerate(entries):
            lo = math.ceil(e["threshold"])
            hi = math.ceil(entries[i + 1]["threshold"]) - 1 if i + 1 < len(entries) else 2147483647
            if hi < lo:
                continue
            mids = models_of(e["model"])
            hs = [h for h in map(top_above_head, mids) if h is not None]
            if hs and max(hs) > MIN_HEIGHT:
                rules.append([mat, lo, hi, round(max(hs) / MULT, 1), mids[0]])

    merged = []
    for r in rules:
        g = merged[-1] if merged else None
        if g and g[0] == r[0] and g[2] == r[1] - 1 and g[3] == r[3]:
            g[2] = r[2]
        else:
            merged.append(r)

    lines = [
        "helmetRulesDebug: false",
        "helmetRulesDebugCooldownMs: 5000",
        "helmetHeightYOffsetMultiplier: 0.017857143",
        "helmetHeightRules:",
    ]
    for mat, lo, hi, h, mid in merged:
        lines += [f"  # {mid}", f"  - material: {mat.upper()}"]
        lines += [f"    customModelData: {lo}"] if lo == hi else [f"    customModelDataMin: {lo}", f"    customModelDataMax: {hi}"]
        lines += [f"    height: {h}"]
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(rules)} entries, {len(merged)} rules -> {OUT}")


if __name__ == "__main__":
    main()
