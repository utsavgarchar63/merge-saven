# Merge Seven — Advanced Features Plan

> Companion to `README.md` (the core Phase 0–34 master plan). That document takes the game to a shippable V1.
> **This** document covers everything *after* the core loop is trustworthy: depth systems, meta progression,
> live-ops, cloud, competitive play, and advanced monetization.
>
> Rule: core rules still live in `README.md`. Any advanced feature that changes a core rule (scoring, merge,
> spawn, game-over) must update `README.md` first, then link back here.

**Task ID format:** `AF<phase>-<n>` (e.g. `AF3-04`). Use these IDs in commits and PR titles.
**Checkbox meaning:** `[ ]` not started · `[~]` in progress · `[x]` done and merged with tests.

---

## Baseline snapshot (as of this plan)

What already exists in the repo, and what advanced work has to build on top of:

| Area | State | Implication for this plan |
|---|---|---|
| Hex geometry, board, merge, chain, score, game-over engines | Implemented + unit tested | Safe foundation to extend |
| Screens: Home, Game, Levels, Daily, Settings | Implemented (Compose + Canvas) | New modes need routes in `AppNavGraph` |
| Boosters (undo, shuffle, rotate, remove) | Implemented in `GameViewModel`, **no cost, no gating** | AF3 must add economy gating |
| `UserDataRepository`, `LevelRepository` | Room-backed as of AF0-05/06 | Safe to build economy and progression on |
| `RoomGameRepository` (save/resume) | Live: autosaves, resumes, and backs "Continue" on Home | Per-mode save slots are ready for AF2 |
| Room (`GameDatabase`) | Live, schema v1 exported to `app/schemas` | Bump version only with a migration + test |
| Engine randomness | Seeded and reproducible via `GameRandom` / `RngState` (AF0-13/14/15) | AF2 daily puzzles can just pass a date-derived seed |
| Feature flags | `FeatureFlags` / `Feature.AF1`…`AF12` (AF0-16/17); off in release | Later phases check `isEnabled` before lighting up |
| Analytics | `AnalyticsTracker` no-op (AF0-18); Firebase in AF6 | Call sites can log now without a Firebase dep |
| Debug menu | Long-press version in Settings (DEBUG only, AF0-19) | Grant coins, unlock level, force GO, toggle flags |
| Firebase, AdMob, Play Billing | Dependencies commented out in `app/build.gradle.kts` | AF6/AF9 turn them on |
| Settings (`SettingsRepository`) | Real DataStore persistence | Pattern to copy for other repos |

**Hard rule:** no phase past AF0 starts until AF0 lands. Everything advanced assumes durable state.

---

# AF0 — Foundations (blocking prerequisites)

**Goal:** make state durable, deterministic, and observable so every later feature is buildable and testable.
**Depends on:** nothing · **Effort:** ~1.5 weeks · **Priority:** P0

### Persistence
- [x] **AF0-01** Define Room entities: `ActiveGameEntity`, `LevelProgressEntity`, `UserProfileEntity`, `UnlockEntity` *(plus `DailyQuestEntity` — quests are a list, so they get their own table rather than a JSON blob)*
- [x] **AF0-02** Enable `GameDatabase` (uncomment, add DAOs, `exportSchema = true`, commit schema JSON)
- [x] **AF0-03** Add Room migration test harness (`MigrationTestHelper`) before the first real migration ships
- [x] **AF0-04** Replace `GameRepositoryImpl` stub with a Room-backed implementation
- [x] **AF0-05** Port `UserDataRepository` (coins, stars, streak, quests, challenge) from `MutableStateFlow` to DataStore/Room
- [x] **AF0-06** Port `LevelRepository` (stars, best scores, unlock pointer) to Room
- [x] **AF0-07** Write a one-time migration for players upgrading from the in-memory build (default seed values, no crash on empty) *(no legacy data existed on disk, so this became first-run seeding plus the versioned upgrade hook in `PersistenceSeeder`)*

### Save / resume
- [x] **AF0-08** Add `@Serializable` snapshot model for `GameState` (board, tray, score, coins, seed, undo stack) *(`GameStateSnapshot` v2 completes this — the seed and draw cursor landed with AF0-14)*
- [x] **AF0-09** Autosave on every resolved action and on `onStop()`; debounce writes to avoid I/O per frame *(400 ms debounce, flushed immediately from `LifecycleStartEffect`; writes run on the application-scoped persistence scope so they outlive the ViewModel)*
- [x] **AF0-10** Restore active game on launch; show "Continue" on Home when a snapshot exists
- [x] **AF0-11** Handle snapshot version mismatch by discarding safely instead of crashing
- [~] **AF0-12** Test resume after process death (`adb shell am kill`) and after force-stop — `ActiveGamePersistenceTest` covers the reopen-from-disk guarantee automatically; the two `adb` kill steps are documented in that test and still need one manual pass on a device

### Determinism
- [x] **AF0-13** Introduce a seeded RNG abstraction and route `SpawnEngine` through it (no bare `Random`) *(`GameRandom`, a SplitMix64 cursor whose whole state is one `Long`. It also hands out tile and piece ids, because `PlacementEngine` and `MergeEngine` were minting them from `System.nanoTime()` — randomness alone would not have made a run reproducible. The one remaining `Random` picks the seed for a brand-new run.)*
- [x] **AF0-14** Persist the seed and draw counter in the snapshot so resume reproduces the same piece stream *(`RngState` lives on `GameState`, so it rides along with the existing autosave; snapshot version bumped to 2 and v1 saves are discarded rather than resumed with a fresh sequence. Undo rewinds the cursor too, so replaying an undone move deals the same piece.)*
- [x] **AF0-15** Add a replay harness: seed + action list → final state, used by tests and bug reports *(`Replay` + `ReplayRunner`; `Replay.encode()` produces JSON small enough to attach to a bug report. Illegal actions are skipped and counted rather than thrown, so a replay from another build still runs and reports how far it diverged.)*

### Feature flags & telemetry plumbing
- [x] **AF0-16** Create a `FeatureFlags` interface with a local (BuildConfig/debug-menu) implementation *(`LocalFeatureFlags` + DataStore `"feature_flags"`; release hard-returns false and ignores writes)*
- [x] **AF0-17** Gate every AF phase behind a flag so unfinished work can ship dark *(`Feature.AF1`…`AF12` + `requireEnabled`; defaults off; call-site pattern documented on `Feature`)*
- [x] **AF0-18** Create an `AnalyticsTracker` interface with a no-op impl now, Firebase impl in AF6 *(`NoOpAnalyticsTracker` + `AnalyticsEvents`; `app_open` from `MergeSevenApplication`)*
- [x] **AF0-19** Add a debug menu (long-press version in Settings): grant coins, force game-over, set level, toggle flags *(`DebugMenuSheet` via long-press version when `BuildConfig.DEBUG`; `DebugCommands` bus for force GO; `LevelRepository.unlockThrough`)*

### Acceptance criteria
Kill the app mid-game → relaunch → identical board, tray, score, coins, and next-piece sequence.
Coins and level stars survive reboot. All flags default to **off** in release.

---

# AF1 — Advanced Gameplay Systems

**Goal:** add strategic depth to the merge loop without breaking existing engine tests.
**Depends on:** AF0 · **Effort:** ~2.5 weeks · **Priority:** P1

### Special tiles
- [x] **AF1-01** Extend `Tile` with a `TileTrait` (NORMAL, BOMB, WILDCARD, FROZEN, STONE, MULTIPLIER) *(`freezeStage` + `multiplierFactor` fields; factories on `Tile`)*
- [x] **AF1-02** **Wildcard** — matches any value when forming a connected group; resolves to the group's value
- [x] **AF1-03** **Bomb** — on merge, clears its 6 neighbours; scores at a reduced rate to avoid an exploit loop *(`BOMB_SCORE_FACTOR` 0.5; no nested boom)*
- [x] **AF1-04** **Frozen** — cannot merge until an adjacent merge thaws it (2-stage thaw)
- [x] **AF1-05** **Stone** — permanent blocker, occupies a cell, removable only by bomb/booster
- [x] **AF1-06** **Multiplier** — ×2/×3 the merge score of the group containing it
- [x] **AF1-07** Define trait interaction matrix (bomb+frozen, wildcard+multiplier, etc.) and encode it as a table, not `if` chains *(`TraitInteractions`)*
- [x] **AF1-08** Extend `MergeEngine` + `ChainReactionEngine` for traits; keep existing tests green
- [x] **AF1-09** Renderer support: distinct silhouette per trait (not colour-only — see AF11) *(cracked frozen + ×N badge)*

### Board variety
- [x] **AF1-10** Level-defined board templates (holes, non-radial shapes) loaded from JSON in `assets/` *(`LevelDefinition` + `LevelBoardFactory`; gated by `Feature.AF1`; samples `level_02` / `level_03`)*
- [x] **AF1-11** Pre-filled starting layouts per level *(starting tiles with traits; seeded ids)*
- [x] **AF1-12** Cells with modifiers (score pad, spawn vent, locked-until-cleared) *(`CellModifier` on `BoardState`; Canvas underlays; snapshot `modifiers` list)*

### Objectives beyond "reach value N"
- [x] **AF1-13** Objective types: reach value, clear all stone, survive N moves, score X in N moves, collect N of value V *(`LevelObjective` + `ObjectiveEvaluator`; collect via merge counters; samples `level_02`–`04`)*
- [x] **AF1-14** Multi-objective levels (all must pass) with per-objective HUD chips *(`objectiveChips` on `GameUiState` / `GameScreen`)*
- [x] **AF1-15** Star rating driven by objective + efficiency, replacing the hardcoded `starsEarned = 3` in `GameViewModel` *(`StarRating.compute`; optional JSON `starMoves`)*

### Acceptance criteria
Every trait has ≥3 unit tests including one adversarial interaction. Existing `MergeEngineTest` and
`ChainReactionEngineTest` pass unchanged. Traits are absent from levels where the level JSON doesn't request them.

---

# AF2 — Game Modes

**Goal:** more than one reason to open the app.
**Depends on:** AF0, AF1 (traits optional per mode) · **Effort:** ~2 weeks · **Priority:** P1

- [x] **AF2-01** Extract a `GameMode` abstraction (rules + win/lose conditions + HUD config) so modes aren't `if (mode ==)` branches
- [x] **AF2-02** **Campaign** — existing level flow, refactored onto `GameMode`
- [x] **AF2-03** **Endless** — no target, escalating spawn difficulty, personal best tracking
- [x] **AF2-04** **Time Attack** — 3-minute run, merges add time, combo urgency
- [x] **AF2-05** **Zen** — no game over, no ads, undo unlimited, calm palette + music variant
- [x] **AF2-06** **Daily Puzzle** — fixed seed per date, identical board for all players, one scored attempt (wire into existing `DailyScreen`)
- [x] **AF2-07** **Weekly Challenge** — 7-day seeded run with a modifier (e.g. "all pieces are triples")
- [x] **AF2-08** Mode selector UI on Home + routes in `AppNavGraph`
- [x] **AF2-09** Per-mode records, stats, and separate save slots
- [x] **AF2-10** Per-mode analytics dimension on every gameplay event

### Acceptance criteria
Each mode is independently save/resumable. Daily Puzzle produces a byte-identical board for the same date
on two different devices. Mode unlock rules are data-driven, not hardcoded.

---

# AF3 — Booster 2.0 & Economy

**Goal:** turn the current free, ungated boosters into a real resource system.
**Depends on:** AF0 · **Effort:** ~1.5 weeks · **Priority:** P1

- [x] **AF3-01** Give each `BoosterType` cost, owned-count, cooldown, and validation rules in one config object
- [x] **AF3-02** Gate `onBoosterUndo` / `onBoosterShuffle` / `onBoosterRemoveTile` in `GameViewModel` behind spend-and-confirm
- [x] **AF3-03** Implement **CONTINUE** (the one declared `BoosterType` with no implementation): revive after game over, clears N tiles, escalating cost
- [x] **AF3-04** New boosters: Hammer (destroy one tile), Value Up (+1 tier on a tile), Magnet (pull a value together), Time Freeze (Time Attack only)
- [x] **AF3-05** Booster inventory persisted, with earn sources (levels, quests, ads, IAP)
- [x] **AF3-06** Shop screen: booster bundles, coin packs, remove-ads entry (UI only; purchases land in AF9)
- [x] **AF3-07** Insufficient-funds flow → offer earn options rather than a dead end
- [x] **AF3-08** Economy simulation spreadsheet: coin income vs. sink per 100 sessions; tune before launch
- [x] **AF3-09** Anti-abuse: cap undo depth per run, ignore undo after a revive

### Acceptance criteria
No booster can be used without a successful spend. Coin balance can never go negative under any
sequence of rapid taps (test with a concurrency stress test).

---

# AF4 — Hints, Solver & Adaptive Difficulty

**Goal:** reduce rage-quits and unwinnable states.
**Depends on:** AF1 · **Effort:** ~2 weeks · **Priority:** P2

- [x] **AF4-01** Board evaluation function (open cells, cluster quality, largest-value reachability)
- [x] **AF4-02** Depth-limited search that returns the best placement for the current tray
- [x] **AF4-03** Run the solver off the main thread with a hard time budget (~30 ms) and a cached result
- [x] **AF4-04** Hint UI: highlight suggested cell, cost coins or a rewarded ad, rate-limited
- [x] **AF4-05** Deadlock prediction — warn before the board becomes unplayable
- [x] **AF4-06** Solvability guarantee for Daily Puzzle seeds: reject a seed if the solver can't reach the target
- [x] **AF4-07** Adaptive spawn bias: after N failed attempts on a level, gently favour helpful values (flagged, off by default)
- [x] **AF4-08** A/B-ready difficulty profiles (easy / standard / hard curve) selectable by Remote Config in AF6

### Acceptance criteria
Solver returns a legal move in <30 ms on a low-end device for a full board. Adaptive bias is invisible
in analytics event names (so it can be A/B tested honestly) and can be disabled remotely.

---

# AF5 — Meta Progression & Collections

**Goal:** long-term retention hooks beyond the level list.
**Depends on:** AF0 · **Effort:** ~2 weeks · **Priority:** P2

- [x] **AF5-01** Player XP + player level, awarded per merge/level/quest
- [x] **AF5-02** Achievement system: definition table, progress tracking, unlock toast, achievements screen
- [x] **AF5-03** Tile skin system — swap tile rendering (`GameColors` + Canvas draw) via a `TileTheme` token set
- [x] **AF5-04** Board/background themes (wood, marble, neon, seasonal), respecting `DESIGN_SYSTEM.md` tokens
- [x] **AF5-05** Unlock sources: player level, achievements, coins, events; persisted unlock inventory
- [x] **AF5-06** Cosmetics preview + equip screen
- [x] **AF5-07** Stats screen: total merges, biggest tile, longest chain, playtime, per-mode bests
- [x] **AF5-08** Milestone rewards ("first 1000 merges") with claim flow

### Acceptance criteria
Equipping a skin never changes gameplay values or hit testing. Cosmetic state survives reinstall once
AF7 cloud save lands. Colour-only skins still pass the AF11 colourblind check.

---

# AF6 — Live-Ops Infrastructure

**Goal:** change the game without shipping an APK.
**Depends on:** AF0 · **Effort:** ~1.5 weeks · **Priority:** P1 (unlocks tuning for everything else)

- [x] **AF6-01** Enable Firebase in `app/build.gradle.kts` (uncomment plugins + BOM), add `google-services.json` (keep out of git)
- [x] **AF6-02** Implement the Firebase-backed `AnalyticsTracker` from AF0-18; wire the event list in `README.md` Phase 23
- [x] **AF6-03** Crashlytics with custom keys: level, mode, seed, board hash, last action
- [x] **AF6-04** Remote Config for: spawn weights, level targets, booster costs, ad frequency, feature flags
- [x] **AF6-05** Remote Config fetch policy: cached defaults, activate on next launch, never block startup
- [x] **AF6-06** A/B test wiring for difficulty curve and booster pricing
- [x] **AF6-07** FCM push notifications: daily reminder, streak-at-risk, event start (all opt-in, respect settings)
- [x] **AF6-08** Notification permission flow for Android 13+ with a soft-ask before the system prompt
- [x] **AF6-09** Server-driven level packs: download JSON level definitions, validate, cache, fall back to bundled
- [x] **AF6-10** Seasonal events: time-boxed board theme + modifier + leaderboard + reward track
- [x] **AF6-11** Kill switch for ads, IAP, and any AF feature that misbehaves in production

### Acceptance criteria
A booster price change is live for users within one app restart, with no store update. Startup time
is unaffected when the network is offline or slow (verify with airplane mode + throttled network).

---

# AF7 — Accounts & Cloud Save

**Goal:** players don't lose progress when they change devices.
**Depends on:** AF0 (durable local state first) · **Effort:** ~2 weeks · **Priority:** P2

- [x] **AF7-01** Play Games Services sign-in (silent first, explicit fallback), plus a fully functional guest mode
- [x] **AF7-02** Cloud snapshot format with a version field and a device/timestamp header
- [x] **AF7-03** Upload triggers: level complete, purchase, app background — never per move
- [x] **AF7-04** Conflict resolution: prefer higher progress, present a chooser for ambiguous cases; never silently drop coins
- [x] **AF7-05** Restore flow on fresh install with a clear "keep local / use cloud" dialog
- [x] **AF7-06** Offline queue for pending syncs with exponential backoff
- [x] **AF7-07** Account deletion / data export path (required by Play data-safety rules)
- [x] **AF7-08** Test matrix: sign in on device B mid-run, conflicting purchases, corrupted snapshot, expired token

### Acceptance criteria
No test scenario results in a net loss of coins, stars, or purchased items. Guest players are never
blocked from playing by a sign-in wall.

---

# AF8 — Competitive & Social

**Goal:** a reason to come back that other players create.
**Depends on:** AF6, AF7 · **Effort:** ~2 weeks · **Priority:** P3

- [x] **AF8-01** Play Games leaderboards: Endless high score, Daily Puzzle, weekly event
- [x] **AF8-02** Score submission with client-side sanity checks before upload
- [x] **AF8-03** Server-side (Cloud Function) validation of Daily Puzzle scores by replaying the seed + action log
- [x] **AF8-04** In-app leaderboard UI (global / friends / me) rather than only the Play overlay
- [x] **AF8-05** Async tournaments: 24 h bracket on a shared seed, coin prizes
- [x] **AF8-06** Share a run: render a result card image and share via `ACTION_SEND`
- [x] **AF8-07** Ghost replay of the current daily leader
- [x] **AF8-08** Abuse handling: score reset path, rate limits, shadow-ban list

### Acceptance criteria
A tampered score fails server validation and never appears on a leaderboard. Leaderboard UI degrades
gracefully offline instead of showing an error screen.

---

# AF9 — Advanced Monetization

**Goal:** revenue that doesn't damage retention.
**Depends on:** AF3, AF6 · **Effort:** ~2 weeks · **Priority:** P1

- [x] **AF9-01** Enable AdMob dependency; implement mediation-ready ad wrapper behind an interface
- [x] **AF9-02** Rewarded placements: continue after game over, double coins, free hint, extra daily attempt
- [x] **AF9-03** Interstitial policy: frequency cap, never after a loss immediately, never during tutorial (drive from Remote Config)
- [x] **AF9-04** Ad preloading with a graceful fallback when no fill is available
- [x] **AF9-05** Enable Play Billing; implement coin packs, booster bundles, Remove Ads (non-consumable)
- [x] **AF9-06** Purchase acknowledgement + pending-purchase handling + restore on reinstall
- [x] **AF9-07** Server-side receipt validation (Cloud Function) before granting entitlements
- [x] **AF9-08** Optional subscription tier (ad-free + daily coin stipend + exclusive skin)
- [x] **AF9-09** Offer engine: starter pack, comeback offer, streak-save offer — all remotely configurable
- [x] **AF9-10** Remove Ads must disable interstitials *and* banner, but keep rewarded ads available as an opt-in
- [x] **AF9-11** Consent/UMP flow for GDPR/CCPA before any ad request

### Acceptance criteria
Buying Remove Ads takes effect immediately without restart and survives reinstall. Every entitlement
grant is idempotent. No ad request happens before consent resolves.

---

# AF10 — Presentation & Game Feel

**Goal:** the polish that makes it feel premium.
**Depends on:** AF1 · **Effort:** ~1.5 weeks · **Priority:** P2

- [x] **AF10-01** Particle system on Canvas (merge burst, chain sparks, level-complete confetti) with a pooled allocator
- [x] **AF10-02** Screen shake + hit-stop scaled by merge size, capped and disableable
- [x] **AF10-03** Chain combo escalation: pitch-shifted SFX, rising combo banner, slow-motion on 4+
- [x] **AF10-04** Piece-drop trail and snap animation with spring physics
- [x] **AF10-05** Adaptive music layers that build with combo intensity
- [x] **AF10-06** Haptic pattern library (place / merge / chain / fail) using `VibrationEffect` composition
- [x] **AF10-07** Animated Home screen background and idle tile shimmer
- [x] **AF10-08** "Reduce motion" setting that disables shake, particles, and slow-motion
- [x] **AF10-09** Frame-budget guard: profile with Macrobenchmark, hold 60 fps on a low-end device, auto-degrade particles

### Acceptance criteria
Sustained 60 fps during a 5-chain merge on the lowest-tier target device, with jank <1% of frames.

---

# AF11 — Accessibility & Device Reach

**Goal:** playable by more people on more hardware.
**Depends on:** AF5 (theme tokens) · **Effort:** ~1 week · **Priority:** P2

- [x] **AF11-01** Colourblind-safe palettes (deuteranopia/protanopia/tritanopia) plus numerals and shape cues on tiles
- [x] **AF11-02** TalkBack support for the Canvas board: semantics per cell, announce value and state
- [x] **AF11-03** Full keyboard/D-pad navigation for the board
- [x] **AF11-04** Respect system font scaling up to 200% without clipping
- [x] **AF11-05** Tablet and foldable layouts (two-pane game + stats), tested across fold/unfold state changes
- [x] **AF11-06** Landscape support for the game screen
- [x] **AF11-07** RTL layout correctness and a localisation pass (extract all strings; no hardcoded text in Composables)
- [x] **AF11-08** Large-touch-target mode for the tray and board

### Acceptance criteria
Accessibility Scanner reports no critical issues on Home, Game, and Shop. The game is completable
using TalkBack alone.

---

# AF12 — Hardening: Security, Quality, Delivery

**Goal:** keep the advanced surface area from becoming a liability.
**Depends on:** runs continuously alongside AF1–AF11 · **Effort:** ongoing · **Priority:** P1

### Integrity
- [ ] **AF12-01** Enable R8/minify + resource shrinking for release (currently `isMinifyEnabled = false`) and write keep rules
- [ ] **AF12-02** Replace the debug signing config on release builds with a real keystore from CI secrets
- [ ] **AF12-03** Encrypt local economy state; detect obvious tampering and reconcile against cloud
- [ ] **AF12-04** Play Integrity API check before granting high-value rewards
- [ ] **AF12-05** Detect clock manipulation for daily rewards/streaks (server time where available)

### Quality
- [ ] **AF12-06** Property-based tests for merge/chain invariants (tile count conservation, score monotonicity)
- [ ] **AF12-07** Golden/snapshot tests for board rendering per theme
- [ ] **AF12-08** Compose UI tests for each new screen (Shop, Achievements, Modes, Leaderboard)
- [ ] **AF12-09** Fuzz the replay harness with random action streams; assert no crash and no illegal state
- [ ] **AF12-10** Raise engine coverage target to 90% and enforce it in CI

### Delivery
- [ ] **AF12-11** CI pipeline: build + unit tests + lint + detekt on every PR
- [ ] **AF12-12** Automated AAB build and internal-track upload on tagged releases
- [ ] **AF12-13** Macrobenchmark + Baseline Profile generation for startup and gameplay
- [ ] **AF12-14** Modularise once build time exceeds ~2 min: `:core`, `:engine`, `:data`, `:feature:*`
- [ ] **AF12-15** Crash-free-sessions and ANR budget gates before each rollout increase

---

# Task board (assignable summary)

| ID | Feature | Priority | Depends on | Est. |
|---|---|---|---|---|
| AF0 | Persistence, save/resume, determinism, flags | P0 | — | 1.5 wk |
| AF1 | Special tiles, board variety, objectives | P1 | AF0 | 2.5 wk |
| AF2 | Game modes (Endless/Time/Zen/Daily/Weekly) | P1 | AF0, AF1 | 2 wk |
| AF3 | Booster 2.0 + economy + shop UI | P1 | AF0 | 1.5 wk |
| AF4 | Hints, solver, adaptive difficulty | P2 | AF1 | 2 wk |
| AF5 | XP, achievements, cosmetics, stats | P2 | AF0 | 2 wk |
| AF6 | Firebase, Remote Config, push, events | P1 | AF0 | 1.5 wk |
| AF7 | Play Games sign-in + cloud save | P2 | AF0 | 2 wk |
| AF8 | Leaderboards, tournaments, sharing | P3 | AF6, AF7 | 2 wk |
| AF9 | Rewarded ads, IAP, offers, subscription | P1 | AF3, AF6 | 2 wk |
| AF10 | Particles, juice, adaptive audio, haptics | P2 | AF1 | 1.5 wk |
| AF11 | Accessibility, tablet/fold, localisation | P2 | AF5 | 1 wk |
| AF12 | Security, test depth, CI/CD | P1 | continuous | ongoing |

---

# Release milestones

**v1.1 — "Solid Ground"** · AF0 + AF3 + AF6
Durable saves, real booster economy, analytics and remote tuning. Nothing player-visible is flashy,
but everything after this is measurable and fixable without a store update.

**v1.2 — "More Game"** · AF1 + AF2 + AF10
Special tiles, four extra modes, and the polish pass. This is the first update worth a store feature-graphic change.

**v1.3 — "Progression"** · AF5 + AF4 + AF11
Achievements, cosmetics, hints, accessibility. Targets long-session and lapsed players.

**v2.0 — "Connected"** · AF7 + AF8 + AF9
Cloud save, leaderboards, tournaments, full monetization stack.

---

# Parallel workstreams

Work can be split across five lanes with minimal collision:

- **Engine** — AF1, AF2 core rules, AF4 solver. Touches `game/engine`, `game/rules`, `game/model`.
- **Data/Platform** — AF0, AF6, AF7, AF12 delivery. Touches `data/*`, `di/*`, Gradle, CI.
- **UI/Feature** — AF3 shop, AF5 screens, AF8 leaderboard UI, AF11. Touches `ui/*`, `AppNavGraph`.
- **Art/Feel** — AF10, AF5 cosmetics, theme tokens. Touches `ui/theme`, `res/*`, Canvas draw code.
- **QA** — AF12 quality, per-phase acceptance criteria, device matrix.

The main collision risk is `GameViewModel` and `GameState`, which AF1, AF2, and AF3 all extend.
Land AF2-01 (the `GameMode` abstraction) early so the other two build against a stable shape.

---

# Definition of done (per task)

1. Behaviour is behind a feature flag and defaults off in release until sign-off.
2. Unit tests cover the rule change, including one failure/edge case.
3. Any new persisted field has a migration and a migration test.
4. Analytics events are emitted and appear in the Firebase DebugView.
5. `README.md` is updated if a core rule changed.
6. Verified on a low-end device, not only an emulator.
7. No regression in existing engine tests and no new lint/detekt warnings.

---

# Metrics to watch per phase

| Phase | Primary metric | Guardrail metric |
|---|---|---|
| AF0 | Crash-free sessions | Save write latency, ANR rate |
| AF1 | Levels completed per session | Level-1 completion rate (don't spike difficulty) |
| AF2 | Sessions per DAU | Campaign progression rate |
| AF3 | Booster usage rate | Coin balance distribution (no runaway inflation) |
| AF4 | Level retry-to-clear rate | Hint dependency rate |
| AF5 | D7 retention | Session length (grinding fatigue) |
| AF6 | Config adoption latency | Startup time |
| AF7 | Sign-in rate | Sync conflict rate, support tickets |
| AF8 | Daily Puzzle participation | Invalid score submission rate |
| AF9 | ARPDAU | D1/D7 retention, uninstall rate after first interstitial |
| AF10 | Session length | Frame jank %, battery drain |
| AF11 | Reach across device tiers | Accessibility scanner issues |

---

# Explicitly out of scope

Not planned for this document; revisit only with data:
real-time multiplayer, user-generated level sharing, NFT/crypto anything, a companion web app,
Wear OS or TV builds, and cross-platform (iOS) porting.

---

# Decision log (advanced features)

Add a row **before** implementing any feature whose rules aren't fully specified above.

| ID | Question | Decision | Date | Impact |
|---|---|---|---|---|
| ADV-001 | Does Continue reset the undo stack? | Yes — CONTINUE clears `previousState` (cannot undo through revive). Max 5 undos/run; after CONTINUE, undo blocked until next successful place. Zen keeps free/uncapped Undo when AF3 on. | 2026-09-09 | AF3 |
| ADV-002 | Can wildcards chain with each other? | TBD | | AF1 |
| ADV-003 | Is Daily Puzzle one attempt or unlimited with only the first scored? | First finished run of the calendar day locks official score + rewards; further plays that day are practice (attempts++ only). | AF2-06 | AF2 |
| ADV-004 | Does Remove Ads include rewarded ads? | TBD | | AF9 |
| ADV-005 | Cloud-save conflict tiebreaker when progress is equal? | **Locked:** progress tuple `(totalStars, completedLevelCount, totalMerges, coins)`; strictly higher wins auto merge-safe apply; equal/non-dominating → chooser; apply uses max coins/stars, unlock union by quantity, level max stars/score | 2026-09-09 | AF7 |
| ADV-006 | How are hints monetized? | Coins first (HINT_COST=30); insufficient funds → AF3 funds sheet (rewarded stub grants coins). Free ad-for-hint deferred to AF9-02. Rate limit 1/10s, max 5/run. | 2026-09-09 | AF4 |
| ADV-007 | Cosmetics depth + XP curve + unlock persistence | Real TileTheme/BoardTheme token sets (Classic/Marble/Neon/Seasonal colour+overlay). XP: `100*level`; awards merge+2 / chain+1 / clear+50 / quest+25. Unlocks reuse UnlockEntity categories; milestones require Claim. | 2026-09-09 | AF5 |
| ADV-008 | Remote Config fetch / activate policy | Bootstrap from last-activated cache on startup (never block). Background `fetchAndActivate` with 12h prod / 0 debug interval and 8s timeout; airplane mode keeps defaults. Booster costs / flags refresh via LiveConfig revision after activate. | 2026-09-09 | AF6 |
