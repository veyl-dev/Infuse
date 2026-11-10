#!/usr/bin/env python3
"""Build Minecraft-version-specific ZIPs from the bundled original S3 resource pack."""
from __future__ import annotations

import base64
import io
import json
import zipfile
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/resourcepack/InfuseSMP-S3-Original.zip.b64"
OUTPUT = ROOT / "dist/resourcepacks"

VERSIONS = {
    "1.21.1": (34, False, False),
    "1.21.2": (42, False, False),
    "1.21.4": (46, True, False),
    "1.21.5": (55, True, False),
    "1.21.6": (63, True, True),
    "1.21.8": (64, True, True),
    "1.21.10": (69, True, True),
    "1.21.11": (75, True, True),
    "26.1": (84, True, True),
    "26.2": (88, True, True),
    "26.3": (97.1, True, True),
}


def legacy_potion_model() -> bytes:
    ids = list(range(414001, 414014)) + list(range(414101, 414114))
    overrides = [
        {"predicate": {"custom_model_data": model_id}, "model": "minecraft:item/potion_custom"}
        for model_id in ids
    ]
    return json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "minecraft:item/potion"},
        "overrides": overrides,
    }, separators=(",", ":")).encode()


def modern_potion_model() -> bytes:
    ids = list(range(414001, 414014)) + list(range(414101, 414114))
    entries = [
        {
            "threshold": model_id,
            "model": {
                "type": "model",
                "model": "minecraft:item/potion_custom",
                "tints": [{"type": "potion", "default": 16253176}],
            },
        }
        for model_id in ids
    ]
    return json.dumps({
        "model": {
            "type": "range_dispatch",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "scale": 1,
            "entries": entries,
            "fallback": {
                "type": "model",
                "model": "minecraft:item/potion",
                "tints": [{"type": "potion", "default": 16253176}],
            },
        }
    }, separators=(",", ":")).encode()


def pack_metadata(pack_format: int | float) -> bytes:
    if pack_format <= 64:
        pack = {
            "pack_format": int(pack_format),
            "description": "Infuse SMP Season 3 - original textures and sounds",
        }
    else:
        version = [97, 1] if pack_format == 97.1 else [int(pack_format), 0]
        pack = {
            "min_format": version,
            "max_format": version,
            "description": "Infuse SMP Season 3 - original textures and sounds",
        }
    return json.dumps({"pack": pack}, indent=2).encode()


def build_one(version: str, spec: tuple[int | float, bool, bool], source_entries: dict[str, bytes]) -> str:
    pack_format, modern_items, has_wind_charge_models = spec
    entries = dict(source_entries)
    entries["pack.mcmeta"] = pack_metadata(pack_format)

    if not modern_items:
        entries.pop("assets/minecraft/items/potion.json", None)
        entries.pop("assets/minecraft/items/wind_charge.json", None)
        entries.pop("assets/minecraft/models/wind_charge_base.json", None)
        entries.pop("assets/minecraft/models/wind_charge_wind.json", None)
        entries["assets/minecraft/models/item/potion.json"] = legacy_potion_model()
    else:
        entries["assets/minecraft/items/potion.json"] = modern_potion_model()
        if not has_wind_charge_models:
            entries.pop("assets/minecraft/items/wind_charge.json", None)
            entries.pop("assets/minecraft/models/wind_charge_base.json", None)
            entries.pop("assets/minecraft/models/wind_charge_wind.json", None)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    target = OUTPUT / f"InfuseSMP-S3-{version}.zip"
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in entries.items():
            info = zipfile.ZipInfo(name, date_time=(2020, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    with zipfile.ZipFile(target, "r") as archive:
        bad = archive.testzip()
        if bad:
            raise RuntimeError(f"{target.name} contains a corrupt entry: {bad}")
        if "pack.mcmeta" not in archive.namelist():
            raise RuntimeError(f"{target.name} has no root pack.mcmeta")
    digest = hashlib.sha1(target.read_bytes()).hexdigest()\n    print(f"Built {target.name} (resource pack format {pack_format}, SHA-1 {digest})")\n    return digest


def main() -> None:
    if not SOURCE.is_file():
        raise FileNotFoundError(f"Bundled pack source not found: {SOURCE}")
    decoded = base64.b64decode(SOURCE.read_bytes(), validate=False)
    with zipfile.ZipFile(io.BytesIO(decoded), "r") as archive:
        source_entries = {
            item.filename: archive.read(item.filename)
            for item in archive.infolist()
            if not item.is_dir()
        }
    if "pack.mcmeta" not in source_entries:
        raise RuntimeError("Original S3 pack has no root pack.mcmeta")
    hashes = {}
    for version, spec in VERSIONS.items():
        hashes[f"InfuseSMP-S3-{version}.zip"] = build_one(version, spec, source_entries)

    hash_file = ROOT / "src/main/resources/resourcepack/pack-hashes.properties"
    hash_file.write_text(
        "# Generated by scripts/build-resource-packs.py; do not edit by hand.\n"
        + "".join(f"{asset}={digest}\n" for asset, digest in sorted(hashes.items())),
        encoding="utf-8",
    )
    print(f"Wrote runtime pack hashes to {hash_file}")


if __name__ == "__main__":
    main()
