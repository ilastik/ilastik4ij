package org.ilastik.ilastik4ij.hdf5;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

import java.util.List;

public interface DatasetEntryProvider {
    List<DatasetEntry> findAvailableDatasets(String path);

    class ReadException extends RuntimeException {
        public ReadException(String message, HDF5Exception e) {
            super(message, e);
        }
    }

    class DatasetEntry {
        public final String path;
        public final String axisTags;
        public final String verboseName;
        public final int rank;

        public DatasetEntry(String path, int rank, String axisTags, String verboseName) {
            this.path = path;
            this.rank = rank;
            this.axisTags = axisTags;
            this.verboseName = verboseName;
        }

    }
}
