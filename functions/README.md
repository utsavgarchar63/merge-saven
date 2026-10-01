# Local backend retirement

No purchases remain in the app. It does not call receipt validation; src/index.ts no longer exports the old purchase callable, and its Google Play validation dependency was removed.

These edits are local only. Nothing was deployed or deleted externally. A separately authorized release must explicitly retire any currently deployed purchase endpoint; an empty local export does not delete a Firebase function. Preserve unrelated cloud-save, leaderboard and account behavior.
