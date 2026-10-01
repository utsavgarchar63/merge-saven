# Local Android QA

Device: workspace-owned `MergeSeven_QA30`, AOSP Android 30, x86_64, software graphics. This is an isolated QA device, not a physical-device performance benchmark. No external account, Git publishing or backend deployment was used.

## Observed gameplay and persistence

- Campaign: completed first level; level 2 unlocked with stars retained. Placed pieces by tapping, rotated a piece, merged three 4 tiles into 8 (24 points), and completed the playable tutorial.
- Undo: an owned charge changed from 4 to 3; score reverted from 24 to 0; wallet stayed at 200. Save-and-exit and restart preserved the board.
- Endless: opened its larger board, selected and placed a tray piece, paused and saved.
- Time Attack: opened with 3:00, placed a piece, paused at 2:26; a later UI dump still showed 2:26. Resume and save worked.
- Pause: independent Sound/Music/Haptics switch states visible. Haptics Off survived restart. The no-audio emulator cannot validate speakers or physical haptic feedback.
- Home: discovered Campaign-priority resume despite a newer Endless save; fixed by reading existing `updatedAt` order. Final-build replay is recorded below when complete.

## Observed ads

- Google SDK rewarded test creative displayed Test Ad, then Reward granted. Dismissal after earning updated 200 to 300 coins and the daily coin claim allowance from 3 to 2. Force-stop/relaunch preserved 300 coins.
- Home test banner was visible and separate from navigation. Home-to-Rewards revealed a blank incoming banner due to outgoing-host disposal; fixed with owner-scoped lifecycle operations.
- Rewarded offers previously loaded separate SDK objects for one unit; changed to one shared slot with load/backoff deduplication, offer-specific callbacks and expiry invalidation.
- All formats now read root `admob.properties`. Adaptive banner uses Google's adaptive demo unit; debug always stays on demo inventory. Both currently built variants select test ads.

## Layout observations

- Small phone: 320×640 dp, font scale 1.5; board, three tray pieces and primary boosters remain visible.
- Tall phone: 480×853 dp; tutorial, board, tray, pause and navigation usable.
- Tablet: 640×1067 dp; bounded board plus stacked booster/tray pane; labels readable.
- Resizing caught a zero-space text-fitting loop and cramped layouts; those fixes are retained. Captures occur after recreation has settled.

## Final-build checks

Pending final APK install, device tests, banner transition retest, dismissal without reward, newest-mode resume, remaining Zen/Daily/Weekly checks and landscape capture.

## Release-only checks

API 24/current Android and OEM physical devices, measured frame timing, TalkBack traversal, real audio/haptics, UMP regions, AdMob account ownership/serving, Play Games/Firebase accounts/cloud conflicts, real production upgrades and five-person tutorial acceptance remain in `RELEASE_CHECKLIST.md`.
