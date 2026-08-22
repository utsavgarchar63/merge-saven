# Merge Seven — Design System

This document outlines the visual language and design tokens for **Merge Seven**. It serves as the single source of truth for the game's UI aesthetics, aligning the implementation with the intended product design.

## 1. Color Palette

The game uses a warm, premium, wood-inspired aesthetic.

**Core Wood Tones:**
- `WoodDark` (#FF6F3B24): Used for the main background.
- `WoodMid` (#FF9B5A35): Used for panels, dialog backgrounds, and empty slots.
- `WoodLight` (#FFC98953): Used for borders, highlights, and secondary elements.

**Tile Progression Colors:**
- Value 2: `TileBlue` (#FF35A9E0)
- Value 4: `TileGreen` (#FF62D95C)
- Value 8: `TileRed` (#FFEB665B)
- Value 16: `TilePurple` (#FF7567DD)
- Value 32: `TilePink` (#FFD955A8)
- Value 64: `TileGold` (#FFF1B62B)
- Value 128: `TileTeal` (#FF26C6DA)
- Value 256: `TileOrange` (#FFFF8A65)
- Value 512: `TileIndigo` (#FF5C6BC0)
- Value 1024: `TileLime` (#FFAED581)
- Value 2048: `TileCyan` (#FF4DD0E1)
*(Higher values cycle gracefully through the palette)*

**Text Colors:**
- `TextWhite` (#FFFFFFFF): High contrast text for dark wood and colored tiles.
- `TextDark` (#FF3C241A): Used on bright elements like the Gold buttons or Coin icons.

**UI Accents:**
- `CoinGold` (#FFFFD54A): For economy-related elements.
- `Success` (#FF58D66F): Positive actions/notifications.
- `Warning` (#FFFFB648): Warnings and alerts.
- `Error` (#FFE9534F): Destructive actions.

## 2. Typography Scale

Currently defaults to `SansSerif`, intended to be a rounded, readable font (e.g., Nunito or Baloo).

- **Display Large**: 48sp, Bold (Tile Numbers > 1000)
- **Display Medium**: 36sp, Bold (Tile Numbers 100-999)
- **Display Small**: 28sp, Bold (Tile Numbers < 100)
- **Headline Large**: 32sp, Bold (Screen Titles, Dialog Headers)
- **Headline Medium**: 24sp, Bold (Score)
- **Title Large**: 22sp, SemiBold
- **Title Medium**: 16sp, SemiBold
- **Body Large**: 16sp, Normal (Descriptions)
- **Label Large**: 14sp, Bold (Primary Buttons)

## 3. Spacing Scale

- `extraSmall`: 4.dp
- `small`: 8.dp
- `medium`: 16.dp (Standard padding)
- `large`: 24.dp
- `extraLarge`: 32.dp
- `huge`: 48.dp
- `massive`: 64.dp

## 4. Corner Radius & Shapes

- `small`: 4.dp
- `medium`: 8.dp
- `large`: 16.dp (Standard buttons, dialogs)
- `extraLarge`: 24.dp
- `pill`: 50.dp (For chip-style UI, settings toggles)

## 5. Elevation & Shadows

- `none`: 0.dp
- `level1`: 2.dp
- `level2`: 4.dp (Action buttons)
- `level3`: 8.dp (Floating action menus, tiles being dragged)
- `level4`: 12.dp
- `level5`: 16.dp (Dialogs)

## 6. Tile Specification

- **Shape**: Flat-top hexagon.
- **Shadow**: Drop shadow offset X: 2dp, Y: 3dp.
- **Border**: Outer stroke (2dp) using the tile's fill color with 70% opacity.
- **Highlight**: Inner top-left semi-transparent white shape (15% opacity) for a pseudo-3D "glassy" effect.
- **Number Treatment**: Bold, white, dynamically sized based on digit length to ensure no overflow.

## 7. Component Specifications

**Buttons:**
- **Primary**: Pill/Rounded (16dp), `TileGold` background, `TextDark` text, 60dp height.
- **Secondary**: Pill/Rounded (16dp), `WoodLight` (30% opacity) background, `TextWhite` text, 48dp height.

**Background:**
- Original game background relies on a subtle vignette gradient combining `WoodDark` and `WoodMid`.

**Icons:**
- **Coin Icon**: Gold, shiny circular coin or geometric representation.
- **Booster Icons**: Simple line-art or filled semi-transparent icons set inside a small `WoodLight` (20% opacity) rounded square.
- **Progress Indicator**: Hex-based or segmented progress bar using `TileGold` and `WoodMid`.

## 8. Asset Naming Convention

All visual assets follow a strict naming convention:

- `bg_wood_main.webp` (Backgrounds)
- `tile_style_base.svg` (Tile masks/shapes)
- `icon_pause.svg` (UI Icons)
- `icon_coin.svg` (Economy Icons)
- `icon_booster_undo.svg` (Booster Icons)
- `sfx_tile_place.ogg` (Audio)
- `ui_button_primary.svg` (UI elements)
