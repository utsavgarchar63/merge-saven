# AdMob production review — October 8, 2026

Pulled `origin/feature/new-design`: already up to date at `4ed76f0`, no conflicts. The user supplied the published Play URL `https://play.google.com/store/apps/details?id=com.mergeseven.game`; its package matches the application ID. The web reader could not retrieve the listing, so no independent Store version, developer website or AdMob readiness status was established.

## Code review and fixes

All app/unit IDs still come from `admob.properties`, whose default profile is production. Demo QA uses a build override; the published build uses production inventory from the start. A successful demo request is not a prerequisite or serving guarantee for a public user's production request. Release demo fallback stays disabled. Physical QA must register a test device; the emulator is automatically SDK-marked. No live ad click or external account change is part of QA.

The existing implementation deduplicates rewarded requests into one inventory slot, uses earned callbacks and durable result/date claims, preloads asynchronously, observes consent and the ad kill switch, prevents overlapping fullscreen ads, discards expired inventory, and distinguishes load/show/impression/paid/reward events. Interstitial presentation still requires an eligible successful campaign transition, grace period and frequency caps. Rewarded offers require player action. No automatic fullscreen show is added on launch or after a late load.

Three reliability defects were fixed:

- Native adaptive banners were retained whenever the container was the same, even when its width changed. Layout changes now rebind using the measured container width; older suspended bindings cannot replace a newer one. Zero-width layout waits rather than using the full screen width.
- A delayed retry was abandoned if a fullscreen ad was still showing when its delay ended. It now waits for that ad to close and then rechecks consent, kill switch, inventory generation and banner ownership before retrying.
- A synchronous SDK `show()` exception could leave `fullscreenShowing=true`. Exceptions now release that state, return a failed result, record diagnostics and allow replacement inventory. Fullscreen presentation also verifies the Activity is resumed and that a rewarded call actually uses a rewarded placement.

## Consent behavior

No custom dialog is forced. The existing UMP flow calls `loadAndShowConsentFormIfRequired`; when no form is required or cached SDK permission permits requests, ads load directly. The SDK's `canRequestAds()` result remains authoritative, including after an update error. Required regional consent/privacy options are retained rather than hardcoding permission to true. Removing this check cannot repair an AdMob HTTP 403 rejection. [Google UMP guidance](https://developers.google.com/admob/android/privacy).

## Dashboard verification for the published app

These are owner checks, not verified account states:

1. In Apps → Merge Seven → App settings, link the exact published Play URL to the app ID `ca-app-pub-6926810742930516~9728984035`; avoid creating another app and replacing IDs without checking the current association. [Store linking](https://support.google.com/admob/answer/10037806).
2. Verify the app readiness status is Ready and complete any requested account verification. Play publication alone is not AdMob readiness approval. Review Policy Center and serving limits. [Readiness](https://support.google.com/admob/answer/10564477).
3. Check the app-ads.txt verification state and the developer website linked in Play. Publish the account's personalized snippet at that website's root `/app-ads.txt` if needed; do not package it as an Android resource. [Setup](https://support.google.com/admob/answer/9363762).
4. Check each existing unit is attached to this app and has the matching rewarded/interstitial/banner format. Check mediation only for networks intentionally configured in the account.
5. Publish applicable regional messages in Privacy & messaging for this exact app. No dialog is displayed when the SDK determines one is unnecessary. Missing forms and serving rejection are separate diagnostics.

If HTTP 403 persists after dashboard corrections, Google's load-error guidance recommends capturing the request using Ad Inspector and contacting support. The response alone does not identify the precise account restriction. [Load errors](https://developers.google.com/admob/android/ad-load-errors).

## Validation

Debug build, APK/Android test assembly and debug lint passed in **3m 46s** (`validation-ads-oct8-demo-final-build.log`). All **297 debug unit tests** passed with zero failures, errors or skips. A test-helper compile error was corrected before this successful build; no source compilation failure remains.

The demo SDK check loaded rewarded, interstitial and adaptive banner inventory. The resized landscape banner also loaded, establishing that the container-width change triggers fresh native inventory. An actual interstitial displayed Google's **Test Ad** label; logcat recorded `ad_shown` and `ad_impression`, and `AdActivity` was the resumed Activity. Manually closing the visible SDK X returned `AdResult.Completed`, cleared fullscreen state and reloaded interstitial inventory (`ads-oct8-demo-sdk-tests.log`, `ads-oct8-demo-initial-logcat.log`). This was a manually assisted display check, not an automated creative-close guarantee. A later attempt to automate Back/accessibility-close exposed a test-helper assumption: the creative did not expose a usable accessible close action. That helper was removed; SDK-owned close UI stays a manual QA check. The stable regression suite retains the actual inventory/rotation check.

The final stable-device suite passed in **105.127 seconds** (`ads-oct8-final-device-tests.log`): **24 discovered, 23 executed successfully and one intentionally skipped manual consent-reset helper**. This includes saves/schema upgrades, tutorial, navigation, all six modes, daily-gift recreation, offline reminders, update UI and actual demo inventory including banner rotation. The crash buffer was empty. Testing used the owned Android 30 emulator at 480×800/240 dpi; physical-device performance and live serving are not established by this emulator check.

The real Rewards-screen button displayed a Google rewarded test creative. SDK logcat recorded one earned-reward event; after the visible close button, coins changed **860 → 960** and the daily allowance **3 → 2**. Restart retained **960** (`ads-oct8-final-demo-logcat.log`). The SDK controls, advertiser link and Install button were not used to generate a live click. Consent gathering showed no unnecessary dialog in this session. Fullscreen inventory reloads asynchronously after consumption, so the next offer may briefly say Try ad again.

The production release APK/AAB build passed in **6m 36s** (`validation-ads-oct8-production-build.log`), including all **297 release unit tests** and release vital lint. Release resources retain all four production IDs with `TEST_ADS=false` and `QA_AD_FALLBACK=false`. APK v2 signature, 16KB-page zip alignment, AAB JAR verification, archive integrity, compressed dex and unchanged music-hash checks passed.

| Output | Bytes | MiB |
|---|---:|---:|
| Production APK | 5,963,296 | 5.69 |
| Production AAB | 11,185,582 | 10.67 |

Current hashes are in `admob-review-artifacts.json`, `compressed-artifacts.json` and `artifact-hashes.json`. Version remains **1.7.0 / code 8**; no Play upload or release version change was performed. Confirm the Store version code and upload signing identity before preparing a higher-code update. The final installed production APK preserved the **960-coin** wallet. Fresh logcat (`ads-oct8-production-logcat.log`) records the Google adapter READY and **HTTP 403** for all three production formats. No production inventory loaded or displayed; no demo fallback ran. UMP independently reported no forms configured for the production app ID, but its SDK permission result was `can_request_ads=true`, so the observed requests already loaded directly without a consent form. Removing UMP would not repair this observed server rejection.

Rewards displayed Try ad again. Tapping it granted nothing: wallet stayed **960** and allowance **2 of 3**. Home navigation remained available; the crash buffer was empty. Production-unit tests were on an automatically marked emulator, not evidence of live serving to public players. The exact account restriction and dashboard configuration remain unverified. The owner checklist above is required to investigate production serving; if the app is linked and Ready with no restrictions, use Ad Inspector's rejected-request details with Google support.
