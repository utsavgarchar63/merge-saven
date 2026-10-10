"""Export the reviewed v4 imagegen masters. Requires Pillow; run from any directory."""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
MASTERS = ROOT / "art/masters/promo_v4"
STORE = ROOT / "store"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
RESAMPLE = Image.Resampling.LANCZOS


def export(source, target, size, **options):
    with Image.open(MASTERS / source) as image:
        # The masters already match these ratios to within a pixel. Fit avoids distortion.
        image = ImageOps.fit(image.convert("RGB"), size, method=RESAMPLE)
        target.parent.mkdir(parents=True, exist_ok=True)
        image.save(target, **options)


def icon_on_background(size, proportion):
    with Image.open(MASTERS / "launcher_emblem.png") as source:
        assert source.mode == "RGBA", "Launcher master must retain its generated alpha"
        alpha = source.getchannel("A")
        assert alpha.getextrema() == (0, 255), "Expected genuine transparent margins"
        subject = source.crop(alpha.getbbox())
        subject.thumbnail((round(size * proportion), round(size * proportion)), RESAMPLE)
    foreground = Image.new("RGBA", (size, size))
    foreground.alpha_composite(subject, ((size - subject.width) // 2, (size - subject.height) // 2))
    background = Image.new("RGBA", (size, size), "#28180F")
    background.alpha_composite(foreground)
    return foreground, background.convert("RGB")


def main():
    export("feature.png", STORE / "feature_graphic_v4.png", (1024, 500), optimize=True)
    export("feature.png", STORE / "banners/merge_seven_landscape_v4.png", (2048, 1000), optimize=True)
    export("home_banner.png", DRAWABLE / "home_banner_v4.webp", (1024, 426), quality=86, method=6)
    for name in ("01-place-match-merge", "02-six-ways-to-play", "03-build-big-chains", "04-a-fresh-challenge"):
        export(name + ".png", STORE / "promotional" / (name + ".png"), (1080, 1920), optimize=True)

    foreground, _ = icon_on_background(432, 0.60)
    foreground.save(DRAWABLE / "ic_launcher_foreground_v4.webp", lossless=True, method=6)
    _, store_icon = icon_on_background(512, 0.82)
    store_icon.save(STORE / "app_icon_v4.png", optimize=True)
    store_icon.save(ROOT / "app/src/main/ic_launcher-playstore.png", optimize=True)
    for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        _, icon = icon_on_background(size, 0.82)
        folder = ROOT / "app/src/main/res" / ("mipmap-" + density)
        icon.save(folder / "ic_launcher.png", optimize=True)
        circular_mask = Image.new("L", (size, size))
        ImageDraw.Draw(circular_mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon = icon.convert("RGBA")
        round_icon.putalpha(circular_mask)
        round_icon.save(folder / "ic_launcher_round.png", optimize=True)

    # Show actual Android 108dp layer geometry: 72dp viewport and 66dp circular mask.
    # All nontransparent foreground pixels must fit inside the 66dp safe circle.
    alpha = foreground.getchannel("A")
    for y in range(432):
        for x in range(432):
            if alpha.getpixel((x, y)) > 8:
                assert (x - 215.5) ** 2 + (y - 215.5) ** 2 <= 132 ** 2, "Icon exceeds Android's safe circle"
    preview = Image.new("RGB", (768, 240), "#F6ECD9")
    draw = ImageDraw.Draw(preview)
    for i, shape in enumerate(("circle", "rounded", "square")):
        bg = Image.new("RGBA", (432, 432), "#28180F")
        bg.alpha_composite(foreground)
        bg = bg.crop((72, 72, 360, 360)).resize((180, 180), RESAMPLE)
        mask = Image.new("L", (180, 180))
        md = ImageDraw.Draw(mask)
        if shape == "circle":
            md.ellipse((0, 0, 179, 179), fill=255)
        elif shape == "rounded":
            md.rounded_rectangle((0, 0, 179, 179), radius=40, fill=255)
        else:
            md.rectangle((0, 0, 179, 179), fill=255)
        preview.paste(bg, (i * 256 + 38, 18), mask)
        draw.text((i * 256 + 80, 210), shape, fill="#28180F")
    preview.save(STORE / "icon_masks_v4.png", optimize=True)

    contact = Image.new("RGB", (1080, 990), "#28180F")
    with Image.open(STORE / "feature_graphic_v4.png") as feature:
        contact.paste(feature.resize((1080, 527), RESAMPLE), (0, 0))
    for i, path in enumerate(sorted((STORE / "promotional").glob("*.png"))):
        with Image.open(path) as image:
            contact.paste(image.resize((252, 448), RESAMPLE), (i * 270 + 9, 536))
    contact.save(STORE / "promo_contact_sheet_v4.jpg", quality=92)

    manifest_path = ROOT / "art/promo-manifest-v4.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    exports = [STORE / "feature_graphic_v4.png", STORE / "banners/merge_seven_landscape_v4.png",
               STORE / "app_icon_v4.png", DRAWABLE / "home_banner_v4.webp",
               DRAWABLE / "ic_launcher_foreground_v4.webp"]
    exports.extend(sorted((STORE / "promotional").glob("*.png")))
    manifest["exports"] = []
    for path in exports:
        with Image.open(path) as image:
            manifest["exports"].append({"path": path.relative_to(ROOT).as_posix(), "dimensions": list(image.size),
                                        "mode": image.mode, "bytes": path.stat().st_size})
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("Exported v4 banner, four posters, store icon and Android assets; alpha and safe-circle checks passed.")


if __name__ == "__main__":
    main()
