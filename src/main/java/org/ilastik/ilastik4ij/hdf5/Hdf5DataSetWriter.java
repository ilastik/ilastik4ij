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
import java.util.stream.Collectors;

public class Hdf5DataSetWriter<T extends Type<T>> {
    private static final int ARGB_CHANNEL_NUM = 4;
    private final ImgPlus<T> image;
    private final int numFrames;
    private final int numChannels;
    private final int dimZ;
    private final int dimY;
    private final int dimX;
    private final LogService log;
    private final StatusService statusService;
    private final String filename;
    private final String dataset;
    private final int compressionLevel;

    public Hdf5DataSetWriter(ImgPlus<T> image, String filename, String dataset, int compressionLevel, LogService log,
                             StatusService statusService) {
        this.image = image;
        this.numFrames = getDimension(image, Axes.TIME);
        this.numChannels = getDimension(image, Axes.CHANNEL);
        this.dimZ = getDimension(image, Axes.Z);
        this.dimY = getDimension(image, Axes.Y);
        this.dimX = getDimension(image, Axes.X);
        this.filename = filename;
        this.dataset = dataset;
        this.compressionLevel = compressionLevel;
        this.log = log;
        this.statusService = statusService;
    }

    public void write() {
        final long[] dims = {numFrames, dimZ, dimY, dimX, numChannels};
        String shape = Arrays.stream(dims)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
        log.info(String.format("Exporting image of shape (%s). Axis order: 'TZYXC'", shape));

        try (IHDF5Writer writer = HDF5Factory.open(filename)) {
            T val = image.firstElement();

            if (val instanceof UnsignedByteType) {
                writeUint8(writer, dims);
            } else if (val instanceof UnsignedShortType) {
                writeUint16(writer, dims);
            } else if (val instanceof UnsignedIntType) {
                writeUint32(writer, dims);
            } else if (val instanceof UnsignedLongType) {
                writeUint64(writer, dims);
            } else if (val instanceof FloatType) {
                writeFloat32(writer, dims);
            } else if (val instanceof ARGBType) {
                writeARGB(writer, dims);
            } else {
                throw new IllegalArgumentException("Unsupported Type: " + val.getClass());
            }
        }
    }

    private void writeUint8(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving as 'uint8'. Compression level: " + compressionLevel);
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.uint8().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));

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
                    byte[] flatArr = new byte[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));

                            int index = y * dimX + x;
                            UnsignedByteType type = (UnsignedByteType) rai.get();
                            byte value = Integer.valueOf(type.get()).byteValue();
                            flatArr[index] = value;
                        }
                    }
                    // save data
                    MDByteArray arr = new MDByteArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.uint8().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }

    private void writeUint16(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving as 'uint16'. Compression level: " + compressionLevel);
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.uint16().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));

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
                    short[] flatArr = new short[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));

                            int index = y * dimX + x;
                            UnsignedShortType type = (UnsignedShortType) rai.get();
                            short value = Integer.valueOf(type.get()).shortValue();
                            flatArr[index] = value;
                        }
                    }
                    // save data
                    MDShortArray arr = new MDShortArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.uint16().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }


    private void writeUint32(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving as 'uint32'. Compression level: " + compressionLevel);
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.uint32().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));

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
                    int[] flatArr = new int[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));

                            int index = y * dimX + x;
                            UnsignedIntType type = (UnsignedIntType) rai.get();
                            int value = Long.valueOf(type.get()).intValue();
                            flatArr[index] = value;
                        }
                    }
                    // save data
                    MDIntArray arr = new MDIntArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.uint32().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }


    private void writeUint64(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving as 'uint64'. Compression level: " + compressionLevel);
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.uint64().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));

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
                    long[] flatArr = new long[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));

                            int index = y * dimX + x;
                            UnsignedLongType type = (UnsignedLongType) rai.get();
                            flatArr[index] = type.get();
                        }
                    }
                    // save data
                    MDLongArray arr = new MDLongArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.uint64().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }


    private void writeFloat32(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving as 'float32'. Compression level: " + compressionLevel);
        final int totalCheckpoints = numFrames * numChannels * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.float32().createMDArray(dataset, datasetDims, blockSize, HDF5FloatStorageFeatures.createDeflationDelete(compressionLevel));

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
                    float[] flatArr = new float[dimY * dimX];
                    // copy data XY slice
                    for (int x = 0; x < dimX; x++) {
                        rai.setPosition(x, image.dimensionIndex(Axes.X));
                        for (int y = 0; y < dimY; y++) {
                            rai.setPosition(y, image.dimensionIndex(Axes.Y));

                            int index = y * dimX + x;
                            FloatType type = (FloatType) rai.get();
                            flatArr[index] = type.get();
                        }
                    }
                    // save data
                    MDFloatArray arr = new MDFloatArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.float32().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }

    private void writeARGB(IHDF5Writer writer, long[] datasetDims) {
        log.info("Saving ARGB as 'uint8' (4 channels). Compression level: " + compressionLevel);

        boolean isAlphaChannelPresent = true;
        if (numChannels == ARGB_CHANNEL_NUM - 1) {
            log.warn("Only 3 channel RGB found. Setting ALPHA channel to -1 (transparent).");
            isAlphaChannelPresent = false;
            datasetDims[4] = ARGB_CHANNEL_NUM; // set channel dimension to 4 explicitly
        }

        final int totalCheckpoints = numFrames * ARGB_CHANNEL_NUM * dimZ;
        int checkpoint = 0;
        statusService.showStatus(checkpoint, totalCheckpoints, "Exporting HDF5...");

        int[] blockSize = Hdf5Utils.blockSize(datasetDims);
        writer.uint8().createMDArray(dataset, datasetDims, blockSize, HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));

        RandomAccess<T> rai = image.randomAccess();
        for (int t = 0; t < numFrames; t++) {
            if (image.dimensionIndex(Axes.TIME) >= 0)
                rai.setPosition(t, image.dimensionIndex(Axes.TIME));
            for (int z = 0; z < dimZ; z++) {
                if (image.dimensionIndex(Axes.Z) >= 0)
                    rai.setPosition(z, image.dimensionIndex(Axes.Z));
                for (int c = 0; c < ARGB_CHANNEL_NUM; c++) {
                    // init MD-array
                    byte[] flatArr = new byte[dimY * dimX];
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
                    // save data
                    MDByteArray arr = new MDByteArray(flatArr, Hdf5Utils.getXYSliceDims(datasetDims));
                    long[] offset = {t, z, 0, 0, c};
                    writer.uint8().writeMDArrayBlockWithOffset(dataset, arr, offset);
                    // update progress bar
                    statusService.showProgress(++checkpoint, totalCheckpoints);
                }

            }
        }
        statusService.showStatus("Finished Exporting HDF5.");
    }

    private int getDimension(ImgPlus<T> image, AxisType axis) {
        int dimensionIndex = image.dimensionIndex(axis);
        if (dimensionIndex >= 0) {
            return Math.toIntExact(image.dimension(dimensionIndex));
        } else {
            if (Axes.X.equals(axis) || Axes.Y.equals(axis)) {
                throw new IllegalStateException("Image must have X and Y dimensions");
            }
            return 1;
        }
    }
}
