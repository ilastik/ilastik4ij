package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import org.ilastik.ilastik4ij.util.Hdf5Utils;

import java.util.HashMap;
import java.util.Map;


public class Hdf5DataSetConfig {
    public final int numFrames;
    public final int dimX;
    public final int dimY;
    public final int dimZ;
    public final int numChannels;
    public final String typeInfo;
    public final int bitdepth;
    private final Map<Character, Integer> axisIndices = new HashMap<>();
    private final Map<Character, Integer> axisExtents = new HashMap<>();


    public Hdf5DataSetConfig(HDF5DataSetInformation dsInfo, String axesorder) {
        bitdepth = 8 * dsInfo.getTypeInformation().getElementSize();
        for (int index = 0; index < axesorder.length(); index++) {
            char axis = axesorder.charAt(index);
            axisIndices.put(axis, index);
            axisExtents.put(axis, (int) dsInfo.getDimensions()[index]);
        }

        if (dsInfo.getRank() != axesorder.length())
            throw new IllegalArgumentException("Provided axesorder and dataset have different numbers of axes!");

        if (!(axisIndices.containsKey('x') && axisIndices.containsKey('y')))
            throw new IllegalArgumentException("Provided axesorder must contain x and y axes!");

        numFrames = axisExtents.getOrDefault('t', 1);
        dimX = axisExtents.getOrDefault('x', 1);
        dimY = axisExtents.getOrDefault('y', 1);
        dimZ = axisExtents.getOrDefault('z', 1);
        numChannels = axisExtents.getOrDefault('c', 1);

        // datatype
        typeInfo = Hdf5Utils.getTypeInfo(dsInfo);
    }

    /**
     * @return true if X axis is before Y, or false otherwise
     */
    public boolean isXYOrder() {
        return axisIndices.get('x') < axisIndices.get('y');
    }

    public long[] getSliceOffset(int t, int z, int c) {
        int numAxes = axisIndices.size();
        long[] result = new long[numAxes];
        for (Map.Entry<Character, Integer> entry : axisIndices.entrySet()) {
            char axis = entry.getKey();
            int index = entry.getValue();

            switch (axis) {
                case 't':
                    result[index] = t;
                    break;
                case 'z':
                    result[index] = z;
                    break;
                case 'c':
                    result[index] = c;
                    break;
                default:
                    result[index] = 0;
                    break;
            }
        }

        return result;
    }

    public int[] getXYSliceExtent() {
        int numAxes = axisIndices.size();
        int[] result = new int[numAxes];
        for (Map.Entry<Character, Integer> entry : axisIndices.entrySet()) {
            int axis = entry.getKey();
            int index = entry.getValue();

            switch (axis) {
                case 'x':
                    result[index] = dimX;
                    break;
                case 'y':
                    result[index] = dimY;
                    break;
                default:
                    result[index] = 1;
                    break;
            }
        }

        return result;
    }
}

