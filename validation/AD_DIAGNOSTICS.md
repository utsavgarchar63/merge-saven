# Ad configuration and diagnostics

All formats and the application ID come from root `admob.properties`. `profile=production` is selected for both debug and release. IDs are generated as resources from this file. `profile=test` selects Google demo inventory when needed.

`qa.fallback=true` enables a single demo-unit retry after a production load fails, only in a debug build on an SDK-marked test device. Release builds hard-disable this fallback. Fresh banner binding and fullscreen preloads start with the production unit; fallback never auto-shows a fullscreen ad, changes consent, or grants simulated rewards. Logs and analytics distinguish `production_units` and `qa_demo_fallback`. All demo IDs and optional physical test-device hashes remain in the same configuration file.

The production values are the user's existing IDs: app `ca-app-pub-6926810742930516~9728984035`, rewarded `/4988240353`, interstitial `/6590105790`, banner `/8854886068`.

## QA workflow

1. Use an emulator, which Google automatically treats as a test device, or a physical device registered in AdMob. For programmatic physical-device registration, copy the SDK's hashed test-device ID from logcat into comma-separated `test.devices` in `admob.properties`, then rebuild. Verify a **Test Ad** label before interacting with the creative.
2. Open Home and Rewards. Inspect banner loading and the exact-benefit rewarded offer. Normal play must remain available on no fill or consent failure.
3. Run `tools/capture_ad_logs.ps1` to capture `MergeSevenAds`, `ConsentManager` and SDK `Ads` messages. Pass an explicit serial for a physical QA device.
4. Both debug and release log selected inventory/unit, consent and request gates, load start/success, error domain/code/message/cause, response ID, adapter and latency. They log impressions, show failures and earned rewards, without account credentials or request URLs. Do not infer account approval from a successful marked test-device response.

For an independent demo build, run `gradlew.bat :app:assembleDebug -PadmobProfile=test`. This selects the sample application ID as well as sample units without editing the production configuration. Copy the resulting APK before another profile build replaces it. Do not publish a demo-profile build.

## Review — October 5

Fresh logcat from the previously installed production release reproduced two distinct failures:

- UMP: `Publisher misconfiguration ... no form(s) configured ... ca-app-pub-6926810742930516~9728984035`.
- Ads: `Received error HTTP response code: 403`, then `Ad failed to load : 3`, on the SDK-marked QA emulator. This is a server rejection, not sufficient evidence to identify the precise account restriction.

The retained profile reported SDK consent status 1 and still allowed requests despite the update error. Follow-up checks used the official SDK reset API and a separate, empty Android user on the owned QA emulator. Both reported status 0 with `can_request_ads=true` after the production configuration error; actual production requests still failed with HTTP 403. Thus the observed serving failure on this emulator is server rejection. Do not infer published consent messages from `canRequestAds=true`, or describe this check as consent blocking all requests. The client follows the SDK permission result and never forces consent. A demo unit fallback still changes neither the manifest application ID nor its UMP configuration.

Code changes: update GMA to 24.9.0 and UMP to 4.0.0; retain optimized SDK initialization/loading; publish UMP's own cached permission immediately after starting the launch update; rerun consent gathering when AF9 becomes enabled; expose release diagnostics; support a build-time demo profile override. Remove the obsolete `gma_ad_services_config` manifest override: GMA 24 removed its conflicting property and resource, so Analytics supplies its own configuration. Production IDs and release fallback restrictions are unchanged. GMA 25.5 was evaluated but requires Kotlin metadata 2.3, incompatible with this project's Kotlin 2.1 toolchain; use the compatible supported 24.9 SDK. [SDK release notes](https://developers.google.com/admob/android/rel-notes).

The newer branch commit was integrated, preserving version **1.7.0 / code 8**. Its `test.devices` value was an advertising-ID UUID, rather than the SDK's programmatic test-device hash. That UUID is preserved as `test.dashboard.advertising_ids`, a reference for AdMob's dashboard registration, and is never sent to `setTestDeviceIds()`. `test.devices` accepts only the 32-character hash printed in the Google Ads logcat hint; Gradle rejects UUIDs and normalizes valid hashes. No physical phone was attached to obtain its SDK hash, so the SDK field remains empty. Emulator registration is automatic. This configuration error can prevent programmatic physical test-device recognition and debug demo fallback, but does not explain server rejection on an already marked emulator. [Google test registration](https://developers.google.com/admob/android/test-ads).

The malformed-UUID configuration was tested directly with Gradle: configuration failed with the expected SDK-hash guidance, and the original production configuration was restored byte-for-byte. The version-8 demo build then passed 286 unit tests and the device suite (18 discovered: 17 executed checks passed, explicit reset helper skipped). The SDK reset helper separately passed when explicitly opted in on the marked emulator.

### Dashboard actions, in priority order

1. **Privacy & messaging:** create and publish the applicable privacy messages, selecting this exact app ID. In particular configure the European regulations message for EEA/UK/Switzerland users, and applicable US-state messages. Verify each message is published and linked to Merge Seven. [UMP setup](https://developers.google.com/admob/android/privacy).
2. **Apps → Merge Seven → App settings / status:** check Android package `com.mergeseven.game`, the actual store listing association, approval/readiness and any required account verification/payment details. The 403 alone does not prove which of these is wrong. [App readiness](https://support.google.com/admob/answer/10564477?hl=en).
3. **App verification / app-ads.txt:** if prompted, host AdMob's exact account snippet at the developer website's `/app-ads.txt`, ensure that domain is in the store listing, then Verify app → Check for updates. Do not invent a publisher snippet. [Verification guide](https://support.google.com/admob/answer/14538460?hl=en).
4. **Ad units:** verify `/4988240353` is Rewarded, `/6590105790` is Interstitial, `/8854886068` is Banner, all under app `~9728984035`, active and owned by this account. Review Policy Center for serving limitations; resolve the displayed issue rather than repeatedly recreating units.
5. **Mediation:** review Vungle/AppLovin/AdColony configuration. Earlier initialization logs reported missing adapter classes. Remove networks you do not intend to use, or integrate compatible adapters for the networks you do intend to use. Missing adapters do not establish the cause of Google's HTTP 403. [Load-error guidance](https://developers.google.com/admob/android/ad-load-errors).
6. **Test devices / ad inspector:** register physical QA devices before interacting with production-unit creatives. If 403 persists after setup is corrected, use ad inspector to capture the failing request for Google support. [Inspector](https://developers.google.com/admob/android/ad-inspector), [test ads](https://developers.google.com/admob/android/test-ads).

No authenticated dashboard was inspected or modified in this review. Account-specific approval, message publication and serving restrictions remain dashboard checks. Fresh logs are local ignored files, starting with `validation/ads-review-before.log`.

### Demo validation — October 5

The independent sample-app/sample-unit build with GMA 24.9.0 and UMP 4.0.0 passed **286 unit tests** and **17 Android device tests**. The final suite discovered 18 tests: 17 normal checks passed, while the explicit manual SDK-reset helper was skipped without its opt-in argument. That helper was then run explicitly and passed its reset/status/permission assertions. The online smoke test reset only SDK consent, preserved game data, and verified Home banner, rewarded and interstitial inventory loaded through the real AdMob service after fresh consent. Each format returned an SDK response ID; UMP reported status 1, `can_request_ads=true` and no required privacy options on the India QA emulator.

Home/Rewards displayed a marked Test Ad banner. Manually opening the exact-benefit coin offer displayed the marked Test Ad rewarded creative, logged a real impression and earned callback, and granted **760→860 coins**, leaving two of three daily claims. Closing the creative returned to Rewards; restart preserved 860 coins. No Install/Open advertisement CTA was clicked. Interstitial loading is verified, but an eligible campaign-transition display remains a separate gameplay check.

Demo artifact: `app/build/outputs/apk/admob-qa/merge-seven-demo-ads.apk`. Diagnostic evidence: `validation/ads-review-demo.log`, `validation/ads-review-device-tests.log` and `validation-ads-demo-build.log`. The demo APK is a QA build and does not demonstrate production serving.

### Updated production release — October 5

The final version **1.7.0 / code 8** production APK/AAB build succeeded in 14m 25s (`validation-ads-v8-production-build.log`), with release vital lint and **286 release unit tests** passing. Generated release resources retain the four production IDs; `TEST_ADS=false` and `QA_AD_FALLBACK=false`. APK signature, 16KB-page zip alignment, archive integrity, compressed dex and runtime music-hash checks passed. APK is **5,946,202 bytes (5.67 MiB)**; AAB is **11,140,756 bytes (10.62 MiB)**. See `admob-review-artifacts.json` for current hashes.

Final version-8 production smoke evidence is in `ads-review-production-v8.log`: Google initialization READY, the exact three production units requested, each rejected with HTTP 403, and UMP reporting the same missing-form configuration. Upgrade retained 860 coins and the Resume save; Home and Rewards remained usable. The crash buffer was empty.

`ads-review-production-cached.log` confirms all three units returned HTTP 403 through the updated SDK. Google initialization was READY; the retained profile also logged Vungle/AppLovin/AdColony adapter creation failures. `ads-review-production-fresh.log` records the official SDK-reset check. `ads-review-production-clean-profile.log` records an independent empty Android profile with starting coins 100, no previous game/SDK state and all three production formats rejected by HTTP 403. On this profile UMP still allowed requests after reporting the missing messages; no client permission bypass was introduced. The unavailable rewarded offer left coins at 100 and all three claims intact, showed an explanatory message, and kept navigation usable. No crash-buffer events appeared. The temporary QA user was removed and the original 860-coin profile retained.

Production serving is **not verified**: dashboard configuration and persistent HTTP 403 require account-side investigation. Successful demo integration does not fix or establish account approval. Regional consent forms and physical-device serving still need QA after dashboard correction.

Rewarded offers share one inventory slot. Concurrent requests are deduplicated. Failed loads retry at bounded delays (30, 60, 90 seconds), while manual retry remains available. Banner retries verify the current attached owner. Ads expire after one hour; loads never force a late show or block navigation. Consent and the ad kill switch gate all requests and shows.

For an SDK load failure, inspect the actual message and response rather than treating every failure as no fill. For a consent misconfiguration, fix the app's messages in AdMob; the client must not bypass UMP. Account approval, app association, unit format and serving restrictions require AdMob account inspection.

Sources: [Google test devices](https://developers.google.com/admob/android/test-ads), [load errors](https://developers.google.com/admob/android/ad-load-errors), [response information](https://developers.google.com/admob/android/response-info), [UMP](https://developers.google.com/admob/android/privacy).

## Actual production-unit observations — October 2

The production build passed all 15 Android tests. Logcat records SDK initialization before load requests and confirms the three production units. The automatically marked test emulator received **code 3, domain com.google.android.gms.ads, HTTP 403** for rewarded, interstitial and banner loads. No production-unit ads were served during this check. Google describes this HTTP response as an AdMob server rejection; the exact account restriction is not established by the response alone. See [Google's SDK load-error table](https://developers.google.com/admob/android/ad-load-errors).

UMP separately reported **publisher misconfiguration: no form(s) configured for the production application ID**. Review this app's Privacy & messaging configuration in AdMob, app/unit association, approval/serving status and Policy Center. This emulator had prior consent state from earlier demo QA; it is not evidence that a fresh production installation can request ads when consent setup fails. Client code retains canRequestAds gating.

Adapter initialization reported the Google adapter ready and three configured mediation adapter classes unavailable (Vungle/AppLovin/AdColony). Review the account's mediation setup and include only intentionally configured, compatible adapters, or remove unused networks from the account. No account configuration was changed; no authenticated AdMob tab was available. Do not mistake these diagnostics for verified ownership or live approval.

Exact logs are in production-ad-diagnostics.log. Ordinary navigation/play and the existing wallet remain usable when these requests fail. Demo-profile visual integration testing follows separately; it cannot fix or prove production serving.

## Production-first debug fallback verified — October 2

With profile=production and qa.fallback=true on the SDK-marked emulator, each format logged its actual production unit, HTTP 403, qa_fallback_started and a successful qa_demo_fallback load with an SDK response ID. Banner `/9214589741`, rewarded `/5224354917` and interstitial `/1033173712` all loaded. Home and Rewards displayed marked Test Ad adaptive banners. The fallback rewarded creative displayed Test Ad and Reward granted; closing it granted exactly 100 coins (660→760), reduced the daily allowance to zero, and restart retained 760. Interstitial inventory loaded; an eligible live interstitial transition is still a release device check.

The exact combined log is production-fallback-ad-diagnostics.log. The successful demo requests do not establish production serving. Release configuration keeps QA_AD_FALLBACK=false. Missing production messages require dashboard correction; the October 5 fresh-profile checks above supersede any earlier assumption that this error necessarily blocks SDK request permission.

The later October 5 gift/reminder/Play-update rebuild supersedes the earlier artifact sizes and hashes above. The current JSON manifests describe that build; see `GIFTS_REMINDERS_UPDATES_REVIEW.md` for its results. UMP setup and production IDs were preserved.

## Production review — October 8

The user confirmed the published Play URL uses package `com.mergeseven.game`. The later ad-reliability rebuild preserves the production IDs and UMP SDK gate. It fixes adaptive-banner resizing, retries lost during fullscreen ads, and stuck state after synchronous SDK show exceptions. All 297 debug and 297 release unit tests passed, plus 23 executed device checks. Demo banner/rewarded/interstitial loaded; actual test interstitial display/dismiss/reload and rewarded +100 (860→960, restart retained) were verified.

Fresh production-unit requests still receive HTTP 403 for every format. The Google adapter was READY and UMP can_request_ads=true; no consent form appeared or blocked these requests. The no-forms configuration warning remains a separate account diagnostic. No production inventory displayed, and release fallback remains disabled. See `ADMOB_PRODUCTION_REVIEW_2026-10-08.md` for exact evidence and the published-app checklist: store association/readiness, account/Policy Center, app-ads.txt, unit formats and applicable privacy messages. Its current artifact hashes supersede earlier measurements above.
