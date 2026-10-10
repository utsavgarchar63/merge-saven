# Merge Seven — walnut design system

Updated 2026-10-10. Preserve all six game modes and engine rules. Compose/Canvas owns presentation; engine/configuration owns gameplay.

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

Four primary destinations: Home, Challenges, Rewards, Profile. Settings opens from headers. Home offers one-tap Play/Resume, next level, daily challenge and expandable six-mode cards. Challenges groups Daily/Weekly, login gifts and quests. Rewards offers optional exact-benefit ads, owned booster quantities and coin costs, and cosmetics. Profile links local achievements, statistics and cosmetics. There is no sign-in, leaderboard, save upload or account export.

Compact Home omits its decorative hero so Play/Resume appears first. Enlarged-text headers move balances to a second row when needed; navigation labels stay on one line. All secondary pages respect safe drawing insets and the centered page bound. Cosmetics scrolls its preview with the catalog; narrow Challenges uses one gift column. Native confirmation dialogs support Android Back and scrollable level details. Result actions wrap instead of squeezing labels.

Home's decorative hero now uses text-free v4 walnut-and-hex artwork with a dark gradient behind native text. The v4 launcher is a bold ivory 7 on a gold hex with cyan/green/coral accents; its transparent foreground is fitted within the Android 66dp safe circle. Opaque square store icons and five legacy launcher densities use the same emblem. Feature banners and four portrait promotional illustrations live in store, with prompts and export records in art/promo-manifest-v4.json. Promotional panels are artwork; actual app screenshots remain in store/screenshots.

Gameplay hides navigation and banners. Select/tap-to-place and drag are supported. Drag starts after touch slop so rotation remains tappable. Press/drag previews placement and affected cells. Invalid moves explain rotation or another location without penalty. Primary boosters are Undo, Shuffle, Remove; More has advanced descriptions. Owned charges precede coins. Coin use requires confirmation. Zen preserves unlimited undo and ad suppression.

Time Attack Continue is limited to a blocked board with time remaining; clearing tiles cannot resolve timeout. Undo restores a move without rewinding its countdown or already-used freeze time.

First campaign play opens a three-page native animated placement/rotation/merge guide. Its page and completion persist; reduced motion uses still diagrams. Skip, Settings replay and Pause help remain available. The playable tutorial also persists actual placement, rotation and merge progress. Pause offers Resume, restart confirmation, audio controls and save-and-exit. Results show score/stars and persisted coin reward. Reward doubling shows that exact base amount as extra coins. Challenge results return Home rather than advance Campaign rules.

Reduced motion suppresses shake and excessive effects via JuiceController. Sound/music/haptics are independent. Cell accessibility labels, larger touch targets and color cues remain. Preview text must survive enlarged fonts; portrait tablets center the board above controls, while landscape and wide tablets use side controls with navigation insets. Save before exit/fullscreen ads; pause timers/audio around ads.

Ads-only economy: no purchases, subscriptions, paid products, purchase restoration or paid offers. New wallets start at 100; upgrades preserve old wallets/inventory/saves/settings. Rewarded ads require an earned callback and durable claim. Interstitials only at successful Campaign Next Level with grace/cooldown/caps. Home/Rewards adaptive banners have dedicated slots. UMP gates SDK initialization/load/show; Settings shows privacy options when required. All builds read the selected production app/unit IDs from admob.properties. QA uses marked test devices; profile=test remains available for demo inventory.

Existing wood, board and Zen background images are unchanged. Generated badge icons have transparent surroundings while retaining their interior artwork. Versioned runtime assets are optimized WebP and vectors. Masters, reference atlases, SVG diagrams, prompts and manifest stay in art, outside the APK. Store files stay in store. Verify transparent edges and adaptive masking; inspect icons at 24–48dp. See art/asset-manifest.json. Device/font/contrast and first-time human usability tests remain release requirements; static contrast tests are only one part of validation.

On compact screens, tutorial instructions use concise prompts. Hints and advanced boosters live in More so the board keeps practical space. Canvas drawing skips nonpositive geometry, and number fitting is a bounded proportional calculation.

## Smooth board presentation

Use short placement springs, 160 ms rotation interpolation and 260 ms merge flights. Keep labels upright and commit engine moves immediately. Reuse normalized Canvas hex paths and bounded text paint caches. Run particle frame clocks only while effects are active; pause them offscreen and cancel them with reduced motion. Read per-frame shake offsets in graphicsLayer. Deduplicate identical drag previews.

Ad inventory is configured exclusively in root `admob.properties`; profile=production is selected for all builds. Emulators automatically receive test ads; physical QA hashes belong in test.devices. Rewarded offers share one loaded SDK slot and retain placement-specific benefits. Banner disposal/pause/resume is scoped to its owning screen. Home resumes the newest valid save by its existing update timestamp.

Music uses one original 40-second ambient loop with asynchronous preparation, persisted preferences, focus ducking and short volume ramps. Opt-in local WorkManager reminders target the evening, suppress quiet hours/foreground/completed daily puzzles, cancel when disabled and open Daily. They use no remote push token or account.

Debug-only qa.fallback retries a failed production load once with a demo unit on an SDK-marked test device. It never bypasses consent and is disabled in release. Log and impression inventory labels distinguish demo fallback from production requests.
