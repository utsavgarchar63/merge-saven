# Free economy model — 100 sessions per cohort

Run `python tools/model_free_economy.py` to regenerate. No ads or purchases. Starting wallet: 100 coins.

| Cohort | Wins | Earned | Spent | End wallet | Lowest wallet | Unaffordable requests |
|---|---:|---:|---:|---:|---:|---:|
| Beginner | 20 | 8400 | 1000 | 7500 | 100 | 0 |
| Moderate | 50 | 9700 | 2800 | 7000 | 100 | 0 |
| Heavy | 50 | 9700 | 9800 | 0 | 0 | 136 |

Assumptions: ten sessions per day for ten days; beginners win one in five, other cohorts one in two. First thirty wins clear unique campaign levels; later wins replay them. Every loss has ten valid moves. Each day claims login and the ten-merge quest; a win also claims the one-level quest. Beginners request one booster every five sessions, moderate users every two, and heavy users three every session. Requests cycle Undo, Randomize and Remove, consuming starter and first-clear Undo charges before coins.

Acceptance: beginner and moderate requests must all be affordable without ads. Heavy use is a sensitivity case: unaffordable requests are declined without blocking normal play. This is an explicit demand model, not a forecast. Long gaps between logins and players who do not claim quests earn less; tune only after observing real play.

Shipping result formulas are tested independently in Af9MonetizationTest. Costs are read from Constants.kt.
