package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;
import net.imagej.Dataset;
import net.imglib2.RandomAccess;

/**
 *
 * @author chaubold
 */
public class Hdf5DataSetReader {

    private String filename;
    private String dataset;
    private String axesorder;
    private LogService log;
    private DatasetService datasetService;

    public Hdf5DataSetReader(String filename, String dataset, String axesorder, LogService log, DatasetService ds) {
        this.filename = filename;
        this.dataset = dataset;
        this.axesorder = axesorder;
        this.log = log;
        this.datasetService = ds;
    }

    public ImgPlus read() {
        IHDF5Reader reader = HDF5Factory.openForReading(filename);
        Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
        log.info("Found dataset '" + dataset + "' of type " + dsConfig.typeInfo);

        MDFloatArray rawdata_float = null;
        float[] flat_data_float = null;

        MDByteArray rawdata_byte = null;
        byte[] flat_data_byte = null;

        MDShortArray rawdata_short = null;
        short[] flat_data_short = null;

        MDIntArray rawdata_int = null;
        int[] flat_data_int = null;

        // construct output image
        long[] dims = {dsConfig.dimX, dsConfig.dimY, dsConfig.numChannels, dsConfig.dimZ, dsConfig.numFrames};
        AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        log.info("Constructing output image of shape " + dims[0] + ", " + dims[1] + ", " + dims[2] + ", " + dims[3] + ", " + dims[4]);

        Dataset ds = null;
        switch (dsConfig.typeInfo) {
            case "float32":
                ds = datasetService.create(new FloatType(), dims, filename, axes);
                break;
            case "uint8":
                ds = datasetService.create(new UnsignedByteType(), dims, filename, axes);
                break;
            case "uint16":
                ds = datasetService.create(new UnsignedShortType(), dims, filename, axes);
                break;
            case "uint32":
                ds = datasetService.create(new UnsignedIntType(), dims, filename, axes);
                break;
            default:
                throw new IllegalArgumentException("Dataset uses not yet supported datatype " + dsConfig.typeInfo + "!");
        }
        
        ImgPlus image = ds.getImgPlus();
        if(image == null)
        {
            log.error("Could not get imgPlus from dataset");
            return null;
        }
        
        RandomAccess rai = image.randomAccess();
        log.info("Created image of shape: " + image.dimension(image.dimensionIndex(Axes.X))
                + ", " + image.dimension(image.dimensionIndex(Axes.Y))
                + ", " + image.dimension(image.dimensionIndex(Axes.CHANNEL))
                + ", " + image.dimension(image.dimensionIndex(Axes.Z))
                + ", " + image.dimension(image.dimensionIndex(Axes.TIME)));

        for (int frame = 0; frame < dsConfig.numFrames; ++frame) {
            rai.setPosition(frame, image.dimensionIndex(Axes.TIME));
            for (int lev = 0; lev < dsConfig.dimZ; ++lev) {
                rai.setPosition(lev, image.dimensionIndex(Axes.Z));
                for (int c = 0; c < dsConfig.numChannels; ++c) {
                    rai.setPosition(c, image.dimensionIndex(Axes.CHANNEL));

                    int[] extents = dsConfig.getXYSliceExtent();
                    long[] offset = dsConfig.getSliceOffset(frame, lev, c);

                    try{
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
                            rai.setPosition(x, image.dimensionIndex(Axes.X));
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                rai.setPosition(y, image.dimensionIndex(Axes.Y));
                                int destIndex = y * dsConfig.dimX + x;
                                if(dsConfig.axisIndices.get('x') < dsConfig.axisIndices.get('y'))
                                {
                                    destIndex = x * dsConfig.dimY + y;
                                }

                                switch (dsConfig.typeInfo) {
                                    case "float32":
                                        {
                                            float value = flat_data_float[destIndex];
                                            FloatType f = (FloatType)rai.get();
                                            f.set(value);
                                            break;
                                        }
                                    case "uint8":
                                        {
                                            byte value = flat_data_byte[destIndex];
                                            UnsignedByteType f = (UnsignedByteType)rai.get();
                                            f.set(value);
                                            break;
                                        }
                                    case "uint16":
                                        {
                                            short value = flat_data_short[destIndex];
                                            UnsignedShortType f = (UnsignedShortType)rai.get();
                                            f.set(value);
                                            break;
                                        }
                                    case "uint32":
                                        {
                                            int value = flat_data_int[destIndex];
                                            UnsignedIntType f = (UnsignedIntType)rai.get();
                                            f.set(value);
                                            break;
                                        }
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        String extentsStr = "";
                        for(int x : extents)
                        {
                            extentsStr += String.valueOf(x) + ", ";
                        }
                        
                        String offsetStr = "";
                        for(long x : offset)
                        {
                            offsetStr += String.valueOf(x) + ", ";
                        }
                        log.warn("Could not read data starting at " + offsetStr + " with size " + extentsStr);
                    }
                }
            }
        }

        
        // configure options of image
        image.initializeColorTables(dsConfig.numFrames * dsConfig.numChannels * dsConfig.dimZ);
        image.setValidBits(dsConfig.bitdepth);

        reader.close();
        image.setName(filename + "/" + dataset);

        return image;
    }
}
