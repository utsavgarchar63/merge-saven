# Walnut artwork v2

Generated through the built-in imagegen workflow and visually inspected before export. Source PNG masters are in masters; reusable subject briefs, dimensions, formats and runtime paths are in asset-manifest.json. Atlas crops are individual 256px optimized WebP, never a full reference sheet in the app. Brand has genuine alpha transparency; adaptive foreground uses central safe padding.

Tutorial geometry has SVG masters in tutorial and VectorDrawable runtime exports. Nine booster icons and functional controls extend the existing vector/Canvas language. Tile numbers are real text. Font is Nunito (OFL license bundled). Legacy reference sheets were moved here from runtime assets; keep them as editable reference material only.

Store icon/feature/share exports live in ../store. Actual screenshots are captured from the running application. Requested promotional illustrations live separately in ../store/promotional and are identified as artwork. Adaptive masks and final small-icon readability require device review before distribution.

The v3 icon atlas was edited with built-in imagegen to remove only rectangular surrounds, retaining the gold badges and interior wood. Transparent individual PNG masters and lossless 256px WebP exports are recorded in the manifest; unused exports are excluded from runtime. Original full-screen backgrounds remain unchanged. See MUSIC_MANIFEST.md for the original bundled audio.

## Promotional artwork and launcher — v4

`masters/promo_v4/` contains the reviewed built-in imagegen banner, text-free Home artwork, four illustrated promotional panels and transparent launcher emblem. The exact five marketing prompts and launcher prompt are recorded in `promo-manifest-v4.json`; the recovered Home master has a labeled reusable brief. Export with `../tools/export_promo_art.py` (Pillow). This verifies real alpha transparency and the foreground's fit inside Android's 66dp safe circle, and exports launcher mask previews for inspection.

Runtime assets are `home_banner_v4.webp` and `ic_launcher_foreground_v4.webp`; Home copy remains native Compose text and the decorative banner stays hidden on compact screens. Legacy launcher and Play Store icons now use the gold 7 emblem. Store deliverables and gallery are in `../store/`; portrait panels illustrate the existing rules and modes and contain no app screenshots or device frames.
