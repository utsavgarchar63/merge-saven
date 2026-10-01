# Merge Seven — walnut design system

Updated 2026-10-01. Preserve all six game modes and engine rules. Compose/Canvas owns presentation; engine/configuration owns gameplay.

| Element | Shipping direction |
|---|---|
| Wood dark / mid / light | #28180F / #3E281B / #8B6347 |
| Actions | Gold with dark text, primary minimum 56dp; other controls minimum 48dp |
| Type | Bundled Nunito; OFL in assets/licenses |
| Spacing | 8dp rhythm, 16–24dp page padding |
| Panels | Opaque walnut with restrained border, rounded 16–24dp |
| Tile text | Dark/light selected from actual fill, 4.5:1 contrast; fit by digit count |
| Pages | Scrollable, centered, maximum 720dp; bounded board on tablets |

Shared components: WoodPage, WoodPanel, WoodLink, GoldButton. Background texture stays behind readable panels. Small icons, boosters, tile geometry, numbers, traits and tutorial diagrams remain vector/Canvas. Bitmap generation supplies textures, brand exploration, illustrations and ornaments. Labels are real text.

Four primary destinations: Home, Challenges, Rewards, Profile. Settings opens from headers. Home offers one-tap Play/Resume, next level, daily challenge and expandable six-mode cards. Challenges groups Daily/Weekly, login gifts and quests. Rewards offers optional exact-benefit ads, owned booster quantities and coin costs, and cosmetics. Profile links achievements, statistics, cosmetics and leaderboards.

Gameplay hides navigation and banners. Select/tap-to-place and drag are supported. Drag starts after touch slop so rotation remains tappable. Press/drag previews placement and affected cells. Invalid moves explain rotation or another location without penalty. Primary boosters are Undo, Shuffle, Remove; More has advanced descriptions. Owned charges precede coins. Coin use requires confirmation. Zen preserves unlimited undo and ad suppression.

Tutorial persists placement, rotation and merge progress; Skip and Settings replay remain available. Pause offers Resume, restart confirmation, audio controls, help and save-and-exit. Results show score/stars and persisted coin reward. Reward doubling shows that exact base amount as extra coins. Challenge results return Home rather than advance Campaign rules.

Reduced motion suppresses shake and excessive effects via JuiceController. Sound/music/haptics are independent. Cell accessibility labels, larger touch targets and color cues remain. Preview text must survive enlarged fonts; tablets center the board and use side controls. Save before exit/fullscreen ads; pause timers/audio around ads.

Ads-only economy: no purchases, subscriptions, paid products, purchase restoration or paid offers. New wallets start at 100; upgrades preserve old wallets/inventory/saves/settings. Rewarded ads require an earned callback and durable claim. Interstitials only at successful Campaign Next Level with grace/cooldown/caps. Home/Rewards adaptive banners have dedicated slots. UMP gates SDK initialization/load/show; Settings shows privacy options when required. Debug uses Google test units; production IDs stay unchanged.

Versioned runtime assets are optimized WebP and vectors. Masters, reference atlases, SVG diagrams, prompts and manifest stay in art, outside the APK. Store files stay in store. Verify transparent edges and adaptive masking; inspect icons at 24–48dp. See art/asset-manifest.json. Device/font/contrast and first-time human usability tests remain release requirements; static contrast tests are only one part of validation.

On compact screens, tutorial instructions use concise prompts. Hints and advanced boosters live in More so the board keeps practical space. Canvas drawing skips nonpositive geometry, and number fitting is a bounded proportional calculation.

## Smooth board presentation

Use short placement springs, 160 ms rotation interpolation and 260 ms merge flights. Keep labels upright and commit engine moves immediately. Reuse normalized Canvas hex paths and bounded text paint caches. Run particle frame clocks only while effects are active; pause them offscreen and cancel them with reduced motion. Read per-frame shake offsets in graphicsLayer. Deduplicate identical drag previews.

Ad inventory is configured exclusively in root `admob.properties`; test is the default for local debug and release QA. Debug always uses Google's demo inventory. Rewarded offers share one loaded SDK slot and retain placement-specific benefits. Banner disposal/pause/resume is scoped to its owning screen. Home resumes the newest valid save by its existing update timestamp.
