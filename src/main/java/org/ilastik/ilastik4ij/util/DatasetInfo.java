package org.ilastik.ilastik4ij.util;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DatasetInfo {
    public static String name(String path, HDF5DataSetInformation dsInfo) {
        String shape = Arrays.stream(dsInfo.getDimensions())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        return String.format("%s: (%s)", path, shape);
    }

    public static String parsePath(String name) {
        return name.split(":")[0].trim();
    }
}
