import difflib
import json
import re
import struct
import sys
import zipfile
from pathlib import Path

import moderngl

ROOT = Path(__file__).resolve().parents[2]
CORE = "assets/minecraft/shaders/core/"
INCLUDE = "assets/minecraft/shaders/include/"
TITLE = 64
SHADOW = 63
LIGHT = 224
LIGHTMAPS = (255, 252, 251, 250, 128)
ORTHO = (1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)
PERSPECTIVE = (1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0)
SIZES = {"mat4": (16, 64), "vec4": (16, 16), "vec3": (16, 12), "float": (4, 4)}


def overlays(major, minor):
    meta = json.loads((ROOT / "pack.mcmeta").read_text(encoding="utf-8"))
    dirs = []
    for entry in meta["overlays"]["entries"]:
        high = entry["max_format"]
        high = (high[0], high[1]) if isinstance(high, list) else (high, 0)
        if entry["min_format"] <= major and (major, minor) <= high:
            dirs.append(entry["directory"])
    return dirs


def resolve_vertex(name, dirs):
    for base in [*reversed(dirs), None]:
        path = ROOT / "darkmode" / (base or "") / CORE / name
        if path.exists():
            return path
    raise AssertionError(f"{name} 沒有暗色模式覆寫")


def preprocess(source, jar, defines):
    seen = set()

    def expand(text):
        def include(match):
            name = match.group(1).split(":")[-1]
            if name in seen:
                return ""
            seen.add(name)
            body = jar.read(INCLUDE + name).decode()
            return expand(re.sub(r"^#version.*$", "", body, flags=re.M))

        return re.sub(r"^#(?:moj_import|include) <(.+?)>", include, text, flags=re.M)

    head = "".join(f"\n#define {d}" for d in defines)
    return re.sub(r"^(#version.*)$", lambda m: m.group(1) + head, expand(source), count=1, flags=re.M)


def transforms_block(jar):
    text = jar.read(INCLUDE + "dynamictransforms.glsl").decode()
    data, offset = bytearray(256), 0
    for kind, name in re.findall(r"(mat4|vec4|vec3)\s+(\w+);", text):
        align, size = SIZES[kind]
        offset = (offset + align - 1) // align * align
        if kind == "mat4":
            struct.pack_into("16f", data, offset, *ORTHO)
        if name == "ColorModulator":
            struct.pack_into("4f", data, offset, 1, 1, 1, 1)
        offset += size
    return bytes(data)


def draw(ctx, program, jar, gray, projection, lightmap):
    white = ctx.texture((1, 1), 4, bytes([255] * 4))
    light = ctx.texture((16, 16), 4, bytes([lightmap, lightmap, lightmap, 255]) * 256)
    white.filter = light.filter = (moderngl.NEAREST, moderngl.NEAREST)
    white.use(0)
    light.use(2)
    blocks = {
        "DynamicTransforms": transforms_block(jar),
        "Projection": struct.pack("16f", *projection),
        "Fog": bytes(64),
    }
    buffers = []
    for binding, (name, data) in enumerate(blocks.items()):
        if name in program:
            buffer = ctx.buffer(data)
            buffer.bind_to_uniform_block(binding)
            program[name].binding = binding
            buffers.append(buffer)
    for name, unit in (("Sampler0", 0), ("Sampler2", 2)):
        if name in program:
            program[name] = unit
    vertices = bytearray()
    for x, y in ((-1, -1), (3, -1), (-1, 3)):
        vertices += struct.pack("3f", x, y, 1)
        vertices += bytes([gray, gray, gray, 255])
        vertices += struct.pack("2f", 0, 0)
        if "UV2" in program:
            vertices += struct.pack("2i", 240, 240)
    formats = "3f 4f1 2f" + (" 2i" if "UV2" in program else "")
    names = ["Position", "Color", "UV0"] + (["UV2"] if "UV2" in program else [])
    vao = ctx.vertex_array(program, [(ctx.buffer(bytes(vertices)), formats, *names)])
    fbo = ctx.simple_framebuffer((1, 1))
    fbo.use()
    fbo.clear()
    vao.render(moderngl.TRIANGLES)
    return fbo.read(components=4)[0]


def near(actual, expected):
    return abs(actual - expected) <= 1


def check(ctx, jar_path):
    jar = zipfile.ZipFile(jar_path)
    version = json.loads(jar.read("version.json"))
    major = version["pack_version"]["resource_major"]
    minor = version["pack_version"]["resource_minor"]
    dirs = overlays(major, minor)
    names = set(jar.namelist())
    if CORE + "text.vsh" in names:
        cases = [
            ("text", (), False, True),
            ("text", ("IS_GRAYSCALE",), False, True),
            ("text", ("IS_SEE_THROUGH",), False, False),
            ("text", ("IS_GUI",), True, False),
            ("text", ("IS_GUI", "IS_GRAYSCALE"), True, False),
        ]
    else:
        cases = [("rendertype_text", (), True, True), ("rendertype_text_intensity", (), True, True)]
    for shader, defines, recolor, lit in cases:
        path = resolve_vertex(shader + ".vsh", dirs)
        vanilla = jar.read(f"{CORE}{shader}.vsh").decode()
        override = path.read_text(encoding="utf-8")
        removed = [l for l in difflib.ndiff(vanilla.splitlines(), override.splitlines()) if l.startswith("- ")]
        assert len(removed) <= 1, f"{path} 與原版差異過大：{removed}"
        for directory in ["", *dirs]:
            stale = ROOT / "darkmode" / directory / CORE / (shader + ".fsh")
            assert not stale.exists(), f"不應覆寫 {stale}"
        program = ctx.program(
            vertex_shader=preprocess(override, jar, defines),
            fragment_shader=preprocess(jar.read(f"{CORE}{shader}.fsh").decode(), jar, defines),
        )
        for lightmap in LIGHTMAPS if lit else (255,):
            scale = lightmap / 255 if lit else 1
            label = f"{shader}{defines} lm={lightmap}"
            actual = draw(ctx, program, jar, TITLE, ORTHO, lightmap)
            assert near(actual, round((LIGHT if recolor else TITLE) * scale)), f"{label}: 標題 {actual}"
            actual = draw(ctx, program, jar, SHADOW, ORTHO, lightmap)
            assert near(actual, round(SHADOW * scale)), f"{label}: 陰影 {actual}"
            if lit:
                actual = draw(ctx, program, jar, TITLE, PERSPECTIVE, lightmap)
                assert near(actual, round(TITLE * scale)), f"{label}: 世界文字 {actual}"
    print(f"OK {version['id']} format={major}.{minor} overlays={dirs} shaders={sorted({c[0] for c in cases})}")


def main():
    ctx = moderngl.create_standalone_context(require=330)
    failed = False
    for jar_path in sys.argv[1:]:
        try:
            check(ctx, jar_path)
        except Exception as error:
            failed = True
            print(f"FAIL {jar_path}: {error}")
    sys.exit(1 if failed or len(sys.argv) < 2 else 0)


main()
