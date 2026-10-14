# Phone and tablet responsiveness

## Layout behavior

- Primary destinations use bottom navigation on narrow portrait windows and a navigation rail on windows at least 600 dp wide, or landscape windows at least 480 dp wide. Short rails use compact items to keep all four destinations reachable.
- Home keeps the generated Merge Seven graphic first. Wide or short windows place the play/resume card beside it; narrower windows stack them. Mode cards form one or two columns using the actual page width and font scale.
- Rewards booster cards and daily quests form readable card grids. Daily gifts use up to four columns. Cards share the page's scroll container, so controls remain reachable without nested vertical scrolling.
- Shared pages cap their content width at 1040 dp. Headers adjust to short windows and enlarged text. On short windows, the ad footer scrolls with the page so it does not consume most of the content viewport. On taller windows, it remains in the separate footer.
- Gameplay uses safe drawing insets on every edge. Landscape windows place the board next to a 240–360 dp control pane, with the piece tray first and boosters below it. Portrait tablets keep the tray below the board unless the window is at least 1000 dp wide. The board remains square and capped at 600 dp; game content is capped at 1200 dp.
- Objective chips wrap when necessary. The existing Settings, Cosmetics, Achievements, and Stats pages retain bounded, scrollable content; the level grid remains adaptive.

Breakpoints use available space rather than a tablet model name. Changing window dimensions or orientation recomposes the layout with the current game state.

## Validation

Production debug and instrumentation APK assembly passed. Android lint passed. All 305 JVM tests passed with zero failures and zero skips.

All five Android window checks passed on the isolated Android 30 `MergeSeven_AdsOct14` emulator. Native captures were visually reviewed for Home, gameplay, Challenges, Settings, and the landscape pause dialog.

| Window | Logical display | Native capture | Density | Text scale | Result |
| --- | --- | --- | --- | --- | --- |
| Phone portrait | 360 × 640 dp | 720 × 1280 px | 320 dpi | 1.0 | Passed |
| Phone landscape | 640 × 360 dp | 960 × 540 px | 240 dpi | 1.0 | Passed |
| Tablet portrait | 800 × 1280 dp | 800 × 1280 px | 160 dpi | 1.0 | Passed |
| Tablet landscape | 1280 × 800 dp | 960 × 600 px | 120 dpi | 1.0 | Passed |
| Phone with enlarged text | 360 × 640 dp | 720 × 1280 px | 320 dpi | 1.5 | Passed |

The emulator initially clamped some requested pixel widths. Those three cases were rerun at sizes within its physical resize limit, using density to achieve the intended logical window. Screenshot assertions verified their actual pixel dimensions. The portrait phone captures independently match their requested dimensions.

Reviewed Home/game captures: [phone portrait](screenshots/responsive/phone-portrait-home.png), [phone landscape](screenshots/responsive/phone-landscape-home.png), [tablet portrait](screenshots/responsive/tablet-portrait-home.png), [tablet landscape](screenshots/responsive/tablet-landscape-home.png), [landscape tablet gameplay](screenshots/responsive/tablet-landscape-game.png), [enlarged-text gameplay](screenshots/responsive/phone-large-text-game.png).

The instrumented `ResponsiveWindowTest` checks every primary destination, all six Home mode cards, daily gifts and quests, the last Rewards link, Settings and Cosmetics controls, level selection, all three game pieces, board bounds, rotate, pause, save, and resume. Captures are produced by Android's screenshot API, without modifying image content.

The runner [validate_responsive_windows.py](../tools/validate_responsive_windows.py) verifies the emulator's AVD identity before resizing it and restores its previous display, font, and rotation settings afterward. Each test preserves the previous Zen save and game preferences.

On the isolated AOSP emulator, `--root-for-captures` temporarily enables root-capable adbd to retrieve app-owned screenshot files, then restores its previous state. The first checks ran without root; captures were collected afterward. The corrected size checks collected their captures directly. Root is a QA file-access option, not an app requirement.

## Repeat

Build and install the production-profile debug and instrumentation APKs, then run:

```powershell
python tools/validate_responsive_windows.py --adb '<Android SDK>/platform-tools/adb.exe' --root-for-captures
```

The default serial is `emulator-5556` and the expected AVD name is `MergeSeven_AdsOct14`. Use `--profiles` for selected windows. `--collect-only` retrieves previously captured files without repeating tests. Home/game captures are saved under `validation/screenshots/responsive`; other captures go to the ignored `validation/build-temp/responsive` directory. Instrumentation logs are saved as ignored `validation-responsive-<profile>.log` files.
