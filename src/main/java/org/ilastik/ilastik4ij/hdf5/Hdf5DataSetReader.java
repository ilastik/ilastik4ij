package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chaubold
 */
public class Hdf5DataSetReader {
    private static final Map<String, Type<?>> H5_TO_IMGLIB2_TYPE;

    static {
        Map<String, NativeType<?>> map = new HashMap<>();
        map.put("float32", new FloatType());
        map.put("uint8", new UnsignedByteType());
        map.put("uint16", new UnsignedShortType());
        map.put("uint32", new UnsignedIntType());
        H5_TO_IMGLIB2_TYPE = Collections.unmodifiableMap(map);
    }

    private final String filename;
    private final String dataset;
    private final String axesorder;
    private final LogService log;
    private final StatusService statusService;
    private static final List<AxisType> AXES = Arrays.asList(Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME);

    public Hdf5DataSetReader(String filename, String dataset, String axesorder, LogService log, StatusService statusService) {
        this.filename = filename;
        this.dataset = dataset;
        this.axesorder = axesorder;
        this.log = log;
        this.statusService = statusService;
    }

    public <T extends NativeType<T>> Img<T> read() {
        IHDF5Reader reader = HDF5Factory.openForReading(filename);
        Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
        log.info(String.format("Found dataset '%s' of type '%s'", dataset, dsConfig.typeInfo));

        MDFloatArray rawdata_float = null;
        float[] flat_data_float = null;

        MDByteArray rawdata_byte = null;
        byte[] flat_data_byte = null;

        MDShortArray rawdata_short = null;
        short[] flat_data_short = null;

        MDIntArray rawdata_int = null;
        int[] flat_data_int = null;

        // construct output image
        final long[] dims = {dsConfig.dimX, dsConfig.dimY, dsConfig.numChannels, dsConfig.dimZ, dsConfig.numFrames};

        String strDims = Arrays.stream(dims)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        log.info(String.format("Constructing output image of shape (%s). Axis order: 'XYCZT'", strDims));

        @SuppressWarnings("unchecked") final T type = (T) H5_TO_IMGLIB2_TYPE.get(dsConfig.typeInfo);

        if (type == null) {
            throw new IllegalArgumentException("Unsupported data type: " + dsConfig.typeInfo);
        }
        // used default cell dimensions
        final ImgFactory<T> imgFactory = new CellImgFactory<>();

        final Img<T> result = imgFactory.create(dims, type);

        RandomAccess rai = result.randomAccess();

        for (int frame = 0; frame < dsConfig.numFrames; ++frame) {
            rai.setPosition(frame, AXES.indexOf(Axes.TIME));
            for (int lev = 0; lev < dsConfig.dimZ; ++lev) {
                rai.setPosition(lev, AXES.indexOf(Axes.Z));
                for (int c = 0; c < dsConfig.numChannels; ++c) {
                    rai.setPosition(c, AXES.indexOf(Axes.CHANNEL));

                    int[] extents = dsConfig.getXYSliceExtent();
                    long[] offset = dsConfig.getSliceOffset(frame, lev, c);

                    try {
                        switch (dsConfig.typeInfo) {
                            case "float32":
                                rawdata_float = reader.float32().readMDArrayBlockWithOffset(dataset, extents, offset);
                                flat_data_float = rawdata_float.getAsFlatArray();
                                break;
                            case "uint8":
                                rawdata_byte = reader.uint8().readMDArrayBlockWithOffset(dataset, extents, offset);
                                flat_data_byte = rawdata_byte.getAsFlatArray();
                                break;
                            case "uint16":
                                rawdata_short = reader.uint16().readMDArrayBlockWithOffset(dataset, extents, offset);
                                flat_data_short = rawdata_short.getAsFlatArray();
                                break;
                            case "uint32":
                                rawdata_int = reader.uint32().readMDArrayBlockWithOffset(dataset, extents, offset);
                                flat_data_int = rawdata_int.getAsFlatArray();
                                break;
                            default:
                                throw new IllegalArgumentException("Dataset uses not yet supported datatype " + dsConfig.typeInfo + "!");
                        }

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            rai.setPosition(x, AXES.indexOf(Axes.X));
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                rai.setPosition(y, AXES.indexOf(Axes.Y));
                                int destIndex = y * dsConfig.dimX + x;
                                if (dsConfig.axisIndices.get('x') < dsConfig.axisIndices.get('y')) {
                                    destIndex = x * dsConfig.dimY + y;
                                }

                                switch (dsConfig.typeInfo) {
                                    case "float32": {
                                        float value = flat_data_float[destIndex];
                                        FloatType f = (FloatType) rai.get();
                                        f.set(value);
                                        break;
                                    }
                                    case "uint8": {
                                        byte value = flat_data_byte[destIndex];
                                        UnsignedByteType f = (UnsignedByteType) rai.get();
                                        f.set(value);
                                        break;
                                    }
                                    case "uint16": {
                                        short value = flat_data_short[destIndex];
                                        UnsignedShortType f = (UnsignedShortType) rai.get();
                                        f.set(value);
                                        break;
                                    }
                                    case "uint32": {
                                        int value = flat_data_int[destIndex];
                                        UnsignedIntType f = (UnsignedIntType) rai.get();
                                        f.set(value);
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        String extentsStr = "";
                        for (int x : extents) {
                            extentsStr += String.valueOf(x) + ", ";
                        }

                        String offsetStr = "";
                        for (long x : offset) {
                            offsetStr += String.valueOf(x) + ", ";
                        }
                        log.warn("Could not read data starting at " + offsetStr + " with size " + extentsStr);
                    }
                }
            }
        }


        reader.close();

        return result;
    }
}
