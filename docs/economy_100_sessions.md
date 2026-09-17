# Economy simulation — 100 sessions (AF3-08)

Assumptions for tuning `BoosterCatalog` / `Constants` before launch. Not a live spreadsheet —
numbers are modeled from current catalog costs and earn hooks.

## Session mix (100 sessions)

| Mode | Sessions | Notes |
|------|----------|-------|
| Campaign | 45 | Levels with objectives; +1 Undo on clear (AF3) |
| Endless | 25 | Higher booster usage, no level grant |
| Daily | 15 | Quest claims + daily reward |
| Time Attack | 10 | Time Freeze relevant |
| Zen | 5 | Free uncapped Undo when AF3 on |

## Catalog coin sinks (reference)

| Sink | Cost |
|------|------|
| Undo | 50 |
| Swap | 40 |
| Shuffle | 80 |
| Remove | 120 |
| Hammer | 150 |
| Value Up | 120 |
| Magnet | 180 |
| Time Freeze | 100 |
| Continue (1st / 2nd) | 100 / 200 (cap 800; max 2 coin + 1 rewarded) |

Prefer **owned charges first**, then coins.

## Modeled income (100 sessions)

| Source | Estimate | Coins / charges |
|--------|----------|-----------------|
| Level complete (45) | +50 coins avg + 1 Undo | ~2,250 coins · 45 Undo |
| Daily quest claims (~20) | coins from quest table + 1 booster | ~3,000 coins · ~20 boosters |
| Daily login streak | mixed | ~1,500 coins |
| Rewarded stub (funds sheet / shop) | occasional | ~800 coins |
| **Total income** | | **~7,550 coins** + inventory grants |

## Modeled sinks (100 sessions)

| Sink | Est. uses | Coin spend (after inventory) |
|------|-----------|------------------------------|
| Undo | 120 | ~3,000 (many from inventory) |
| Shuffle / Swap / Remove | 60 | ~3,200 |
| Hammer / Value Up / Magnet | 25 | ~2,800 |
| Continue (coin) | 18 | ~2,400 |
| Time Freeze | 8 | ~400 |
| **Total sinks** | | **~11,800** |

## Net over 100 sessions

| | |
|--|--|
| Starting wallet | 100 |
| Income | +7,550 |
| Spend | −11,800 |
| **Net** | **≈ −4,150** (slightly sink-heavy — intentional early soft pressure toward Shop stubs / ads) |

Players who rely on owned charges (level Undo grants, quest boosters) stay closer to break-even; heavy Continue users feel the escalate faster.

## Recommended tweaks

1. Keep **Undo at 50** / starting owned **3** — soft onboarding.
2. If net sink feels too harsh in playtests, drop **Remove** to 100 or grant **+1 Remove** every 3 campaign clears.
3. Cap Continue already at **800** / **2 coin** — do not raise without more income.
4. Magnet at **180** is the luxury tool; leave high until AF9 IAP packs ship.
5. Revisit after AF6 remote config can A/B cost rows without a client release.

_Last aligned with `BoosterCatalog` costs in AF3._
