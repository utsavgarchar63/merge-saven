# Merge Seven â€” local implementation report

Updated 2026-10-02. The user authorized committing and pushing the source to feature/new-design. Backend deployment, store upload and publishing remain outside this task.

## App and gameplay

The six existing modes, merge/scoring rules, deterministic spawning, saves, achievements, coins and booster inventory remain. Home resumes the newest valid mode save in one tap. Home, Challenges, Rewards and Profile have focused navigation; Settings has independent audio/haptics, tutorial replay, reduced motion, color accessibility and privacy options.

Sign-in, leaderboards, remote tournaments, ghost downloads, account export/upload and cloud synchronization have been removed from production source and dependencies. Weekly remains a personal challenge. Local persistence and explicit score-image sharing remain. No purchases, subscriptions, receipt validation or paid offers are exposed.

Tap/drag placement, valid/affected-cell preview, rotation, owned-first boosters and the explanatory More sheet are retained. Placement springs, rotation interpolation, merge flights, short chain effects and reduced-motion behavior are implemented. Geometry, hex paths, paints, fonts and unchanged drag previews are reused. Particle updates run only while active; timers and audio pause during fullscreen ads and backgrounding. Collapsed drawing areas cannot enter a number-fitting loop.

First-time campaign play opens an offline animated guide for placement, sixty-degree rotation and merging three equal tiles. Its page and completion persist in DataStore; Settings can replay it and Pause can open help in any mode. Reduced motion uses still diagrams. Native Canvas animation avoids GIF/video decoding. Portrait tablets use a larger centered board with controls below; landscape board padding includes system navigation insets. Mode headers and Home resume summaries identify the actual saved mode.

## Backgrounds, icons, font and music

The original wood, board and Zen background image bytes are unchanged. Their image-based rendering is restored. Only the rectangular surrounds of generated mode/reward icons were removed using the built-in imagegen edit workflow. Gold frames and interior wood remain. Ten transparent, padded 256px WebP exports are used at runtime; source atlas, individual masters, prompt and export details are in [the asset manifest](../art/asset-manifest.json).

Real static Nunito weights 400/500/600/700 are used in Compose, board numbers and result sharing, with cached typefaces. Visual QA found that declaring several weights against the same variable font left every style at its ExtraLight default; the export tool now creates the actual weights. The font license is retained. An original 40-second ambient music loop replaces three simultaneous stems. The runtime is now a 143,744-byte Ogg Vorbis export; its lossless master stays in art/music outside the app. Asynchronous preparation, audio focus, independent settings and short volume ramps avoid blocking navigation. Sound effects wait for SoundPool readiness. Music-off preferences are read before foreground playback. See [music production notes](../art/MUSIC_MANIFEST.md). Speaker/headphone listening still requires a physical device.

## Ads and free economy

Root [admob.properties](../admob.properties) is the single source for the app ID, all three units and optional hashed physical QA test devices. Production profile is selected in both build variants; Google demo profile is available from that file. Emulators receive marked test inventory even when requesting the production units. This cannot prove live account approval.

After a failed production load, debug builds with `qa.fallback=true` can try a demo unit once on an SDK-marked test device. Release disables fallback. Logs distinguish the production request from QA demo inventory, and consent still gates requests. Actual production-unit checks returned HTTP 403 for all formats, while UMP reported no configured forms; the account configuration must be corrected before production serving can be verified. See [actual diagnostics](AD_DIAGNOSTICS.md).

SDK requests wait asynchronously for initialization, consent and the ad kill switch. Rewarded offers share one loaded inventory slot. Requests are deduplicated; failures retry at bounded 30/60/90-second delays; fullscreen inventory expires after an hour. Banner hosts are claimed before initialization can suspend, preventing duplicate or stale views. Debug logcat includes unit/profile, domain/code/message, response ID and adapter latency. Actual impression events are separate from show attempts.

Reward offers state their exact benefits. Durable claim keys and transactional writes protect grants and caps across repeated callbacks, taps and restart. Closing an ad alone grants nothing. Result doubling uses the stored base reward. Campaign interstitials are restricted to successful Next Level transitions with tutorial/early-play grace, three-win spacing, fullscreen cooldown and rolling/daily caps; an unavailable ad is skipped immediately. Adaptive banners occupy separate footer areas on Home and Rewards.

Zero-ad play remains available in every normal mode. Campaign first completion earns 100 plus Undo; replay 30; qualifying loss 10. Qualifying Endless/Time Attack earns min(50, score/100). Remove costs 100. Existing starter inventory, daily/quest rewards and continue rules remain. The [100-session economy model](ECONOMY_MODEL.md) funds beginner and moderate demand under its stated assumptions; heavy demand can exceed income without blocking ordinary play. It is not a revenue or retention forecast.

## Persistence and reminders

Room v4 preserves the additive migration chain. Wallet and durable claims are written together; inventory grant-once operations are transactional. Snapshots retain old-save defaults and add stable run/result metadata. Stale local restoration cannot erase earned claims. Cloud SDKs and account reconciliation are no longer part of the app.

Reminders are opt-in, scheduled locally with unique periodic WorkManager work around 7pm and no network constraint. Permission denial leaves them off. Quiet hours, foreground use, completed daily challenges and already-sent dates suppress delivery. Disabling the setting cancels work and the notification. Tapping a reminder opens Daily. Android may delay periodic work; this does not use exact alarms or server push.

## Store draft and verification

The sourced [keyword research](../store/KEYWORD_RESEARCH.md) and English title/short/full description are local drafts. Proposed title: **Merge Seven: Hexa Puzzle**. The copy describes actual number-merge mechanics and makes no ranking guarantee.

The reviewed source passed **286 unit tests with zero failures/errors/skips**. The final installed APK passed **16 Android tests in 28.725 seconds**; manual verification is recorded in CURRENT_GAME_REVIEW.md. Debug/test APKs, lint and release bundle packaging passed together in validation-current-game-review-final-build.log (BUILD SUCCESSFUL, 14m 29s). Lint has 0 errors, 105 existing warnings and no unused resources. Artifact SHA-256 values are in artifact-hashes.json.

The existing-screen review improves compact Home priority, enlarged-text headers, navigation wrapping, safe insets, tablet bounds, catalog scrolling, Settings row interactions and native dialog dismissal. Time Attack cannot spend on Continue after timeout, and Undo no longer rewinds its countdown/freeze. Four regression tests cover these timed-mode cases and untimed Undo compatibility. See [current game review](CURRENT_GAME_REVIEW.md) for screenshots, observed checks and remaining physical-device limitations.

Manual final-device checks verified the three guide pages, changing native animation frames, exact 100-coin fallback reward and restart persistence, all-format marked fallback loads, Daily/Weekly placements and saves, newest Weekly resume, tablet/landscape insets and enlarged-font layout. Six genuine app captures were refreshed. Source/dependency checks found no Billing, sign-in, leaderboard, account export/upload or Firebase messaging paths. Actual production-unit serving is blocked by observed HTTP 403 and missing UMP forms; marked demo success is separate QA evidence. Detailed device results are in DEVICE_QA.md and AD_DIAGNOSTICS.md. Physical-device performance, audio/haptics, current Android notification permission, live account approval, published-app upgrade and first-time human acceptance remain in the release checklist.

Purchase-validation backend source was retired locally in the earlier work. Deployed endpoints are unchanged. Crashlytics mapping/symbol upload tasks remain disabled for local builds.


Local delivery: app/build/outputs/apk/debug/app-debug.apk and app/build/outputs/bundle/release/app-release.aab. The debug APK enables marked test-device fallback; the release bundle disables it. Production serving and Play-ready signing remain external release checks.


Size delivery: release APK and AAB configuration, verified sizes, reproducible music export and release-variant checks are in [SIZE_CONFIGURATION.md](SIZE_CONFIGURATION.md). Generated APK/AAB files and private/local tool configuration are excluded from the source push.
