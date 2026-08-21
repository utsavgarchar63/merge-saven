# Merge Seven — Complete Android Game Development Master Plan

## 0. Document Purpose

This document is the **single source of truth** for developing an original Merge Seven-style hex-number puzzle game for Android using **Kotlin + Jetpack Compose**.

It covers the project from **empty folder → Android project setup → playable prototype → complete game → polish → monetization → QA → Play Store release → post-launch operations**.

### Product goals

- Build a polished, original hex-grid number merge game inspired by the supplied screenshots.
- Preserve the *type of gameplay* while creating original visual assets, branding, sounds, effects, UI copy, and code.
- Make the game responsive across Android phone sizes.
- Keep the game engine independent from Android UI so the rules can be tested thoroughly.
- Use a scalable architecture that can later support levels, daily challenges, boosters, analytics, ads, IAP, remote configuration, and live updates.

---

# 1. Product Definition

## 1.1 Working title

**Project name:** `Merge Seven`

Use this as a temporary development name. Before release, perform a trademark/name/store availability check and choose an original final brand.

## 1.2 Target platform

- Android phones first
- Portrait orientation
- Kotlin
- Jetpack Compose
- Android Studio
- Google Play distribution

## 1.3 Core gameplay

The player receives a piece made of one or more numbered hex tiles and places it on a hexagonal board.

Core loop:

```text
Generate Piece
    ↓
Preview Piece
    ↓
Drag / Select
    ↓
Preview Valid Position
    ↓
Place
    ↓
Detect Connected Same-Value Group
    ↓
If Group Size >= 3
    ↓
Merge to Next Number
    ↓
Animate
    ↓
Score + Coins
    ↓
Check Chain Reaction
    ↓
Generate Next Piece
    ↓
Check Game Over
```

## 1.4 Core number progression

```text
2
↓
4
↓
8
↓
16
↓
32
↓
64
↓
128
↓
256
↓
512
↓
1024
↓
2048
↓
4096
...
```

The progression should remain data-driven rather than hard-coded to a maximum value.

---

# 2. Scope

## MVP

The minimum playable release candidate must contain:

- Hex board
- Number tiles
- Multi-tile pieces
- Piece preview
- Placement
- Rotation
- Connected-group detection
- 3+ same-value merge rule
- Chain reactions
- Score
- Best score
- Coins
- Level progression
- Game over detection
- Restart
- Pause/resume
- Undo
- Save/load active game
- Basic sound
- Basic vibration
- Responsive UI

## Release V1

Add:

- Home screen
- Level selection
- Daily reward
- Daily challenge
- Booster shop
- Rewarded ad continue
- Ad-free purchase
- Analytics
- Crash reporting
- Settings
- Privacy/terms links
- Tutorial
- Achievements / milestones
- Production-ready art and effects

## Future

Potential additions:

- Themes
- Seasonal events
- Leaderboards
- Cloud save
- Account system
- Missions
- Battle/pass system
- Limited-time boards
- Accessibility options
- Remote-configured balancing
- A/B testing

---

# 3. Originality / IP Guardrails

Use the supplied screenshots only as **gameplay and visual reference**.

Do NOT ship:

- Original game's logo
- Original game's name
- Original game's exact artwork
- Original game's sound effects
- Original game's music
- Original game's exact UI icons
- Original game's copyrighted text
- Exact copied assets
- Decompiled/copied code

Create original:

- Logo
- Tile treatment
- Colors
- Wood/background texture
- Buttons
- Booster icons
- Coin icon
- Progress indicator
- Animations
- Sounds
- Music
- UI wording
- Level artwork
- Store screenshots

---

# 4. Technology Decisions

## 4.1 Required stack

| Area | Decision |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Rendering | Compose Canvas |
| Architecture | MVVM + domain/game engine |
| State | StateFlow |
| Dependency Injection | Hilt |
| Database | Room |
| Preferences | DataStore |
| Async | Kotlin Coroutines |
| Navigation | Navigation Compose |
| Audio | SoundPool / MediaPlayer as appropriate |
| Ads | Google Mobile Ads SDK |
| Analytics | Firebase Analytics |
| Crash reporting | Firebase Crashlytics |
| Billing | Google Play Billing |
| Testing | JUnit + Compose UI tests + Android instrumentation |

## 4.2 Rendering decision

Use **one primary Canvas for the game board**.

Canvas is preferred for:

- Hex shapes
- Tile positioning
- Tile shadows
- Board cells
- Selection highlights
- Drag previews
- Merge animations
- Particles
- Number text
- Board effects

Use normal Compose UI for:

- Top bar
- Pause button
- Coins
- Progress/level UI
- Booster controls
- Bottom piece queue
- Dialogs
- Menus
- Shop
- Settings
- Home screen

---

# 5. Development Environment Setup

## 5.1 Install

Install:

1. Android Studio stable
2. Android SDK required by the selected stable Android Studio version
3. Android SDK Platform
4. Android SDK Build Tools
5. Android Emulator or physical Android phone
6. Git
7. JDK bundled with Android Studio unless project requirements specify otherwise

## 5.2 Verify tools

Terminal:

```bash
java -version
git --version
```

Check Android SDK using Android Studio:

```text
Android Studio
→ Settings
→ Languages & Frameworks
→ Android SDK
```

## 5.3 Android Studio project creation

Create:

```text
New Project
→ Empty Activity
→ Kotlin
→ Jetpack Compose
```

Recommended setup:

```text
Application ID:
com.yourcompany.mergeseven
```

Replace `yourcompany` with the final company/package namespace.

## 5.4 Initial project settings

Configure:

- App name
- Package name
- Minimum Android version based on the desired device reach
- Target SDK supported by current Play requirements
- Kotlin
- Compose
- Release and debug build types
- Version code
- Version name

Do not manually guess current Play target-SDK requirements; verify them in current Google Play documentation before release.

---

# 6. Git Repository Setup

From the project root:

```bash
git init
git branch -M main
```

Create `.gitignore`:

```gitignore
.gradle/
.idea/
*.iml
local.properties
build/
**/build/
captures/
.externalNativeBuild/
.cxx/
.DS_Store
```

Initial commit:

```bash
git add .
git commit -m "chore: initialize Merge Seven Android project"
```

Recommended branches:

```text
main
develop
feature/*
bugfix/*
release/*
hotfix/*
```

Branch example:

```bash
git checkout -b feature/hex-board
```

---

# 7. Recommended Project Structure

Use this structure from the beginning.

```text
app/
└── src/
    ├── main/
    │   ├── java/com/yourcompany/mergeseven/
    │   │   ├── MainActivity.kt
    │   │   │
    │   │   ├── app/
    │   │   │   ├── MergeSevenApplication.kt
    │   │   │   └── AppNavGraph.kt
    │   │   │
    │   │   ├── core/
    │   │   │   ├── Constants.kt
    │   │   │   ├── Result.kt
    │   │   │   ├── DispatcherProvider.kt
    │   │   │   └── Extensions.kt
    │   │   │
    │   │   ├── game/
    │   │   │   ├── engine/
    │   │   │   │   ├── GameEngine.kt
    │   │   │   │   ├── BoardEngine.kt
    │   │   │   │   ├── MergeEngine.kt
    │   │   │   │   ├── PlacementEngine.kt
    │   │   │   │   ├── SpawnEngine.kt
    │   │   │   │   ├── ScoreEngine.kt
    │   │   │   │   ├── GameOverEngine.kt
    │   │   │   │   └── ChainReactionEngine.kt
    │   │   │   │
    │   │   │   ├── model/
    │   │   │   │   ├── HexCoord.kt
    │   │   │   │   ├── HexCell.kt
    │   │   │   │   ├── Tile.kt
    │   │   │   │   ├── TilePiece.kt
    │   │   │   │   ├── BoardState.kt
    │   │   │   │   ├── GameState.kt
    │   │   │   │   ├── GameAction.kt
    │   │   │   │   ├── GameResult.kt
    │   │   │   │   └── BoosterType.kt
    │   │   │   │
    │   │   │   ├── rules/
    │   │   │   │   ├── MergeRule.kt
    │   │   │   │   ├── SpawnRule.kt
    │   │   │   │   └── LevelRule.kt
    │   │   │   │
    │   │   │   └── repository/
    │   │   │       └── GameRepository.kt
    │   │   │
    │   │   ├── data/
    │   │   │   ├── local/
    │   │   │   │   ├── GameDatabase.kt
    │   │   │   │   ├── GameDao.kt
    │   │   │   │   └── entities/
    │   │   │   └── preferences/
    │   │   │       └── SettingsRepository.kt
    │   │   │
    │   │   ├── ui/
    │   │   │   ├── theme/
    │   │   │   ├── components/
    │   │   │   ├── home/
    │   │   │   ├── game/
    │   │   │   ├── levels/
    │   │   │   ├── shop/
    │   │   │   ├── daily/
    │   │   │   ├── settings/
    │   │   │   └── dialogs/
    │   │   │
    │   │   ├── audio/
    │   │   ├── vibration/
    │   │   ├── ads/
    │   │   ├── billing/
    │   │   ├── analytics/
    │   │   └── crash/
    │   │
    │   └── res/
    │       ├── drawable/
    │       ├── mipmap/
    │       ├── font/
    │       ├── raw/
    │       └── values/
    │
    └── test/
        └── ...
```

---

# 8. Data Model

## 8.1 HexCoord

Use axial coordinates.

```kotlin
data class HexCoord(
    val q: Int,
    val r: Int
)
```

Six directions:

```kotlin
val HEX_DIRECTIONS = listOf(
    HexCoord(1, 0),
    HexCoord(1, -1),
    HexCoord(0, -1),
    HexCoord(-1, 0),
    HexCoord(-1, 1),
    HexCoord(0, 1)
)
```

## 8.2 Tile

```kotlin
data class Tile(
    val id: Long,
    val value: Int,
    val cell: HexCoord
)
```

## 8.3 TilePiece

```kotlin
data class PieceCell(
    val offset: HexCoord,
    val value: Int
)

data class TilePiece(
    val id: Long,
    val cells: List<PieceCell>,
    val rotation: Int = 0
)
```

## 8.4 BoardState

```kotlin
data class BoardState(
    val cells: Map<HexCoord, Tile?>,
    val playableCells: Set<HexCoord>
)
```

## 8.5 GameState

```kotlin
data class GameState(
    val board: BoardState,
    val currentPiece: TilePiece,
    val nextPieces: List<TilePiece>,
    val score: Long,
    val bestScore: Long,
    val coins: Int,
    val level: Int,
    val targetValue: Int,
    val moves: Int,
    val isPaused: Boolean,
    val isGameOver: Boolean,
    val isBusy: Boolean
)
```

---

# 9. Board Design

## 9.1 Board shape

The visual board may resemble a large honeycomb/hex arrangement.

Do not store pixels as board state.

Store logical cells:

```text
HexCoord(q, r)
```

and calculate their screen positions.

## 9.2 Hex → screen

For flat-top hexes, derive position from axial coordinates.

Keep this transformation in one utility:

```text
HexGeometry
 ├── hexToPixel()
 ├── pixelToHex()
 ├── hexCorners()
 ├── distance()
 └── nearestCell()
```

## 9.3 Board renderer

Responsibilities:

- Draw empty board cells
- Draw tile shadows
- Draw tile body
- Draw tile highlight
- Draw number
- Draw selection state
- Draw valid placement previews
- Draw merge effects

Do not put game rules inside drawing functions.

---

# 10. Merge Rule Specification

## Rule

A connected group of **3 or more adjacent tiles with the same value** can merge.

Example:

```text
4 + 4 + 4 → 8
```

```text
8 + 8 + 8 → 16
```

```text
16 + 16 + 16 → 32
```

## Connected means

Tiles must be connected through the six hex neighbors.

Diagonal-in-a-square-grid logic must NOT be used.

## Group finding

Use BFS/DFS.

Pseudo:

```text
start
 ↓
read tile value
 ↓
visit six neighbors
 ↓
same value?
 ├─ no → ignore
 └─ yes → add to group and continue
```

## Minimum merge

```kotlin
const val MIN_MERGE_COUNT = 3
```

Make this configurable so balancing can change later.

---

# 11. Merge Resolution

After placement:

```text
1. Determine affected cells
2. Find matching connected groups
3. For every group >= 3:
   - pick merge destination
   - remove source tiles
   - create upgraded tile
   - calculate score
   - trigger animation event
4. Re-scan board
5. Repeat until no additional merges
6. Generate/advance queue
7. Check game over
```

## Merge destination

Choose a deterministic rule so replays are consistent.

Recommended:

- The newly placed tile is preferred as destination.
- If not available, use the center-most cell of the connected group.

Document this rule in tests.

---

# 12. Chain Reaction

Example:

```text
4 4 4
```

becomes:

```text
8
```

Suppose the new `8` creates:

```text
8 8 8
```

Then merge again:

```text
16
```

Resolution should continue until the board reaches a stable state.

Pseudo:

```kotlin
while (true) {
    val groups = findMergeableGroups()

    if (groups.isEmpty()) break

    mergeGroups(groups)
}
```

Guard against accidental infinite loops by ensuring each merge strictly reduces tile count or otherwise changes state.

---

# 13. Piece System

Pieces can contain one or more tiles.

Examples:

```text
[4, 8]
[8, 8]
[4, 4]
[2, 4, 8]
```

Represent shape using offsets:

```text
  A
  B
```

or:

```text
A B
```

or:

```text
  A
B C
```

The game can eventually support many piece shapes.

## Piece shape data

Keep shapes in configuration:

```kotlin
data class PieceShape(
    val id: String,
    val cells: List<PieceCell>
)
```

---

# 14. Rotation

Support six rotations:

```text
0°
60°
120°
180°
240°
300°
```

Rotation must happen in axial coordinates.

Do not rotate screen pixels.

Pipeline:

```text
Original offsets
    ↓
Axial rotation
    ↓
Normalize
    ↓
Placement validation
```

Normalize offsets so the preview remains centered and stable.

---

# 15. Placement Rules

A piece can be placed only when every cell maps to:

- An existing playable cell
- An empty cell

Validation:

```kotlin
fun canPlace(
    board: BoardState,
    piece: TilePiece,
    origin: HexCoord
): Boolean
```

Return false if:

- Any piece cell is outside the board
- Any target cell is blocked
- Any target cell is outside playable area

---

# 16. Drag-and-Drop UX

Recommended interaction:

```text
Touch piece
    ↓
Drag upward
    ↓
Show ghost piece
    ↓
Find nearest hex origin
    ↓
Highlight valid cells
    ↓
Release
    ↓
If valid → place
If invalid → return to tray
```

States:

```text
idle
dragging
valid
invalid
placing
merging
```

Disable input during:

```text
placing
merge animation
chain reaction
game over transition
```

Use a state flag such as:

```kotlin
isBusy
```

---

# 17. Piece Queue

Recommended:

```text
Current
Next
Next
```

or three visible slots as in the reference.

Rules:

- Current piece is selected from queue[0]
- After placement, queue shifts left
- A generated piece is appended
- Random generation is weighted

Never regenerate all three queue items after every move.

---

# 18. Spawn / Random System

Start with weighted distributions.

Example prototype:

```text
2  = 35%
4  = 35%
8  = 20%
16 = 8%
32 = 2%
```

For multi-cell pieces, the distribution should consider:

- Current level
- Empty board ratio
- Recent history
- Difficulty
- Player performance

Avoid pure randomness that causes impossible/boring boards.

## Anti-frustration rules

Optional later balancing:

- Prevent excessive consecutive high-value pieces
- Prevent impossible piece sequence streaks
- Guarantee a usable piece when possible
- Reduce difficulty after repeated failures

---

# 19. Game Over

Game over means **none of the next playable pieces can be placed**.

Do not only check whether the board is visually full.

Algorithm:

```text
For every candidate piece
    For every playable origin
        For every allowed rotation
            canPlace?
                YES → not game over
If none work
    → GAME OVER
```

Optimization can be added later.

---

# 20. Scoring

Use a dedicated score service.

Example starting formula:

```text
mergeScore = mergedValue × mergedTileCount
```

Additional chain bonus:

```text
chainMultiplier:
1st merge = 1.0
2nd = 1.25
3rd = 1.5
4th = 2.0
```

Keep numbers in configuration so balancing is easy.

Do not lock final scoring until playtesting.

---

# 21. Coins

Coin sources:

- Normal merge
- Larger merges
- Chain merge
- Level completion
- Daily reward
- Daily challenge
- Rewarded ad
- Achievements

Coin sinks:

- Boosters
- Continue
- Cosmetic themes (future)
- Special events (future)

Never let client-side UI directly add coins. All earning/spending should pass through one economy service.

---

# 22. Boosters

Implement through a common interface.

```kotlin
enum class BoosterType {
    SWAP,
    RANDOMIZE,
    REMOVE,
    UNDO,
    CONTINUE
}
```

## Swap

Example behavior:

```text
Selected tile/piece
↓
Exchange with another eligible piece/value
```

## Randomize

Replace the current piece with another valid generated piece.

## Remove

Remove one selected tile from the board.

## Undo

Restore the previous state.

## Continue

After game over, restore a playable state under controlled rules.

Every booster must define:

- Cost
- Availability
- Validation
- Effect
- Animation
- Analytics event
- Failure case

---

# 23. Undo Architecture

Before every irreversible move:

```text
currentState
    ↓
save to history
```

History:

```kotlin
ArrayDeque<GameState>()
```

Limit:

```text
3 previous states
```

Potential future monetization:

```text
Undo costs coins after free uses
```

---

# 24. Level System

Define levels as data.

```kotlin
data class LevelConfig(
    val level: Int,
    val target: Long,
    val startingBoardPreset: String,
    val spawnProfile: String,
    val boardProfile: String
)
```

Possible goals:

```text
Reach score
Create target tile
Survive N moves
Create N merges
Complete special board
```

Do not make every level identical.

---

# 25. Difficulty Progression

Difficulty can increase by:

- More blocked cells
- More complex piece shapes
- Higher target
- Different spawn distribution
- Limited boosters
- Smaller playable area
- Special challenge rules

Never increase difficulty only by making random values bigger.

---

# 26. Tutorial

First launch:

```text
Welcome
 ↓
Show board
 ↓
Highlight current piece
 ↓
"Drag this piece onto the board"
 ↓
Show placement
 ↓
Show three matching tiles
 ↓
Show merge animation
 ↓
"Three matching tiles become the next number"
 ↓
Explain rotation
 ↓
Finish
```

Tutorial must be skippable.

Persist:

```text
tutorial_completed = true
```

---

# 27. Home Screen

Structure:

```text
Logo
Best Score
Coins

[ PLAY ]

[ LEVELS ]
[ DAILY ]

[ SHOP ]

Settings
```

Optional:

- Continue active game
- Daily challenge badge
- Streak
- New content badge

---

# 28. Game Screen

Top:

```text
Pause
Level / Goal
Coins
```

Center:

```text
Progress indicator
Hex board
```

Bottom:

```text
Boosters
Piece queue
```

Keep the board as the primary visual focus.

---

# 29. Pause Screen

Buttons:

- Resume
- Restart
- Settings
- Sound toggle
- Music toggle
- Exit to home

Do not lose the current game when the app is backgrounded.

---

# 30. Game Over Screen

Show:

```text
GAME OVER

Score
Best
Highest Tile
Moves

Coins earned

[ Continue ]
[ Retry ]
[ Home ]
```

If Continue uses a rewarded ad:

```text
[ Watch Ad + Continue ]
```

The ad should never appear before the player chooses it.

---

# 31. Level Complete Screen

Show:

```text
LEVEL COMPLETE

Target
Score
Best score
Coins
Max tile
Chain record

[ NEXT LEVEL ]
[ PLAY AGAIN ]
[ HOME ]
```

Use a short celebration animation.

---

# 32. Daily Challenge

Daily challenge must use deterministic content.

Recommended:

```text
challengeSeed = YYYY-MM-DD
```

All players get the same daily setup.

Show:

- Challenge title
- Goal
- Special rules
- Attempt count
- Best result
- Reward

Daily rewards should be server-aware if later implemented competitively.

---

# 33. Shop

Sections:

```text
Coins
Boosters
Remove Ads
Special Packs
```

Example:

```text
UNDO
50 coins

RANDOMIZE
80 coins

REMOVE
120 coins
```

Prices are examples only; balance after user testing.

---

# 34. Settings

Include:

- Sound
- Music
- Haptics
- Notifications
- Language
- Restore purchases
- Privacy policy
- Terms
- Support
- About
- Version

Use DataStore.

---

# 35. Visual Design System

## Palette

Base palette inspired by the reference but made original.

Example:

```text
Wood Dark:     #6F3B24
Wood Mid:      #9B5A35
Wood Light:    #C98953

Tile Blue:     #35A9E0
Tile Green:    #62D95C
Tile Red:      #EB665B
Tile Purple:   #7567DD
Tile Pink:     #D955A8
Tile Gold:     #F1B62B

Text White:    #FFFFFF
Text Dark:     #3C241A
Coin Gold:     #FFD54A
Success:       #58D66F
Warning:       #FFB648
Error:         #E9534F
```

These are starting values, not final production colors.

## Tile treatment

Each tile should use:

```text
Outer shadow
↓
Outer border
↓
Main fill
↓
Inner highlight
↓
Soft specular highlight
↓
Number
```

For high-value tiles:

- Glow
- Metallic treatment
- Particle effect
- Distinct border

---

# 36. Typography

Use a readable rounded game-friendly typeface.

Requirements:

- Strong digits
- Clear at small sizes
- Good contrast
- Consistent baseline

Numbers should be center aligned.

Do not use a decorative font that hurts readability.

---

# 37. Background

The supplied reference uses a wood surface.

Create an original seamless wood texture.

Requirements:

- Subtle grain
- No distracting knots behind the board
- Warm tone
- Slight vignette
- Slightly darker edge
- Enough contrast for tiles

Use texture plus gradient rather than a flat background.

---

# 38. Effects

Required first-pass effects:

```text
Tile place
Tile merge
Big merge
Chain merge
Coin reward
Level complete
Game over
Invalid placement
Button press
Booster activation
```

Particle budget should stay small to protect mobile performance.

---

# 39. Animation Timings

Suggested starting values:

```text
Tap scale:              80–120 ms
Tile place:             120–180 ms
Tile slide:             150–250 ms
Merge move:             120–180 ms
Merge pop:              100–180 ms
New tile appear:        100–160 ms
Level complete:         500–900 ms
Dialog open:            180–250 ms
```

Tune after playtesting.

---

# 40. Audio Plan

Create original audio.

Minimum:

```text
tap.wav
tile_place.wav
merge_small.wav
merge_big.wav
chain.wav
booster.wav
coin.wav
level_complete.wav
game_over.wav
button.wav
```

Music:

- Main menu theme
- Gameplay loop
- Celebration/game-over variations if desired

Provide toggles.

Use SoundPool for short effects.

---

# 41. Haptics

Suggested:

```text
Tile placed → light
Merge → medium
Large merge → stronger
Level complete → success pattern
Error → short low-intensity
```

Never make vibration mandatory.

---

# 42. Game Engine API

Keep UI ignorant of implementation details.

Example:

```kotlin
interface GameEngine {

    fun placePiece(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): GameResult

    fun rotatePiece(
        piece: TilePiece
    ): TilePiece

    fun canPlace(
        state: GameState,
        piece: TilePiece,
        origin: HexCoord
    ): Boolean

    fun isGameOver(
        state: GameState
    ): Boolean
}
```

---

# 43. GameResult

Return structured events.

```kotlin
data class GameResult(
    val state: GameState,
    val events: List<GameEvent>
)
```

Events:

```kotlin
sealed interface GameEvent {

    data class TilePlaced(...): GameEvent

    data class MergeStarted(...): GameEvent

    data class MergeCompleted(...): GameEvent

    data class ChainCompleted(...): GameEvent

    data class CoinsEarned(...): GameEvent

    data class LevelCompleted(...): GameEvent

    data object GameOver: GameEvent
}
```

This cleanly separates game rules from animation.

---

# 44. Animation Event Pipeline

```text
User places piece
      ↓
GameEngine returns GameResult
      ↓
ViewModel publishes state
      ↓
UI reads events
      ↓
Animation controller consumes event
      ↓
Sound/Haptic/Particles
      ↓
Next event
```

Never pause the game engine waiting for UI animations unless the rule system specifically requires sequenced resolution.

---

# 45. Persistence

## DataStore

Store:

```text
sound_enabled
music_enabled
haptics_enabled
tutorial_completed
selected_theme
language
```

## Room

Store:

```text
active_game
game_history
level_progress
daily_challenge
achievement_progress
```

## Save strategy

Auto-save:

- After placement
- After merge chain
- Before app leaves foreground
- Before background transition when practical

Do not rely on `onPause()` alone for critical data.

---

# 46. Save Game Format

Version the saved state.

```kotlin
data class SavedGame(
    val schemaVersion: Int,
    val timestamp: Long,
    val gameStateJson: String
)
```

When the data model changes:

```text
schemaVersion 1
→ migration
→ schemaVersion 2
```

This avoids broken saves after app updates.

---

# 47. Navigation

Routes:

```text
home
game
levels
daily
shop
settings
```

Navigation events should never create duplicate game sessions.

Example:

```text
Home → Play
      ↓
Existing active game?
 ├─ Yes → Resume dialog
 └─ No  → New game
```

---

# 48. ViewModel Responsibilities

ViewModel may:

- Expose state
- Accept UI actions
- Call game engine
- Call repository
- Trigger analytics
- Coordinate save
- Start/stop timers when needed

ViewModel must NOT:

- Draw Canvas
- Know pixel coordinates
- Directly manipulate Compose UI
- Contain hard-coded tile rendering logic

---

# 49. Dependency Injection

Use Hilt for:

- GameEngine
- Repositories
- Database
- DataStore
- Analytics
- Ads wrapper
- Billing wrapper
- Audio manager if appropriate

Example dependency flow:

```text
GameScreen
   ↓
GameViewModel
   ↓
GameEngine
   ↓
GameRepository
   ↓
Room / DataStore
```

---

# 50. Phase Roadmap

## PHASE 0 — Product Definition

### Tasks

- [ ] Confirm game name placeholder
- [ ] Define original brand direction
- [ ] Define gameplay rule document
- [ ] Confirm 3+ merge rule
- [ ] Confirm piece types
- [ ] Confirm board shape
- [ ] Confirm rotation support
- [ ] Confirm boosters
- [ ] Define MVP scope
- [ ] Define V1 scope
- [ ] Create acceptance criteria
- [ ] Create risk register

### Deliverable

`PRODUCT_REQUIREMENTS.md`

---

# 51. PHASE 1 — Project Bootstrap

## Tasks

- [ ] Install Android Studio
- [ ] Install Android SDK
- [ ] Create Compose project
- [ ] Set package ID
- [ ] Set app namespace
- [ ] Set minimum SDK
- [ ] Set target SDK
- [ ] Configure Kotlin
- [ ] Configure Compose
- [ ] Create Git repository
- [ ] Create `.gitignore`
- [ ] Create README
- [ ] Create project folders
- [ ] Create application class
- [ ] Configure Hilt
- [ ] Configure navigation
- [ ] Create basic theme
- [ ] Verify debug build
- [ ] Run on emulator
- [ ] Run on physical device

### Acceptance criteria

```text
Build succeeds
App installs
App launches
No startup crash
Git repository is clean
```

### Commit

```text
chore: bootstrap android project
```

---

# 52. PHASE 2 — Design System

## Tasks

- [ ] Define colors
- [ ] Define typography
- [ ] Define spacing
- [ ] Define corner radius
- [ ] Define elevation
- [ ] Define shadows
- [ ] Create game background
- [ ] Create tile specification
- [ ] Create button specification
- [ ] Create coin icon
- [ ] Create booster icon style
- [ ] Create progress indicator
- [ ] Create logo concept
- [ ] Create Figma/design source if applicable
- [ ] Export production assets

### Deliverable

`DESIGN_SYSTEM.md`

---

# 53. PHASE 3 — Hex Geometry

## Tasks

- [ ] Implement HexCoord
- [ ] Implement six neighbor directions
- [ ] Implement axial distance
- [ ] Implement hex rotation
- [ ] Implement hexToPixel
- [ ] Implement pixelToHex
- [ ] Implement hex corners
- [ ] Implement nearest-cell lookup
- [ ] Unit test coordinate conversions
- [ ] Visual debug grid

### Acceptance criteria

- Every board cell maps to the correct screen location.
- Touching a visual cell returns the same logical cell.
- Round-trip coordinate conversions remain stable within expected tolerance.

### Commit

```text
feat: add hex coordinate engine
```

---

# 54. PHASE 4 — Board Engine

## Tasks

- [ ] Define playable cell layout
- [ ] Create BoardState
- [ ] Create empty board
- [ ] Draw board cells
- [ ] Draw tile placeholders
- [ ] Add board center alignment
- [ ] Add responsive sizing
- [ ] Add board debug overlay
- [ ] Add placement occupancy map

### Acceptance criteria

```text
Board renders correctly on:
360x640
390x844
412x915
1080x1920
```

---

# 55. PHASE 5 — Tile Renderer

## Tasks

- [ ] Render hex tile
- [ ] Add shadow
- [ ] Add border
- [ ] Add highlight
- [ ] Render numbers
- [ ] Implement value → theme mapping
- [ ] Render 2
- [ ] Render 4
- [ ] Render 8
- [ ] Render 16
- [ ] Render 32
- [ ] Render 64
- [ ] Render 128
- [ ] Render 256+
- [ ] Add adaptive font size
- [ ] Add overflow handling for large values

---

# 56. PHASE 6 — Piece System

## Tasks

- [ ] Create PieceShape
- [ ] Create piece generator
- [ ] Create weighted spawn config
- [ ] Create current piece
- [ ] Create next-piece queue
- [ ] Render bottom trays
- [ ] Select current piece
- [ ] Rotate piece
- [ ] Reset selection after placement

---

# 57. PHASE 7 — Placement Engine

## Tasks

- [ ] Detect nearest origin
- [ ] Validate placement
- [ ] Highlight valid cells
- [ ] Highlight invalid cells
- [ ] Implement tap placement
- [ ] Implement drag placement
- [ ] Snap piece into board
- [ ] Prevent overlapping placement
- [ ] Prevent outside-board placement
- [ ] Lock input while placing

### Acceptance criteria

Invalid moves never mutate board state.

---

# 58. PHASE 8 — Merge Engine

## Tasks

- [ ] Find matching neighbor
- [ ] BFS group detection
- [ ] Group size validation
- [ ] Merge 3 tiles
- [ ] Merge 4 tiles
- [ ] Merge 5+ tiles
- [ ] Select deterministic destination
- [ ] Upgrade value
- [ ] Update score
- [ ] Emit merge event
- [ ] Re-scan for chain reaction

### Mandatory tests

```text
3x 4 → 8
3x 8 → 16
Separated 4s → no merge
Connected mixed values → only matching value
```

---

# 59. PHASE 9 — Chain Reaction

## Tasks

- [ ] Build merge loop
- [ ] Prevent duplicate group processing
- [ ] Define merge ordering
- [ ] Emit chain index
- [ ] Apply chain score multiplier
- [ ] Add chain sound
- [ ] Add chain particles
- [ ] Test 2-step chain
- [ ] Test 3+ step chain
- [ ] Test complex cluster

---

# 60. PHASE 10 — Score / Coins / Level

## Tasks

- [ ] Score engine
- [ ] Best score
- [ ] Coin economy
- [ ] Coin reward rules
- [ ] Level config
- [ ] Target tracking
- [ ] Level completion detection
- [ ] Level transition
- [ ] Persistent level progress

---

# 61. PHASE 11 — Game Over Engine

## Tasks

- [ ] Enumerate pieces
- [ ] Enumerate rotations
- [ ] Enumerate origins
- [ ] Validate all possible placements
- [ ] Optimize candidate search
- [ ] Trigger game over
- [ ] Prevent false game overs
- [ ] Test almost-full board
- [ ] Test completely full board
- [ ] Test no valid placement despite empty cells

---

# 62. PHASE 12 — Core UX Screens

## Tasks

### Home

- [ ] Logo
- [ ] Play
- [ ] Continue
- [ ] Levels
- [ ] Daily
- [ ] Shop
- [ ] Settings

### Game

- [ ] Pause
- [ ] Level
- [ ] Target
- [ ] Coins
- [ ] Board
- [ ] Boosters
- [ ] Piece queue

### Dialogs

- [ ] Pause dialog
- [ ] Game over
- [ ] Level complete
- [ ] Restart confirmation
- [ ] Exit confirmation

---

# 63. PHASE 13 — Animation & Effects

## Tasks

- [ ] Tile press
- [ ] Drag feedback
- [ ] Valid placement glow
- [ ] Tile landing
- [ ] Merge movement
- [ ] Merge pop
- [ ] Number transition
- [ ] Chain effect
- [ ] Coin burst
- [ ] Level celebration
- [ ] Game-over transition
- [ ] Booster effect
- [ ] Button press animation

Performance target:

```text
Stable 60 FPS on representative mid-range devices.
```

---

# 64. PHASE 14 — Audio / Haptics

## Tasks

- [ ] Import original SFX
- [ ] Configure SoundPool
- [ ] Add music player
- [ ] Add sound toggle
- [ ] Add music toggle
- [ ] Add haptic toggle
- [ ] Map gameplay events to audio
- [ ] Add merge pitch variation
- [ ] Test audio focus
- [ ] Test background/foreground behavior

---

# 65. PHASE 15 — Save / Resume

## Tasks

- [ ] Room database
- [ ] Active game entity
- [ ] Serialization
- [ ] Schema version
- [ ] Auto-save after moves
- [ ] Resume after app restart
- [ ] Save settings
- [ ] Save level progress
- [ ] Test corrupted/invalid data
- [ ] Test upgrade migration

---

# 66. PHASE 16 — Tutorial

## Tasks

- [ ] New-user detection
- [ ] Tutorial overlay
- [ ] Highlight piece
- [ ] Highlight board
- [ ] Demonstrate placement
- [ ] Demonstrate merge
- [ ] Demonstrate rotation
- [ ] Explain objective
- [ ] Skip tutorial
- [ ] Save completion flag

---

# 67. PHASE 17 — Boosters

## Tasks

- [ ] Booster framework
- [ ] Swap
- [ ] Randomize
- [ ] Undo
- [ ] Remove
- [ ] Continue
- [ ] Coin cost
- [ ] Availability rules
- [ ] UI states
- [ ] Animation
- [ ] Sound
- [ ] Haptics
- [ ] Analytics
- [ ] Edge-case handling

---

# 68. PHASE 18 — Daily Features

## Tasks

- [ ] Daily reward
- [ ] Daily challenge
- [ ] Deterministic seed
- [ ] Challenge goal
- [ ] Reward
- [ ] Streak
- [ ] Reset handling
- [ ] Timezone testing
- [ ] Offline behavior

---

# 69. PHASE 19 — Shop / Economy

## Tasks

- [ ] Coin balance UI
- [ ] Booster prices
- [ ] Purchase confirmation
- [ ] Insufficient coins state
- [ ] Reward animation
- [ ] Transaction safety
- [ ] Economy logging
- [ ] Balance testing

---

# 70. PHASE 20 — Ads

## Tasks

- [ ] Create AdMob app
- [ ] Integrate SDK
- [ ] Create ad abstraction
- [ ] Configure test ads
- [ ] Rewarded ad
- [ ] Optional interstitial
- [ ] Frequency cap
- [ ] Reward callback validation
- [ ] Handle no-fill
- [ ] Handle offline
- [ ] Respect user experience

Preferred placements:

```text
Rewarded:
- Continue
- Double reward
- Free booster
- Optional daily bonus
```

Avoid forcing an ad after every move.

---

# 71. PHASE 21 — Billing

## Tasks

- [ ] Configure Play Billing
- [ ] Create products
- [ ] Load product details
- [ ] Purchase flow
- [ ] Purchase acknowledgement
- [ ] Restore purchases
- [ ] Remove ads entitlement
- [ ] Error handling
- [ ] Test cards/products
- [ ] License testing

Never trust the client UI as proof of purchase state.

---

# 72. PHASE 22 — Firebase

## Tasks

- [ ] Create Firebase project
- [ ] Add Android app
- [ ] Add Analytics
- [ ] Add Crashlytics
- [ ] Upload config
- [ ] Configure release mapping
- [ ] Confirm events
- [ ] Confirm crash reports

Do not commit credentials/secrets that should not be public.

---

# 73. PHASE 23 — Analytics Plan

Events:

```text
app_open
tutorial_started
tutorial_completed
game_started
game_resumed
piece_placed
merge_completed
chain_completed
level_started
level_completed
game_over
booster_used
undo_used
reward_ad_started
reward_ad_completed
continue_used
daily_started
daily_completed
shop_opened
purchase_started
purchase_completed
```

Parameters:

```text
level
score
tile_value
merge_count
chain_count
booster_type
piece_type
session_id
```

Do not send personally identifying data unless required and properly handled.

---

# 74. PHASE 24 — QA Foundation

## Unit tests

Test:

```text
Hex coordinate math
Neighbors
Rotation
Placement
Group detection
Merge
Chain reaction
Score
Coins
Game over
Piece generation
Level rules
Save/load
```

## UI tests

Test:

```text
Home → Play
Pause → Resume
Pause → Home
Game over → Retry
Game over → Home
Level complete → Next
Booster UI
Settings toggles
```

---

# 75. Phase 25 — Edge Cases

Mandatory edge cases:

- [ ] 3 matching tiles
- [ ] 4 matching tiles
- [ ] 5+ matching tiles
- [ ] Two separate groups
- [ ] Touching mixed values
- [ ] Chain reaction
- [ ] Merge at board edge
- [ ] Merge in corner
- [ ] Maximum value tile
- [ ] Very large number display
- [ ] Full board
- [ ] Empty board
- [ ] One-cell remaining
- [ ] Piece larger than available area
- [ ] Invalid rotation
- [ ] Fast repeated taps
- [ ] Double placement
- [ ] App killed during animation
- [ ] App killed after placement
- [ ] App resumed from background
- [ ] Rotation during drag
- [ ] Booster during animation
- [ ] Undo after chain
- [ ] Undo after level completion

---

# 76. Phase 26 — Device / Performance QA

Test at minimum:

```text
Low-end Android
Mid-range Android
High-end Android
Small phone
Large phone
Different DPI
Light system theme
Dark system theme
Navigation mode variations
```

Measure:

- Startup time
- Memory
- Frame rate
- Battery drain
- ANR
- Crash rate
- GPU overdraw
- Animation smoothness

---

# 77. Phase 27 — Accessibility

Add where practical:

- Content descriptions for controls
- Large touch targets
- High contrast
- No color-only meaning
- Haptics optional
- Sound optional
- Text scaling behavior reviewed
- Screen-reader labels
- Reduced-animation option if required later

Number values must remain distinguishable without relying solely on tile color.

---

# 78. Phase 28 — Security / Integrity

For a mostly local game:

- Validate all economy operations centrally.
- Do not expose secret API keys in the app.
- Do not hard-code private backend credentials.
- Validate purchase callbacks.
- Protect debug endpoints.
- Remove logs containing sensitive information from release builds.
- Enable R8 where appropriate.
- Review exported Android components.

If leaderboards or competitive scores are added, move score verification to a trusted backend.

---

# 79. Phase 29 — Release Build

## Build types

```text
debug
release
```

Release requirements:

- [ ] Version code updated
- [ ] Version name updated
- [ ] Signing configured
- [ ] R8 tested
- [ ] Crash reporting enabled
- [ ] Ads production IDs
- [ ] Billing production products
- [ ] No test ads
- [ ] No debug logs
- [ ] No test buttons
- [ ] No sample data
- [ ] No placeholder assets

Generate:

```text
Android App Bundle (.aab)
```

---

# 80. Phase 30 — Play Store

Prepare:

- [ ] App name
- [ ] Short description
- [ ] Full description
- [ ] App icon
- [ ] Feature graphic
- [ ] Screenshots
- [ ] Privacy policy
- [ ] Data safety form
- [ ] Content rating
- [ ] Target audience
- [ ] Ads declaration
- [ ] App access instructions if required
- [ ] Account deletion flow if applicable
- [ ] Contact email/support
- [ ] Store category
- [ ] Tags/keywords

Verify current Play Console requirements before production launch.

---

# 81. Phase 31 — Internal Testing

Release to:

```text
Internal testers
```

Test:

```text
install
update
restore
billing
ads
save/load
gameplay
crashes
```

Collect:

- Crash reports
- ANRs
- Device-specific bugs
- Gameplay feedback
- Difficulty feedback
- Ad complaints
- UI readability issues

---

# 82. Phase 32 — Closed Testing

Increase tester group.

Test:

- Retention
- Difficulty
- Tutorial effectiveness
- Economy
- Booster usefulness
- Rewarded ad conversion
- Daily challenge
- Shop

Do not rush to production.

---

# 83. Phase 33 — Production Release

Before rollout:

```text
Release checklist = 100%
```

Start with staged rollout if appropriate.

Monitor:

```text
Crash-free users
ANR
Retention
Average session duration
Game starts/session
Game over rate
Level completion
Rewarded ad completion
Purchases
Reviews
```

---

# 84. Post-Launch Phase

## Week 1

Monitor:

- Crash issues
- Tutorial drop-off
- First-game difficulty
- Game-over frequency
- Ad problems
- Purchase failures

## Week 2–4

Balance:

- Spawn rates
- Level targets
- Booster costs
- Coin rewards
- Daily challenges

## Monthly

Add:

- New levels
- New challenges
- New cosmetics
- New events
- New boosters if useful

---

# 85. Task Priority System

Use:

```text
P0 = blocker / core gameplay
P1 = required for MVP
P2 = important polish
P3 = future enhancement
```

Examples:

```text
P0: Hex engine
P0: Placement
P0: Merge
P0: Game over

P1: Save game
P1: Level system
P1: Tutorial
P1: Audio

P2: Particles
P2: Daily challenge
P2: Advanced animations

P3: Themes
P3: Leaderboards
P3: Cloud sync
```

---

# 86. Developer Task Format

Every task should contain:

```text
Task ID
Title
Priority
Description
Dependencies
Implementation notes
Files/classes affected
Acceptance criteria
Unit tests
UI tests
Definition of done
```

Example:

```text
Task ID: GAME-MERGE-001

Title:
Implement 3-tile merge

Priority:
P0

Dependencies:
Board engine
Hex neighbors
Tile model

Description:
Detect connected same-value groups after placement and
upgrade any group containing three or more tiles.

Acceptance:
3 adjacent 4 tiles become one 8.
Non-adjacent 4 tiles remain separate.
Score event is emitted.
Merge event is emitted.
```

---

# 87. Suggested Milestones

## Milestone M0 — Empty Project

Output:

```text
App launches
Git works
Architecture exists
```

## M1 — Board Prototype

Output:

```text
Hex board visible
Touch returns cells
```

## M2 — Playable Core

Output:

```text
Place pieces
Merge tiles
Chain reaction
Game over
```

## M3 — Complete Gameplay

Output:

```text
Score
Levels
Coins
Undo
Save
```

## M4 — Polished Game

Output:

```text
Animations
Audio
Haptics
Tutorial
Home
Settings
```

## M5 — Monetization

Output:

```text
Ads
Boosters
Shop
Billing
```

## M6 — Release Candidate

Output:

```text
QA pass
AAB
Store assets
Internal testing
```

---

# 88. Recommended First 20 Developer Tasks

Do these in this exact order.

```text
01. Create Android Studio project
02. Set application ID
03. Initialize Git
04. Configure Compose
05. Add Hilt
06. Create package structure
07. Create GameState
08. Create HexCoord
09. Implement neighbor math
10. Implement hex→pixel
11. Implement pixel→hex
12. Render board
13. Render one tile
14. Render number tiles
15. Create TilePiece
16. Create piece queue
17. Implement piece placement
18. Implement BFS matching
19. Implement 3+ merge
20. Implement chain reaction
```

Then:

```text
21. Score
22. Coins
23. Level
24. Game over
25. Game save
26. Undo
27. Drag placement
28. Rotation
29. Animations
30. Audio
31. Tutorial
32. Home
33. Settings
34. Boosters
35. Daily
36. Shop
37. Ads
38. Billing
39. Analytics
40. QA
41. Release
```

---

# 89. Definition of Done

A feature is DONE only when:

- [ ] Code implemented
- [ ] UI implemented if required
- [ ] State handling complete
- [ ] Error case handled
- [ ] Unit test added
- [ ] UI test added where useful
- [ ] No compiler warnings caused by the feature
- [ ] No crash in manual test
- [ ] Tested on at least one low/mid-range device
- [ ] Analytics added if the feature affects business behavior
- [ ] Documentation updated
- [ ] Git commit created

---

# 90. Scratch / Working Notes

Use this section for temporary implementation notes.

## Game rule questions to lock before full production

```text
Q1: Is exactly 3 required, or is 3+ allowed?
Decision:
3+ is recommended.

Q2: What happens when 4/5/6 identical tiles are connected?
Decision:
Define deterministic merge behavior and test it before production.

Q3: Does the newly placed tile always become the merge destination?
Decision:
Preferred for predictable UX.

Q4: Can a piece contain arbitrary shapes?
Decision:
Start with a small curated shape set.

Q5: Are six rotations always available?
Decision:
Recommended yes.

Q6: Can the player swap queue pieces?
Decision:
Only through a booster unless intentionally made free.

Q7: What counts toward level progress?
Decision:
Choose target type per level.

Q8: What happens after game over?
Decision:
Retry, Home, optional rewarded-ad Continue.
```

---

# 91. Scratch Balancing Table

Start with placeholders and replace through playtesting.

| Parameter | Prototype | Final |
|---|---:|---:|
| Minimum merge | 3 | TBD |
| Initial coin balance | 100 | TBD |
| Undo cost | 50 | TBD |
| Randomize cost | 80 | TBD |
| Remove cost | 120 | TBD |
| Continue cost | 100 | TBD |
| Rewarded continue limit | 1/game | TBD |
| Daily reward | 50 | TBD |
| Base score | TBD | TBD |
| Chain multiplier | TBD | TBD |

---

# 92. Scratch Piece Pool

Prototype piece pool:

```text
Single:
[2]
[4]
[8]

Vertical:
[4,8]

Horizontal:
[4,4]

Triangular:
[2,4,8]

Future:
[4,4,4]
[2,4,4]
[8,8]
```

Do not ship this pool without playtesting.

---

# 93. Scratch Level Pool

Prototype:

```text
Level 1
Target: 16

Level 2
Target: 32

Level 3
Target: 64

Level 4
Target: 128

Level 5
Target: 256
```

Later create full data-driven level definitions.

---

# 94. Bug Tracking Template

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

Severity:

```text
S0 = crash/data loss/blocker
S1 = major gameplay break
S2 = normal bug
S3 = cosmetic/minor
```

---

# 95. Performance Checklist

```text
[ ] No unnecessary object allocation per frame
[ ] No bitmap recreation every frame
[ ] Canvas drawing optimized
[ ] Particles capped
[ ] Animations canceled safely
[ ] No memory leaks from Activity/Context
[ ] Database work off main thread
[ ] Image assets compressed
[ ] Release build profiled
[ ] Frame rate tested
[ ] Startup profiled
```

---

# 96. Final Production Architecture

```text
                    ┌──────────────────┐
                    │   Compose UI     │
                    └────────┬─────────┘
                             │
                       User Actions
                             │
                             ▼
                    ┌──────────────────┐
                    │   ViewModel      │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   Game Engine    │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼─────────────────────┐
        ▼                    ▼                     ▼
 Board Engine          Merge Engine         Spawn Engine
        │                    │                     │
        └────────────────────┼─────────────────────┘
                             ▼
                        Game Result
                             │
             ┌───────────────┼────────────────┐
             ▼               ▼                ▼
          UI State         Audio           Haptics
             │
             ▼
       Persistence
       Room/DataStore

Parallel services:
Analytics
Crashlytics
Ads
Billing
```

---

# 97. Final Recommended Build Order

The safest order is:

```text
STEP 1
Empty Android project

STEP 2
Hex coordinate math

STEP 3
Board renderer

STEP 4
Tile renderer

STEP 5
Piece system

STEP 6
Placement

STEP 7
Merge detection

STEP 8
Merge resolution

STEP 9
Chain reaction

STEP 10
Game-over detection

STEP 11
Score

STEP 12
Coins

STEP 13
Levels

STEP 14
Undo

STEP 15
Save/load

STEP 16
Drag/drop

STEP 17
Rotation

STEP 18
UI polish

STEP 19
Animation

STEP 20
Audio/haptics

STEP 21
Home/tutorial/settings

STEP 22
Boosters

STEP 23
Daily challenge

STEP 24
Shop

STEP 25
Ads

STEP 26
Billing

STEP 27
Analytics

STEP 28
QA

STEP 29
Performance

STEP 30
Play Store release
```

---

# 98. First Development Session Checklist

When starting the project from scratch, do exactly this:

```text
[ ] Open Android Studio
[ ] Create Empty Activity
[ ] Select Kotlin
[ ] Select Compose
[ ] Set package
[ ] Build project
[ ] Run on device
[ ] Initialize Git
[ ] Create package structure
[ ] Add Hilt
[ ] Add Navigation Compose
[ ] Add Room
[ ] Add DataStore
[ ] Create GameState
[ ] Create HexCoord
[ ] Create HexGeometry
[ ] Write first hex-coordinate unit tests
[ ] Create GameScreen
[ ] Create HexBoard
[ ] Draw one hex
[ ] Draw a full board
[ ] Commit
```

First commit after successful bootstrap:

```bash
git add .
git commit -m "chore: bootstrap merge seven game"
```

First gameplay commit:

```bash
git add .
git commit -m "feat: implement hex board and tile rendering"
```

First core-rule commit:

```bash
git add .
git commit -m "feat: implement merge engine"
```

---

# 99. Final Release Gate

The game is ready for release only when all are true:

```text
[ ] Core game is fun without monetization
[ ] Merge behavior is deterministic and tested
[ ] No critical crashes
[ ] Save/resume works
[ ] Game over is correct
[ ] Tutorial is understandable
[ ] UI works on target screen sizes
[ ] Animations are smooth
[ ] Audio/haptics are optional
[ ] Economy is balanced
[ ] Ads do not block normal gameplay
[ ] Billing works
[ ] Analytics works
[ ] Crashlytics works
[ ] Privacy/legal content ready
[ ] Production assets ready
[ ] AAB generated
[ ] Internal test passed
[ ] Closed test passed
[ ] Play Console release checklist complete
```

---

# 100. Master Checklist

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

# 101. Single Source of Truth Rule

From this point forward, whenever a developer changes a gameplay rule, update this document first.

Rules that must never be spread across random UI classes:

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

Keep those values/configurations centralized.

The ideal dependency is:

```text
Configuration
     ↓
Game Rules
     ↓
Game Engine
     ↓
ViewModel
     ↓
UI
```

NOT:

```text
UI
 ├── contains score rules
 ├── contains merge rules
 ├── contains coin rules
 └── contains game-over rules
```

---

# 102. Project Success Definition

The project is successful when a developer can clone the repository, open Android Studio, build the project, run the app, play the complete game, and understand every major gameplay rule and development decision without needing undocumented knowledge.

This file is intended to remain the **master development checklist, planning document, architecture guide, QA plan, and release checklist** for the project.
