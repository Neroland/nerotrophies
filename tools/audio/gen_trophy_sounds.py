#!/usr/bin/env python3
"""
gen_trophy_sounds.py -- synthesises every NeroTrophies sound from scratch (no samples, no downloads).

    python3 tools/audio/gen_trophy_sounds.py                # render, encode mono 32 kHz Ogg Vorbis q4, write sounds.json
    python3 tools/audio/gen_trophy_sounds.py --check        # decode every .ogg and prove length, peak, loudness, tail
    python3 tools/audio/gen_trophy_sounds.py --report FILE  # the same measurements as a Markdown table
    python3 tools/audio/gen_trophy_sounds.py --lang         # print the subtitle lang entries
    python3 tools/audio/gen_trophy_sounds.py --wav DIR      # also keep the normalised WAVs in DIR
    python3 tools/audio/gen_trophy_sounds.py --placeholder  # 50 ms of silence at every path

"Struck metal that climbs": every award is the same bell voice on D major pentatonic and the tier is the
number of notes (one, two, three, four). The book is paper and leather, re-claim is a small mint stamp, blocks
are a click, a thump and a damped bell. numpy + stdlib wave from fixed seeds; ffmpeg (libvorbis) does the encode.

Loudness is ITU-R BS.1770 K-weighted mean square over the whole file (ungated: these are shorter than the
gating blocks), in LUFS. Awards sit at -18, everything else at -24, every peak at or under -3 dBFS.
"""
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "nerotrophies")
MOD = "nerotrophies"
SR = 32000

D3, D5, E5, FS5, A5, B5, D6, FS6 = 146.83, 587.33, 659.25, 739.99, 880.00, 987.77, 1174.66, 1479.98
PARTIALS = ((1.00, 1.0), (2.76, 0.45), (5.40, 0.22), (8.93, 0.10))   # struck metal: ratio, gain

PEAK_MAX_DB = -3.0          # hard bound on the decoded file
PEAK_AIM_DB = -3.6          # what the normaliser aims under, leaving room for the encoder's overshoot
LUFS_TOL = 2.0
TAIL_DB = -60.0
CLASSES = {                 # target LUFS, sounds.json volume
    "award": (-18.0, 0.8),
    "book": (-24.0, 0.5),
    "block": (-24.0, 0.7),
}


# ---- building blocks -----------------------------------------------------------------------------
def n(sec):
    return int(round(SR * sec))


def tt(sec):
    return np.arange(n(sec)) / SR


def spectral(x, gain_of_freq):
    """Zero-phase filter: scale the spectrum by gain_of_freq(frequencies)."""
    spec = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1.0 / SR)
    return np.fft.irfft(spec * gain_of_freq(fr), len(x))


def lowpass(x, fc, order=4):
    return spectral(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)))


def highpass(x, fc, order=4):
    return spectral(x, lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-6)) ** (2 * order)))


def unit(x):
    return x / (np.max(np.abs(x)) + 1e-12)


def click(rng, ms=4.0, lo=800.0, hi=9000.0):
    """A band-limited noise click with a half-cosine window."""
    x = rng.standard_normal(max(8, n(ms / 1000.0)))
    pad = np.concatenate([x, np.zeros(n(0.02))])
    y = highpass(lowpass(pad, hi, 2), lo, 2)[: len(x)]
    return unit(y * np.hanning(len(x) + 2)[1:-1])


def bell(rng, freq, base, sec, gain=1.0, click_gain=0.35, octave=0.0):
    """The bell voice: four inharmonic partials, each with T60 = base / ratio^0.7, started by a noise click."""
    t = tt(sec)
    y = np.zeros(len(t))
    for ratio, g in PARTIALS:
        f = freq * ratio
        if f >= SR * 0.45:
            continue
        t60 = base / ratio ** 0.7
        y += g * np.exp(-6.9078 * t / t60) * np.sin(2 * np.pi * f * t + rng.uniform(0, 0.4))
    if octave > 0.0:
        y += octave * np.exp(-6.9078 * t / (base / 2 ** 0.7)) * np.sin(2 * np.pi * 2 * freq * t)
    y *= np.minimum(1.0, t / 0.0015)                      # 1.5 ms attack so the strike is not a step
    c = click(rng)
    y[: len(c)] += click_gain * c
    return gain * y


def thump(freq, decay, sec, drop=0.25):
    """A sine thump whose pitch sags a little as it dies."""
    t = tt(sec)
    f = freq * (1.0 - drop * np.minimum(1.0, t / max(decay, 1e-3)) * 0.5)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.exp(-6.9078 * t / decay) * np.sin(ph)


def swept_bandpass(x, f0, f1, q=1.4):
    """A state-variable band-pass whose centre glides from f0 to f1 over the signal."""
    out = np.zeros(len(x))
    low = band = 0.0
    fc = np.linspace(f0, f1, len(x))
    for i in range(len(x)):
        f = 2.0 * math.sin(math.pi * fc[i] / SR)
        low += f * band
        high = x[i] - low - band / q
        band += f * high
        out[i] = band
    return out


def brown(rng, sec):
    x = np.cumsum(rng.standard_normal(n(sec)))
    return unit(highpass(x - np.mean(x), 60.0, 2))


def place(total, *parts):
    """Mix (start_seconds, signal) pairs into a buffer total seconds long."""
    out = np.zeros(n(total))
    for start, sig in parts:
        i = n(start)
        sig = sig[: max(0, len(out) - i)]
        out[i: i + len(sig)] += sig
    return out


# ---- recipes -------------------------------------------------------------------------------------
def award_bronze(rng):
    return lowpass(bell(rng, D5, 0.25, 0.42, click_gain=0.5), 3000.0, 3)


def award_silver(rng):
    return place(0.62, (0.0, bell(rng, D5, 0.35, 0.60)), (0.110, bell(rng, A5, 0.35, 0.50)))


def award_gold(rng):
    return place(0.92,
                 (0.000, bell(rng, D5, 0.30, 0.60)),
                 (0.090, bell(rng, FS5, 0.30, 0.60)),
                 (0.180, bell(rng, A5, 0.50, 0.72, octave=0.25)))


def award_nero(rng):
    total = 1.50
    strikes = [(0.090 * i, bell(rng, f, 0.45, 0.80, gain=0.9)) for i, f in enumerate((D5, FS5, A5, D6))]
    t = tt(0.9)
    swell = 0.35 * 0.5 * (1.0 - np.cos(2 * np.pi * t / 0.9)) * np.sin(2 * np.pi * D3 * t)
    st = tt(0.9)
    shimmer = np.zeros(len(st))
    for f in (D6, FS6):
        for cents in (-3.0, 3.0):
            shimmer += np.sin(2 * np.pi * f * 2 ** (cents / 1200.0) * st + rng.uniform(0, 2 * np.pi))
    trem = 1.0 - 0.30 * 0.5 * (1.0 + np.sin(2 * np.pi * 6.0 * st))
    fade_out = 0.5 * (1.0 + np.cos(np.pi * st / 0.9))
    shimmer *= 0.085 * trem * fade_out * np.minimum(1.0, st / 0.03)
    return place(total, *strikes, (0.0, swell), (0.270, shimmer))


def award_collection(rng):
    return place(1.25,
                 (0.000, bell(rng, D5, 0.60, 0.85)),
                 (0.025, bell(rng, A5, 0.60, 0.85, gain=0.9)),
                 (0.050, bell(rng, D6, 0.60, 0.85, gain=0.8)),
                 (0.380, bell(rng, FS6, 0.60, 0.80, gain=0.6)))


def book_open(rng):
    cover = swept_bandpass(brown(rng, 0.18), 300.0, 900.0)
    tc = tt(0.18) / 0.18
    cover = unit(cover) * np.sin(np.pi * tc) ** 0.8
    pages = highpass(rng.standard_normal(n(0.06 + 0.02)), 2000.0, 3)[: n(0.06)]
    pages = unit(pages) * np.exp(-tt(0.06) / 0.014) * 0.7
    return place(0.30, (0.0, cover), (0.185, pages))


def book_page(rng):
    sec = rng.uniform(0.090, 0.120)
    x = highpass(rng.standard_normal(n(sec + 0.02)), 1500.0, 3)[: n(sec)]
    t = tt(sec)[: len(x)] / sec
    env = np.minimum(1.0, t / 0.08) * (1.0 - t) ** 1.4
    for _ in range(2):                                      # two flutters where the page buckles
        c, w, d = rng.uniform(0.25, 0.8), rng.uniform(0.04, 0.09), rng.uniform(0.35, 0.6)
        env *= 1.0 - d * np.exp(-0.5 * ((t - c) / w) ** 2)
    return place(sec + 0.03, (0.0, unit(x) * env))


def book_reclaim(rng):
    stamp = thump(70.0, 0.040, 0.10, drop=0.0)
    c = click(rng, ms=3.0)
    return place(0.33, (0.0, stamp), (0.0, 0.6 * c), (0.060, bell(rng, B5, 0.18, 0.26, gain=0.5, click_gain=0.2)))


def trophy_place(rng):
    return place(0.26,
                 (0.0, 0.8 * click(rng, ms=5.0)),
                 (0.0, 0.9 * thump(120.0, 0.060, 0.14)),
                 (0.004, bell(rng, D5, 0.06, 0.20, gain=0.7, click_gain=0.0)))


def pedestal_set(rng):
    return place(0.20,
                 (0.000, 1.0 * click(rng, ms=3.0, lo=1200.0)),
                 (0.045, 0.55 * click(rng, ms=3.0, lo=1200.0)),
                 (0.000, 0.8 * thump(200.0, 0.030, 0.08)))


def cabinet_latch(rng):
    t = tt(0.20)
    f1 = 3100.0 * (1.0 + rng.uniform(-0.03, 0.03))
    f2 = 4700.0 * (1.0 + rng.uniform(-0.03, 0.03))
    ring = 0.3 * np.exp(-6.9078 * t / 0.080) * (np.sin(2 * np.pi * f1 * t) + 0.8 * np.sin(2 * np.pi * f2 * t))
    ring *= np.minimum(1.0, t / 0.001)
    tick = highpass(rng.standard_normal(n(0.015 + 0.02)), 2500.0, 2)[: n(0.015)]
    tick = 0.22 * unit(tick) * np.exp(-tt(0.015) / 0.004)
    return place(0.30, (0.0, 0.9 * click(rng, ms=2.0, lo=2000.0, hi=12000.0)), (0.0, ring),
                 (0.070 + rng.uniform(-0.006, 0.006), tick))


# event, class, [(file, seed)], recipe, max seconds, subtitle text
SOUNDS = [
    ("award.bronze", "award", [("award/bronze", 6101)], award_bronze, 0.45, "Bronze trophy earned"),
    ("award.silver", "award", [("award/silver", 6102)], award_silver, 0.65, "Silver trophy earned"),
    ("award.gold", "award", [("award/gold", 6103)], award_gold, 0.95, "Gold trophy earned"),
    ("award.nero", "award", [("award/nero", 6104)], award_nero, 1.60, "Nero trophy earned"),
    ("award.collection", "award", [("award/collection", 6105)], award_collection, 1.30, "Collection completed"),
    ("book.open", "book", [("book/open", 6111)], book_open, 0.35, "Trophy Book opens"),
    ("book.page", "book", [("book/page1", 6112), ("book/page2", 6113), ("book/page3", 6114)], book_page, 0.20,
     "Page turns"),
    ("book.reclaim", "book", [("book/reclaim", 6115)], book_reclaim, 0.35, "Trophy re-claimed"),
    ("block.trophy.place", "block", [("block/trophy_place", 6121)], trophy_place, 0.30, "Trophy placed"),
    ("block.pedestal.set", "block", [("block/pedestal_set", 6122)], pedestal_set, 0.25, "Pedestal loaded"),
    ("block.cabinet.latch", "block", [("block/cabinet_latch1", 6123), ("block/cabinet_latch2", 6124)],
     cabinet_latch, 0.40, "Cabinet latches"),
]


# ---- measuring and mastering ---------------------------------------------------------------------
def db(x):
    return 20.0 * math.log10(max(float(x), 1e-12))


def k_weight(x):
    """BS.1770 K-weighting (high shelf + high-pass) applied in the frequency domain at this sample rate."""
    def biquad(b, a, w):
        z = np.exp(-1j * w)
        return (b[0] + b[1] * z + b[2] * z * z) / (a[0] + a[1] * z + a[2] * z * z)

    def shelf(fc=1681.9744509555319, gain_db=3.99984385397, q=0.7071752369554193):
        k = math.tan(math.pi * fc / SR)
        vh, vb = 10 ** (gain_db / 20.0), (10 ** (gain_db / 20.0)) ** 0.499666774155
        a0 = 1.0 + k / q + k * k
        return ((vh + vb * k / q + k * k) / a0, 2.0 * (k * k - vh) / a0, (vh - vb * k / q + k * k) / a0), \
               (1.0, 2.0 * (k * k - 1.0) / a0, (1.0 - k / q + k * k) / a0)

    def hp(fc=38.13547087613982, q=0.5003270373253953):
        k = math.tan(math.pi * fc / SR)
        a0 = 1.0 + k / q + k * k
        return (1.0, -2.0, 1.0), (1.0, 2.0 * (k * k - 1.0) / a0, (1.0 - k / q + k * k) / a0)

    pad = np.concatenate([x, np.zeros(SR // 2)])
    w = 2 * np.pi * np.fft.rfftfreq(len(pad), 1.0)
    h = biquad(*shelf(), w) * biquad(*hp(), w)
    return np.fft.irfft(np.fft.rfft(pad) * h, len(pad))


def lufs(x):
    """Ungated K-weighted loudness of the whole file."""
    y = k_weight(x)
    return -0.691 + 10.0 * math.log10(max(float(np.sum(y * y)) / len(x), 1e-12))


def measure(x):
    tail = x[-max(1, n(0.002)):]
    return {"seconds": len(x) / SR, "peak_db": db(np.max(np.abs(x))), "lufs": lufs(x),
            "tail_db": db(np.max(np.abs(tail)))}


def master(x, target_lufs, max_sec):
    """Trim the dead tail, fade, then bring the file to its loudness under the peak ceiling."""
    x = x - np.mean(x)
    thresh = np.max(np.abs(x)) * 10 ** (-54.0 / 20.0)
    last = int(np.max(np.nonzero(np.abs(x) > thresh)[0]))
    x = x[: min(len(x), last + n(0.030), n(max_sec) - 2)]
    fi, fo = n(0.003), max(n(0.020), min(n(0.060), len(x) // 5))
    x[:fi] *= 0.5 * (1.0 - np.cos(np.pi * np.arange(fi) / fi))
    x[-fo:] *= 0.5 * (1.0 + np.cos(np.pi * np.arange(fo) / fo))
    x[-1] = 0.0
    ceiling = 10 ** (PEAK_AIM_DB / 20.0)
    for _ in range(12):
        x = x * 10 ** ((target_lufs - lufs(x)) / 20.0)
        if np.max(np.abs(x)) <= ceiling:
            break
        # a transient stands above the ceiling at this loudness: round it off (soft knee from 60 % of the
        # ceiling) and measure again
        knee = 0.6 * ceiling
        over = np.abs(x) > knee
        x = np.where(over, np.sign(x) * (knee + (ceiling - knee) * np.tanh((np.abs(x) - knee) / (ceiling - knee))), x)
    return x


def render_all():
    out = {}
    for event, cls, files, recipe, max_sec, _ in SOUNDS:
        for name, seed in files:
            out[name] = master(recipe(np.random.default_rng(seed)), CLASSES[cls][0], max_sec)
    return out


def write_wav(path, sig):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(sig, -1, 1) * 32767.0).round().astype("<i2").tobytes())


def read_ogg(ffmpeg, path):
    raw = subprocess.run([ffmpeg, "-v", "error", "-i", path, "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, "<i2").astype(float) / 32768.0


def ogg_path(name):
    return os.path.join(ASSETS, "sounds", *name.split("/")) + ".ogg"


def encode(ffmpeg, wav, ogg):
    os.makedirs(os.path.dirname(ogg), exist_ok=True)
    subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis",
                    "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", ogg],
                   check=True)


def sounds_json():
    doc = {}
    for event, cls, files, _, _, _ in sorted(SOUNDS, key=lambda s: s[0]):
        doc[event] = {
            "subtitle": "subtitles.%s.%s" % (MOD, event),
            "sounds": [{"name": "%s:%s" % (MOD, name), "volume": CLASSES[cls][1], "stream": False}
                       for name, _ in files],
        }
    return json.dumps(doc, indent=2) + "\n"


def lang():
    return {"subtitles.%s.%s" % (MOD, event): text for event, _, _, _, _, text in SOUNDS}


def measurements(ffmpeg):
    """Decode every shipped .ogg and measure it; returns rows and the list of failures."""
    rows, bad = [], []
    for event, cls, files, _, max_sec, text in SOUNDS:
        target = CLASSES[cls][0]
        for name, seed in files:
            path = ogg_path(name)
            if not os.path.isfile(path):
                bad.append("missing " + name + ".ogg")
                continue
            m = measure(read_ogg(ffmpeg, path))
            rows.append((event, name, seed, max_sec, target, m))
            if m["seconds"] > max_sec + 1e-6:
                bad.append("%s is %.3f s, over %.2f s" % (name, m["seconds"], max_sec))
            if m["peak_db"] > PEAK_MAX_DB:
                bad.append("%s peaks at %.2f dBFS" % (name, m["peak_db"]))
            if abs(m["lufs"] - target) > LUFS_TOL:
                bad.append("%s is %.1f LUFS, target %.0f" % (name, m["lufs"], target))
            if m["tail_db"] > TAIL_DB:
                bad.append("%s ends at %.1f dBFS" % (name, m["tail_db"]))
    jpath = os.path.join(ASSETS, "sounds.json")
    if not os.path.isfile(jpath) or open(jpath, encoding="utf-8").read() != sounds_json():
        bad.append("sounds.json is missing or out of date")
    return rows, bad


def report(rows):
    lines = ["| Event | File | Seed | Length (s) | Max (s) | Peak (dBFS) | Loudness (LUFS) | Target | Tail (dBFS) | Subtitle key |",
             "|---|---|---|---|---|---|---|---|---|---|"]
    for event, name, seed, max_sec, target, m in rows:
        lines.append("| `%s:%s` | `sounds/%s.ogg` | %d | %.3f | %.2f | %.2f | %.1f | %.0f | %.0f | `subtitles.%s.%s` |" % (
            MOD, event, name, seed, m["seconds"], max_sec, m["peak_db"], m["lufs"], target,
            max(m["tail_db"], -120.0), MOD, event))
    return "\n".join(lines) + "\n"


def main():
    args = sys.argv[1:]
    ffmpeg = shutil.which("ffmpeg")
    if "--lang" in args:
        print(json.dumps(lang(), indent=2))
        return 0
    if "--check" in args or "--report" in args:
        if not ffmpeg:
            print("ffmpeg not found: cannot decode the .ogg files to measure them")
            return 2
        rows, bad = measurements(ffmpeg)
        if "--report" in args:
            with open(args[args.index("--report") + 1], "w", encoding="utf-8", newline="\n") as f:
                f.write(report(rows))
        for event, name, seed, max_sec, target, m in rows:
            print("%-22s %6.3f s  peak %6.2f dBFS  %6.1f LUFS  tail %6.0f dBFS" % (
                name, m["seconds"], m["peak_db"], m["lufs"], max(m["tail_db"], -120.0)))
        for b in bad:
            print("FAIL", b)
        print("%d files, %d events, %s" % (len(rows), len(SOUNDS), "FAILED" if bad else "ok"))
        return 1 if bad else 0
    if not ffmpeg:
        print("ffmpeg not found; nothing written (the encode needs ffmpeg with libvorbis)")
        return 2
    if "--placeholder" in args:
        sigs = {name: np.zeros(n(0.05)) for _, _, files, _, _, _ in SOUNDS for name, _ in files}
    else:
        sigs = render_all()
    keep = args[args.index("--wav") + 1] if "--wav" in args else None
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sigs.items():
            wav = os.path.join(keep or tmp, name.replace("/", "_") + ".wav")
            write_wav(wav, sig)
            encode(ffmpeg, wav, ogg_path(name))
    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8", newline="\n") as f:
        f.write(sounds_json())
    print("wrote %d .ogg files and sounds.json (%d events)" % (len(sigs), len(SOUNDS)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
