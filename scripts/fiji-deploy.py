#!/usr/bin/env python3

"""Build the project and copy plugin to Fiji."""

import argparse
import shutil
import subprocess
from pathlib import Path


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("fiji", type=Path, help="Path to Fiji directory")
    args = ap.parse_args()

    for subdir in ("plugins", "jars"):
        for old_jar in (args.fiji / subdir).glob("ilastik4ij*.jar"):
            print(f"removing old jar {old_jar}")
            old_jar.unlink()

    print(f"building project")
    subprocess.run(["mvn", "package", "-DskipTests=true"], check=True)

    jars = list(Path("target").glob("ilastik4ij-*-SNAPSHOT.jar"))
    if len(jars) != 1:
        raise RuntimeError(f"Expected exactly one jar, found {jars}")
    jar = jars[0]
    plugins_dir = args.fiji / "plugins"
    print(f"copying {jar} to {plugins_dir}")
    shutil.copy(jar, plugins_dir)


if __name__ == "__main__":
    main()
