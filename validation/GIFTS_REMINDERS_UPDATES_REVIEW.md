# Daily gifts, offline reminders and Play updates — October 5, 2026

## Branch and scope

Pulled `origin/feature/new-design`: already up to date at `1881180`, with no conflicts. Original wood/board artwork, package identity, six-mode rules and production AdMob IDs are preserved. No account, Play publication or backend deployment is part of this change.

## Daily gifts

Previously, each claim advanced `currentStreak` and immediately made the next gift available. Players could claim the whole seven-day cycle in a single date. Claims now use the existing durable profile ledger with a `<local-date>:daily_gift` entry. Wallet, stars and claim are saved together in Room; screen exit cannot cancel the pending gift save. The repository validates the exact day and canonical reward, independently of potentially stale UI items. The next gift becomes available on a later local date; missed-day and completed-cycle reset rules remain intact. The screen refreshes on resume and at local midnight.

Existing profiles retain coins, stars and claimed days. A one-time ledger marker records the old profile's last login as already claimed when legacy claims exist, conservatively preventing an extra gift on upgrade day. The existing serialized ledger supports this without a database schema change. Backward clock changes cannot reset the daily state or reopen a claim; this is an offline local-date policy, not server clock verification.

Loading a repository in a background reminder previously counted as a login. Hydration now only loads data; foreground resume and explicit daily actions record login dates. Notifications preview today's possible reward without advancing progression.

An unfinished daily board crossing midnight must also retain its own date. Both live completion and result recovery pass the session date into the reward repository. A mismatched date cannot consume today's attempt, overwrite today's official score or award today's challenge coins; the old board may still be played. The regression checks that a subsequent valid run for today still earns its reward once.

## Offline reminders

Reminders remain optional, on-device WorkManager jobs with no network requirement, a unique daily schedule around 7 pm, quiet hours, foreground suppression and at most one notification per local date. Disabling reminders cancels work and removes the notification. WorkManager timing is approximate, including under Doze.

Copy now prioritizes an available free gift, an earned quest reward, then today's puzzle. Gift amounts reflect consecutive/missed days and cycle completion. A completed puzzle no longer hides an unclaimed gift; players with all daily rewards completed receive no invitation. Expanded text, a gold accent and a direct claim/play action lead to Challenges. Permission denial and system notification settings remain respected.

## Play Store update prompt

Uses Google's `app-update:2.1.0` flexible update API. A bottom card appears on Home only when Google Play reports an available and allowed update. It reserves space above navigation, separate from banner inventory and gameplay. Update requests explicit player consent through Play; downloads allow continued play. A downloaded update offers an explicit Restart button, including after reopening the app. Later suppresses an available version for 24 hours; a newer version can be offered sooner. Offline/sideloaded check failures never block the game or fabricate availability. Install listeners are removed when the Activity is destroyed.

The Play prompt needs a Play-recognized installation, an eligible test/account track, matching application ID/signing identity and a higher uploaded version code. Google’s fake manager validates local behavior without contacting Play, installing an update or restarting the device. A real Store download/install must be checked through internal testing/internal app sharing; no version has been uploaded by this task. [Integration](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java), [Store testing](https://developer.android.com/guide/playcore/in-app-updates/test).

## UMP

Reviewed existing code against Google's setup guide. The launch update, SDK `canRequestAds()` gate, previous-session permission handling and Settings privacy options are already implemented. No further UMP code change is required for this task. Missing published forms for the production app ID must be fixed in AdMob Privacy & messaging; the client cannot publish them or override permission. Earlier HTTP 403 production serving evidence remains in `AD_DIAGNOSTICS.md`. [UMP setup](https://developers.google.com/admob/android/privacy).

## Validation

The final debug build, Android test APK and full debug lint passed in 6m 21s (`validation-gifts-updates-midnight-debug-build.log`). All **297 debug unit tests** passed with zero failures, errors or skips, including yesterday's board finishing after today's profile rollover.

The final installed debug APK passed the Android suite in **114.606 seconds** (`gifts-updates-midnight-device-tests.log`): 24 discovered, **23 executed successfully and one intentionally skipped manual consent-reset helper**. This covered save/database upgrades, the first-play animated guide, navigation, gift claim/recreation, all six modes' rotation/pause/save/resume, actual offline-worker notification delivery and same-date suppression, demo rewarded/interstitial/banner inventory, and Google's fake update download/explicit restart/Later. The update actions also remained visible at 1.8 font scale in small phone and short landscape layouts. The crash buffer was empty.

Device testing used the workspace-owned Android 30 emulator at 480×800 pixels and 240 dpi. No physical low/mid-range device, Android 13 notification permission prompt, real speaker/haptics, Play-delivered download/install or exact Doze schedule was validated. Existing background artwork was preserved. Production artifact checks follow below.

## Production artifacts

All **297 release unit tests** passed with zero failures, errors or skips. Release vital lint passed. The first packaging attempt ran out of Windows C: temporary space after compilation/optimization (`validation-gifts-updates-production-final-build.log`). Retrying unchanged sources with process-local TEMP/TMP and Gradle JVM `java.io.tmpdir` set to `E:/test/merge-saven/validation/build-temp` passed in **1m 38s** (`validation-gifts-updates-production-temp-retry-build.log`). No unrelated files were deleted.

| Artifact | Bytes | MiB |
|---|---:|---:|
| Production APK | 5,963,287 | 5.69 |
| Production AAB | 11,184,752 | 10.67 |

The package remains `com.mergeseven.game`, **1.7.0 / code 8**. APK v2 signature and 16KB-page zip alignment passed. AAB JAR verification passed; the local signing certificate is self-signed and its identity must match the Play upload configuration before publication. Both archives passed CRC integrity, compressed dex and unchanged runtime music-hash checks. APK music stays uncompressed for MediaPlayer resource descriptors. Original wood/board artwork was not changed. Generated release resources retain the four production AdMob IDs with `TEST_ADS=false` and `QA_AD_FALLBACK=false`.

Current hashes are in `compressed-artifacts.json`, `artifact-hashes.json` and `admob-review-artifacts.json`. Release artifacts are local outputs, not committed binaries or a Play upload. A future Play update needs a greater version code than the installed app; this task does not publish or bump the store release.

The final production APK upgraded the QA app without deleting data: the **860-coin** wallet and existing campaign save remained. Home, Rewards and Challenges opened; campaign result/next-level navigation, tray selection/rotation, pause and save-and-exit worked. Reopening retained Level 2 and 860 coins. No crash entries were recorded. The sideloaded emulator's actual Play availability check failed harmlessly and displayed no fabricated update.

Fresh production logcat (`gifts-updates-production-ad-logcat.log`) again records **HTTP 403 for banner, interstitial and rewarded** and UMP's **no forms configured** for the production app ID. The SDK reported `can_request_ads=true` in this run; consent did not prevent these observed requests. No production ad served. Review the correct app's Privacy & messaging, app/unit association, serving/approval state and Policy Center in AdMob. The rejection's precise account cause requires dashboard inspection; no account settings were changed. [Google load-error guidance](https://developers.google.com/admob/android/ad-load-errors).
