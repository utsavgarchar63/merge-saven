# Merge Seven — next graphics, animation and gameplay plan

Prepared 2026-10-12 against the current `feature/new-design` checkout. Status: implementation started; the first graphics slice is implemented, with remaining release work tracked below. `[x]` means complete, `[~]` means partly implemented, and `[ ]` means pending.

First slice: shared beveled hex renderer for the board, tray, drag ghosts, merge flights and cosmetic previews; Jewel/Marble/Neon/Gold materials; recessed cells; readable placement outlines; shared landing/rotation/merge timings; selection lift; and compact controls that free more room for the board. This does not complete all of Release 1. Validation and remaining limits are recorded in `../validation/JEWEL_RENDERER_REVIEW.md`.

## Product direction

Make Merge Seven feel like a polished, tactile puzzle game: jewel-like hex tiles, a clear walnut board, precise placement, satisfying chains, and meaningful progression. Keep the recognizable gold 7 icon and the v4 promotional art direction. The next release should make the actual playing board as attractive as the marketing art.

Use a 2.5D visual treatment within the existing Compose/Canvas renderer: layered depth, highlights, shadows and selective effects. Keep the existing board geometry and touch mapping. Continue supporting offline play, six modes, saved progress, optional ad rewards, reduced motion and color accessibility.

## What the code already has

| Area | Observed baseline | Next opportunity |
|---|---|---|
| Gameplay | Six modes, deterministic spawning, special tile traits, varied objectives, boosters, hint solver | Present these systems more clearly and build more authored challenges around them |
| Board | Native Canvas hexes with simple offset shadows and inset highlights | Material-aware bevels, recessed cells, focused lighting, clearer selection |
| Cosmetics | Tile themes mostly change colors; Marble, Neon and Seasonal board themes reuse the wood bitmap with overlays | Distinct textures, lighting and complete matching tile/board treatments |
| Interaction | Tap/drag placement, rotation, valid/invalid previews, placement springs and merge flights | Better finger visibility, clearer predicted outcomes and consistent feedback |
| Content | Bundled custom JSON definitions for levels 2–4, plus the normal level-rule system | More authored layouts and a visual chapter journey |
| Progression | XP, achievements, quests, daily gifts, records and cosmetic unlocks | A visible next milestone and better explanation of what the player earned |
| Performance | Particle quality guard; benchmark module covers startup and idle Home | Measure active dragging and busy chain reactions on physical devices |
| Brand | v4 launcher, feature banner, four promotional posters and Home art | Reuse this direction inside gameplay, results and chapter presentation |

The older `ADVANCED_FEATURES_PLAN.md` contains historical statements about accounts, purchases and leaderboards. The current design is an ads-only, local-progress game; AF7 is permanently disabled. Do not use old checked boxes as evidence that a production service is available. This plan extends the current source and `DESIGN_SYSTEM.md`.

## Release 1 — richer graphics and better control

This is the recommended first implementation batch. Deliver one complete, playable visual improvement before expanding content.

- [ ] **GX01 — Measure the baseline.** Capture current gameplay on small phones, a tablet and a representative mid-range physical device. Record startup, drag response, chain frame timing, memory and installed size. Separate emulator correctness checks from physical-device performance evidence.
- [x] **GX02 — Upgrade tile rendering.** Extract a shared renderer from `GameScreen.kt`; use it for board tiles, tray pieces, drag ghosts and cosmetic previews. Add a consistent side face, rim highlight, smooth face gradient and restrained contact shadow. Keep numbers upright, high-contrast and dynamically fitted, including values above 999.
- [~] **GX03 — Upgrade the board.** Add recessed empty cells, a subtle board rim and stronger occupied/empty separation. Use quiet backgrounds and keep the central playing area free of decorations. Make board sizing respond to the actual space after HUD and controls; review compact screens before adding decorative margins.
- [ ] **GX04 — Improve placement clarity.** Lift the ghost above the finger, show every occupied cell in multi-tile pieces and visibly snap to legal origins. Extend the current preview with a clear outline of the immediate matching group and resulting value. Derive predictions from a non-mutating engine simulation; do not alter the RNG, score or saved game during previews.
- [~] **GX05 — Improve the tray and HUD.** Clearly distinguish selected and available pieces, increase rotation clarity, show concise objective progress, and keep primary boosters reachable. Preserve tap-to-place as an equal alternative to dragging. Use at least 48dp for controls; do not pretend densely packed board cells are all 48dp targets.
- [~] **GX06 — Make chains feel rewarding.** Coordinate existing flights, short landing springs, merge pulses, bounded sparks, score movement and audio/haptic cues. Chain feedback should communicate where the merge happened. Keep effects brief and avoid covering tile numbers or consuming the next move.
- [ ] **GX07 — Refresh Home and results.** Keep Play/Resume prominent, show the next campaign milestone, and use illustrated mode cards in a responsive grid where space allows. Replace the long repeated mode descriptions with concise copy. Results explain score, stars, exact rewards and the next action, with a short skippable celebration.
- [ ] **GX08 — Add graphics controls.** Auto/High/Balanced/Low changes particles, trails and decorative shading. Low preserves readable tile depth and gameplay cues. Reduced motion independently disables movement/shake-heavy effects. Stop decorative animation when idle, paused or backgrounded.

Completion gate: all six modes remain playable, existing saves resume, old booster behavior is preserved, and the upgraded board is both clearer and smoother under the measured checks below.

## Release 2 — advanced gameplay and progression

Build on existing systems rather than adding another broad mode selector.

- [ ] **GP01 — Campaign chapters.** Introduce an illustrated chapter map with three initial themes, each containing ten reviewed challenge levels. First ship a ten-level chapter, validate its difficulty, then expand to thirty. Preserve existing level identities and progress; define versioned chapter mappings instead of renumbering saved levels.
- [ ] **GP02 — Better level variety.** Author shaped boards, deliberate starting tiles, frozen/stone obstacles, move-limited objectives and milestone challenge boards using existing traits/objectives. Introduce one mechanic at a time with a short playable explanation. Verify solutions with explicit legal action replays; solver success alone is not proof of a fair difficulty curve.
- [ ] **GP03 — Strategy coach.** Extend hints to explain a move, for example “Connect these three 4s to make 8.” Show what is immediate versus speculative. Run analysis off the main thread, reject stale results, and preserve current challenge-assistance and reward rules.
- [ ] **GP04 — Practice and replay.** Offer local replay of a completed run and an unscored retry of selected puzzle boards. Reuse deterministic replay infrastructure, with speed and pause controls. Practice must not overwrite the original scored Daily/Weekly attempt or award duplicate rewards.
- [ ] **GP05 — Visible mastery.** Show progress toward the next existing achievement or cosmetic unlock. Add chapter completion badges and optional challenge goals; retain persisted XP, inventory and rewards. Award each milestone atomically once, including after restart or rapid taps.
- [ ] **GP06 — More varied daily/weekly puzzles.** Curate rotating themes and objective sets, with compact previews and clear end times. A weekly local medal ladder can provide bronze/silver/gold goals without implying an online leaderboard. Version challenge rules with the seed so resumed runs do not change after an update.

Completion gate: every new authored level has a verified completion path, progression survives process death, practice is isolated from scored attempts, and no feature requires a network connection to start a normal run.

## Release 3 — complete theme collections

- [ ] **TH01 — Walnut Jewel.** Default premium walnut surface, colorful enamel tiles, gold accents and warm directional light.
- [ ] **TH02 — Moonlight Marble.** Distinct cool marble background, muted silver framing and matching stone-like tiles. Keep numerals and trait symbols crisp.
- [ ] **TH03 — Neon Arcade.** Distinct dark background with restrained luminous rims; keep the center quiet and avoid continuous flashing.
- [ ] **TH04 — Preview and equip.** Show the actual renderer and background together before equipping. Apply existing unlock rules and persist selection. Zen keeps its calm behavior and audio/motion defaults.

Treat these as complete material systems, not only color filters. Defer online tournaments, accounts/cloud saves, multiplayer and a level editor to separate projects with their own service and persistence plans.

## Animation plan

Use a consistent motion language: short tactile springs for direct actions, smooth movement for merges, and restrained celebration for achievements. Motion should explain the move and make successful play satisfying. The timings below are initial design targets to tune on devices; keep the existing 160 ms rotation and 260 ms merge flights as the starting baseline.

| Moment | Planned animation | Initial timing | Reduced-motion equivalent |
|---|---|---|---|
| Button or mode-card press | Gentle scale to 0.97 with a slight highlight; return on release | 80–120 ms | Immediate pressed color/border |
| Tray piece selection | Small lift, deeper shadow and gold outline; other pieces remain stable | 120–160 ms | Immediate selection outline |
| Drag pickup and movement | Lift ghost above finger, brighten valid footprint, track pointer continuously | Pickup 100–140 ms; movement follows input | Static lifted ghost and immediate footprint updates |
| Piece rotation | Rotate tile positions by 60 degrees around the piece origin; numbers stay upright | 160 ms | Immediate rotation |
| Valid placement | Snap to the selected origin, settle with a small spring and brief cell highlight | 140–200 ms | Immediate placement with static highlight |
| Invalid placement | Return ghost to tray with a brief warning outline and a concise reason | 120–180 ms | Immediate return with warning text/outline |
| Merge | Matching tiles converge into the destination; upgraded tile expands briefly and settles | Flight 220–280 ms, pulse 100–140 ms | Immediate upgraded tile with resulting value highlighted |
| Chain reaction | Successive merge groups animate in order; bounded gold sparks and a short chain label | Usually 300–700 ms total; shorten long chains | Show final board and concise chain summary |
| Special tile activation | Frozen cracks recede, stone fragments fade, bomb clears a bounded ring, multiplier badge pulses | 180–300 ms | Immediate trait/result update with distinct symbol and announcement |
| Booster use | Undo restores changed cells; shuffle rearranges pieces; remove fades the chosen tile; other boosters highlight affected cells | 160–260 ms | Immediate result and clear affected-cell cue |
| Score and objective updates | Brief score highlight, smooth progress fill and compact milestone emphasis | 150–250 ms | Immediate exact score/progress |
| Level completion | Earned stars appear in sequence, reward illustration settles, a small coin trail reaches the balance | 600–900 ms, skippable | Immediate final stars, reward and balance |
| Game over | Quiet board dim and clear result-panel appearance; timeout immediately shows its reason | 180–240 ms | Immediate result panel |
| Screen and sheet transitions | Short fade with small vertical movement for panels; predictable navigation transitions | 160–220 ms | Immediate transition |
| Chapter unlock and mastery reward | Unlock badge appears once and the newly available destination is highlighted | 300–500 ms, skippable | Static unlocked badge and explanation |
| Theme preview/equip | Preview changes smoothly between complete material sets | 160–220 ms | Immediate theme change |
| Playable tutorial | Demonstrate pickup, rotation and three-tile merge only while that lesson is visible | One brief demonstration per lesson; optional replay | Still diagrams and numbered steps |

### Animation implementation tasks

- [~] **AN01 — Shared motion tokens.** Define consistent durations, easing, spring settings and emphasis limits in the UI layer. Use the same selection, placement and tile animation treatment on board, tray and previews.
- [~] **AN02 — Tile movement.** Build on the existing arrival springs, rotation and `MergeFlight` handling. Use stable tile IDs and authoritative origin/destination coordinates; reset transforms correctly on restart, undo, resize and mode change.
- [ ] **AN03 — Event choreography.** Map actual engine events to placement → merge → chain → score/objective feedback. Add special-trait and booster presentations only where the engine supplies the corresponding outcome. Extend `JuiceController` instead of adding independent clocks for every tile.
- [ ] **AN04 — Responsive continuation.** Commit and save authoritative engine results independently of decorative playback. Preserve engine-resolution input rules, but add no extra animation-only input lock. When another legal action arrives, finish or reconcile old effects against the current board; never render a stale ghost as a real tile.
- [ ] **AN05 — HUD and reward motion.** Animate displayed progress toward the committed value. Show the exact earned reward promptly, make longer celebrations skippable, and never grant coins/stars from animation-completion callbacks. Resume or skip without awarding twice.
- [ ] **AN06 — Navigation and onboarding.** Give Home cards, dialogs, bottom sheets and the chapter map consistent motion. Keep layout stable during button presses, retain screen-reader focus, and stop tutorial demonstrations when the lesson is hidden.
- [ ] **AN07 — Quality and lifecycle.** Use one active effect scheduler per visible board, bounded particle pools, cached draw resources and no bitmap decoding inside a frame loop. High/Balanced/Low scales decorative effects; reduced motion overrides movement regardless of tier. Cancel effects on pause, background, screen exit and process restoration.
- [ ] **AN08 — Audio and haptic sync.** Trigger optional cues from accepted actions and actual merge events. Keep stronger feedback for meaningful chains, respect independent settings and avoid repeated cues on recomposition or replay seeking. Time Attack countdown always uses its authoritative clock.
- [ ] **AN09 — Theme-aware effects.** Walnut uses restrained gold sparks, Marble uses soft cool highlights, and Neon uses short luminous outlines. Keep brightness localized, avoid flashing and keep numbers visible through effects. Zen uses a quieter presentation with no disruptive shake.
- [~] **AN10 — Verification.** Check rapid placements, long chains, rotation during selection, undo, booster confirmation/cancellation, resize, background/return, replay seeking and reduced-motion changes during an effect. Verify final board/score equality with animations enabled and disabled; measure active frame timing rather than judging a promotional animation.

Release 1 includes AN01–AN08 for the existing gameplay, screens and rewards. Release 2 extends the same system to chapters, mastery and practice replay; Release 3 completes AN09 for each theme. AN10 is a gate for every release. This expands GX06 and the existing UI work, rather than introducing a separate animation engine.

### Animation assets

Use native Canvas/Compose motion for hex tiles, numerals, paths, sparks, progress and controls. Generated backgrounds and transparent reward ornaments can be layered or transformed where useful. Generate additional original textures or cutouts only when the art review identifies a missing element; record them in the existing asset manifest. Keep live gameplay animation separate from baked GIF/video sequences so values, themes and interactions stay synchronized.

## Asset generation plan

Generate original bitmap artwork through built-in imagegen when implementation needs it. This planning pass does not need new artwork. Reuse the current v4 icon and banners.

| Asset | Initial amount | Master / runtime target | Use |
|---|---:|---|---|
| Quiet board backgrounds | 3 themes | 1536 × 2048 masters; optimized device-appropriate WebP exports | Walnut, Marble and Neon gameplay; protect central board readability |
| Chapter header illustrations | 3 | About 1536 × 768 masters; about 768 × 384 runtime | Level journey and chapter completion |
| Transparent milestone ornaments | 3 | 1024px square masters; 256px runtime | Chapter badge/reward illustration |
| Updated result/share artwork | 1 | 1080px square master; optimized export | Result framing with space for native score text |

Create runtime tile faces, numerals, trait symbols, control icons, borders and interactive effects in Canvas/vector code. Asset generation supplies textures and illustrations, not baked game boards, button labels or scores.

For every generated asset: record the prompt and role, inspect alpha/cropping/text-free areas, save the master under `art/masters/`, export versioned runtime files, and record dimensions/bytes in a manifest. Review the artwork in the actual screen at small sizes before accepting it. Target no more than 3 MB of additional compressed runtime art across the first two releases; measure decoded memory independently of file size.

## Implementation map

| Work | Main code areas |
|---|---|
| Renderer and visual tokens | `ui/game/GameScreen.kt`, new shared tile renderer, `ui/theme/TileTheme.kt`, `ui/theme/BoardTheme.kt` |
| Preview and interaction | `ui/game/GameScreen.kt`, `ui/game/GameViewModel.kt`, placement/merge engines |
| Effects and quality tiers | `ui/feel/JuiceController.kt`, `ParticleCanvas.kt`, `FrameBudgetGuard.kt`, settings repository/UI |
| Motion choreography and transitions | Shared UI motion tokens, existing merge flights and arrival springs, `app/AppNavGraph.kt`, result/tutorial composables |
| Home, map and results | `ui/home/HomeScreen.kt`, `ui/levels/LevelsScreen.kt`, current result composables |
| Chapters and authored challenges | `game/levels/`, `game/objectives/`, `assets/levels/`, level/progress repositories |
| Coaching and practice | `game/solver/`, `game/replay/`, mode records and routing |
| Validation | Existing engine/persistence tests, `app/src/androidTest/`, `benchmark/` |

Keep presentation separate from rules. Rule changes require an explicit decision recorded in the core plan before implementation. Persist new chapter/practice state through versioned snapshots or Room migrations with meaningful migration coverage. Add narrowly scoped default-off rollout flags for new work; existing AF flags enable many shipping features and should not silently enable unfinished additions.

## Acceptance targets

These are proposed targets to measure, not performance claims about the current build.

- **Smooth gameplay:** target 60 FPS on the selected mid-range device. At 60 Hz, use approximately 16.7 ms as the total display-frame budget. Record Macrobenchmark frame timing/overrun percentiles for repeated dragging, rotation and a busy chain; aim for at most 5% missed-deadline frames in the agreed repeatable workload. Investigate regressions against baseline rather than relying on the particle guard alone.
- **Responsive placement:** target visible feedback within 100 ms of input under that workload, including when effects are active. Confirm legal/illegal previews across rotations, edge cells, holes and multi-tile pieces.
- **Correct animation:** enabled/disabled motion produces identical authoritative game results. Effects reconcile rapid input, cancel cleanly across lifecycle changes and never replay a reward claim. Reduced motion removes shake, flights and looping motion while preserving understandable outcomes.
- **Readable graphics:** every value and special trait is identifiable at normal phone size; labels remain legible at 150% and 200% font scale. Color-accessibility modes and reduced motion still communicate the same result.
- **Durable progress:** force-stop/relaunch preserves board, tray, seed, mode, timer rules, stars, rewards and equipped theme. Practice and replay cannot mutate earned progress.
- **Fair content:** short trials with at least five new players; target four of five completing the first teaching merge without explanation outside the game. Review where players misplace pieces or fail to understand an objective.
- **Stable release:** existing engine tests plus focused preview, chapter, reward-idempotency and migration checks pass; debug and release builds pass; no crash/ANR during a 30-minute device soak. Recheck backgrounding, offline startup, ad return and enlarged-font layouts.

Cache paths, brushes and draw-only resources where useful; keep changing animation state in drawing/layer phases when possible. Android documents the caching behavior of `drawWithCache` and efficient layer transformations: [Compose graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers). The performance measurement approach follows [slow rendering guidance](https://developer.android.com/topic/performance/issues/render) and [Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics).

## Delivery order and effort

| Stage | Rough developer effort | Deliverable |
|---|---|---|
| Baseline and one-screen visual prototype | 2–3 days | Measured baseline plus new board/tile vertical slice |
| Release 1 graphics and interaction | 5–8 days | Full board/tray motion, merge and booster choreography, Home/results transitions and graphics settings |
| Release 2 first chapter and advanced experience | 7–12 days | Ten authored levels, chapter flow, coaching and isolated practice |
| Release 3 themes and expanded content | 5–8 days | Three complete themes and expansion toward thirty reviewed levels |
| Final device checks and fixes | 3–5 days | Release artifacts and documented QA evidence |

Estimate: 22–36 developer days, roughly 5–8 working weeks for one developer including art integration and QA. Actual effort depends on the first rendering measurements and level testing. Each release gets its own verification gate; ship Release 1 independently once it passes. First implementation priority: **shared tile renderer → clearer board/tray → precise placement → coordinated effects → first authored chapter**.
