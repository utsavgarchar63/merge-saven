"""Original, deterministic 40-second ambient score; no third-party samples."""
from pathlib import Path
import wave
import numpy as np

root = Path(__file__).resolve().parents[1]
rate, duration, beat = 22050, 40, .625
audio = np.zeros(rate * duration, dtype=np.float64)
def note(midi, start, length, amplitude, bell=False):
    t = np.arange(int(length * rate)) / rate
    frequency = 440 * 2 ** ((midi-69)/12)
    attack = np.minimum(1, t / (.012 if bell else .45))
    release = np.minimum(1, (length-t) / (.25 if bell else .65))
    envelope = attack * release * (np.exp(-t*3.5) if bell else .8)
    tone = np.sin(2*np.pi*frequency*t) + .18*np.sin(2*np.pi*frequency*2*t) + .06*np.sin(2*np.pi*frequency*3*t)
    indices = (int(start*rate)+np.arange(len(t))) % len(audio)
    audio[indices] += amplitude * envelope * tone

# D major, A major, B minor, G major; four soft variations of the same phrase.
chords = [(50,57,62,66), (45,57,61,64), (47,54,59,62), (43,55,59,62)]
melodies = [(74,78,81,78), (73,76,81,76), (74,78,83,78), (71,74,79,74)]
for bar in range(16):
    start = bar * beat * 4
    chord, melody = chords[bar%4], melodies[bar%4]
    for pitch in chord: note(pitch, start, beat*5, .022)
    for pulse in range(4):
        pitch = melody[(pulse + (bar//4)%2)%4]
        note(pitch, start+pulse*beat, beat*1.8, .052, True)
    if bar%4 == 3: note(86, start+3.5*beat, .8, .015, True)
audio /= max(1., float(np.max(np.abs(audio))/.55))
edge = int(.012*rate)
audio[:edge] *= np.linspace(0,1,edge)
audio[-edge:] *= np.linspace(1,0,edge)
out = root/'art/music/bg_music_master.wav'
out.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(out),'wb') as wav:
    wav.setnchannels(1); wav.setsampwidth(2); wav.setframerate(rate)
    wav.writeframes((audio*32767).astype('<i2').tobytes())
for name in ['bg_music_mid.wav','bg_music_high.wav']:
    path = root/'app/src/main/res/raw'/name
    assert path.resolve().is_relative_to(root)
    path.unlink(missing_ok=True)
print(f'Created {out.name}: {duration}s, original music, peak {np.max(np.abs(audio)):.3f}. Run tools/compress_music.py for the runtime export.')
