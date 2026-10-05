# APK and AAB size configuration

Local configuration lives in `app/build.gradle.kts`:

- Release keeps R8 optimization/minification and unused-resource shrinking enabled with `proguard-android-optimize.txt` and the existing serialization/Room/Hilt safeguards.
- `resourceConfigurations += listOf("en")` retains the initial English UI and default resources, removing unused library translations. Add supported languages here when app translations are introduced.
- `packaging.dex.useLegacyPackaging = true` explicitly compresses bytecode in directly generated APKs. Android extracts it at installation; it does not change gameplay logic. This setting does not control APKs generated from an AAB.
- AAB language, density and ABI splits are explicitly enabled. All existing supported device architectures and screen densities remain available.
- The original ambient composition is exported as Ogg Vorbis, reducing the runtime music file from 1,764,044 to 143,744 bytes. Duration, mono layout and 22,050 Hz sample rate remain. Lossless master and conversion tooling remain outside the app. Wood/board/Zen background files are unchanged.

Generate both release artifacts with:

```powershell
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

Outputs: `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`. Debug APKs contain development code/tooling and are not the size comparison for a shipping release.

An AAB includes compressed crash-deobfuscation mapping metadata for Play. That metadata is retained for diagnostics and is not the game's device download. APK/AAB file bytes, installed size and device-specific Play download size are separate measurements. An additional ZIP is unnecessary; APK and AAB are already archives, and repacking a signed APK invalidates its signature.

Existing signing selection is preserved. These locally signed artifacts do not establish Play upload-key ownership. The user subsequently authorized source commit/push to feature/new-design. Generated binaries and local tooling remain excluded from Git; no publishing or account upload is performed.

## References

- [Android size guidance](https://developer.android.com/topic/performance/reduce-apk-size)
- [Resource-language filtering](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep)
- [AGP 8.7 DexPackaging API](https://developer.android.com/reference/tools/gradle-api/8.7/com/android/build/api/dsl/DexPackaging)
- [App Bundle configuration splits](https://developer.android.com/guide/app-bundle/configure-base)

## Verified local delivery — October 2

| Artifact | Bytes | MiB |
|---|---:|---:|
| Optimized release APK | 5,553,598 | 5.30 |
| Optimized release AAB | 10,378,971 | 9.90 |
| Previous release AAB | 11,843,695 | 11.30 |

The AAB is 12.4% smaller than the previous release bundle. The release APK is a shipping-build artifact; the earlier 27.96 MiB debug APK included development tooling and is not an equivalent release comparison.

- Final build: BUILD SUCCESSFUL in 4m 25s, validation-compressed-release-final-build.log. Release APK/AAB and release vital lint passed.
- Both debug and release unit suites: **286 tests each, zero failures/errors/skips**. Three existing debug-tool tests now verify that release rejects debug coin grants, debug menu/game-over actions and feature overrides; no production safeguard was weakened.
- APK signature verification passed (v2, one signer); 16KB-page zip alignment check passed. Both archive integrity checks passed; all direct APK dex files are compressed. Runtime Ogg hash matches the source; APK music remains stored uncompressed so MediaPlayer can use its resource file descriptor.
- The release APK upgraded the workspace-owned Android 30 QA app without deleting data. Wallet 760 and the Time Attack save remained; Resume, tray selection, rotation, placement, pause and save-and-exit worked. No crash/ANR events appeared. Native MediaPlayer loaded audio/vorbis with the Android Vorbis decoder, mono/22,050 Hz and looping enabled. The headless emulator does not verify listening quality; physical audio QA remains.
- Final hashes and archive details are in compressed-artifacts.json and artifact-hashes.json. Background image bytes remain unchanged.

## AdMob update delivery — October 5

GMA 24.9.0 and UMP 4.0.0 replace the earlier SDKs. The final artifacts preserve version 1.7.0 / code 8. Compression settings, original backgrounds, music and English resource filtering remain unchanged.

| Artifact | Bytes | MiB |
|---|---:|---:|
| Current production release APK | 5,946,202 | 5.67 |
| Current production release AAB | 11,140,756 | 10.62 |

The current release build passed release vital lint and 286 release unit tests. Signature, 16KB-page zip alignment, archive integrity, compressed dex and runtime music-hash checks passed. Current hashes are in `compressed-artifacts.json`, `artifact-hashes.json` and `admob-review-artifacts.json`; they supersede the October 2 artifact hashes. The separate demo QA APK is a debug build, not a release-size comparison. Production requests still receive HTTP 403; see `AD_DIAGNOSTICS.md` for evidence and dashboard actions.
