# Ad configuration and diagnostics

All formats and the application ID come from root `admob.properties`. `profile=production` is selected for both debug and release. IDs are generated as resources from this file. `profile=test` selects Google demo inventory when needed.

`qa.fallback=true` enables a single demo-unit retry after a production load fails, only in a debug build on an SDK-marked test device. Release builds hard-disable this fallback. Fresh banner binding and fullscreen preloads start with the production unit; fallback never auto-shows a fullscreen ad, changes consent, or grants simulated rewards. Logs and analytics distinguish `production_units` and `qa_demo_fallback`. All demo IDs and optional physical test-device hashes remain in the same configuration file.

The production values are the user's existing IDs: app `ca-app-pub-6926810742930516~9728984035`, rewarded `/4988240353`, interstitial `/6590105790`, banner `/8854886068`.

## QA workflow

1. Use an emulator, which Google automatically treats as a test device, or a physical device registered in AdMob. For programmatic physical-device registration, copy the SDK's hashed test-device ID from logcat into comma-separated `test.devices` in `admob.properties`, then rebuild. Verify a **Test Ad** label before interacting with the creative.
2. Open Home and Rewards. Inspect banner loading and the exact-benefit rewarded offer. Normal play must remain available on no fill or consent failure.
3. Run `tools/capture_ad_logs.ps1` to capture `MergeSevenAds`, `ConsentManager` and SDK `Ads` messages. Pass an explicit serial for a physical QA device.
4. The debug tag records selected profile/unit, load start/success, error domain/code/message/cause, response ID, adapter and adapter latency. Do not infer account approval from a successful marked test-device response.

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

The exact combined log is production-fallback-ad-diagnostics.log. The successful demo requests do not establish production serving. Release configuration keeps QA_AD_FALLBACK=false; fresh production consent configuration is still blocked by the observed missing forms.
