"""Export code-native, resolution-independent tutorial diagrams."""
from pathlib import Path
import math

root = Path(__file__).resolve().parents[1]
def hex_path(x, y, radius):
    points = [(x + math.cos(math.radians(i * 60 - 30)) * radius,
               y + math.sin(math.radians(i * 60 - 30)) * radius) for i in range(6)]
    return 'M' + ','.join(f'{v:.2f}' for v in points[0]) + ' ' + ' '.join('L' + ','.join(f'{v:.2f}' for v in p) for p in points[1:]) + 'Z'

diagrams = {
    'placement': [(26, 40, 17, '#FFD54A'), (52, 40, 17, '#FFD54A'), (180, 40, 21, '#57BBDF')],
    'rotation': [(35, 28, 17, '#57BBDF'), (63, 43, 17, '#57BBDF'), (165, 28, 17, '#57BBDF'), (165, 57, 17, '#57BBDF')],
    'merge': [(24, 42, 16, '#EE8067'), (49, 28, 16, '#EE8067'), (49, 56, 16, '#EE8067'), (179, 42, 24, '#FFD54A')],
}
masters = root / 'art/tutorial'
masters.mkdir(parents=True, exist_ok=True)
for name, cells in diagrams.items():
    arrow = 'M88,40 L130,40 M119,30 L130,40 L119,50'
    paths = [f'<path android:fillColor="{c}" android:pathData="{hex_path(x,y,r)}"/>' for x,y,r,c in cells]
    paths += [f'<path android:strokeColor="#FFEBC8" android:strokeWidth="4" android:strokeLineCap="round" android:fillColor="@android:color/transparent" android:pathData="{arrow}"/>']
    xml = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="220dp" android:height="84dp" android:viewportWidth="220" android:viewportHeight="84">\n' + '\n'.join(paths) + '\n</vector>\n'
    (root / f'app/src/main/res/drawable/tutorial_{name}_v2.xml').write_text(xml, encoding='utf-8')
    svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 84">' + ''.join(f'<path fill="{c}" d="{hex_path(x,y,r)}"/>' for x,y,r,c in cells) + f'<path fill="none" stroke="#FFEBC8" stroke-width="4" stroke-linecap="round" d="{arrow}"/></svg>'
    (masters / f'{name}_v2.svg').write_text(svg, encoding='utf-8')
