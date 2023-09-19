#!/usr/bin/env python3

"""Generate HDF5 dataset of the specified size filled with zeros."""

import argparse
import json

import h5py
import numpy


def human_size(shape: tuple[int, ...], itemsize: int) -> str:
    n = int(numpy.prod(shape)) * itemsize
    suffixes = "bytes", "KiB", "MiB", "GiB", "TiB", "PiB"
    suffix = suffixes[0]
    for suffix in suffixes:
        if n < 1024:
            break
        n /= 1024
    return f"{n:.2f} {suffix}"


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "-p",
        "--path",
        help="path to HDF5 file (default: test-dataset.h5)",
        default="test-dataset.h5",
    )
    ap.add_argument(
        "-d", "--dataset", help="dataset name (default: /data)", default="/data"
    )
    ap.add_argument("-t", "--dtype", help="data type (default: uint8)", default="uint8")
    ap.add_argument(
        "-s",
        "--shape",
        help="comma-separated shape (default: 10,64,3,64,64)",
        default="10,64,3,64,64",
    )
    ap.add_argument(
        "-c",
        "--chunk",
        help="comma-separated chunk shape (default: no chunking)",
    )
    args = ap.parse_args()

    path = args.path
    dataset = "/" + args.dataset.removeprefix("/")
    dtype = numpy.dtype(args.dtype)
    shape = tuple(map(int, args.shape))
    chunk = tuple(map(int, args.chunk)) if args.chunk is not None else None

    report = {
        "path": path,
        "dataset": dataset,
        "dtype": str(dtype),
        "shape": ",".join(map(str, shape)),
        "shape_size": human_size(shape, dtype.itemsize),
    }
    if chunk is not None:
        report["chunk"] = ",".join(map(str, chunk))
        report["chunk_size"] = human_size(chunk, dtype.itemsize)
    print(json.dumps(report, indent=2))

    with h5py.File(path, "a") as f:
        if dataset in f:
            del f[dataset]
        ds = f.create_dataset(dataset, shape=shape, dtype=dtype, chunks=chunk)
        ds.write_direct(numpy.zeros(shape, dtype=dtype))


if __name__ == "__main__":
    main()
