/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 *
 * @author chaubold
 */
public class Hdf5DataSetReader {
        private String filename;
        private String dataset;
        private String axesorder;

        public Hdf5DataSetReader(String filename, String dataset, String axesorder) {
                this.filename = filename;
                this.dataset = dataset;
                this.axesorder = axesorder;
        }

        public ImagePlus read() {
                IHDF5Reader reader = HDF5Factory.openForReading(filename);
                Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
                if (!dsConfig.typeInfo.equals("float32")) {
                        throw new IllegalArgumentException("Dataset is not of float datatype!");
                }

                MDFloatArray rawdata = reader.float32().readMDArray(dataset);
                float[] flat_data = rawdata.getAsFlatArray();
                float maxGray = 1;

                ImagePlus image = IJ.createHyperStack(dataset, dsConfig.dimX, dsConfig.dimY, dsConfig.numChannels, dsConfig.dimZ, dsConfig.numFrames,
                                32);

                // TODO: make this work with hyperslabs instead of reading the full HDF5 volume into memory at once!
                for (int frame = 0; frame < dsConfig.numFrames; ++frame) {
                        for (int lev = 0; lev < dsConfig.dimZ; ++lev) {
                                for (int c = 0; c < dsConfig.numChannels; ++c) {
                                        ImageProcessor ip = image.getStack().getProcessor(image.getStackIndex(c + 1, lev + 1, frame + 1));
                                        float[] destData = (float[]) ip.getPixels();

                                        for (int x = 0; x < dsConfig.dimX; x++) {
                                                for (int y = 0; y < dsConfig.dimY; y++) {
                                                        int scrIndex = c + lev * dsConfig.numChannels + y * dsConfig.dimZ * dsConfig.numChannels
                                                                        + x * dsConfig.dimZ * dsConfig.dimY * dsConfig.numChannels
                                                                        + frame * dsConfig.dimZ * dsConfig.dimX * dsConfig.dimY
                                                                                        * dsConfig.numChannels;
                                                        int destIndex = y * dsConfig.dimX + x;
                                                        destData[destIndex] = flat_data[scrIndex];
                                                }
                                        }
                                }
                        }
                }

                // find largest value to automatically adjust the display range
                for (int i = 0; i < flat_data.length; ++i) {
                        if (flat_data[i] > maxGray) {
                                maxGray = flat_data[i];
                        }
                }

                // configure options of image
                for (int c = 1; c <= dsConfig.numChannels; ++c) {
                        image.setC(c);
                        image.setDisplayRange(0, maxGray);
                }

                return image;
        }
}
