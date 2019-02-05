package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author chaubold
 */
public class Hdf5DataSetReader {
    private static final Map<String, NativeType<?>> H5_TO_IMGLIB2_TYPE;

    static {
        Map<String, NativeType<?>> map = new HashMap<>();
        map.put("float32", new FloatType());
        map.put("uint8", new UnsignedByteType());
        map.put("uint16", new UnsignedShortType());
        map.put("uint32", new UnsignedIntType());
        map.put("uint64", new UnsignedLongType());
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
        try {
            Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
            log.info(String.format("Found dataset '%s' of type '%s'", dataset, dsConfig.typeInfo));

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
            int[] extents = dsConfig.getXYSliceExtent();

            int totalCheckpoints = dsConfig.numFrames * dsConfig.numChannels * dsConfig.dimZ;
            int checkpoint = 0;
            statusService.showStatus(checkpoint, totalCheckpoints, "Importing HDF5...");

            for (int frame = 0; frame < dsConfig.numFrames; ++frame) {
                rai.setPosition(frame, AXES.indexOf(Axes.TIME));
                for (int lev = 0; lev < dsConfig.dimZ; ++lev) {
                    rai.setPosition(lev, AXES.indexOf(Axes.Z));
                    for (int c = 0; c < dsConfig.numChannels; ++c) {
                        rai.setPosition(c, AXES.indexOf(Axes.CHANNEL));

                        long[] offset = dsConfig.getSliceOffset(frame, lev, c);

                        Object[] flatArray = getFlatArray(reader, this.dataset, dsConfig.typeInfo, extents, offset);

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            rai.setPosition(x, AXES.indexOf(Axes.X));
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                rai.setPosition(y, AXES.indexOf(Axes.Y));
                                int destIndex = y * dsConfig.dimX + x;
                                if (dsConfig.axisIndices.get('x') < dsConfig.axisIndices.get('y')) {
                                    destIndex = x * dsConfig.dimY + y;
                                }


                                if (type instanceof FloatType) {
                                    FloatType raiType = (FloatType) rai.get();
                                    double d = (double) flatArray[destIndex];
                                    float f = (float) d;
                                    raiType.set(f);
                                } else if (type instanceof UnsignedByteType) {
                                    UnsignedByteType raiType = (UnsignedByteType) rai.get();
                                    raiType.set((int) flatArray[destIndex]);
                                } else if (type instanceof UnsignedShortType) {
                                    UnsignedShortType raiType = (UnsignedShortType) rai.get();
                                    raiType.set((int) flatArray[destIndex]);
                                } else if (type instanceof UnsignedIntType) {
                                    UnsignedIntType raiType = (UnsignedIntType) rai.get();
                                    raiType.set((int) flatArray[destIndex]);
                                } else if (type instanceof UnsignedLongType) {
                                    UnsignedLongType raiType = (UnsignedLongType) rai.get();
                                    raiType.set((long) flatArray[destIndex]);
                                }

                                //statusService.showProgress(++checkpoint, totalCheckpoints);
                            }
                        }
                    }
                }
            }
            statusService.showStatus("Finished Importing HDF5.");
            return result;
        } finally {
            reader.close();
        }
    }

    private Object[] getFlatArray(IHDF5Reader reader, String dataset, String type, int[] extents, long[] offset) {
        switch (type) {
            case "float32":
                float[] floatArray = reader.float32().readMDArrayBlockWithOffset(dataset, extents, offset).getAsFlatArray();
                return IntStream.range(0, floatArray.length).mapToDouble(i -> floatArray[i]).boxed().toArray();
            case "uint8":
                byte[] byteArray = reader.uint8().readMDArrayBlockWithOffset(dataset, extents, offset).getAsFlatArray();
                return IntStream.range(0, byteArray.length).map(i -> byteArray[i]).boxed().toArray();
            case "uint16":
                short[] shortArray = reader.uint16().readMDArrayBlockWithOffset(dataset, extents, offset).getAsFlatArray();
                return IntStream.range(0, shortArray.length).map(i -> shortArray[i]).boxed().toArray();
            case "uint32":
                int[] intArray = reader.uint32().readMDArrayBlockWithOffset(dataset, extents, offset).getAsFlatArray();
                return IntStream.range(0, intArray.length).map(i -> intArray[i]).boxed().toArray();
            case "uint64":
                long[] longArray = reader.uint64().readMDArrayBlockWithOffset(dataset, extents, offset).getAsFlatArray();
                return Arrays.stream(longArray).boxed().toArray();
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }
}
