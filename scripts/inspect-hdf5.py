#!/usr/bin/env python3

"""Print all datasets in an HDF5 file."""

import argparse
import json

import numpy
import h5py


def to_str(seq):
    return "[" + ",".join(map(to_str, seq)) + "]" if isinstance(seq, list) else str(seq)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("hdf5", type=str, help="Path to HDF5 file")
    args = ap.parse_args()

    datasets = {}

    def visit(name, obj):
        attrs = {}
        for k, v in obj.attrs.items():
            if isinstance(v, bytes):
                v = v.decode("utf-8")
            elif isinstance(v, numpy.ndarray):
                v = to_str(v.tolist())
            attrs[k] = v

        if isinstance(obj, h5py.Dataset):
            name = "/" + name.removeprefix("/")
            datasets[name] = {
                "dtype": obj.dtype.name,
                "shape": ",".join(map(str, obj.shape)),
            }
            if attrs:
                datasets[name]["attrs"] = {k: attrs[k] for k in sorted(attrs)}


    with h5py.File(args.hdf5, "r") as f:
        f.visititems(visit)

    datasets = {k: datasets[k] for k in sorted(datasets)}
    print(json.dumps(datasets, indent=2))


if __name__ == "__main__":
    main()
