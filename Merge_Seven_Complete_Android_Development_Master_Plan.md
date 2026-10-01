# Merge Seven — complete Android development plan, ads-only edition

Updated 2026-10-01. This replaces earlier purchase monetization plans. Implement locally in Kotlin/Compose/Canvas, Room/DataStore, Firebase and AdMob. No commit, push, backend deployment or publishing. Preserve package identity and Firebase configuration.

## Product and design

Warm walnut, bright beveled tiles, Nunito, readable opaque panels and gold actions. General casual audience, initially English. Three connected equal tiles merge to the next doubled value. Preserve scoring, special traits, mode objectives and deterministic spawning across Campaign, Endless, Time Attack, Zen, Daily and Weekly. Daily validation uses fixed search work to avoid device-speed-dependent seeds.

Home / Challenges / Rewards / Profile; Settings in headers; focused gameplay. Home prioritizes Play/Resume; modes expand on demand. Chapter level grid shows stars, current progression and objectives. Gameplay supports tap and drag, placement previews, visible rotation, tutorial, inventory-aware booster descriptions, coin confirmation and bounded tablet layout. Results persist their reward for recovery and truthful doubling. Pause saves/exits and offers audio/help/restart controls. Existing cosmetics, statistics, achievements and scoring/account features remain.

## Free economy

| Source | Reward |
|---|---|
| New wallet | 100 coins; original starter inventory |
| First campaign clear | 100 coins + one Undo charge |
| Campaign replay win | 30 coins |
| Campaign loss with at least ten valid moves | 10 coins once/run |
| Endless/Time Attack end with ten valid moves | min(50, score/100) |
| Login and quests | Existing rewards preserved |
| Remove cost | 100 coins; other booster/continue costs and limits retained |

No lives, paid energy, subscription benefit or mandatory ads. Normal modes remain accessible without coins. The reproducible 100-session zero-ad demand model is tools/model_free_economy.py; assumptions/results are validation/ECONOMY_MODEL.md and CSV. Beginner/moderate booster demand must remain affordable; heavy demand can be declined without blocking normal play.

## Ad placements and policy

Keep all test and production configuration in root `admob.properties`. The checked-in profile is `test`, including local release QA artifacts:

| Format | Value |
|---|---|
| App ID | ca-app-pub-6926810742930516~9728984035 |
| Rewarded | ca-app-pub-6926810742930516/4988240353 |
| Interstitial | ca-app-pub-6926810742930516/6590105790 |
| Banner | ca-app-pub-6926810742930516/8854886068 |

App ID is not an ad unit. Gradle generates the manifest/resource values from this one file; ad service code contains no duplicated IDs. Debug always uses test inventory; the profile selects release inventory. Adaptive test banners use Google's `/9214589741` demo unit. Change `profile` only for a separately authorized release after verifying ownership, format, app association and serving in AdMob. Register release QA test devices before inspecting production inventory.

| Optional rewarded placement | Benefit | Initial cap |
|---|---|---|
| Rewards | 100 coins | Three shared coin claims/day |
| Insufficient funds | 50 coins | Same shared cap |
| Game over | Existing board-clear recovery | One/run; mode restrictions |
| Hint | One valid hint | Three ad hints/run; ranked restrictions |
| Successful result | Extra coins equal to persisted base result reward | One/result |
| Daily | Bonus retry retaining official score | One/day |

Buttons state exact benefits. Actual SDK readiness drives availability. Loading/no-fill/denial/failure keep play usable. Only earned callbacks grant; dismissing alone grants nothing. No simulated production rewards when ads/features are disabled.

Interstitials only while advancing a successful Campaign result. Suppress tutorial, first three completed runs, first ten accumulated play minutes, and modes that suppress ads. Require three Campaign wins since previous shown interstitial and 180 seconds since any fullscreen ad closes. Cap two in a rolling fifteen minutes and six/day. Persist session IDs and counters. Skip immediately without ready inventory; never show a late load on the next screen. No ads on loss, launch, resume, exit or during gameplay. Defaults are product choices; bounded experiments cannot increase initial pressure.

Home/Rewards only: anchored adaptive banner in a stable footer, separate from controls/navigation/system insets. Dispose/pause/resume with lifecycle. UMP canRequestAds gates initialization and all requests/shows. Settings presents privacy options when required. Ads preload asynchronously, deduplicate, expire after an hour, recheck consent/kill switch and prevent overlap. SDK operations run on Main. Save before fullscreen ads; timers/audio pause until dismissal. Show attempts, impressions, reward events and paid callbacks are separate telemetry.

References: [test units](https://developers.google.com/admob/android/test-ads), [rewarded integration](https://developers.google.com/admob/android/rewarded), [interstitial breaks](https://support.google.com/admob/answer/6201362?hl=en), [adaptive banners](https://developers.google.com/admob/android/banner), [UMP](https://developers.google.com/admob/android/privacy).

## Engineering and recovery

Room v4 adds rewardClaimsJson default `{}`; migrations retain all prior rows/fields. Active snapshots have additive run ID, result-finished/won, base-reward and hint-count fields. Old snapshots remain readable. Wallet + ledger are saved together. Run/date/placement keys protect repeated callbacks, taps, recreation and restart. Reward writes are NonCancellable in the persistent SDK scope. First-clear Undo uses a transactional unlock marker. Coin booster unlocks write coin deduction and inventory increment in one Room transaction; other booster changes read current database quantities transactionally.

Finished results persist until replaced and reapply outstanding grants safely. Cloud snapshots carry claims; merge unions them, and older snapshots cannot remove local claim markers. Preserve the existing max/union cloud model. It is not an authoritative multi-device currency ledger; offline conflicts require QA. Local release defaults enable shipping features, local debug overrides are ignored, and Remote Config/kill_ads still apply.

Billing dependencies, bootstrap, product catalogue, paid offers, purchase APIs/network clients and restoration UI are removed. Purchase-validation export/dependency is retired locally. A separate authorized release must explicitly retire any currently deployed endpoint; local edits do not delete deployed functions.

## Art and delivery

Generate brand, walnut/board/Zen textures, six mode illustrations, reward/achievement/celebration/sharing ornaments. Retain vectors for all nine booster types and functional icons; Canvas draws tiles/numbers/traits/effects. Tutorial SVG masters export to runtime vectors. Preserve originals and optimized WebP, alpha edges, adaptive safe area, and production briefs in art/asset-manifest.json. Runtime ships no reference sheets or marketing masters. Store needs 512px icon, 1024×500 feature graphic and six screenshots from the implemented app.

Stages: foundation/purchase removal/migration; design/art/navigation; controls/tutorial/accessibility; rewards/ad lifecycle/caps/privacy; tests/economy/device/performance/store/build. See validation/IMPLEMENTATION_REPORT.md for actual verification status.

Required checks: engine modes unchanged; upgrades preserve saves/wallet/inventory/settings; no billing paths; exactly-once rewards and truthful doubling; caps/grace/exclusions; no late ad interruption; offline/consent/no-fill/kill switch play; timer/audio restoration; banners and system insets; small/tall/tablet/enlarged-text/reduced-motion/color layouts; low/mid-range performance. Four of five first-time human testers must place, rotate and merge without help.

Prepare local APK/test APK/release AAB and internal-test checklist. Account ownership/serving, privacy policy/UMP/data safety, signing, Play Games, backend retirement and human/device tests must be verified before distribution. Publishing needs a separate user request.

Track tutorial completion, retention, duration, failures, reward uptake, coin sources/sinks, impressions and paid revenue. Daily revenue = sum(format impressions × observed format eCPM / 1000). Earnings are measured, not guaranteed. Compare revenue/player with retention and use bounded experiments plus an ad kill switch.
