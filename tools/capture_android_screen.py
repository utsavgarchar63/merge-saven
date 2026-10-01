"""Capture genuine Android screenshots and inspect UI labels on the isolated QA emulator."""
from pathlib import Path
import argparse, subprocess, xml.etree.ElementTree as ET
parser=argparse.ArgumentParser()
parser.add_argument("output", nargs="?")
parser.add_argument("--inspect", action="store_true")
args=parser.parse_args()
adb=r"C:\Users\GT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
cmd=[adb,"-s","emulator-5554"]
if args.inspect:
    dump=subprocess.run(cmd+["shell","uiautomator","dump","/sdcard/merge-seven-window.xml"],check=True,capture_output=True,text=True)
    if "ERROR" in dump.stdout + dump.stderr: raise SystemExit("UI is changing; retry after the activity settles")
    xml=subprocess.check_output(cmd+["shell","cat","/sdcard/merge-seven-window.xml"])
    for node in ET.fromstring(xml).iter("node"):
        text=node.get("text") or node.get("content-desc")
        if text: print(text, node.get("bounds"))
if args.output:
    output=Path(args.output).resolve()
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_bytes(subprocess.check_output(cmd+["exec-out","screencap","-p"]))
    print(output)
