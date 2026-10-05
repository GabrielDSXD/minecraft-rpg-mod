"""Sintetiza os sons originais das criaturas (OGG Vorbis mono) e gera sounds.json.
Requer numpy e soundfile. Uso: python tools/gen_sounds.py"""
import json
import os

import numpy as np
import soundfile as sf

SR = 22050
ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "alvorecer")
rng = np.random.default_rng(42)


def t(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(x, attack=0.01, release=0.2):
    n = len(x)
    e = np.ones(n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r)
    return x * e


def glide(f0, f1, sec, wave="sine", vib=0.0):
    tt = t(sec)
    f = np.linspace(f0, f1, len(tt)) * (1 + vib * np.sin(2 * np.pi * 7 * tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if wave == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if wave == "square":
        return np.sign(np.sin(ph))
    return np.sin(ph)


def noise(sec):
    return rng.uniform(-1, 1, int(SR * sec))


def lowpass(x, alpha):
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def bandnoise(sec, alpha_lo, alpha_hi):
    n = noise(sec)
    return lowpass(n, alpha_hi) - lowpass(n, alpha_lo)


def decay(x, rate):
    return x * np.exp(-np.arange(len(x)) / SR * rate)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def seq(*parts, gap=0.0):
    g = np.zeros(int(gap * SR))
    out = []
    for p in parts:
        out += [p, g]
    return np.concatenate(out)


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def chime(f, sec=0.4):
    return decay(np.sin(2 * np.pi * f * t(sec)) + 0.4 * np.sin(2 * np.pi * f * 2.76 * t(sec)), 9)


SOUNDS = {
    "goblin/ambient": lambda: seq(*[env(glide(f, f * 1.3, 0.09, "saw", 0.05), 0.005, 0.04) * 0.6 for f in (700, 850, 640)], gap=0.05),
    "goblin/hurt": lambda: env(glide(950, 480, 0.25, "saw", 0.08), 0.005, 0.1),
    "goblin/death": lambda: env(glide(900, 250, 0.7, "saw", 0.1), 0.005, 0.3),
    "goblin/leap": lambda: env(bandnoise(0.35, 0.02, 0.4) * np.linspace(0.3, 1, int(SR * 0.35)), 0.02, 0.15),
    "skeleton_warrior/ambient": lambda: seq(*[decay(bandnoise(0.06, 0.2, 0.9), 60) for _ in range(5)], gap=0.07),
    "skeleton_warrior/hurt": lambda: mix(decay(bandnoise(0.3, 0.1, 0.8), 15), seq(*[decay(bandnoise(0.05, 0.3, 0.9), 70) for _ in range(3)], gap=0.04)),
    "skeleton_warrior/death": lambda: seq(*[decay(bandnoise(0.08, 0.15, 0.9), 40) * (1 - i / 10) for i in range(9)], gap=0.04),
    "skeleton_warrior/charge": lambda: mix(env(glide(140, 110, 0.6, "saw"), 0.05, 0.2) * 0.7, env(bandnoise(0.6, 0.01, 0.2), 0.05, 0.3) * 0.5),
    "skeleton_warrior/block": lambda: decay(mix(*[np.sin(2 * np.pi * f * t(0.5)) * a for f, a in ((820, 1), (1260, 0.6), (2110, 0.4), (3400, 0.2))]), 8),
    "arcane_wolf/ambient": lambda: mix(env(lowpass(noise(0.8), 0.05) * (1 + 0.6 * np.sin(2 * np.pi * 23 * t(0.8))), 0.1, 0.3),
                                       env(glide(1800, 2200, 0.8) * 0.08, 0.2, 0.3)),
    "arcane_wolf/hurt": lambda: env(glide(1100, 650, 0.25, "square") * 0.4, 0.005, 0.1),
    "arcane_wolf/death": lambda: env(np.concatenate([glide(400, 700, 0.5), glide(700, 300, 0.8)]) * 0.8, 0.05, 0.4),
    "arcane_wolf/blink": lambda: mix(env(glide(600, 2400, 0.35), 0.01, 0.15), chime(1760, 0.4) * 0.5),
    "elemental/ambient": lambda: env(bandnoise(0.9, 0.03, 0.3) * (0.6 + 0.4 * np.sin(2 * np.pi * 3 * t(0.9))), 0.15, 0.3),
    "elemental/hurt": lambda: env(bandnoise(0.3, 0.05, 0.6), 0.005, 0.2),
    "elemental/death": lambda: env(bandnoise(1.2, 0.01, 0.3) * np.linspace(1, 0.1, int(SR * 1.2)), 0.02, 0.6),
    "elemental/shoot": lambda: env(bandnoise(0.35, 0.05, 0.5) * np.linspace(1, 0.2, int(SR * 0.35)), 0.005, 0.1),
    "elemental/pulse": lambda: mix(decay(np.sin(2 * np.pi * 55 * t(0.8)), 5), decay(bandnoise(0.8, 0.005, 0.2), 6) * 0.8),
    "stone_sentinel/ambient": lambda: env(lowpass(noise(1.0), 0.02) * 3, 0.2, 0.4),
    "stone_sentinel/hurt": lambda: mix(decay(bandnoise(0.4, 0.05, 0.5), 10), decay(np.sin(2 * np.pi * 90 * t(0.4)), 8)),
    "stone_sentinel/death": lambda: seq(*[decay(lowpass(noise(0.25), 0.05) * 3, 8) for _ in range(5)], gap=0.03),
    "stone_sentinel/slam": lambda: mix(decay(np.sin(2 * np.pi * np.linspace(70, 35, int(SR * 0.9)) * t(0.9)), 4),
                                       decay(lowpass(noise(0.9), 0.08) * 3, 5)),
    "fairy/ambient": lambda: seq(*[chime(f, 0.18) for f in (2093, 2637, 3136)], gap=0.04),
    "fairy/hurt": lambda: mix(chime(2200, 0.3), chime(2330, 0.3)),
    "fairy/death": lambda: seq(*[chime(f, 0.2) for f in (3136, 2637, 2093, 1568)], gap=0.03),
    "wyrmling/ambient": lambda: env(glide(320, 420, 0.5, "saw", 0.06) * 0.6 + bandnoise(0.5, 0.05, 0.4) * 0.3, 0.02, 0.2),
    "wyrmling/hurt": lambda: env(glide(900, 600, 0.3, "saw", 0.1), 0.005, 0.1),
    "wyrmling/death": lambda: env(glide(500, 120, 1.0, "saw", 0.08) + bandnoise(1.0, 0.02, 0.3) * 0.4, 0.02, 0.5),
    "wyrmling/breath": lambda: env(bandnoise(1.5, 0.02, 0.35) * (0.8 + 0.2 * np.sin(2 * np.pi * 9 * t(1.5))), 0.1, 0.5),
    "npc/greet": lambda: seq(env(glide(200, 230, 0.15) + 0.3 * glide(400, 460, 0.15), 0.02, 0.06),
                             env(glide(240, 210, 0.2) + 0.3 * glide(480, 420, 0.2), 0.02, 0.1), gap=0.05),
    # Etapa 7: Guardião do Primeiro Alvorecer
    "dawn_guardian/ambient": lambda: mix(env(glide(88, 92, 1.4, "saw", 0.02) * 0.35, 0.3, 0.5), chime(660, 1.2) * 0.3, chime(990, 1.0) * 0.15),
    "dawn_guardian/hurt": lambda: mix(decay(mix(*[np.sin(2 * np.pi * f * t(0.6)) * a for f, a in ((420, 1), (690, 0.6), (1130, 0.35))]), 7),
                                      decay(bandnoise(0.6, 0.05, 0.5), 12) * 0.5),
    "dawn_guardian/death": lambda: mix(env(glide(220, 40, 3.0, "saw", 0.05) * 0.6, 0.05, 1.2),
                                       seq(*[chime(f, 0.5) for f in (1320, 990, 880, 660, 495, 440)], gap=0.08) * 0.5),
    "dawn_guardian/roar": lambda: mix(env(glide(110, 68, 1.6, "saw", 0.08) * 0.8, 0.1, 0.6), env(bandnoise(1.6, 0.01, 0.2), 0.1, 0.6) * 0.6),
    "dawn_guardian/slam": lambda: mix(decay(np.sin(2 * np.pi * np.linspace(60, 28, int(SR * 1.2)) * t(1.2)), 3),
                                      decay(bandnoise(1.2, 0.005, 0.25), 4) * 1.2),
    "dawn_guardian/beam_charge": lambda: env(glide(300, 1400, 1.4, "sine", 0.03) * 0.7 + bandnoise(1.4, 0.2, 0.9) * 0.15, 0.05, 0.1),
    "dawn_guardian/beam": lambda: mix(decay(bandnoise(0.9, 0.1, 0.95), 4), decay(np.sin(2 * np.pi * 1500 * t(0.9)), 5) * 0.5),
    "dawn_guardian/ring": lambda: mix(decay(np.sin(2 * np.pi * 180 * t(1.0)), 4), env(bandnoise(1.0, 0.02, 0.3), 0.2, 0.5) * 0.5),
    "dawn_guardian/summon": lambda: seq(*[chime(f, 0.35) for f in (330, 440, 554, 660)], gap=0.02),
    "dawn_guardian/discharge_charge": lambda: env(glide(60, 240, 2.0, "saw", 0.0) * (0.6 + 0.4 * np.sin(2 * np.pi * 12 * t(2.0))) * 0.6, 0.2, 0.2),
    "dawn_guardian/discharge": lambda: mix(decay(lowpass(noise(2.5), 0.08) * 3, 1.5), decay(np.sin(2 * np.pi * np.linspace(120, 30, int(SR * 2.5)) * t(2.5)), 1.5),
                                           seq(*[chime(f, 0.6) for f in (1760, 1320, 880)], gap=0.1) * 0.4),
}


def main():
    sounds_json = {}
    for path, gen in SOUNDS.items():
        out = os.path.join(ROOT, "sounds", path + ".ogg")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        sf.write(out, norm(gen()).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        event = path.replace("/", ".")
        sounds_json[event] = {"subtitle": f"subtitles.alvorecer.{event}", "sounds": [f"alvorecer:{path}"]}
    with open(os.path.join(ROOT, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(sounds_json, f, indent=2)
    print(len(SOUNDS), "sons gerados")


if __name__ == "__main__":
    main()
