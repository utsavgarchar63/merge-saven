# Confirmed unused cleanup

FakeAdService moved to unit tests; unused NoOpAdService deleted. Source masters, save identifiers and Room schemas preserved.

Lint candidates independently checked for Kotlin/Java/XML references before removal:

- color.board_cell_empty
- color.board_cell_highlight
- color.board_cell_invalid
- color.coin_gold
- color.error
- color.seed
- color.success
- color.text_dark
- color.text_white
- color.tile_blue
- color.tile_gold
- color.tile_green
- color.tile_pink
- color.tile_purple
- color.tile_red
- color.warning
- color.wood_light
- color.wood_mid
- string.ad_unavailable
- string.cloud_delete
- string.cloud_delete_body
- string.cloud_export
- string.cloud_guest
- string.cloud_sign_in
- string.cloud_sign_out
- string.game_coins
- string.game_level
- string.game_over_best
- string.game_over_coins_earned
- string.game_over_continue
- string.game_over_continue_cost
- string.game_over_final_score
- string.game_over_highest_tile
- string.game_over_home
- string.game_over_moves
- string.game_over_play_again
- string.game_over_retry
- string.game_over_score
- string.game_over_share
- string.game_over_title
- string.game_over_watch_ad
- string.game_over_watch_ghost
- string.game_pause
- string.game_score
- string.game_target
- string.home_best_score
- string.home_continue
- string.home_daily
- string.home_levels
- string.home_play
- string.home_settings
- string.home_shop
- string.level_complete_coins
- string.level_complete_continue
- string.level_complete_fresh_board
- string.level_complete_home
- string.level_complete_level
- string.level_complete_map
- string.level_complete_next
- string.level_complete_replay
- string.level_complete_title
- string.offer_starter
- string.pause_home
- string.pause_restart
- string.pause_resume
- string.pause_settings
- string.settings_about
- string.settings_language
- string.settings_privacy
- string.settings_support
- string.settings_terms
- string.settings_version
- string.shop_boosters
- string.shop_coins
- string.shop_coins_balance
- string.shop_remove_ads
- string.shop_special_packs
- string.shop_title
- string.tutorial_drag
- string.tutorial_merge
- string.tutorial_rotation
- string.tutorial_skip
- string.tutorial_welcome
- string.watch_ad_double_coins
- string.watch_ad_extra_daily
- string.watch_ad_hint
- drawable.art_share_v2
- drawable.art_sparkle_v2

Additional duplicate legacy icons removed after reference audit:

- app\src\main\res\drawable\ic_booster_swap.xml
- app\src\main\res\drawable\ic_booster_undo.xml
- app\src\main\res\drawable\ic_coin.xml
- app\src\main\res\drawable\ic_pause.xml
- app\src\main\res\drawable-nodpi\ic_launcher_foreground.webp
- app\src\main\res\drawable-nodpi\ic_launcher_playstore.webp

New help/privacy/retry/rotate/share icons are wired to their controls.

Removed three unused GameIcons aliases and their unreferenced legacy resources after a Kotlin/XML audit: `logo_merge_seven.webp`, `bg_wood_main.webp`, `icon_check.xml`. The earlier stage removed 97 runtime resources.

## October 2 scope

Removed account sign-in, export/deletion/upload, cloud synchronization, leaderboard submission/screens, remote tournament and ghost-download code. Their Firebase Auth/Firestore/Functions/Messaging and Play Games dependencies and unused version-catalog aliases are removed. Local Weekly play, deterministic engine tests, score-image sharing and package/Firebase identity are preserved. Replay validation helpers needed only by unit tests now live in test sources.

Removed old mode/reward exports after replacing them with ten transparent v3 WebP icons; unused sparkle/share exports remain as source masters only. Removed two unused music stems after replacing the mix with one original loop. Removed six unreferenced leaderboard/Games/ghost/close resources identified by lint and reference checks. Removed one-time refactoring scripts created during this work after their changes were applied; reusable music, screenshot and ad-log tools remain.

The existing full-screen wood, board and Zen images are preserved byte-for-byte. Generated icon editing removes the outer rectangular surrounds only.

Three static tutorial XML resources were removed after the runtime guide switched to native animated Canvas diagrams. Their original SVG masters remain under art/tutorial as source records.
