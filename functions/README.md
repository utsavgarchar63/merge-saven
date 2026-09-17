# AF8 Cloud Functions

Deploy with Firebase CLI after `npm install && npm run build` in this folder.

## Callable APIs

- `validateDailyScore` — AF8-03 Daily replay envelope + shadow-ban gate; writes `daily_scores/{date}/players/{playerId}`
- `fetchDailyLeaderGhost` — AF8-07 returns best day's `replayJson`

## Admin: shadow ban / score reset (AF8-08)

```
# shadow ban
firestore: shadow_bans/{playerId} = { reason: "...", at: ... }

# reset a daily score
delete daily_scores/{date}/players/{playerId}
```

Android falls back to local `ReplayRunner` validation when Functions are unreachable.
