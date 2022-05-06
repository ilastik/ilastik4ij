package org.ilastik.ilastik4ij.hdf5;

import hdf.hdf5lib.exceptions.HDF5Exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface DatasetEntryProvider {
    List<DatasetEntry> findAvailableDatasets(String path);

    class ReadException extends RuntimeException {
        public ReadException(String message, HDF5Exception e) {
            super(message, e);
        }
    }

    class DatasetEntry {
        public final String path;
        public final long[] shape;
        public final String dtype;
        public final String axisTags;

        public DatasetEntry(String path, long[] shape, String dtype, String axisTags) {
            this.path = path;
            this.shape = shape;
            this.dtype = dtype;
            this.axisTags = axisTags;
        }

        @Override
        public String toString() {
            String shapeString = Arrays.stream(shape)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", "));
            return String.format("%s (%s) %s", path, shapeString, dtype);
        }
    }
}
