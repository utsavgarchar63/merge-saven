"""Inspect/tap UI controls only on the workspace-owned Android QA emulator."""
import argparse, re, subprocess, xml.etree.ElementTree as ET
ADB = r"C:\Users\GT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
DEVICE = [ADB, "-s", "emulator-5554"]
def adb(*args):
    return subprocess.check_output(DEVICE + list(args))
def nodes():
    dump=subprocess.run(DEVICE+["shell", "uiautomator", "dump", "/sdcard/merge-seven-window.xml"],check=True,capture_output=True,text=True)
    if "ERROR" in dump.stdout + dump.stderr: raise SystemExit("UI is changing; retry after the activity settles")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/merge-seven-window.xml")).iter("node"))
parser = argparse.ArgumentParser()
parser.add_argument("--tap")
parser.add_argument("--contains", action="store_true")
parser.add_argument("--last", action="store_true")
parser.add_argument("--dump", action="store_true")
args = parser.parse_args()
assert "MergeSeven_QA30" in adb("emu", "avd", "name").decode(), "Refusing to touch another emulator"
items = nodes()
if args.dump:
    for n in items:
        label = n.get("text") or n.get("content-desc")
        if label: print(label, n.get("bounds"))
if args.tap:
    matches = [n for n in items if any((args.tap in n.get(k, "") if args.contains else args.tap == n.get(k)) for k in ("text", "content-desc"))]
    matches = [n for n in matches if n.get("bounds") != "[0,0][0,0]"]
    if not matches: raise SystemExit("Visible control not found: " + args.tap)
    if len(matches) > 1 and not args.last: raise SystemExit("Ambiguous control: " + args.tap)
    n = matches[-1] if args.last else matches[0]
    x1,y1,x2,y2 = map(int, re.findall(r"-?\d+", n.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    print("Tapped", args.tap, n.get("bounds"))
