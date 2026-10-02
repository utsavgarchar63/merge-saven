"""Export real offline Nunito weights; the source variable font defaults to ExtraLight."""
from pathlib import Path
from shutil import copyfile
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "art/fonts/nunito-variable.ttf"
resources = ROOT / "app/src/main/res/font"
source.parent.mkdir(parents=True, exist_ok=True)
if not source.exists():
    copyfile(resources / "nunito.ttf", source)
for weight, suffix, style in [(400, "", "Regular"), (500, "_medium", "Medium"),
                              (600, "_semibold", "SemiBold"), (700, "_bold", "Bold")]:
    font = instantiateVariableFont(TTFont(source), {"wght": weight}, inplace=False)
    font["OS/2"].usWeightClass = weight
    for item in font["name"].names:
        value = {1: "Nunito", 2: style, 4: f"Nunito {style}", 6: f"Nunito-{style}",
                 16: "Nunito", 17: style}.get(item.nameID)
        if value:
            item.string = value.encode(item.getEncoding())
    font.save(resources / f"nunito{suffix}.ttf")
    print(f"nunito{suffix}.ttf: real weight {weight}, static font")
