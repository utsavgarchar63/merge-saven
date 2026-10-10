# Local store and sharing artwork

No files have been uploaded or published.

## Current promotional artwork — v4

Original built-in imagegen artwork, reviewed and exported on 2026-10-10. Open `promo-preview.html` for a downloadable gallery, or `promo_contact_sheet_v4.jpg` for an overview.

| File | Dimensions | Purpose |
|---|---|---|
| app_icon_v4.png | 512 × 512 | Current store icon; gold 7 with colorful hex tiles |
| feature_graphic_v4.png | 1024 × 500 | Current game feature graphic |
| banners/merge_seven_landscape_v4.png | 2048 × 1000 | Large landscape promotional banner |
| promotional/01-place-match-merge.png | 1080 × 1920 | Three matching tiles merging into the next value |
| promotional/02-six-ways-to-play.png | 1080 × 1920 | Campaign, Endless, Time Attack, Zen, Daily and Weekly |
| promotional/03-build-big-chains.png | 1080 × 1920 | Illustrated chain reactions |
| promotional/04-a-fresh-challenge.png | 1080 × 1920 | Daily puzzle and gift artwork |
| icon_masks_v4.png | 768 × 240 | Circular, rounded and square adaptive-icon previews |

The portrait panels are promotional illustrations, created without app captures or phone frames. The existing `screenshots/` directory contains actual app captures. Use the asset type appropriate to the placement.

The app uses an optimized text-free Home banner with native Compose copy, a transparent adaptive foreground inside Android's 66dp safe circle, and square/round legacy launcher icons at all five densities. Promotional posters and PNG masters are excluded from the APK. Store icon backgrounds are opaque and square; the launcher applies its own system mask.

Source masters and prompts: `../art/masters/promo_v4/`, `../art/promo-manifest-v4.json`. Regenerate deterministic exports with `python tools/export_promo_art.py` from the project root (requires Pillow). Some exports resize the generated masters slightly; this is recorded in the manifest. Publishing to a store has not been requested.

## Previous artwork — v2

| File | Dimensions | Purpose |
|---|---|---|
| app_icon_v2.png | 512 × 512 | Store icon, generated emblem with a composed walnut background |
| feature_graphic_v2.png | 1024 × 500 | Store feature art with Nunito title composed separately |
| share_result_card_v2.png | 1080 × 1080 | Source master for the dynamic in-game score card |
| screenshots/ | Actual emulator captures | Six screenshots of the implemented app; marked demo ads may be visible during debug QA |

Artwork masters and generation/export records are in `art/`. Runtime exports are in `app/src/main/res/drawable-nodpi/`; store graphics, contact sheets and source masters are excluded from the APK. Launcher icon masking still needs review on representative OEM launchers.

English listing drafts and sourced keyword research are in en-US/ and KEYWORD_RESEARCH.md. Metadata is prepared locally and makes no guaranteed ranking claim.

Before submission, replace local test-ad captures with approved release-QA screenshots, verify current Play Console screenshot requirements, complete the privacy/data-safety and target-audience declarations, and review the local release checklist. Publishing requires a separate request.
