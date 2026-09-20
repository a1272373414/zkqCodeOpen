# -*- coding: utf-8 -*-
import os, subprocess, sys

ADB = r"C:\Users\TANG\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"

def capture(out_path):
    out_path = os.path.abspath(out_path)
    cmd = [ADB, "-s", SERIAL, "exec-out", "screencap", "-p"]
    with open(out_path, "wb") as fp:
        subprocess.run(cmd, stdout=fp, check=True)
    return out_path

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "_tmp_cap.png"
    print(capture(out))
