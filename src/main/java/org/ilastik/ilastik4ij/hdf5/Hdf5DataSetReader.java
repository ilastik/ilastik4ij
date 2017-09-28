/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.scijava.log.LogService;

/**
 *
 * @author chaubold
 */
public class Hdf5DataSetReader {
    private String filename;
    private String dataset;
    private String axesorder;
    private LogService log;
    
    public Hdf5DataSetReader(String filename, String dataset, String axesorder, LogService log) {
        this.filename = filename;
        this.dataset = dataset;
        this.axesorder = axesorder;
        this.log = log;
    }

    public ImagePlus read() {
        IHDF5Reader reader = HDF5Factory.openForReading(filename);
        Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
        log.info("Found dataset '" + dataset + "' of type " + dsConfig.typeInfo);
        float maxGray = 1;
        
        MDFloatArray rawdata_float = null;
        float[] flat_data_float = null;
        
        MDByteArray rawdata_byte = null;
        byte[] flat_data_byte = null;
        
        MDShortArray rawdata_short = null;
        short[] flat_data_short = null;
        
        MDIntArray rawdata_int = null;
        int[] flat_data_int = null;

        ImagePlus image = IJ.createHyperStack(dataset, dsConfig.dimX, dsConfig.dimY, dsConfig.numChannels, dsConfig.dimZ, dsConfig.numFrames, dsConfig.bitdepth);

        // TODO: make this work with hyperslabs instead of reading the full HDF5 volume into memory at once!
        for (int frame = 0; frame < dsConfig.numFrames; ++frame) {
            for (int lev = 0; lev < dsConfig.dimZ; ++lev) {
                for (int c = 0; c < dsConfig.numChannels; ++c) {
                    ImageProcessor ip = image.getStack().getProcessor(image.getStackIndex(c + 1, lev + 1, frame + 1));
                    
                    int[] extents = { 1, 1, dsConfig.dimY, dsConfig.dimX, 1 };
                    long[] offset = { frame, lev, 0, 0, c };
                    
                    if(dsConfig.typeInfo.equals("float32"))
                    {
                        rawdata_float = reader.float32().readMDArrayBlockWithOffset(dataset, extents, offset);
                        flat_data_float = rawdata_float.getAsFlatArray();
                    }
                    else if(dsConfig.typeInfo.equals("uint8"))
                    {
                        rawdata_byte = reader.uint8().readMDArrayBlockWithOffset(dataset, extents, offset);
                        flat_data_byte = rawdata_byte.getAsFlatArray();
                    }
                    else if(dsConfig.typeInfo.equals("uint8"))
                    {
                        rawdata_byte = reader.uint8().readMDArrayBlockWithOffset(dataset, extents, offset);
                        flat_data_byte = rawdata_byte.getAsFlatArray();
                    }
                    else if(dsConfig.typeInfo.equals("uint16"))
                    {
                        rawdata_short = reader.uint16().readMDArrayBlockWithOffset(dataset, extents, offset);
                        flat_data_short = rawdata_short.getAsFlatArray();
                    }
                    else if(dsConfig.typeInfo.equals("uint32"))
                    {
                        rawdata_int = reader.uint32().readMDArrayBlockWithOffset(dataset, extents, offset);
                        flat_data_int = rawdata_int.getAsFlatArray();
                    }
                    else
                    {
                        throw new IllegalArgumentException("Dataset uses not yet supported datatype " + dsConfig.typeInfo + "!");
                    }
                    
                    if(dsConfig.typeInfo.equals("float32"))
                    {
                        float[] destData_float = (float[]) ip.getPixels();

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                int destIndex = x * dsConfig.dimY + y;
                                float value = flat_data_float[destIndex];
                                if((float)value > maxGray)
                                    maxGray = (float)value;
                                destData_float[destIndex] = value;
                            }
                        }
                    }
                    else if(dsConfig.typeInfo.equals("uint8"))
                    {
                        byte[] destData_byte = (byte[]) ip.getPixels();

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                int destIndex = x * dsConfig.dimY + y;
                                byte value = flat_data_byte[destIndex];
                                if((float)value > maxGray)
                                    maxGray = (float)value;
                                destData_byte[destIndex] = value;
                            }
                        }
                    }
                    else if(dsConfig.typeInfo.equals("uint16"))
                    {
                        short[] destData_short = (short[]) ip.getPixels();

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                int destIndex = x * dsConfig.dimY + y;
                                short value = flat_data_short[destIndex];
                                if((float)value > maxGray)
                                    maxGray = (float)value;
                                destData_short[destIndex] = value;
                            }
                        }
                    }
                    else if(dsConfig.typeInfo.equals("uint32"))
                    {
                        // reading it as float instead of int, because uint32 is not supported natively
                        float[] destData_float = (float[]) ip.getPixels();

                        for (int x = 0; x < dsConfig.dimX; x++) {
                            for (int y = 0; y < dsConfig.dimY; y++) {
                                int destIndex = x * dsConfig.dimY + y;
                                int value = flat_data_int[destIndex];
                                if((float)value > maxGray)
                                    maxGray = (float)value;
                                destData_float[destIndex] = value;
                            }
                        }
                    }
                }
            }
        }

        // configure options of image
        for (int c = 1; c <= dsConfig.numChannels; ++c) {
            image.setC(c);
            image.setDisplayRange(0, maxGray);
        }

        reader.close();
        image.setTitle(filename + "/" + dataset);
        
        return image;
    }
}
