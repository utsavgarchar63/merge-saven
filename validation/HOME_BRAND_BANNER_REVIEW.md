# Home brand banner — 2026-10-12

Home now displays the generated Merge Seven feature artwork as its first scrollable content item on all screen sizes. It replaces the text-free hero and its compact-screen visibility condition. Play/Resume follows the banner.

The original master remains `art/masters/promo_v4/feature.png`. `tools/export_promo_art.py` exports `home_brand_banner_v4.webp` at 1024 × 500, quality 90 (125,054 bytes). The Image uses the matching aspect ratio and ContentScale.Fit to retain the full title/artwork, rounded corners and an accessible brand/tagline description. No new generation was needed because the requested branded artwork already existed.

`:app:assembleDebug` passed; see ignored `validation-home-brand-banner-build.log`.

Native QA on the workspace-owned MergeSeven_QA30 emulator verified the banner description appears before the resume panel. `screenshots/home-brand-banner-compact.png` shows the 320dp-wide layout, including its marked test ad. On that short screen, a normal content swipe brought Resume into view. `screenshots/home-brand-banner-standard.png` shows the 480dp-wide layout with the complete banner and Resume visible. The emulator's original 240dpi override was restored after review.

Existing generated masters and store art were reused without overwriting them. This change does not modify gameplay or ad configuration.
