"""Export the original music master as a compact, offline Ogg Vorbis resource."""
from pathlib import Path
import json
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "validation/tool-deps/audio"))
import numpy as np
import soundfile as sf
import imageio_ffmpeg

master = root / "art/music/bg_music_master.wav"
legacy = root / "app/src/main/res/raw/bg_music.wav"
output = root / "app/src/main/res/raw/bg_music.ogg"
if not master.exists():
    master.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(legacy, master)

audio, rate = sf.read(master)
assert audio.ndim == 1 and rate == 22050 and len(audio) == rate * 40
subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-hide_banner", "-loglevel", "error",
                "-i", str(master), "-c:a", "libvorbis", "-q:a", "5", str(output)], check=True)
decoded, decoded_rate = sf.read(output)
assert decoded_rate == rate and len(decoded) == len(audio)
assert np.isfinite(decoded).all() and float(np.max(np.abs(decoded))) < 1.0
correlation = float(np.corrcoef(audio, decoded)[0, 1])
assert correlation > 0.98, f"Unexpected audio degradation: {correlation}"
stats = {
    "master": master.relative_to(root).as_posix(),
    "runtime": output.relative_to(root).as_posix(),
    "duration_seconds": len(decoded) / decoded_rate,
    "sample_rate": decoded_rate,
    "channels": 1,
    "master_bytes": master.stat().st_size,
    "runtime_bytes": output.stat().st_size,
    "decoded_correlation": correlation,
    "decoded_peak": float(np.max(np.abs(decoded))),
    "encoder": f"FFmpeg {imageio_ffmpeg.get_ffmpeg_version()}, libvorbis",
    "vorbis_quality": 5,
}
(root / "validation/music-compression.json").write_text(json.dumps(stats, indent=2) + "\n")
# Only remove the redundant runtime PCM after the compressed export validates.
assert legacy.resolve().is_relative_to(root / "app/src/main/res/raw")
legacy.unlink(missing_ok=True)
print(json.dumps(stats, indent=2))
