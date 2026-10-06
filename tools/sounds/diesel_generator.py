"""Seamless diesel-generator idle, written as assets/stagetents/sounds/generator.ogg.

A twin at a fixed 1500 rpm-style idle: one heavy firing and a lighter one each crank
turn, with exhaust rumble underneath. The loop is an exact number of turns, so it
does not click when Minecraft repeats it.
"""
import os
import subprocess
import wave

import numpy as np

SR = 44100
# 28 Hz crank. Two seconds is 56 turns, an integer, so the loop closes on a firing.
PERIOD = 1575
CYCLES = 56
FADE = int(0.04 * SR)


def ema(x, a):
    y = np.empty_like(x)
    y[0] = x[0]
    keep = 1.0 - a
    for i in range(1, len(x)):
        y[i] = a * x[i] + keep * y[i - 1]
    return y


def pulse(p, decay):
    return np.exp(-p * decay)


def render():
    n = PERIOD * CYCLES
    total = n + FADE
    rng = np.random.default_rng(11)
    t = np.arange(total) / SR
    p = (np.arange(total) % PERIOD) / PERIOD
    # Uneven twin: a strong bang, then a lighter one half a turn later.
    env = pulse(p, 8.5) + 0.62 * pulse((p + 0.5) % 1.0, 11.0)
    # Slow idle wander, one full cycle per loop, so the join stays smooth.
    wander = 0.82 + 0.18 * np.sin(2 * np.pi * t * (SR / n))

    noise = rng.standard_normal(total)
    low = ema(noise, 0.045)
    mid = ema(noise, 0.28) - low
    air = ema(noise, 0.55) - ema(noise, 0.18)

    # Combustion knock and the exhaust puff that follows it.
    knock = mid * env * 1.35
    exhaust = low * (0.45 + 0.55 * env) * 1.25
    # Block resonance, pitched down through each bang.
    body = np.sin(2 * np.pi * (70 + 40 * pulse(p, 6)) * t) * pulse(p, 7.5) * 0.55
    body += 0.35 * np.sin(2 * np.pi * 48 * t) * (0.35 + 0.65 * env)
    # Crank and alternator, steady under the rhythm.
    drone = (0.42 * np.sin(2 * np.pi * 56 * t)
             + 0.22 * np.sin(2 * np.pi * 84 * t)
             + 0.05 * np.sin(2 * np.pi * 120 * t))
    fan = air * 0.07

    sig = (drone + body + knock + exhaust + fan) * wander
    sig = np.tanh(sig * 1.15)
    sig = ema(sig, 1 - np.exp(-2 * np.pi * 2800 / SR))

    ramp = np.linspace(0.0, 1.0, FADE, endpoint=False)
    out = sig[:n].copy()
    out[:FADE] = sig[:FADE] * ramp + sig[n:n + FADE] * (1.0 - ramp)
    peak = np.max(np.abs(out))
    out *= 0.9 / peak
    return out


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    out_dir = os.path.join(root, "src", "main", "resources", "assets", "stagetents", "sounds")
    os.makedirs(out_dir, exist_ok=True)
    wav = os.path.join(out_dir, "generator.wav")
    ogg = os.path.join(out_dir, "generator.ogg")
    samples = np.clip(render(), -1, 1)
    pcm = (samples * 32767).astype(np.int16)
    with wave.open(wav, "w") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(pcm.tobytes())
    subprocess.check_call(["ffmpeg", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", "-ac", "1", ogg])
    os.remove(wav)
    print(f"wrote {ogg} ({os.path.getsize(ogg)} bytes)")


if __name__ == "__main__":
    main()
