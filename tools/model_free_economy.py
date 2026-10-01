"""Reproducible 100-session, zero-ad sensitivity model; reads shipping booster prices.

This models demand, not player behavior or retention. Ten sessions/day over ten days.
Only day-login and merge/level quests are claimed; no score quest, daily-challenge
prizes, ad rewards, achievements, or giveaways inflate the baseline.
"""
from pathlib import Path
import csv
import re

ROOT = Path(__file__).resolve().parents[1]
constants = (ROOT / 'app/src/main/java/com/mergeseven/game/core/Constants.kt').read_text(encoding='utf-8')
prices = {name: int(amount) for name, amount in re.findall(r'const val (\w+)_COST\s*=\s*(\d+)', constants)}
login = [50, 100, 150, 200, 300, 500, 1000]
rows, summary = [], []
for cohort, win_every, spend_every, demand in [('Beginner', 5, 5, 1), ('Moderate', 2, 2, 1), ('Heavy', 2, 1, 3)]:
    coins = 100
    inventory = {'UNDO': 3, 'RANDOMIZE': 2, 'REMOVE': 1}
    clears, earned, spent, refused, minimum = 0, 0, 0, 0, coins
    for session in range(1, 101):
        source = 0
        if (session - 1) % 10 == 0:
            source += login[((session - 1) // 10) % 7] + 100  # login + ten-merge quest
        won = session % win_every == 0
        if won:
            clears += 1
            if clears <= 30:
                source += 100
                inventory['UNDO'] += 1
            else:
                source += 30
            if session % 10 == 0:
                source += 200  # one level quest per day
        else:
            source += 10  # failed campaign run has at least ten valid moves
        coins += source
        earned += source
        sinks = 0
        if session % spend_every == 0:
            for index in range(demand):
                kind = ['UNDO', 'RANDOMIZE', 'REMOVE'][(session // spend_every + index) % 3]
                if inventory[kind] > 0:
                    inventory[kind] -= 1
                elif coins >= prices[kind]:
                    coins -= prices[kind]
                    sinks += prices[kind]
                else:
                    refused += 1  # player can still play; never require an ad
        spent += sinks
        minimum = min(minimum, coins)
        rows.append([cohort, session, int(won), source, sinks, coins, refused])
    summary.append([cohort, clears, earned, spent, coins, minimum, refused])

out = ROOT / 'validation'
out.mkdir(exist_ok=True)
with (out / 'economy_100_sessions.csv').open('w', newline='', encoding='utf-8') as stream:
    writer = csv.writer(stream)
    writer.writerow(['cohort', 'session', 'win', 'coins_earned', 'coins_spent', 'balance', 'unaffordable_requests'])
    writer.writerows(rows)
lines = ['# Free economy model — 100 sessions per cohort', '',
         'Run `python tools/model_free_economy.py` to regenerate. No ads or purchases. Starting wallet: 100 coins.', '',
         '| Cohort | Wins | Earned | Spent | End wallet | Lowest wallet | Unaffordable requests |',
         '|---|---:|---:|---:|---:|---:|---:|']
lines += ['| ' + ' | '.join(map(str, row)) + ' |' for row in summary]
lines += ['', 'Assumptions: ten sessions per day for ten days; beginners win one in five, other cohorts one in two. '
          'First thirty wins clear unique campaign levels; later wins replay them. Every loss has ten valid moves. '
          'Each day claims login and the ten-merge quest; a win also claims the one-level quest. '
          'Beginners request one booster every five sessions, moderate users every two, and heavy users three every session. '
          'Requests cycle Undo, Randomize and Remove, consuming starter and first-clear Undo charges before coins.', '',
          'Acceptance: beginner and moderate requests must all be affordable without ads. Heavy use is a sensitivity case: '
          'unaffordable requests are declined without blocking normal play. This is an explicit demand model, not a forecast. '
          'Long gaps between logins and players who do not claim quests earn less; tune only after observing real play.', '',
          'Shipping result formulas are tested independently in Af9MonetizationTest. Costs are read from Constants.kt.']
(out / 'ECONOMY_MODEL.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
assert summary[0][-1] == 0 and summary[1][-1] == 0, 'Zero-ad moderate progression failed'
print('\n'.join(lines[:10]))
