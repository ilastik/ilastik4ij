package org.ilastik.ilastik4ij.util;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Hdf5Utils {

    private static final long DEFAULT_BLOCK_SIZE = 128;

    public static int[] blockSize(long[] datasetDims) {
        // expect tzyxc axis
        int[] result = new int[datasetDims.length];

        for (int i = 0; i < datasetDims.length; i++) {
            result[i] = (int) Math.min(DEFAULT_BLOCK_SIZE, datasetDims[i]);
        }
        result[0] = 1;
        result[1] = 1;
        result[4] = 1;

        return result;
    }

    public static long[] getXYSliceDims(long[] datasetDims) {
        // expect tzyxc axis
        long[] result = datasetDims.clone();
        result[0] = 1;
        result[1] = 1;
        result[4] = 1;

        return result;
    }

    public static String dropdownName(String path, HDF5DataSetInformation dsInfo) {
        String shape = Arrays.stream(dsInfo.getDimensions())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        String dtype = getTypeInfo(dsInfo);
        return String.format("%s: (%s) %s", path, shape, dtype);
    }

    public static String parseDataset(String dropdownName) {
        return dropdownName.split(":")[0].trim();
    }

    public static String getTypeInfo(HDF5DataSetInformation dsInfo) {
        HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
        int bitdepth = 8 * dsType.getElementSize();
        String type = "";

        if (!dsType.isSigned()) {
            type += "u";
        }

        switch (dsType.getDataClass()) {
            case INTEGER:
                type += "int" + bitdepth;
                break;
            case FLOAT:
                type += "float" + bitdepth;
                break;
            default:
                type += dsInfo.toString();
        }
        return type;
    }
}
