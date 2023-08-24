#!/usr/bin/env python

"""Generate HDF5 dataset of the specified size filled with zeros."""

import argparse
import json

import h5py
import numpy


def parse_shape(s: str) -> tuple[int, ...]:
    return tuple(map(int, reversed(s.split(","))))


def show_shape(shape: tuple[int, ...]) -> str:
    return ",".join(map(str, reversed(shape)))


def human_size(shape: tuple[int, ...], itemsize: int) -> str:
    n = numpy.prod(shape) * itemsize
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
        help="comma-separated column-major shape (default: 64,64,3,64,10)",
        default="64,64,3,64,10",
    )
    ap.add_argument(
        "-c",
        "--chunk",
        help="comma-separated column-major chunk shape (default: no chunking)",
    )
    args = ap.parse_args()

    path = args.path
    dataset = "/" + args.dataset.removeprefix("/")
    dtype = numpy.dtype(args.dtype)
    shape = parse_shape(args.shape)
    chunk = parse_shape(args.chunk) if args.chunk is not None else None

    report = {
        "path": path,
        "dataset": dataset,
        "dtype": str(dtype),
        "shape": show_shape(shape),
        "shape_size": human_size(shape, dtype.itemsize),
    }
    if chunk is not None:
        report["chunk"] = show_shape(chunk)
        report["chunk_size"] = human_size(chunk, dtype.itemsize)
    print(json.dumps(report, indent=2))

    with h5py.File(path, "w") as f:
        ds = f.create_dataset(dataset, shape=shape, dtype=dtype, chunks=chunk)
        ds.write_direct(numpy.zeros(shape, dtype=dtype))


if __name__ == "__main__":
    main()
