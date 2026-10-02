# Local Android QA — October 2

Device: workspace-owned MergeSeven_QA30, AOSP Android 30 x86_64, software graphics, headless/no-audio. These checks establish functional behavior, not physical-device performance or production AdMob approval. No accounts, publishing, deployment or Git push were used.

## Gameplay and persistence

- Campaign replay completed with its 30-coin base reward. An actual marked rewarded creative granted exactly 30 extra, and the result offer disappeared after one claim.
- Endless: tap placement, two rotations, three connected 2 tiles merging into 4, score 12, owned Undo reducing inventory 3 to 2 and returning score to zero without spending coins. Home resumed the newer Endless save.
- Time Attack: paused at 1:51; UI dumps about twenty seconds apart retained 1:51. Save-and-exit worked.
- Zen: placement and Undo did not change the wallet or owned Undo inventory; controls explained unlimited/free Undo.
- Independent music/sound/haptic settings persisted. Music Off is read before foreground playback. Native player state during a fullscreen rewarded ad showed the looping player paused. The no-audio emulator cannot verify speakers or haptic strength.
- Profile and Settings expose no sign-in, leaderboard, account upload or data export. Local saves and explicit score-image sharing remain.

## Ads

Production IDs were verified in SDK logs for all three formats. All returned HTTP 403; UMP reported no forms configured for the production app ID. Prior emulator consent was cached, so this does not establish consent on a fresh production installation. See AD_DIAGNOSTICS.md.

Separate demo-profile QA displayed marked Test Ad adaptive banners on Home and Rewards in dedicated footer slots. SDK earned callbacks changed 400→500→600 coins, with daily allowance 3→2→1; restart preserved 600. The later 30-coin campaign replay and exact doubling produced 660. Three fullscreen demo rewards completed without a fresh ANR on a clean host. An early Back attempt did not dismiss the SDK video before its reward time; live dismissal-before-earning remains a device checklist item, while unit tests cover no grant on dismissal/failure.

An earlier software renderer/WebView ANR occurred while Gradle and the emulator ran concurrently. Clean-host demo QA did not reproduce it. No physical-device performance fix is inferred from that result.

## Layout and artwork

Real Nunito weights corrected the previously thin ExtraLight rendering. Coin, gift and six mode icons have transparent outer edges; original wood/board/Zen background bytes are preserved. Small-phone 320×640dp at 150% font scale and canonical 480×853dp layouts were inspected.

Tablet and landscape QA identified an unnecessarily narrow portrait board and landscape cells extending under the system navigation bar. The current source uses stacked controls on portrait tablets and navigation insets for the landscape board. Final-build results are recorded below.

## Automated checks

The production-configured and subsequent font/demo builds each passed 15 Android tests covering navigation, compatible persistence/migration and unique offline reminder scheduling/cancellation. The latest suite adds guide auto-display, persisted page, recreation and completed-guide dismissal. Final results are recorded below.

## Remaining release checks

Physical API 24/current Android and OEM devices, measured frame timing, TalkBack traversal, real music/haptics, Android 13+ notification permission, evening delivery after reboot/Doze, UMP regions and account serving, upgrade from the published app, and five-person first-play acceptance remain in RELEASE_CHECKLIST.md. Local WorkManager scheduling has no network constraint; actual reminder timing is Android-controlled.

The first latest-suite cold start exposed WorkManager not being initialized when reminder sync ran. The application now implements Configuration.Provider and removes only WorkManager's automatic initializer, retaining other App Startup components. WorkManager.getInstance(context) initializes it on demand. This follows https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration. The original failure is retained in guide-startup-failure.log; a rebuilt rerun verifies the fix.

## Final rebuilt APK results

- All **16 Android tests passed in 31.902 seconds**, including guide auto-display, page restoration after recreation, completion staying dismissed and offline reminder uniqueness/cancellation. Startup fix, unit tests and lint passed in validation-reminder-startup-fix-build.log.
- Manual Settings replay showed all three guide pages. An eight-frame placement capture showed five changed diagram frames; reduced-motion still presentation was exercised by the instrumented test. Guide images are in screenshots/guide-*-final.png and guide-live-frame*.png.
- All three formats successfully loaded marked demo fallback after the production HTTP 403. A real fallback rewarded ad granted 660→760 coins, exhausted the shared three-claim allowance, and retained 760 after restart. Home/Rewards banners remained in separate footer areas. No final-run crash/ANR event matched the logcat check.
- Portrait tablet 640×1067dp now has a larger board and controls below. Landscape 1067×640dp has all cells above the system navigation bar; rotation and placement worked. Small phone 320×640dp at 150% text retains readable controls, tray and board. Screenshots were visually inspected after resizing settled.
- Daily accepted rotated 16/4 placement; Weekly accepted 2/4 placement. Both saved. Home offered Weekly as the newest save, and one tap restored its tiles and correct Weekly challenge header. Reminder action opened Challenges with today's puzzle visible.
- Six genuine final-build captures refreshed under store/screenshots. They are local debug QA references, with marked demo ads where visible, not submission-ready evidence of production serving.


## Existing-screen review

The final reviewed build passed 286 unit tests and all 16 Android tests (28.725 seconds), debug/release packaging and lint (0 errors, no unused resources). Small-phone 150% text, secondary-page scrolling/insets, native level-dialog dismissal, tablet gameplay and Time Attack timeout/Undo were rechecked. Live Undo preserved the running clock and wallet. See [CURRENT_GAME_REVIEW.md](CURRENT_GAME_REVIEW.md) for fixes, logs and final captures. Physical-device and production-account limits remain as above.
