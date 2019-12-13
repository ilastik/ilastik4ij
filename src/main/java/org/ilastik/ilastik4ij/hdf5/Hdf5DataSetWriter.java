package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.base.mdarray.*;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccess;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.ilastik.ilastik4ij.util.Hdf5Utils;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Hdf5DataSetWriter<T extends Type<T>> {
    private static final int ARGB_CHANNEL_NUM = 4;
    private final ImgPlus<T> image;
    private final int numFrames;
    private final int numChannels;
    private final int dimZ;
    private final int dimY;
    private final int dimX;
    private final LogService logService;
    private final Optional<StatusService> statusService;
    private final String filename;
    private final String dataset;
    private final int compressionLevel;

    public Hdf5DataSetWriter(ImgPlus<T> image, String filename, String dataset, int compressionLevel, LogService logService, StatusService statusService) {
        this.image = image;
        this.numFrames = getDimension(image, Axes.TIME);
        this.numChannels = getDimension(image, Axes.CHANNEL);
        this.dimZ = getDimension(image, Axes.Z);
        this.dimY = getDimension(image, Axes.Y);
        this.dimX = getDimension(image, Axes.X);
        this.filename = filename;
        this.dataset = dataset;
        this.compressionLevel = compressionLevel;
        this.logService = logService;
        this.statusService = Optional.ofNullable(statusService);
    }

    @SuppressWarnings("unchecked")
    public void write() {
        final long[] dims = {numFrames, dimZ, dimY, dimX, numChannels};
        String shape = Arrays.stream(dims)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
        logService.info(String.format("Exporting image of shape (%s). Axis order: 'TZYXC'", shape));

        try (IHDF5Writer writer = HDF5Factory.open(filename)) {
            T val = image.firstElement();
            if (val instanceof UnsignedByteType) {
                write(writer, dims, (Class<T>) UnsignedByteType.class);
            } else if (val instanceof UnsignedShortType) {
                write(writer, dims, (Class<T>) UnsignedShortType.class);
            } else if (val instanceof UnsignedIntType) {
                write(writer, dims, (Class<T>) UnsignedIntType.class);
            } else if (val instanceof UnsignedLongType) {
                write(writer, dims, (Class<T>) UnsignedLongType.class);
            } else if (val instanceof FloatType) {
                write(writer, dims, (Class<T>) FloatType.class);
            } else if (val instanceof ARGBType) {
                writeARGB(writer, dims);
            } else {
                throw new IllegalArgumentException("Unsupported Type: " + val.getClass());
            }
        }
    }

    private void write(IHDF5Writer writer, long[] datasetDims, Class<T> pixelClass) {
        logService.info(String.format("Saving as '%s'. Compression level: %d", Hdf5Utils.getDtype(pixelClass), compressionLevel));
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        final AtomicInteger checkpoint = new AtomicInteger(0);
        statusService.ifPresent(status -> status.showStatus(checkpoint.get(), totalCheckpoints, "Exporting HDF5..."));


        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        createMDArray(writer, datasetDims, blockSize, pixelClass);

        RandomAccess<T> rai = image.randomAccess();
        for (int t = 0; t < numFrames; t++) {
            if (image.dimensionIndex(Axes.TIME) >= 0)
                rai.setPosition(t, image.dimensionIndex(Axes.TIME));
            for (int c = 0; c < numChannels; c++) {
                if (image.dimensionIndex(Axes.CHANNEL) >= 0)
                    rai.setPosition(c, image.dimensionIndex(Axes.CHANNEL));
                for (int z = 0; z < dimZ; z++) {
                    if (image.dimensionIndex(Axes.Z) >= 0)
                        rai.setPosition(z, image.dimensionIndex(Axes.Z));

                    // init MD-array
                    Object[] flatArr = new Object[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));
                            int index = y * dimX + x;
                            flatArr[index] = getValue(rai, pixelClass);
                        }
                    }
                    long[] offset = {t, z, 0, 0, c};
                    long[] sliceDims = Hdf5Utils.getXYSliceDims(datasetDims);
                    // save data
                    writeMDArray(writer, flatArr, offset, sliceDims, pixelClass);
                    // update progress bar
                    statusService.ifPresent(status -> status.showProgress(checkpoint.incrementAndGet(), totalCheckpoints));
                }

            }
        }
        statusService.ifPresent(status -> status.showStatus("Finished Exporting HDF5."));
    }

    private void writeMDArray(IHDF5Writer writer, Object[] flatArr, long[] offset, long[] sliceDims, Class<T> pixelClass) {
        if (pixelClass == UnsignedByteType.class) {
            byte[] arr = new byte[flatArr.length];
            for (int i = 0; i < flatArr.length; i++) {
                arr[i] = (byte) flatArr[i];
            }
            MDByteArray mdArray = new MDByteArray(arr, sliceDims);
            writer.uint8().writeMDArrayBlockWithOffset(dataset, mdArray, offset);
        } else if (pixelClass == UnsignedShortType.class) {
            short[] arr = new short[flatArr.length];
            for (int i = 0; i < flatArr.length; i++) {
                arr[i] = (short) flatArr[i];
            }
            MDShortArray mdArray = new MDShortArray(arr, sliceDims);
            writer.uint16().writeMDArrayBlockWithOffset(dataset, mdArray, offset);
        } else if (pixelClass == UnsignedIntType.class) {
            int[] arr = new int[flatArr.length];
            for (int i = 0; i < flatArr.length; i++) {
                arr[i] = (int) flatArr[i];
            }
            MDIntArray mdArray = new MDIntArray(arr, sliceDims);
            writer.uint32().writeMDArrayBlockWithOffset(dataset, mdArray, offset);
        } else if (pixelClass == UnsignedLongType.class) {
            long[] arr = new long[flatArr.length];
            for (int i = 0; i < flatArr.length; i++) {
                arr[i] = (long) flatArr[i];
            }
            MDLongArray mdArray = new MDLongArray(arr, sliceDims);
            writer.uint64().writeMDArrayBlockWithOffset(dataset, mdArray, offset);
        } else if (pixelClass == FloatType.class) {
            float[] arr = new float[flatArr.length];
            for (int i = 0; i < flatArr.length; i++) {
                arr[i] = (float) flatArr[i];
            }
            MDFloatArray mdArray = new MDFloatArray(arr, sliceDims);
            writer.float32().writeMDArrayBlockWithOffset(dataset, mdArray, offset);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + pixelClass);
        }
    }

    private Object getValue(RandomAccess<T> rai, Class<T> pixelClass) {
        if (pixelClass == UnsignedByteType.class) {
            UnsignedByteType type = (UnsignedByteType) rai.get();
            return Integer.valueOf(type.get()).byteValue();
        } else if (pixelClass == UnsignedShortType.class) {
            UnsignedShortType type = (UnsignedShortType) rai.get();
            return Integer.valueOf(type.get()).shortValue();
        } else if (pixelClass == UnsignedIntType.class) {
            UnsignedIntType type = (UnsignedIntType) rai.get();
            return Long.valueOf(type.get()).intValue();
        } else if (pixelClass == UnsignedLongType.class) {
            UnsignedLongType type = (UnsignedLongType) rai.get();
            return type.get();
        } else if (pixelClass == FloatType.class) {
            FloatType type = (FloatType) rai.get();
            return type.get();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + pixelClass);
        }
    }

    private void createMDArray(IHDF5Writer writer, long[] datasetDims, int[] blockSize, Class<T> pixelClass) {
        if (pixelClass == UnsignedByteType.class) {
            writer.uint8().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
        } else if (pixelClass == UnsignedShortType.class) {
            writer.uint16().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
        } else if (pixelClass == UnsignedIntType.class) {
            writer.uint32().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
        } else if (pixelClass == UnsignedLongType.class) {
            writer.uint64().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
        } else if (pixelClass == FloatType.class) {
            writer.float32().createMDArray(dataset, datasetDims, blockSize, HDF5FloatStorageFeatures.createDeflationDelete(compressionLevel));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + pixelClass);
        }
    }

    private void writeARGB(IHDF5Writer writer, long[] datasetDims) {
        logService.info("Saving ARGB as 'uint8' (4 channels). Compression level: " + compressionLevel);

        boolean isAlphaChannelPresent = true;
        if (numChannels == ARGB_CHANNEL_NUM - 1) {
            logService.warn("Only 3 channel RGB found. Setting ALPHA channel to -1 (transparent).");
            isAlphaChannelPresent = false;
            datasetDims[4] = ARGB_CHANNEL_NUM; // set channel dimension to 4 explicitly
        }

        final int totalCheckpoints = numFrames * ARGB_CHANNEL_NUM * dimZ;
        final AtomicInteger checkpoint = new AtomicInteger(0);
        statusService.ifPresent(s -> s.showStatus(checkpoint.get(), totalCheckpoints, "Exporting HDF5..."));

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        createMDArray(writer, datasetDims, blockSize, (Class<T>) UnsignedByteType.class);

        RandomAccess<T> rai = image.randomAccess();
        for (int t = 0; t < numFrames; t++) {
            if (image.dimensionIndex(Axes.TIME) >= 0)
                rai.setPosition(t, image.dimensionIndex(Axes.TIME));
            for (int z = 0; z < dimZ; z++) {
                if (image.dimensionIndex(Axes.Z) >= 0)
                    rai.setPosition(z, image.dimensionIndex(Axes.Z));
                for (int c = 0; c < ARGB_CHANNEL_NUM; c++) {
                    // init MD-array
                    Object[] flatArr = new Object[dimY * dimX];
                    boolean skipCopying = false;
                    if (isAlphaChannelPresent) {
                        if (image.dimensionIndex(Axes.CHANNEL) >= 0)
                            rai.setPosition(c, image.dimensionIndex(Axes.CHANNEL));
                    } else {
                        if (c == 0) {
                            Arrays.fill(flatArr, (byte) -1);  // hardcode alpha channel
                            skipCopying = true;
                        } else {
                            if (image.dimensionIndex(Axes.CHANNEL) >= 0) {
                                rai.setPosition(c - 1, image.dimensionIndex(Axes.CHANNEL));
                            }
                        }
                    }
                    if (!skipCopying) {
                        // copy data XY slice
                        for (int x = 0; x < dimX; x++) {
                            rai.setPosition(x, image.dimensionIndex(Axes.X));
                            for (int y = 0; y < dimY; y++) {
                                rai.setPosition(y, image.dimensionIndex(Axes.Y));

                                int index = y * dimX + x;
                                ARGBType type = (ARGBType) rai.get();
                                byte value = Integer.valueOf(type.get()).byteValue();
                                flatArr[index] = value;
                            }
                        }
                    }
                    long[] offset = {t, z, 0, 0, c};
                    long[] sliceDims = Hdf5Utils.getXYSliceDims(datasetDims);
                    // save data
                    writeMDArray(writer, flatArr, offset, sliceDims, (Class<T>) UnsignedByteType.class);
                    // update progress bar
                    statusService.ifPresent(s -> s.showProgress(checkpoint.incrementAndGet(), totalCheckpoints));
                }

            }
        }
        statusService.ifPresent(s -> s.showStatus("Finished Exporting HDF5."));
    }

    private int getDimension(ImgPlus<T> image, AxisType axis) {
        int dimensionIndex = image.dimensionIndex(axis);
        if (dimensionIndex >= 0) {
            final int dimension = Math.toIntExact(image.dimension(dimensionIndex));
            return dimension;
        } else {
            if (Axes.X.equals(axis) || Axes.Y.equals(axis)) {
                throw new IllegalStateException("Image must have X and Y dimensions");
            }
            return 1;
        }
    }
}
