# Merge Seven — Master Task & Phase Plan (Consolidated)

> This file merges **`Merge_Seven_Complete_Android_Development_Master_Plan.md`** and **`Merge_Seven_Game_Complete_Product_Design_Engineering_Plan.docx`** into one single source of truth, organized strictly by **Phase → Tasks**, so a team can track execution top to bottom. All checklist items are actionable dev/design/QA/release tasks pulled from both documents (duplicates merged, nothing dropped).

**Stack:** Kotlin + Jetpack Compose + Canvas rendering · MVVM + domain game engine · Hilt · Room · DataStore · Coroutines/StateFlow · Navigation Compose · Firebase (Analytics/Crashlytics) · Google Mobile Ads · Google Play Billing

**Core loop:** Generate Piece → Preview → Drag/Select → Preview Valid Position → Place → Detect Connected Same-Value Group (≥3) → Merge → Animate → Score + Coins → Chain Reaction Check → Next Piece → Game Over Check

---

## How to use this document

- [ ] boxes = discrete, assignable developer/design/QA tasks.
- Phases are sequential but Phase 2 (Design System) can run in parallel with Phase 1.
- Update this file first whenever a gameplay rule changes (score, merge, coins, spawn, levels, game-over, boosters) — see **Single Source of Truth Rule** at the end.

---

# PHASE 0 — Product Definition & Decisions Lock

**Goal:** Lock every product/rule decision before any code is written.
**Effort guide:** 1–2 days

### Tasks
- [ ] Confirm working game name placeholder (`Merge Seven`) and plan trademark/store-availability check before release
- [ ] Define original brand direction (name, logo concept, tone)
- [ ] Write a one-page gameplay rule sheet with explicit examples for merge group sizes 3, 4, 5+
- [ ] Confirm the 3+ same-value connected-tile merge rule
- [ ] Decide whether 4+ connected equal tiles merge all at once in one resolution (recommended: yes)
- [ ] Confirm piece types/shapes (single tile, pairs, triangles, etc.)
- [ ] Confirm whether pieces are fixed-shape or procedurally generated
- [ ] Confirm board shape and whether board is fixed or level-defined (recommended: level-defined templates)
- [ ] Confirm rotation support (yes/no, how many steps)
- [ ] Confirm booster set and exact semantics (Swap, Randomize, Undo, Remove, Continue) before UI/art work starts
- [ ] Define score, coin, level-target, and continuation-reward formulas
- [ ] Define analytics/privacy event naming convention
- [ ] Define MVP scope
- [ ] Define Release V1 scope
- [ ] Define Future/post-launch scope
- [ ] Create acceptance criteria for "done"
- [ ] Create a risk register
- [ ] Define non-goals for the first production build (explicitly exclude: accounts, cloud save, leaderboards, seasonal events, etc. unless promoted to MVP)
- [ ] Set up a **Decision Log** (see template in Appendix D) and get sign-off from Product + Engineering owners for each entry

### IP / Originality Guardrails (apply for the whole project)
- [ ] Confirm supplied screenshots are used only as gameplay/visual reference, never shipped
- [ ] Do NOT ship: original game's logo, name, exact artwork, sound effects, music, exact UI icons, copyrighted text, decompiled/copied code
- [ ] Plan to create original: logo, tile treatment, colors, background texture, buttons, booster icons, coin icon, progress indicator, animations, sounds, music, UI wording, level artwork, store screenshots

### Deliverables
`PRODUCT_REQUIREMENTS.md`, signed-off rule sheet, screen map, Decision Log

---

# PHASE 1 — Project Bootstrap

**Goal:** A clean, buildable, empty Android/Compose project with tooling in place.
**Effort guide:** 2–3 days

### Environment setup
- [x] Install Android Studio (stable channel)
- [x] Install required Android SDK Platform + Build Tools
- [x] Set up Android Emulator or physical test device
- [x] Install Git
- [x] Verify JDK bundled with Android Studio (unless project needs otherwise)
- [x] Verify tools via terminal: `java -version`, `git --version`
- [x] Verify SDK install via Android Studio → Settings → Languages & Frameworks → Android SDK

### Project creation
- [x] Create new project: Empty Activity → Kotlin → Jetpack Compose
- [x] Set Application ID (e.g. `com.yourcompany.mergeseven`)
- [x] Set app name / package name
- [x] Set minimum SDK based on desired device reach
- [x] Set target SDK per **current** Play Store requirement (verify live, don't assume)
- [x] Configure debug and release build types
- [x] Set initial version code / version name

### Git
- [x] `git init`, `git branch -M main`
- [x] Create `.gitignore` (`.gradle/`, `.idea/`, `*.iml`, `local.properties`, `build/`, `**/build/`, `captures/`, `.externalNativeBuild/`, `.cxx/`, `.DS_Store`)
- [x] Initial commit: `chore: initialize Merge Seven Android project`
- [x] Set up branch strategy: `main`, `develop`, `feature/*`, `bugfix/*`, `release/*`, `hotfix/*`
- [x] Create README

### Project structure & core wiring
- [x] Create recommended package structure (`app/`, `core/`, `game/engine`, `game/model`, `game/rules`, `game/repository`, `data/local`, `data/preferences`, `ui/*`, `audio/`, `vibration/`, `ads/`, `billing/`)
- [x] Create Application class
- [x] Configure Hilt DI
- [x] Configure Navigation Compose skeleton (Home / Game / Settings)
- [x] Create base theme, typography, and spacing tokens
- [x] Add unit-test dependencies
- [x] Create a fake `GameState` preview screen for UI-only iteration
- [x] Verify debug build succeeds
- [x] Run on emulator
- [x] Run on physical device

### Acceptance criteria
- Build succeeds, app installs, app launches, no startup crash, Git repo is clean

### Commit
`chore: bootstrap android project`

---

# PHASE 2 — Design System

**Goal:** Original, reusable visual language before screens are built.
*(Can run in parallel with Phase 1.)*

### Tasks
- [x] Define color tokens (primary wood tone, panel brown, accents, tile theme colors)
- [x] Define typography scale
- [x] Define spacing scale
- [x] Define corner radius tokens
- [x] Define elevation tokens
- [x] Define shadow styles
- [x] Design original game background (wood texture + subtle vignette, no logic tied to image size)
- [x] Design tile specification (shape, shadow, border, highlight, number treatment)
- [x] Design button specification
- [x] Design coin icon
- [x] Design booster icon style
- [x] Design progress indicator
- [x] Design logo concept
- [x] Create Figma (or equivalent) design source file
- [x] Export production-ready assets
- [x] Define recommended asset file-naming convention (see Appendix C)

### Deliverable
`DESIGN_SYSTEM.md`

---

# PHASE 3 — Hex Geometry

**Goal:** Rock-solid coordinate math the rest of the engine depends on.

### Tasks
- [x] Implement `HexCoord` (axial q, r)
- [x] Implement six neighbor directions
- [x] Implement axial distance calculation
- [x] Implement hex rotation
- [x] Implement `hexToPixel`
- [x] Implement `pixelToHex`
- [x] Implement hex corner calculation
- [x] Implement nearest-cell lookup / hit testing
- [x] Unit test all coordinate conversions (round-trip stability)
- [x] Build a visual debug grid (overlay showing q/r coordinates)
- [x] Verify geometry across at least 4 aspect ratios/screen sizes

### Acceptance criteria
- Every board cell maps to the correct screen location
- Touching a visual cell returns the same logical cell
- Round-trip coordinate conversions remain stable within tolerance

### Commit
`feat: add hex coordinate engine`

---

# PHASE 4 — Board Engine

### Tasks
- [x] Define playable cell layout / board templates (support level-defined layouts, not just one fixed board)
- [x] Create `BoardState`
- [x] Create empty board
- [x] Draw board cells
- [x] Draw tile placeholders
- [x] Add board center alignment
- [x] Add responsive sizing across densities/screen sizes
- [x] Add board debug overlay
- [x] Add placement occupancy map

### Acceptance criteria
Board renders correctly at minimum on: 360×640, 390×844, 412×915, 1080×1920

---

# PHASE 5 — Tile Renderer

### Tasks
- [x] Render base hex tile
- [x] Add shadow
- [x] Add border
- [x] Add highlight
- [x] Render numbers on tiles
- [x] Implement value → theme/color mapping
- [x] Render values 2, 4, 8, 16, 32, 64, 128, 256+ (progression stays data-driven, not hard-coded to a max)
- [x] Add adaptive font size
- [x] Add overflow handling for very large numbers

---

# PHASE 6 — Piece System

### Tasks
- [x] Create `PieceShape` model
- [x] Create `Tile` / `TilePiece` models
- [x] Create piece generator
- [x] Create weighted spawn configuration
- [x] Implement deterministic random seed (for tests/debug)
- [x] Create "current piece" state
- [x] Create "next piece" queue
- [x] Render bottom piece trays
- [x] Implement piece selection
- [x] Implement piece rotation
- [x] Reset selection state after placement

---

# PHASE 7 — Placement Engine

### Tasks
- [x] Detect nearest placement origin from touch/drag
- [x] Validate placement legality
- [x] Highlight valid cells
- [x] Highlight invalid cells
- [x] Implement tap-to-place
- [x] Implement drag-to-place
- [x] Snap piece into board on valid placement
- [x] Prevent overlapping placement
- [x] Prevent outside-board placement
- [x] Lock input while a placement is resolving

### Acceptance criteria
Invalid moves never mutate board state

---

# PHASE 8 — Merge Engine

### Tasks
- [x] Find matching neighbor tiles
- [x] Implement BFS/connected-component group detection
- [x] Validate minimum group size (3+)
- [x] Merge exactly 3 tiles
- [x] Merge 4 tiles
- [x] Merge 5+ tiles (oversized groups — define and implement the resolution rule)
- [x] Select deterministic merge destination cell
- [x] Upgrade tile value to next progression step
- [x] Update score on merge
- [x] Emit merge event (for animation/audio/analytics pipeline)
- [x] Re-scan board for chain-reaction opportunities

### Mandatory tests
- [x] 3× value-4 tiles → merges to 8
- [x] 3× value-8 tiles → merges to 16
- [x] Separated (non-adjacent) same-value tiles → no merge
- [x] Connected mixed values → only the matching-value subgroup merges

---

# PHASE 9 — Chain Reaction

### Tasks
- [x] Build the chain/merge resolution loop
- [x] Prevent duplicate group processing within one resolution pass
- [x] Define deterministic merge ordering
- [x] Emit chain index per step
- [x] Apply chain score multiplier
- [ ] Add chain sound cue
- [ ] Add chain particle effect
- [x] Test a 2-step chain
- [x] Test a 3+ step chain
- [x] Test a complex multi-cluster chain

---

# PHASE 10 — Score / Coins / Level

### Tasks
- [x] Build score engine
- [x] Track best score
- [x] Build coin economy engine
- [x] Define coin reward rules
- [x] Build level configuration model
- [x] Implement target tracking
- [x] Implement level-completion detection
- [ ] Implement level transition flow
- [ ] Persist level progress

---

# PHASE 11 — Game Over Engine

### Tasks
- [x] Enumerate possible pieces
- [x] Enumerate possible rotations
- [x] Enumerate possible placement origins
- [x] Validate whether any legal placement exists
- [x] Optimize the candidate search (avoid perf hit on large boards)
- [x] Trigger game-over state when no legal moves remain
- [x] Prevent false-positive game overs
- [x] Test near-full board
- [x] Test completely full board
- [x] Test "no valid placement despite empty cells" edge case

---

# PHASE 12 — Core UX Screens

### Home screen
- [ ] Logo
- [ ] Play button
- [ ] Continue button (shown only if a saved game exists)
- [ ] Levels entry
- [ ] Daily entry
- [ ] Shop entry
- [ ] Settings entry

### Game screen
- [ ] Pause control
- [ ] Level indicator
- [ ] Target/progress indicator
- [ ] Coins HUD
- [ ] Board
- [ ] Booster tray
- [ ] Piece queue (current + next)

### Dialogs
- [ ] Pause dialog
- [ ] Game-over dialog
- [ ] Level-complete dialog
- [ ] Restart confirmation
- [ ] Exit confirmation

### Screen-level scenarios to implement/validate
- [ ] First launch → short tutorial or guided first game, no login required
- [ ] Returning user with saved game → show Continue; otherwise show Play
- [ ] Splash screen: 2.0–2.5s max visual hold, no fake loading progress, navigate immediately once bootstrap completes
- [ ] Pause must never lose game state

---

# PHASE 13 — Animation & Effects

### Tasks
- [ ] Tile press feedback
- [ ] Drag feedback (low-latency ghost following finger)
- [ ] Valid-placement glow
- [ ] Tile landing animation
- [ ] Merge movement animation
- [ ] Merge "pop" animation
- [ ] Number transition animation
- [ ] Chain effect animation
- [ ] Coin burst animation
- [ ] Level-completion celebration
- [ ] Game-over transition
- [ ] Booster effect animation
- [ ] Button press animation
- [ ] Ensure animation never blocks input longer than the approved interaction window

### Performance target
Stable 60 FPS on representative mid-range devices; no visible frame drop during a typical chain (cap particle count)

---

# PHASE 14 — Audio / Haptics

### Tasks
- [ ] Import/produce original SFX (tap, place, merge small, merge big, level complete, game over)
- [ ] Configure SoundPool
- [ ] Add background music player
- [ ] Add sound on/off toggle
- [ ] Add music on/off toggle
- [ ] Add haptics on/off toggle
- [ ] Map gameplay events → audio/haptic cues
- [ ] Add pitch variation on merges
- [ ] Test audio focus handling
- [ ] Test background/foreground audio behavior
- [ ] Ensure no audio spam on rapid actions

---

# PHASE 15 — Save / Resume

### Tasks
- [ ] Set up Room database
- [ ] Define "active game" entity / serialized state
- [ ] Implement serialization
- [ ] Define schema version
- [ ] Auto-save after every resolved move
- [ ] Persist only after atomic move resolution (never a partial state)
- [ ] Resume game after app restart
- [ ] Save settings
- [ ] Save level progress
- [ ] Save coins and best score
- [ ] Implement undo history architecture
- [ ] Test corrupted/invalid save data handling
- [ ] Test schema upgrade/migration path
- [ ] Test process death and background/foreground transitions

---

# PHASE 16 — Tutorial

### Tasks
- [ ] Implement new-user detection
- [ ] Build tutorial overlay
- [ ] Highlight the current piece
- [ ] Highlight the board / legal cell
- [ ] Demonstrate placement
- [ ] Demonstrate a merge
- [ ] Demonstrate rotation
- [ ] Explain the objective
- [ ] Allow skipping the tutorial
- [ ] Persist tutorial-completion flag

---

# PHASE 17 — Boosters

### Tasks
- [ ] Build a generic booster framework
- [ ] Implement Swap booster
- [ ] Implement Randomize booster
- [ ] Implement Undo booster
- [ ] Implement Remove booster
- [ ] Implement Continue booster
- [ ] Define coin cost per booster
- [ ] Define availability rules
- [ ] Build booster UI states (selected, preview, confirm, result)
- [ ] Add booster animation
- [ ] Add booster sound
- [ ] Add booster haptics
- [ ] Add booster analytics events
- [ ] Handle edge cases (booster during animation, booster with no valid target, etc.)
- [ ] Ensure no booster silently changes the board — always show selected state → outcome preview → confirm/commit → result → cost/cooldown

---

# PHASE 18 — Daily Features

### Tasks
- [ ] Implement daily reward
- [ ] Implement daily challenge
- [ ] Implement deterministic daily seed
- [ ] Define challenge goal logic
- [ ] Define challenge reward
- [ ] Implement streak tracking
- [ ] Implement reset handling
- [ ] Test across timezones
- [ ] Test offline behavior

---

# PHASE 19 — Shop / Economy

### Tasks
- [ ] Build coin balance UI
- [ ] Define booster prices
- [ ] Build purchase confirmation flow
- [ ] Handle insufficient-coins state
- [ ] Add reward animation
- [ ] Ensure transaction safety (no duplicate/lost rewards)
- [ ] Add economy event logging
- [ ] Run economy balance testing
- [ ] Define economy reward sources/sinks with caps (e.g., daily login reward capped, level-completion reward tuned via config)

---

# PHASE 20 — Ads

### Tasks
- [ ] Create AdMob (or equivalent) app entry
- [ ] Integrate ads SDK
- [ ] Create an ad-provider abstraction layer
- [ ] Configure test ads for development
- [ ] Implement rewarded ad (continue, double reward, free booster, optional daily bonus)
- [ ] Implement optional interstitial (used sparingly)
- [ ] Add frequency capping
- [ ] Validate reward callbacks server-side/logically before granting reward
- [ ] Handle no-fill scenario
- [ ] Handle offline scenario
- [ ] Avoid forcing an ad after every move — protect user experience
- [ ] Test ad cancellation/failure paths

---

# PHASE 21 — Billing

### Tasks
- [ ] Configure Google Play Billing
- [ ] Create in-app products (e.g. remove-ads)
- [ ] Load product details
- [ ] Implement purchase flow
- [ ] Implement purchase acknowledgement
- [ ] Implement restore purchases
- [ ] Implement remove-ads entitlement
- [ ] Implement error handling
- [ ] Test with test cards/products
- [ ] Perform license testing
- [ ] Never trust client UI alone as proof of purchase state
- [ ] Test purchase failure/retry flow

---

# PHASE 22 — Firebase (Analytics & Crashlytics)

### Tasks
- [ ] Create Firebase project
- [ ] Add Android app to Firebase
- [ ] Add Firebase Analytics
- [ ] Add Firebase Crashlytics
- [ ] Upload/configure `google-services.json`
- [ ] Configure release mapping (deobfuscation)
- [ ] Confirm events are received in console
- [ ] Confirm crash reports arrive correctly
- [ ] Never commit credentials/secrets that should stay private

---

# PHASE 23 — Analytics Plan

### Events to implement
- [ ] `app_open`
- [ ] `tutorial_started`
- [ ] `tutorial_completed`
- [ ] `game_started`
- [ ] `game_resumed`
- [ ] `piece_placed`
- [ ] `merge_completed`
- [ ] `chain_completed`
- [ ] `level_started`
- [ ] `level_completed`
- [ ] `game_over`
- [ ] `booster_used`
- [ ] `undo_used`
- [ ] `reward_ad_started`
- [ ] `reward_ad_completed`
- [ ] `continue_used`
- [ ] `daily_started`
- [ ] `daily_completed`
- [ ] `shop_opened`
- [ ] `purchase_started`
- [ ] `purchase_completed`

### Parameters to support
`level`, `score`, `tile_value`, `merge_count`, `chain_count`, `booster_type`, `piece_type`, `session_id`

### Dashboards to build (post-integration)
- [ ] Retention dashboard: first-session completion, D1/D7 retention, games per user
- [ ] Difficulty dashboard: average moves to first game over, top merge value by level, restart rate
- [ ] Economy dashboard: coins earned/spent per session, booster use rate, rewarded-ad opt-in rate
- [ ] Quality dashboard: crash-free sessions, ANR rate, failed load/save events, invalid-state diagnostics

### Guardrail
- [ ] Do not send personally identifying data unless required and properly handled

---

# PHASE 24 — QA Foundation

### Unit tests
- [ ] Hex coordinate math
- [ ] Neighbor calculation
- [ ] Rotation
- [ ] Placement validation
- [ ] Connected-group detection
- [ ] Merge resolution
- [ ] Chain reaction
- [ ] Score calculation
- [ ] Coin calculation
- [ ] Game-over detection
- [ ] Piece generation
- [ ] Level rules
- [ ] Save/load

### UI tests
- [ ] Home → Play
- [ ] Pause → Resume
- [ ] Pause → Home
- [ ] Game over → Retry
- [ ] Game over → Home
- [ ] Level complete → Next
- [ ] Booster UI flow
- [ ] Settings toggles

### Unit test matrix examples (Given/When/Then)
- [ ] Three connected value-4 tiles + placing a 4th → merges to one 8; the three 4s disappear
- [ ] Two connected value-4 tiles + placing an unrelated tile → no merge occurs

---

# PHASE 25 — Edge Cases

### Mandatory edge cases to test
- [ ] 3 matching tiles
- [ ] 4 matching tiles
- [ ] 5+ matching tiles
- [ ] Two separate groups
- [ ] Touching mixed values
- [ ] Chain reaction
- [ ] Merge at board edge
- [ ] Merge in a corner
- [ ] Maximum value tile reached
- [ ] Very large number display/overflow
- [ ] Full board
- [ ] Empty board
- [ ] One-cell-remaining board
- [ ] Piece larger than available area
- [ ] Invalid rotation
- [ ] Fast repeated taps
- [ ] Double placement attempt
- [ ] App killed during animation
- [ ] App killed right after placement
- [ ] App resumed from background
- [ ] Rotation during drag
- [ ] Booster triggered during animation
- [ ] Undo after a chain reaction
- [ ] Undo after level completion

---

# PHASE 26 — Device / Performance QA

### Devices/configs to test at minimum
- [ ] Low-end Android device
- [ ] Mid-range Android device
- [ ] High-end Android device
- [ ] Small phone
- [ ] Large phone
- [ ] Different DPI settings
- [ ] Light system theme
- [ ] Dark system theme
- [ ] Navigation-mode variations (gesture vs. buttons)
- [ ] Touch input variety: single tap, drag, slow drag, fast drag, edge drop, accidental multi-touch
- [ ] Display variety: 19.5:9, 16:9, display cutout, increased font scale

### Metrics to measure
- [ ] Startup time
- [ ] Memory usage
- [ ] Frame rate
- [ ] Battery drain
- [ ] ANR rate
- [ ] Crash rate
- [ ] GPU overdraw
- [ ] Animation smoothness

### Performance targets
- [ ] Board rendering: smooth 60fps on target mid-range device during normal play
- [ ] Merge chains: no visible frame drop; particle count capped

---

# PHASE 27 — Accessibility

### Tasks
- [ ] Add content descriptions for controls
- [ ] Ensure large touch targets
- [ ] Ensure high contrast
- [ ] Ensure no meaning is conveyed by color alone (numbers must stay distinguishable without relying on tile color)
- [ ] Make haptics optional
- [ ] Make sound optional
- [ ] Review text scaling behavior
- [ ] Add screen-reader labels
- [ ] Consider a reduced-animation option for later

---

# PHASE 28 — Security / Integrity

### Tasks
- [ ] Centrally validate all economy operations
- [ ] Do not expose secret API keys in the app
- [ ] Do not hard-code private backend credentials
- [ ] Validate purchase callbacks through the supported billing flow
- [ ] Protect/remove debug endpoints in release builds
- [ ] Remove logs containing sensitive information from release builds
- [ ] Enable R8/code shrinking where appropriate
- [ ] Review exported Android components
- [ ] Treat local coin balance as non-authoritative if a server-backed economy is added later
- [ ] Keep analytics payloads limited to gameplay metadata only
- [ ] Do not ship debug overlays or forced-RNG controls in production builds
- [ ] If leaderboards/competitive scores are added later, move score verification to a trusted backend

---

# PHASE 29 — Release Build

### Tasks
- [ ] Update version code
- [ ] Update version name
- [ ] Configure signing
- [ ] Test R8/shrinking on release build
- [ ] Confirm crash reporting is enabled
- [ ] Switch to production Ads IDs
- [ ] Switch to production Billing products
- [ ] Remove all test ads
- [ ] Remove all debug logs
- [ ] Remove all test/debug buttons
- [ ] Remove all sample/placeholder data and assets
- [ ] Generate signed Android App Bundle (.aab)

---

# PHASE 30 — Play Store Preparation

### Tasks
- [ ] Write app name
- [ ] Write short description
- [ ] Write full description
- [ ] Finalize app icon
- [ ] Create feature graphic
- [ ] Create store screenshots
- [ ] Write/publish privacy policy
- [ ] Complete Data Safety form
- [ ] Set content rating
- [ ] Define target audience
- [ ] Complete ads declaration
- [ ] Prepare app-access instructions if required
- [ ] Implement account-deletion flow if applicable
- [ ] Provide contact email/support
- [ ] Choose store category
- [ ] Choose tags/keywords
- [ ] Verify current Play Console requirements before production launch (don't assume stale info)

---

# PHASE 31 — Internal Testing

### Tasks
- [ ] Release build to internal testers
- [ ] Test install
- [ ] Test update
- [ ] Test restore
- [ ] Test billing
- [ ] Test ads
- [ ] Test save/load
- [ ] Test gameplay end-to-end
- [ ] Test for crashes

### Collect
- [ ] Crash reports
- [ ] ANRs
- [ ] Device-specific bugs
- [ ] Gameplay feedback
- [ ] Difficulty feedback
- [ ] Ad complaints
- [ ] UI readability issues

---

# PHASE 32 — Closed Testing

### Tasks
- [ ] Expand tester group
- [ ] Evaluate retention
- [ ] Evaluate difficulty
- [ ] Evaluate tutorial effectiveness
- [ ] Evaluate economy balance
- [ ] Evaluate booster usefulness
- [ ] Evaluate rewarded-ad conversion
- [ ] Evaluate daily challenge engagement
- [ ] Evaluate shop engagement
- [ ] Do not rush to production — hold until issues are resolved

---

# PHASE 33 — Production Release

### Pre-rollout
- [ ] Confirm release checklist is 100% complete
- [ ] Consider staged rollout percentage

### Monitor post-release
- [ ] Crash-free users
- [ ] ANR rate
- [ ] Retention
- [ ] Average session duration
- [ ] Game starts per session
- [ ] Game-over rate
- [ ] Level completion rate
- [ ] Rewarded ad completion rate
- [ ] Purchases
- [ ] Store reviews

---

# PHASE 34 — Post-Launch Operations

### Week 1
- [ ] Monitor crash issues
- [ ] Monitor tutorial drop-off
- [ ] Monitor first-game difficulty
- [ ] Monitor game-over frequency
- [ ] Monitor ad problems
- [ ] Monitor purchase failures
- [ ] Prepare hotfix/rollback plan and keep it ready

### Week 2–4
- [ ] Rebalance spawn rates
- [ ] Rebalance level targets
- [ ] Rebalance booster costs
- [ ] Rebalance coin rewards
- [ ] Rebalance daily challenges

### Monthly
- [ ] Add new levels
- [ ] Add new challenges
- [ ] Add new cosmetics
- [ ] Add new events
- [ ] Add new boosters if data supports it

---

# Release Gates (must all pass before production rollout)

1. [ ] Core rules signed off by product owner
2. [ ] UI design signed off by design owner
3. [ ] All P0/P1 bugs resolved or explicitly waived
4. [ ] Ad/billing test flows passed
5. [ ] Save/resume tested after process death
6. [ ] Accessibility and performance sanity checks passed
7. [ ] Store metadata and privacy statements match actual behavior
8. [ ] Production rollout plan and rollback/hotfix process prepared

## Final Release Gate (from master plan)
- [ ] Core game is fun without monetization
- [ ] Merge behavior is deterministic and tested
- [ ] No critical crashes
- [ ] Save/resume works
- [ ] Game over is correct
- [ ] Tutorial is understandable
- [ ] UI works on target screen sizes
- [ ] Animations are smooth
- [ ] Audio/haptics are optional
- [ ] Economy is balanced
- [ ] Ads do not block normal gameplay
- [ ] Billing works
- [ ] Analytics works
- [ ] Crashlytics works
- [ ] Privacy/legal content ready
- [ ] Production assets ready
- [ ] AAB generated
- [ ] Internal test passed
- [ ] Closed test passed
- [ ] Play Console release checklist complete

---

# Appendix A — Suggested First Sprint (fastest path to a trustworthy prototype)

Don't attempt the whole game in sprint 1 — get a gray-box playable loop working before polishing art.

1. [ ] Create the Kotlin/Compose project and base theme
2. [ ] Implement `HexCoord` + six-neighbor math
3. [ ] Create the first board template matching the reference silhouette
4. [ ] Render empty hex cells in Canvas
5. [ ] Implement piece model and one hard-coded test piece
6. [ ] Implement tap-to-place on a valid empty cell
7. [ ] Implement 3+ connected equal-value group detection
8. [ ] Implement merge + score + next-piece queue
9. [ ] Add deterministic debug controls and unit tests
10. [ ] Finish one complete 60-second playable loop before adding monetization or detailed art

**Sprint acceptance:** a tester can launch the build, place pieces, trigger at least one merge, see the value increase, receive the next piece, intentionally lose by blocking the board, and restart the run. No visual polish required until this behavior is stable.

---

# Appendix B — Developer Task Board (Ready to Assign)

| ID | Task | Priority | Dependency | Output |
|---|---|---|---|---|
| ENG-001 | Create project + theme | P0 | None | Buildable app shell |
| ENG-002 | Implement axial hex coordinates | P0 | ENG-001 | HexCoord utilities |
| ENG-003 | Implement board templates | P0 | ENG-002 | BoardLayout repository |
| ENG-004 | Canvas renderer | P0 | ENG-003 | Visible board |
| ENG-005 | Hit testing | P0 | ENG-004 | Cell selection |
| ENG-006 | Piece model + queue | P0 | ENG-003 | Playable piece preview |
| ENG-007 | Placement validator | P0 | ENG-005, ENG-006 | Safe placement |
| ENG-008 | Connected group search | P0 | ENG-002 | Merge candidate finder |
| ENG-009 | Merge resolver | P0 | ENG-008 | Core mechanic |
| ENG-010 | Chain resolution | P0 | ENG-009 | Stable state |
| ENG-011 | Score/level | P0 | ENG-010 | Progression |
| ENG-012 | Game over | P0 | ENG-007 | End condition |
| ENG-013 | Drag/rotate UI | P1 | ENG-007 | Polished input |
| ENG-014 | Merge animation | P1 | ENG-010 | Game feel |
| ENG-015 | Persistence | P1 | ENG-011 | Resume |
| ENG-016 | Boosters | P1 | ENG-011 | Economy hooks |
| ENG-017 | Ads/analytics | P1 | ENG-016 | Monetization instrumentation |
| ENG-018 | QA suite | P0 | ENG-009 | Regression safety |
| ENG-019 | Release build | P0 | All | AAB + store release |

---

# Appendix C — Asset Naming Convention

```text
bg_wood_main.webp
tile_style_base.svg
icon_pause.svg
icon_coin.svg
icon_booster_undo.svg
sfx_tile_place.ogg
sfx_merge_small.ogg
sfx_merge_big.ogg
ui_button_primary.svg
```

---

# Appendix D — Decision Log Template

| ID | Question | Decision | Date | Impact |
|---|---|---|---|---|
| DEC-001 | What merges? | 3+ connected equal-value tiles | TBD | Core engine |
| DEC-002 | How are 4+ groups handled? | Entire connected component merges at once | TBD | Core engine |
| DEC-003 | ... | ... | ... | ... |

Use this table to record every locked product/gameplay decision going forward. Add a row before implementing any rule that isn't already specified in Phase 0.

---

# Appendix E — Bug Tracking Template

```text
Bug ID:
Title:
Build:
Device:
Android version:
Severity:
Steps:
Expected:
Actual:
Screenshot/video:
Logs:
Status:
Owner:
Fix commit:
Regression tested:
```

**Severity levels**
```text
S0 = crash / data loss / blocker
S1 = major gameplay break
S2 = normal bug
S3 = cosmetic / minor
```

---

# Appendix F — Master Checklist (all workstreams, flat view)

```text
PRODUCT
[ ] Product requirements
[ ] Game rules
[ ] Level design
[ ] Economy design
[ ] Monetization design

PROJECT
[ ] Android project
[ ] Git
[ ] Hilt
[ ] Compose
[ ] Navigation
[ ] Room
[ ] DataStore

GAME ENGINE
[ ] Hex coordinates
[ ] Board
[ ] Tile
[ ] Piece
[ ] Placement
[ ] Rotation
[ ] Merge
[ ] Chain
[ ] Spawn
[ ] Score
[ ] Game over

UI
[ ] Home
[ ] Game
[ ] Level
[ ] Pause
[ ] Game over
[ ] Level complete
[ ] Shop
[ ] Settings
[ ] Daily

POLISH
[ ] Artwork
[ ] Background
[ ] Tile graphics
[ ] Icons
[ ] Animations
[ ] Particles
[ ] Sound
[ ] Haptics

DATA
[ ] Save game
[ ] Settings
[ ] Level progress
[ ] Daily data

BUSINESS
[ ] Ads
[ ] Billing
[ ] Analytics
[ ] Crashlytics

QA
[ ] Unit tests
[ ] UI tests
[ ] Manual testing
[ ] Device testing
[ ] Performance
[ ] Accessibility
[ ] Release regression

STORE
[ ] App icon
[ ] Screenshots
[ ] Description
[ ] Privacy policy
[ ] Data safety
[ ] Content rating
[ ] AAB
[ ] Internal testing
[ ] Closed testing
[ ] Production
```

---

# Single Source of Truth Rule

Whenever a developer changes a gameplay rule, **update this document first.**

Rules that must never be scattered across random UI classes — keep them centralized in configuration/game-rules layers:
```text
Merge count
Tile progression
Scoring
Coin economy
Piece generation
Level targets
Game-over logic
Booster behavior
```

Ideal dependency direction:
```text
Configuration → Game Rules → Game Engine → ViewModel → UI
```

Not:
```text
UI
 ├── contains score rules
 ├── contains merge rules
 ├── contains coin rules
 └── contains game-over rules
```

**Project success definition:** a developer can clone the repository, open Android Studio, build the project, run the app, play the complete game, and understand every major gameplay rule and development decision — without needing undocumented knowledge.