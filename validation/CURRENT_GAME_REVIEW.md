# Current game review — October 2, 2026

Scope: improve existing layouts and interactions only. No new product features, changed wood/board/Zen background artwork, Git commit/push, deployment or publishing.

## Fixes

- Home prioritizes Play/Resume on compact screens. Headers move balances below titles when enlarged text needs space. Bottom navigation keeps labels on one line; the full label remains available to accessibility services.
- Shared pages consume navigation insets once. Settings, achievements, statistics, cosmetics and campaign levels respect safe drawing areas and a centered 720dp content limit.
- Cosmetics scrolls its preview and catalog together, allowing the entire catalog to remain reachable in landscape. Preview labels no longer compete with skin titles; category buttons have more text space.
- Challenges uses a single gift column when enlarged text makes two columns too narrow. Rewards says “More coins tomorrow” after the shared daily allowance is exhausted.
- Settings toggles respond to the whole row with one switch action. About copy is centered; final controls remain reachable above the system navigation area.
- Level details and existing booster/funds confirmations use native dialogs. Android Back dismisses the level dialog without leaving the level list; tapping its content does not dismiss it. Level details scroll when necessary.
- Result actions wrap when text grows and respect navigation insets. Gold button text is centered when it wraps.
- Time Attack no longer offers or charges for board-clearing Continue after timeout. The timeout explanation identifies the actual cause. Continue remains available for a blocked board while time remains.
- Time Attack Undo restores the board and score without rewinding elapsed countdown or freeze time. Untimed Undo retains its prior snapshot behavior. Merge, scoring and deterministic spawning rules are unchanged.
- Removed the now-unused confirmation string.

## Evidence collected

The first review build passed 284 unit tests, 16 Android tests, debug/test APK packaging and lint. The final source passed **286 unit tests with zero failures/errors**. Four new regression tests cover expired Continue, blocked-board Continue, timed Undo and unchanged untimed Undo. Final packaging and emulator status will be recorded below after verification.

Manual checks on the workspace-owned Android 30 emulator:

- 320×640dp phone at 150% font scale: one-tap Resume remains in the initial viewport; primary page balances have space; Challenges remains usable; exhausted Rewards displays the correct disabled offer. Settings row tap toggled sound once and a second tap restored it. About/version content remained above navigation.
- 853×480dp landscape phone: the Cosmetics preview scrolls away and every board cosmetic remains reachable. Statistics and achievements scroll within safe areas. Level detail content taps retain the dialog; Android Back dismisses it while retaining Levels; Start works.
- Time Attack: rotated two-tile placement worked; owned Undo used inventory and kept the wallet at 760. The countdown then expired to “Time’s up”; no Continue offer appeared and the wallet remained 760. This run exposed the timer rewind subsequently fixed with regression coverage.
- All three background image byte hashes still match HEAD; see background-preservation.json.

Screenshots under screenshots/review-* are genuine emulator captures. Pre-final captures can still show the navigation wrapping and Cosmetics header subsequently corrected. Final recaptures will use the `-final` suffix.

## Practical limits

The software-rendered, headless emulator verifies functionality and layout, not physical-device frame timing, music quality, haptics or current-Android notification permissions. Those remain in RELEASE_CHECKLIST.md. Previously observed production AdMob HTTP 403 and missing UMP forms remain account-side release checks; marked demo success does not establish production serving. No ad configuration was changed in this review.

## Final verified build

- Combined debug APK, test APK, unit tests, lint and release bundle: **BUILD SUCCESSFUL in 14m 29s**, validation-current-game-review-final-build.log.
- **286 unit tests**, zero failures/errors/skips. **16 Android tests passed in 28.725 seconds** on the final installed APK, current-review-final-instrumentation.log.
- Lint: **0 errors, 105 existing warnings, 0 unused resources**.
- Final 320×640dp / 150% recaptures confirm single-line navigation labels, readable Levels/Cosmetics headers, separate balances and readable Cosmetics tabs. Full navigation labels remain in semantics despite visual ellipsis.
- Final live Time Attack Undo: timer 2:52 before / 2:37 after; placed tile removed; owned Undo 1→0; coins 760→760. Logs: review-timed-undo-before.log and review-timed-undo-after.log. It did not restore the snapshot's older countdown. The run was paused, saved and returned Home.
- Final 640×1067dp tablet Cosmetics and 1067×640dp gameplay captures confirm bounded content, readable controls and a board above the system navigation area. Images: review-cosmetics-tablet-portrait-final.png and review-game-tablet-landscape-final.png.
- Debug APK: 29,321,192 bytes. Release bundle: 11,843,695 bytes. SHA-256: artifact-hashes.json. Release generated flags remain TEST_ADS=false and QA_AD_FALLBACK=false.
- Final logcat event check found zero crash/ANR events; see current-review-crash-events.json. This is a functional QA observation, not a physical-device performance benchmark.
- No source changes followed this successful build. Docs and validation records were refreshed locally. No commit, push, deployment or publishing occurred.
