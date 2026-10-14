# Production ad recovery and navigation

Validated on 14 October 2026 after `git pull --ff-only` reported the branch up to date. The owner reports AdMob app status **Ready**.

## Delivered behavior

- Reward offers show privacy-check, loading, ready, or unavailable states instead of a permanent disabled “Ad not ready yet” button.
- Unavailable offers can request a retry. Retry never schedules a surprise fullscreen ad; a separate tap on a ready offer starts the ad. Earned callbacks remain the only reward source.
- All reward placements share one observable inventory state. In-flight loads are deduplicated. Explicit retry preserves the minimum 30-second request interval and failure count; automatic recovery continues with capped exponential backoff.
- Failed consent checks recover while the app is visible. Overlapping UMP updates are suppressed, and requests remain gated by UMP permission, AF9, and the live-ops gate.
- Default production builds use production IDs with QA demo fallback disabled. Release already enforces fallback off.
- Rewards now has a working header Back control. Rewards → daily gifts uses primary-tab navigation, preserving one Home anchor. Secondary Back and game Home actions have a Home fallback.
- System Back pauses an active game; Back from the pause screen resumes it. Modal actions close first. Game results return Home.

## Checks

| Check | Result |
| --- | --- |
| Production debug APK and instrumentation APK | Passed |
| JVM suite | 305 tests, zero failures, zero skips |
| Android lint | Passed |
| AppNavigationTest | Four tests passed |
| GameModesSmokeTest | Passed: all six modes rotate, pause, save, resume |
| NavigationRecoveryTest | Both tests passed: repeated Rewards/Challenges navigation and system Back preserving a Zen board |
| Explicit consent reset on SDK-marked QA emulator | Passed; no game data cleared |
| Explicit production inventory integration | Passed: all three formats ready, matching observable states, fallback disabled |

One existing solver test exceeded its wall-clock budget while the emulator was starting. It passed in isolation and on the subsequent full-suite run; solver implementation was unchanged. The initial navigation-test run exposed a non-void Kotlin test signature; the signature was corrected and both new tests passed on the rebuilt test APK.

## Fresh production configuration evidence

Isolated Android 30 QA emulator: `MergeSeven_AdsOct14`, serial `emulator-5556`. The SDK explicitly marks it as a **test device**. Generated configuration: `TEST_ADS=false`, `QA_AD_FALLBACK=false`.

Fresh UMP response at 10:25:50 IST: consent status 1, `can_request_ads=true`, privacy options not required. SDK initialization completed and all formats requested their configured production units. No consent bypass or demo substitution was used.

The final explicit integration run loaded all formats in the same session:

| Format | Production unit | Loaded at (IST) | Response ID |
| --- | --- | --- | --- |
| Banner | `ca-app-pub-6926810742930516/6120170224` | 10:29:27 | `KQzPatjhO9-_3LUPhMeBmQ8` |
| Interstitial | `ca-app-pub-6926810742930516/7995435230` | 10:29:31 | `KQzPaoS7Os3k4t4PqPCv8Aw` |
| Rewarded | `ca-app-pub-6926810742930516/4615516865` | 10:29:33 | `KQzPaoiAOJ7D3LUP3YzT8Q8` |

App ID: `ca-app-pub-6926810742930516~9728984035`. All successful responses reported `com.google.ads.mediation.admob.AdMobAdapter`. An earlier interstitial request returned code 3, “No fill”; a subsequent automatic retry loaded successfully. The previous October 8 HTTP 403 observation did not reproduce in this session.

These results confirm production-unit configuration, consent, SDK requests, recovery, and readiness propagation on a test device. They do not establish monetized serving on a physical device or guarantee fill for every request. No ad creative was clicked.

A subsequent Home launch recorded a banner impression. [Home screenshot](screenshots/home-production-ad-oct14.png) shows the generated game banner first and the SDK banner below the scrollable content; the ad is visibly labeled **Test Ad** because the emulator is an SDK test device.

Local diagnostic logs (ignored by Git): `validation-ads-oct14-production-final.log`, `validation-ads-oct14-production-inventory.log`, `validation-ads-oct14-navigation.log`, `validation-ads-oct14-navigation-recovery.log`, `validation-ads-navigation-oct14-full-unit.log`, `validation-ads-oct14-sdk-build.log`.

## Repeat explicit online QA

Build with the default production profile. Install both APKs on an SDK-marked QA emulator, then run:

```powershell
adb -s <qa-serial> shell am instrument -w -r -e productionInventoryQa true -e class com.mergeseven.game.ads.AdSdkInventoryTest#productionInventoryPublishesReadyStateWithoutDemoFallback com.mergeseven.game.test/androidx.test.runner.AndroidJUnitRunner
```

The test is skipped without explicit QA arguments, in release, with the test profile, or on an unmarked device. It waits for consent and all inventory states; it does not show fullscreen ads or grant coins.

References: [Google consent guidance](https://developers.google.com/admob/android/privacy), [load-error diagnostics](https://developers.google.com/admob/android/ad-load-errors).
