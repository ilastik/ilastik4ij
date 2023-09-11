#!/usr/bin/env python3

"""Build the project and copy plugin to Fiji."""

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("fiji", type=Path, help="Path to Fiji directory")
    args = ap.parse_args()

    if not shutil.which("mvn"):
        sys.exit("error: maven not found in system path")

    print("cleaning project")
    subprocess.run(["mvn", "clean"], check=True)

    print("building project")
    subprocess.run(["mvn", "package", "-DskipTests=true"], check=True)

    jars = list(Path("target").glob("ilastik4ij-*-SNAPSHOT.jar"))
    if len(jars) != 1:
        sys.exit(f"error: expected exactly one jar, found {jars}")
    jar = jars[0]

    for subdir in ("plugins", "jars"):
        for old_jar in (args.fiji / subdir).glob("ilastik4ij*.jar"):
            print(f"removing {old_jar}")
            old_jar.unlink()

    plugins_dir = args.fiji / "plugins"
    print(f"copying {jar} to {plugins_dir}")
    shutil.copy(jar, plugins_dir)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
