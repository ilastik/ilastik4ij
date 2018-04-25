package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccess;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;

import java.util.Arrays;

import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.*;
import static java.lang.Long.min;

public class Hdf5DataSetWriterFromImgPlus<T extends Type<T>> {
    private static final int NUM_OF_ARGB_CHANNELS = 4;
    private final ImgPlus<T> image;
    private static final int RANK = 5;
    private final long nFrames;
    private final long nChannels;
    private final long nLevs;
    private long nRows;
    private long nCols;

    private LogService log;
    private String filename;
    private String dataset;
    private int compressionLevel;
    private final int cols;
    private final int rows;
    private int fileId = -1;
    private int dataspaceId = -1;
    private int datasetId = -1;
    private int dcplId = -1;
    private final long[] maxDims = {
            HDF5Constants.H5S_UNLIMITED,
            HDF5Constants.H5S_UNLIMITED,
            HDF5Constants.H5S_UNLIMITED,
            HDF5Constants.H5S_UNLIMITED,
            HDF5Constants.H5S_UNLIMITED
    };

    public Hdf5DataSetWriterFromImgPlus(ImgPlus<T> image, String filename, String dataset, int compressionLevel, LogService log) {
        this.image = image;

        if (image.dimensionIndex(Axes.TIME) >= 0) {
            this.nFrames = image.dimension(image.dimensionIndex(Axes.TIME));
        } else {
            this.nFrames = 1;
        }
        if (image.dimensionIndex(Axes.CHANNEL) >= 0) {
            this.nChannels = image.dimension(image.dimensionIndex(Axes.CHANNEL));
        } else {
            this.nChannels = 1;
        }
        if (image.dimensionIndex(Axes.Z) >= 0) {
            this.nLevs = image.dimension(image.dimensionIndex(Axes.Z));
        } else {
            this.nLevs = 1;
        }
        if (image.dimensionIndex(Axes.X) < 0 || image.dimensionIndex(Axes.Y) < 0) {
            throw new IllegalArgumentException("image must have X and Y dimensions!");
        }

        this.nRows = image.dimension(image.dimensionIndex(Axes.Y));
        this.nCols = image.dimension(image.dimensionIndex(Axes.X));
        this.cols = Math.toIntExact(nCols); //One time cast from long to int data type.
        this.rows = Math.toIntExact(nRows); //One time cast from long to int data type.
        this.filename = filename;
        this.dataset = dataset;
        this.compressionLevel = compressionLevel;
        this.log = log;
    }

    public void write() {
        long[] chunk_dims = {1,
                min(nLevs, 256),
                min(nRows, 256),
                min(nCols, 256),
                1
        };
        log.info("Export Dimensions in tzyxc: " + String.valueOf(nFrames) + "x" + String.valueOf(nLevs) + "x"
                + String.valueOf(nRows) + "x" + String.valueOf(nCols) + "x" + String.valueOf(nChannels));

        try {

            fileId = H5.H5Fcreate(filename, H5F_ACC_TRUNC, H5P_DEFAULT, H5P_DEFAULT);
            dcplId = H5.H5Pcreate(H5P_DATASET_CREATE);
            H5.H5Pset_chunk(dcplId, RANK, chunk_dims);
            H5.H5Pset_deflate(dcplId, compressionLevel);

            T val = image.firstElement();
            if (val instanceof UnsignedByteType) {
                log.info("Writing uint 8.");
                writeIndividualChannels(H5T_NATIVE_UINT8);
            } else if (val instanceof UnsignedShortType) {
                log.info("Writing uint 16.");
                writeIndividualChannels(H5T_NATIVE_UINT16);
            } else if (val instanceof UnsignedIntType) {
                log.info("Writing uint 32.");
                writeIndividualChannels(H5T_NATIVE_UINT32);
            } else if (val instanceof FloatType) {
                log.info("Writing float 32.");
                writeIndividualChannels(H5T_NATIVE_FLOAT);
            } else if (val instanceof ARGBType) {
                log.info("Writing ARGB to 4 uint8 channels.");
                writeARGB();
            } else {
                log.error("Type Not handled yet!" + val.getClass());
                throw new IllegalArgumentException("Unsupported Type: " + val.getClass());
            }
        } catch (HDF5Exception err) {
            log.error("HDF5 API error occurred while creating '" + filename + "'." + err.getMessage());
            throw new RuntimeException(err);
        } catch (Exception err) {
            log.error("An unexpected error occurred while creating '" + filename + "'." + err.getMessage());
            throw new RuntimeException(err);
        } catch (OutOfMemoryError o) {
            log.error("Out of Memory Error while creating '" + filename + "'." + o.getMessage());
            throw new RuntimeException(o);
        } finally {
            H5.H5Sclose(dataspaceId);
            H5.H5Pclose(dcplId);
            H5.H5Dclose(datasetId);
            H5.H5Fclose(fileId);
        }
    }

    private void writeARGB() {
        long[] channelDimsRGB = new long[RANK];
        channelDimsRGB[0] = nFrames; //t
        channelDimsRGB[1] = nLevs; //z
        channelDimsRGB[2] = nRows; //y
        channelDimsRGB[3] = nCols; //x
        channelDimsRGB[4] = NUM_OF_ARGB_CHANNELS;//c

        long[] colorIniDims = new long[RANK];
        colorIniDims[0] = 1;
        colorIniDims[1] = 1;
        colorIniDims[2] = nRows;
        colorIniDims[3] = nCols;
        colorIniDims[4] = 1;

        try {
            dataspaceId = H5.H5Screate_simple(RANK, colorIniDims, maxDims);
            datasetId = H5.H5Dcreate(fileId, dataset, H5T_NATIVE_UINT8, dataspaceId, H5P_DEFAULT, dcplId, H5P_DEFAULT);
        } catch (HDF5Exception ex) {
            log.error("H5D dataspace creation failed." + ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } catch (Exception err) {
            log.error("An error occurred at writeARGB method." + err.getMessage(), err);
            throw new RuntimeException(err);
        }

        @SuppressWarnings("unchecked")
        RandomAccess<ARGBType> rai = (RandomAccess<ARGBType>) image.randomAccess();
        boolean isAlphaChannelPresent = true;
        Object[][] pixelsByte;
        H5.H5Dset_extent(datasetId, channelDimsRGB);

        for (long t = 0; t < nFrames; t++) {
            if (image.dimensionIndex(Axes.TIME) >= 0)
                rai.setPosition(t, image.dimensionIndex(Axes.TIME));

            for (long z = 0; z < nLevs; z++) {
                if (image.dimensionIndex(Axes.Z) >= 0)
                    rai.setPosition(z, image.dimensionIndex(Axes.Z));

                if (nChannels == NUM_OF_ARGB_CHANNELS - 1) {
                    log.warn("Only 3 channel RGB found. Setting ALPHA channel to -1 (transparent).");
                    isAlphaChannelPresent = false;
                }

                for (long c = 0; c < NUM_OF_ARGB_CHANNELS; c++) {
                    // Construct 2D array of appropriate data
                    pixelsByte = new Byte[rows][cols];

                    if (!isAlphaChannelPresent) {
                        if (c == 0) {
                            for (Byte[] row : (Byte[][]) pixelsByte) {
                                Arrays.fill(row, (byte) -1);  // hard code alpha channel.
                            }
                        } else {
                            if (image.dimensionIndex(Axes.CHANNEL) >= 0) {
                                rai.setPosition(c - 1, image.dimensionIndex(Axes.CHANNEL));
                            }
                            fillStackSlice(rai, pixelsByte);
                        }
                    } else {
                        if (image.dimensionIndex(Axes.CHANNEL) >= 0) {
                            rai.setPosition(c, image.dimensionIndex(Axes.CHANNEL));
                        }
                        fillStackSlice(rai, pixelsByte);
                    }
                    // write it out
                    long[] start = {t, z, 0, 0, c};
                    writeHyperslabs(H5T_NATIVE_UINT8, pixelsByte, start, colorIniDims);
                }
            }
        }
        log.info("CompressionLevel: " + String.valueOf(compressionLevel));
        log.info("Finished writing the HDF5.");
    }


    private void writeIndividualChannels(int hdf5DataType) {
        if (nLevs < 1) {
            log.error("got less than 1 z?");
            return;
        }
        long[] channelDims = new long[RANK];
        channelDims[0] = nFrames; // t
        channelDims[1] = nLevs; // z
        channelDims[2] = nRows; //y
        channelDims[3] = nCols; //x
        channelDims[4] = nChannels; // c

        long[] iniDims = new long[RANK];
        iniDims[0] = 1;
        iniDims[1] = 1;
        iniDims[2] = nRows;
        iniDims[3] = nCols;
        iniDims[4] = 1;

        try {
            dataspaceId = H5.H5Screate_simple(RANK, iniDims, maxDims);
            datasetId = H5.H5Dcreate(fileId, dataset, hdf5DataType, dataspaceId, H5P_DEFAULT, dcplId, H5P_DEFAULT);
        } catch (HDF5Exception ex) {
            log.error("H5D dataspace creation failed." + ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } catch (Exception err) {
            log.error("An error occurred at writeIndividualChannels method." + err.getMessage(), err);
            throw new RuntimeException(err);
        }

        RandomAccess<T> rai = image.randomAccess();
        Object[][] pixelSlice;
        H5.H5Dset_extent(datasetId, channelDims);

        for (long t = 0; t < nFrames; t++) {
            if (image.dimensionIndex(Axes.TIME) >= 0)
                rai.setPosition(t, image.dimensionIndex(Axes.TIME));

            for (long z = 0; z < nLevs; z++) {
                if (image.dimensionIndex(Axes.Z) >= 0)
                    rai.setPosition(z, image.dimensionIndex(Axes.Z));

                for (long c = 0; c < nChannels; c++) {
                    if (image.dimensionIndex(Axes.CHANNEL) >= 0)
                        rai.setPosition(c, image.dimensionIndex(Axes.CHANNEL));

                    // Construct 2D array of appropriate data type.
                    if (hdf5DataType == H5T_NATIVE_UINT8) {
                        pixelSlice = new Byte[rows][cols];
                    } else if (hdf5DataType == H5T_NATIVE_UINT16) {
                        pixelSlice = new Short[rows][cols];
                    } else if (hdf5DataType == H5T_NATIVE_UINT32) {
                        pixelSlice = new Integer[rows][cols];
                    } else if (hdf5DataType == H5T_NATIVE_FLOAT) {
                        pixelSlice = new Float[rows][cols];
                    } else {
                        throw new IllegalArgumentException("Trying to save dataset of unknown datatype.");
                    }
                    fillStackSlice(rai, pixelSlice);
                    long[] start = {t, z, 0, 0, c};
                    writeHyperslabs(hdf5DataType, pixelSlice, start, iniDims);
                }
            }
        }

        log.info("compressionLevel: " + String.valueOf(compressionLevel));
        log.info("Finished writing the HDF5.");
    }

    private <E> void writeHyperslabs(int hdf5DataType, E[][] pixelsSlice, long[] start, long[] colorIniDims) {
        try {
            dataspaceId = H5.H5Dget_space(datasetId);
            H5.H5Sselect_hyperslab(dataspaceId, HDF5Constants.H5S_SELECT_SET, start, null, colorIniDims, null);
            int memSpace = H5.H5Screate_simple(RANK, colorIniDims, null);
            H5.H5Dwrite(datasetId, hdf5DataType, memSpace, dataspaceId, H5P_DEFAULT, pixelsSlice);
        } catch (HDF5Exception e) {
            log.error("Error while writing extended hyperslabs." + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("An error occurred at writeHyperslabs method." + e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType"})
    private <E, T> void fillStackSlice(RandomAccess<T> rai, E[][] pixelArray) {

        for (int x = 0; x < cols; x++) {
            rai.setPosition(x, image.dimensionIndex(Axes.X));
            for (int y = 0; y < rows; y++) {
                rai.setPosition(y, image.dimensionIndex(Axes.Y));
                T value = rai.get();
                if (value instanceof UnsignedByteType) {
                    pixelArray[y][x] = (E) (Byte) (new Integer(((UnsignedByteType) value).get()).byteValue());
                } else if (value instanceof UnsignedShortType) {
                    pixelArray[y][x] = (E) (Short) (new Integer((((UnsignedShortType) value).get())).shortValue());
                } else if (value instanceof UnsignedIntType) {
                    pixelArray[y][x] = (E) (Integer) (new Long((((UnsignedIntType) value).get())).intValue());
                } else if (value instanceof FloatType) {
                    pixelArray[y][x] = (E) (new Float((((FloatType) value).get())));
                } else if (value instanceof ARGBType) {
                    pixelArray[y][x] = (E) (Byte) (new Integer(((ARGBType) value).get()).byteValue());
                } else {
                    log.error("Type Not handled yet!");
                }
            }
        }
    }


}
