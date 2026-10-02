# Original local music

The original procedural composition is preserved in `art/music/bg_music_master.wav`, created with `tools/create_music.py`. Its compact runtime export is `app/src/main/res/raw/bg_music.ogg`, produced with `tools/compress_music.py`. It uses no third-party recordings or samples.

- Master: 40 seconds; mono PCM, 22,050 Hz, 16 bit, 1,764,044 bytes. Runtime: Ogg Vorbis quality 5, same duration/sample rate, 143,744 bytes (91.9% smaller than PCM).
- 96 BPM, D major / A major / B minor / G major, gentle bell arpeggios and soft harmonic accompaniment.
- Short boundary fades prevent a discontinuity click. Android loops the bundled file; no streaming or download is required.
- One asynchronously prepared MediaPlayer replaces three simultaneous music players. SoundPool keeps short placement/merge effects responsive.
- Music observes foreground, pause, audio focus and the persisted music preference. Focus ducking and short volume ramps reduce abrupt changes. Real speaker/headphone listening remains part of physical-device QA.

Reproduce locally with NumPy and the pinned tooling: `python -m pip install --no-deps --target validation/tool-deps/audio soundfile==0.13.1 imageio-ffmpeg==0.6.0`, then run the two scripts. Tooling stays outside the APK and is Git-ignored. The export validates duration, channel count, finite samples, peak and correlation before deleting redundant runtime PCM. The current decoded/master correlation is 0.99943; this is a signal comparison, not a substitute for listening. Exact statistics and encoder version are in `validation/music-compression.json`.
