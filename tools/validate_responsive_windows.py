"""Run real UI checks at phone/tablet window sizes on an isolated Merge Seven emulator."""
import argparse
from pathlib import Path
import re
import subprocess


WINDOWS = {
    "phone-portrait": (720, 1280, 320, 1.0),
    # Stay within the QA display's physical resize limit; density supplies the target dp size.
    "phone-landscape": (960, 540, 240, 1.0),
    "tablet-portrait": (800, 1280, 160, 1.0),
    "tablet-landscape": (960, 600, 120, 1.0),
    "phone-large-text": (720, 1280, 320, 1.5),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", default="emulator-5556")
    parser.add_argument("--expected-avd", default="MergeSeven_AdsOct14")
    parser.add_argument("--profiles", nargs="+", choices=WINDOWS, default=list(WINDOWS))
    parser.add_argument("--collect-only", action="store_true", help="Collect existing captures without rerunning UI checks")
    parser.add_argument("--root-for-captures", action="store_true", help="Temporarily root adbd on this isolated AOSP emulator to read app captures")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]

    def adb(*command, timeout=30):
        return subprocess.run([args.adb, "-s", args.serial, *command], check=True,
                              capture_output=True, text=True, timeout=timeout).stdout

    def verify_device():
        if args.expected_avd not in adb("emu", "avd", "name").splitlines():
            raise RuntimeError("Refusing to resize a different emulator")

    verify_device()
    size_match = re.search(r"Override size: (\d+x\d+)", adb("shell", "wm", "size"))
    density_match = re.search(r"Override density: (\d+)", adb("shell", "wm", "density"))
    original_size = size_match.group(1) if size_match else "reset"
    original_density = density_match.group(1) if density_match else "reset"
    settings = {key: adb("shell", "settings", "get", scope, key).strip() for scope, key in (
        ("system", "font_scale"), ("system", "accelerometer_rotation"), ("system", "user_rotation"))}
    screenshots = root / "validation" / "screenshots" / "responsive"
    captures = root / "validation" / "build-temp" / "responsive"
    screenshots.mkdir(parents=True, exist_ok=True)
    captures.mkdir(parents=True, exist_ok=True)
    was_root = adb("shell", "id", "-u").strip() == "0"
    if args.root_for_captures and not was_root:
        adb("root")
        adb("wait-for-device")
        verify_device()
        if adb("shell", "id", "-u").strip() != "0":
            raise RuntimeError("Capture collection requires an AOSP emulator with root-capable adbd")

    def collect(name):
        collected = 0
        for screen in ("home", "rewards", "challenges", "settings", "cosmetics", "levels", "game", "pause"):
            remote = f"/sdcard/Android/data/com.mergeseven.game/files/responsive-qa/{name}-{screen}.png"
            target = screenshots if screen in ("home", "game") else captures
            exists = subprocess.run([args.adb, "-s", args.serial, "shell", "test", "-f", remote],
                                    capture_output=True, timeout=30).returncode == 0
            if exists:
                adb("pull", remote, str(target / f"{name}-{screen}.png"))
                collected += 1
        if not collected:
            print(f"{name}: captures need --root-for-captures on this AOSP emulator", flush=True)

    try:
        if args.collect_only:
            for name in args.profiles:
                collect(name)
            return
        for name in args.profiles:
            verify_device()
            width, height, density, font = WINDOWS[name]
            adb("shell", "am", "force-stop", "com.mergeseven.game")
            adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
            adb("shell", "settings", "put", "system", "user_rotation", "0")
            adb("shell", "wm", "size", f"{width}x{height}")
            adb("shell", "wm", "density", str(density))
            adb("shell", "settings", "put", "system", "font_scale", str(font))
            print(f"Checking {name}: {width * 160 // density} x {height * 160 // density} dp, font {font}", flush=True)
            output = adb("shell", "am", "instrument", "-w", "-r", "-e", "capturePrefix", name,
                         "-e", "expectedPixelWidth", str(width), "-e", "expectedPixelHeight", str(height),
                         "-e", "class", "com.mergeseven.game.ui.ResponsiveWindowTest",
                         "com.mergeseven.game.test/androidx.test.runner.AndroidJUnitRunner", timeout=240)
            (root / f"validation-responsive-{name}.log").write_text(output, encoding="utf-8")
            collect(name)
            if "OK (1 test)" not in output:
                raise RuntimeError(f"{name} failed; see validation-responsive-{name}.log")
            print(f"Passed {name}", flush=True)
    finally:
        verify_device()
        adb("shell", "am", "force-stop", "com.mergeseven.game")
        adb("shell", "wm", "size", original_size)
        adb("shell", "wm", "density", original_density)
        for key, value in settings.items():
            if value == "null":
                adb("shell", "settings", "delete", "system", key)
            else:
                adb("shell", "settings", "put", "system", key, value)
        if args.root_for_captures and not was_root:
            adb("unroot")
            adb("wait-for-device")


if __name__ == "__main__":
    main()
