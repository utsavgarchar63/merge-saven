# First graphics implementation — 2026-10-12

Scope: the first playable slice of `docs/NEXT_GAMEPLAY_GRAPHICS_PLAN.md`, not the entire roadmap.

## Changes

- Shared `HexTileRenderer` for board tiles, tray pieces, floating drag previews, merge flights and cosmetic previews. Reuses a normalized hex, bounded surface/text caches and the bundled Nunito font.
- Beveled rims, side walls, contact shadows, a face gradient with unchanged central fill for numeral contrast, and Jewel/Marble/Neon/Gold material details. Theme IDs, value colors, special-trait overlays and color-accessibility cues remain supported.
- Recessed empty cells and translucent placement/hint outlines that keep occupied numbers visible. Merge-flight fallback colors now follow the equipped theme and color-accessibility palette.
- Actual hex/material/background previews in Cosmetics, replacing square sample tiles and board cells.
- Shared rotation (160ms), merge (260ms) and landing spring tokens, plus a new 140ms tray-selection lift that respects reduced motion.
- Short-screen header/booster/tray spacing improvements at normal font sizes. Primary controls and tray slots remain at least 48dp; large-touch and enlarged-font variants keep their previous sizing. This addresses the tiny board found during initial 320dp-wide emulator inspection.

No new generated bitmap assets are required for this slice. Existing game rules, RNG, timers, snapshots, wallet, unlock IDs and ad behavior are not changed.

## Automated checks

- `:app:assembleDebug` and `:app:testDebugUnitTest`: passed; 300 tests, zero failures/errors.
- `:app:assembleDebugAndroidTest` and `:app:lintDebug`: passed. Lint reports zero errors and 105 warnings.
- `HexTileRenderingTest`: exercises actual Android drawing across all five tile themes, extreme numeric values, every trait and cell states; checks that invalid/nonfinite geometry does not paint or crash.
- `GameModesSmokeTest`: exercises actual controls for Campaign, Endless, Time Attack, Zen, Daily and Weekly, including rotation, pause, save and resume with reduced motion enabled. Test fixtures restore previous save slots and settings.

Final runner result: **OK (3 tests)** in 81.609 seconds, covering the two rendering cases and the six-mode scenario. This was rerun after the compact-layout changes.

Final build log: `validation-jewel-renderer-compact-build.log`. Final emulator runner output: `validation-jewel-renderer-final-instrumentation.log`. Logs are intentionally ignored by Git.

## Visual and device review

QA uses only the workspace-owned `MergeSeven_QA30` emulator. `screenshots/jewel-materials-preview.png` is a native rendering specimen, not promotional art. It shows all themes, large labels, trait silhouettes and empty/selected/occupied/hint cells. The specimen was made responsive after the first capture clipped its lower rows at the emulator's 240dpi override.

The initial compact gameplay capture is `screenshots/jewel-gameplay-compact-initial.png`. It was taken after the renderer upgrade, before reducing header/control spacing; it documents the board-space issue rather than a before-image of the old renderer.

- `screenshots/jewel-gameplay-compact.png`: final 320dp-wide compact layout at 240dpi. The center-cell semantic bounds increased from 13px to 22px wide with the same 61-cell campaign board. This is a layout observation, not a touch-target guarantee for every board cell.
- `screenshots/jewel-gameplay-placed.png`: selected slot 2, rotated it and tapped cell (0,0). Two 8 tiles appeared at (0,0) and (1,0); the objective advanced to 8/16 and wallet stayed at 960.
- `screenshots/jewel-gameplay-drag.png`: dragged the following 2/2 piece onto empty space. Two 2 tiles appeared at (1,1) and (0,2), confirming lifted-ghost/drop alignment after the layout change.
- `screenshots/jewel-gameplay-standard.png`: 480dp-wide portrait layout at 160dpi, visually inspected with occupied tiles and readable tray controls.
- `screenshots/jewel-gameplay-largefont.png`: same portrait layout with 150% font scale, visually inspected for board/control separation and complete labels.

Restored the emulator's original 240dpi override and 1.0 font scale after these captures. No app data was cleared. The manual placements are confined to this workspace-owned QA emulator.

## Remaining work

More board framing, predicted merge outcomes, broader effect choreography, graphics quality settings, refreshed Home/results, authored chapters and new background collections remain in the roadmap. Existing wood textures still back board themes in this slice.

These are emulator correctness/visual checks. Physical-device frame timing, input latency, battery behavior, real audio/haptics, TalkBack traversal and the complete enlarged-font/tablet test matrix remain release checks. No measured 60 FPS claim is made. The roadmap's acceptance targets remain targets.
