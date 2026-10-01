# Merge Seven — local implementation report

Delivered locally on 2026-10-01. No Git commit, push, backend deployment, store upload or publishing was performed.

## Implemented app

- Warm walnut design, bundled Nunito, bright readable hex tiles, per-color number contrast, responsive wood panels, gold primary actions and minimum control touch targets.
- Home, Challenges, Rewards and Profile navigation; one-tap resume of the newest saved mode; six mode cards; chapter-based campaign selection; clearer pause (explicit Sound/Music/Haptics switches), result and game-over screens; cosmetics, statistics, achievements, leaderboards and Settings remain available.
- Tap selection/placement and dragging; valid/affected-cell preview; free rotation control; compact primary booster tray and explanatory More sheet. Narrow screens use compact cards; tablet/landscape controls stack in a scrollable pane.
- Persistent placement/rotation/merge tutorial, expandable vector illustrations, Settings replay, reduced-motion/color-accessibility settings and independent sound/music/haptics controls.
- Brief placement spring, rotation interpolation and source-to-destination merge animations preserve immediate input. Reduced motion cancels existing effects. Normalized hex geometry and fitted text are reused; drag previews deduplicate unchanged targets; particles redraw only while active and pause with lifecycle; shake state is read by the graphics layer.
- Confirmed unused runtime strings/colors/art and no-op ad service removed; fake rewards are exclusively in unit tests. See `UNUSED_CLEANUP.md`.
- Board bounds now follow actual drawable geometry. Drawing skips collapsed sizes and uses proportional number fitting, preventing the resize freeze found during QA.
- Background and fullscreen-ad guards pause music and timers. Results retain their stable run ID and save metadata across recreation/restart.

## Monetization and economy

- Billing dependencies, initialization, purchase APIs, receipt clients, paid offers/stipends and restoration UI removed. Legacy cosmetic IDs/entitlements stay compatible with existing saves and no longer expose real-money products.
- AdMob app/unit IDs centralized in `admob.properties`; `profile=test` selects Google test inventory for both local debug and release builds. Debug always stays on test inventory, even if a future release selects production. The original production IDs remain there for a future verified release. Production account ownership/status has not been verified.
- Test inventory follows [Google test-ad guidance](https://developers.google.com/admob/android/test-ads); the sample application ID matches the [official Android rewarded sample](https://github.com/googleads/googleads-mobile-android-examples/blob/main/kotlin/admob/RewardedVideoExample/app/src/main/AndroidManifest.xml).
- Optional rewarded coins (100, or 50 for insufficient funds, shared three/day limit), once/run continue, three hints/run, exact base-result doubling and one extra daily attempt. Zen and ranked assistance restrictions remain.
- SDK earned callbacks initiate durable grants. Duplicate claim keys, shared caps and result IDs protect against repeated taps/callbacks/restarts; dismissal and no fill grant nothing.
- Campaign-only Next Level interstitial transition, tutorial/first-three-run/ten-minute grace, three-win frequency, 180-second fullscreen cooldown, two/15-minute and six/day caps. No wait for an unloaded ad and no late show after navigation.
- Dedicated adaptive banners on Home and Rewards with screen-scoped ownership, so outgoing screens cannot destroy incoming banners; consent and kill-switch checks; UMP privacy options in Settings when required; async preload and readiness/expiry checks. Rewarded offers share one SDK inventory slot, load deduplication and failure backoff while keeping offer-specific rewards and analytics.
- Actual load/show/impression, earned reward and paid-value events; tutorial outcomes; coin source/spending events. Earnings estimates must use observed format eCPM rather than promises.
- Starting 100 coins and existing starter inventory preserved. New campaign first clears earn 100 plus Undo, replays 30, qualifying losses 10; qualifying Endless/Time Attack runs earn min(50, score/100). Remove costs 100; existing login/quest rewards and continue rules remain.

## Compatible persistence

Room version 4 adds rewardClaimsJson with an empty-object default; the existing migration chain remains. Wallet and claims are written together; inventory mutation/grant-once operations use transactions. Profile hydration, save serialization and cloud ledger unions preserve local earned claims. Snapshots add run/result metadata with defaults for old saves. Existing package identity and Firebase configuration remain.

Cloud synchronization still uses the project's existing merge model. Claims are unioned and stale snapshots cannot erase recently earned local claims. Conflicting offline changes and real-account linking/restore need the release checks below; this work does not introduce a new authoritative server economy.

## Assets

Generated bitmap masters, optimized WebP exports, launcher/adaptive icons, six mode illustrations, reward artwork, wood/Zen backgrounds, vector UI icons, three tutorial diagrams, store icon/feature art and dynamic sharing background are local. Runtime resources exclude source masters/reference sheets/store graphics. Duplicate legacy icons were removed; help/privacy/retry/rotate/share icons are connected to controls. The generated emblem has true alpha; framed mode/reward illustrations intentionally have opaque walnut backgrounds.

- [Asset manifest](E:/test/merge-saven/art/asset-manifest.json)
- [Art production notes](E:/test/merge-saven/art/README.md)
- [Store assets and screenshot notes](E:/test/merge-saven/store/README.md)
- [Design system](E:/test/merge-saven/DESIGN_SYSTEM.md)
- [Updated master plan](E:/test/merge-saven/Merge_Seven_Complete_Android_Development_Master_Plan.md)

## Verification

| Check | Evidence/status |
|---|---|
| Unit suite | 285 tests; zero failures/errors/skips, including existing engine/mode tests, duplicate rewards, caps, doubling, cloud claim preservation, collapsed geometry, reduced motion and shared ad-load/backoff checks |
| Android persistence/migrations | Final device rerun pending after ad/resume fixes; previous build passed 12 tests including navigation, test IDs, migrations, snapshot reload and booster grant-once |
| Debug/test APK | Built locally; final UI captures follow the last responsive layout changes |
| Android lint | 0 errors, 112 warnings; Gradle lint task passed, down from 212 before cleanup |
| Release bundle | Built locally with test ad profile; signing must be reviewed before upload |
| Purchase source audit | No active Billing/receipt/premium-offer paths found |
| Economy model | 100 sessions each for beginner/moderate/heavy users, zero ads; beginner and moderate demand fully funded |
| Git whitespace check | Pass, apart from normal Windows LF/CRLF informational warnings |

The [economy model](E:/test/merge-saven/validation/ECONOMY_MODEL.md) documents all assumptions. Beginner ends with 7,500 coins, moderate with 7,000. Heavy use exhausts coins and declines 136 booster requests, while ordinary play remains available. This demand model assumes daily reward/quest claims and is not a retention or revenue forecast.

Visual checks use a software-rendered, isolated emulator, including small/tall phone dimensions, 150% text and a tablet layout. QA found and fixed overflowing board geometry, a zero-space number-fitting loop, cramped tablet labels and a squeezed compact board. Final screen evidence is under `validation/screenshots/`; selected genuine app captures are under `store/screenshots/`.

## Release checks still required

Real AdMob ownership/serving and region-specific UMP behavior, physical-device smoothness/API/OEM testing, Google Play Games/Firebase account flows, conflicting cloud changes, production upgrade testing, five-person tutorial acceptance, release signing/version review and final store declarations/screenshots remain. The emulator has no Play Games services and does not establish physical-device performance or first-time player acceptance.

[Release checklist](E:/test/merge-saven/validation/RELEASE_CHECKLIST.md) contains the concrete checks. Purchase-validation backend source was retired locally; any deployed endpoint is unchanged and requires a separate deployment/removal request. Crashlytics mapping/symbol upload tasks are disabled for these local builds.
