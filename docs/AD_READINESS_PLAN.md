# Ad readiness and Home modes

Implemented on 9 October 2026.

## Behavior

- Register rewarded and interstitial demand when the app launches and resumes.
- Request inventory only after UMP permits ads, AF9 is enabled, and the live-ops ad gate allows it.
- Keep one shared rewarded ad ready for coin, hint, continue, daily, and double-coin offers. Deduplicate SDK loads across all offers.
- Check missing inventory while the app is in the foreground. Recover load failures with shared delays of 30, 60, 120, 240, then 300 seconds. Continue recovering after the third failure. Explicit retry can shorten the longer delays, while preserving the minimum 30-second interval and deduplicating in-flight requests.
- Replenish requested formats as soon as a fullscreen ad closes or fails to show. Grant rewards only through the earned callback.
- Expire fullscreen inventory after one hour using elapsed time, clear its availability, and refresh when the app is active.
- Show distinct privacy-check, loading, ready, and unavailable states. When unavailable, offer an explicit retry; when ready, offer the actual reward. Retry requests inventory without opening an ad later. Rewards still require a separate tap on ready inventory and the SDK earned callback.
- Retry failed consent updates while the app is visible and on an explicit availability check. Deduplicate UMP requests and continue using the SDK's authoritative permission.
- Disable automatic demo fallback in the default production profile, including debug builds.
- Show all six Home mode cards directly, with no Show/Hide modes button. Keep the adaptive banner in a separate footer below the scrollable content so every mode remains reachable.
- Cancel banner retries when detached. Pause maintenance while the app is in the background; invalidate inventory and pending work when consent or the ad gate is withdrawn.

“App open” here means preloading existing formats at application startup. No new app-open ad placement or automatic launch ad is added.

## Validation

- JVM regression tests cover shared inventory, duplicate loads, retry boundaries, more than three consecutive failures, the five-minute cap, successful-load reset, and consent invalidation.
- Demo SDK instrumentation checks startup loading without explicit test preload calls and loading a newly sized banner after rotation. A Compose navigation regression checks direct access to all six mode cards without a visibility toggle.
- Build with the normal production profile for delivery. Use `-PadmobProfile=test` for online demo instrumentation; keep that override out of production configuration.

### Results

- Production-profile debug build passed; generated `TEST_ADS` is false.
- All 300 JVM tests passed.
- Demo SDK startup inventory and adaptive-banner rotation test passed on the available Android 17 preview emulator.
- Manual Home inspection confirmed all six cards are accessible directly with the banner below the scrollable content.
- Manual demo reward check confirmed a tap opens a rewarded ad, the earned callback grants exactly 100 coins, and dismissal reloads rewarded inventory and restores the ready Watch ad button.
- The Compose navigation regression compiles but could not run on this preview image: Espresso 3.6.1 fails during initialization because `InputManager.getInstance` is unavailable. Run it on a supported Android image.

Actual fill still depends on AdMob serving, network access, and consent. Preloading cannot guarantee an ad before the first load completes or while offline.

Updated recovery and navigation validation: [14 October review](../validation/ADS_NAVIGATION_REVIEW_2026-10-14.md).

Reference: [Google rewarded ad guidance](https://developers.google.com/admob/android/rewarded) describes preloading, one-hour expiration, and reloading after dismissal.
