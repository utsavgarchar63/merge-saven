# Merge Seven local release checklist

Implementation date: 2026-10-01. No commit, push, deployment or store upload is authorized or performed.

## Before internal testing

- Both local debug and release artifacts request Google test inventory. All IDs and the selected profile are in `admob.properties` (`profile=test`). Debug always uses test units; the profile selects release inventory. Change that single file to production only after verification for an authorized release; register release-QA test devices before checking production inventory.
- Verify in the AdMob account that app ID `ca-app-pub-6926810742930516~9728984035` and rewarded `/4988240353`, interstitial `/6590105790`, banner `/8854886068` belong to this app, have the correct formats and can serve. Source inspection cannot verify ownership or account status.
- Review release signing. The existing Gradle configuration selects the available local keystore and can fall back to a debug key; a successful local release build does not establish Play-ready signing. Provision production signing securely and choose a version code above the currently published version.
- Validate Firebase configuration, account linking, leaderboard sign-in, score upload, cloud restore and conflicting offline changes with real accounts. The AOSP emulator has no Play Games services.
- Confirm production Remote Config defaults and `kill_ads`; configuration can reduce ad pressure but cannot exceed the conservative local bounds.
- Purchase-validation exports have been retired in local source only. Review and explicitly authorize the deletion of any deployed legacy purchase endpoint separately. Unrelated cloud features remain.
- Crashlytics mapping/symbol upload tasks are disabled for local builds. Re-enable only in an authorized release pipeline.

## Device and ads QA

- Test at least a small phone, tall phone, tablet and representative low/mid-range physical Android devices, including API 24 and the current target API.
- Check enlarged fonts, color accessibility, TalkBack placement actions, reduced motion, landscape, system insets and OEM launcher masks.
- Ask five first-time users to place, rotate and merge without assistance; require at least four to succeed. Automated or single-operator emulator checks do not establish this criterion.
- Upgrade from the published app with real saves; verify coins, nine booster inventories, settings, achievements, campaign and daily progress.
- Test UMP consent granted, denied, errors and required privacy-options UI in applicable regions using official test-device settings.
- Test rewarded earned/dismissed/failed/no-fill behavior, double taps, recreation, restart during an earned reward, three shared coin claims/day, three hint ads/run, once/run continue and exact result doubling.
- Test interstitial tutorial/first-three-run/ten-minute grace, three campaign successes, 180-second fullscreen cooldown, two/15-minute and six/day caps. An unloaded ad must never delay or interrupt the next screen.
- Verify timers pause and audio resumes around actual fullscreen test ads; test repeated background/foreground transitions, offline play and the ad kill switch.
- Confirm Home/Rewards adaptive banners stay separate from controls, navigation and insets. No banners belong on gameplay or overlays.
- Measure input smoothness, frame timing, allocations and ad loading on physical devices. The headless software-rendered emulator is not a performance benchmark.

## Store preparation

- Review the generated art and real captures under `store/`; keep source masters outside the app.
- Capture final release-QA screenshots and review current Play Console size/format rules before upload. Debug captures may contain test ads and are reference material.
- Complete privacy policy, data-safety, ads, content-rating and target-audience declarations using actual SDK/data behavior; verify required app-ads.txt configuration.
- Confirm no Billing SDK, paid product UI, purchase restoration or receipt-validation calls remain in the shipped app.
- Use observed paid-event revenue and impressions to estimate earnings: sum(impressions × observed format eCPM / 1,000). Review revenue alongside retention, tutorial completion, failures and session completion.
- Publish, deploy backend changes and push Git changes only after a separate user request.
