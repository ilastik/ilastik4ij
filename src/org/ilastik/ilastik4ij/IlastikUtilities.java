/**
 * MIT License
 * 
 * Copyright (c) 2017 ilastik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Author: Carsten Haubold
 */
package org.ilastik.ilastik4ij;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.scijava.log.LogService;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class IlastikUtilities {
        /**
         * Utility method to obtain a unique filename
         * 
         * @param extension
         * @return
         * @throws IOException
         */
        public static String getTemporaryFileName(String extension) throws IOException {
                File f = File.createTempFile("ilastik4j", extension, null);
                String filename = f.getAbsolutePath();
                f.delete();
                return filename;
        }

        /**
         * Redirect an input stream to the log service (used for command line
         * output)
         *
         * @param in
         *                input stream
         * @param logService
         * @throws IOException
         */
        public static void redirectOutputToLogService(final InputStream in, final LogService logger, final Boolean isErrorStream) {
                Thread t = new Thread() {
                        @Override
                        public void run() {

                                String line;

                                try (BufferedReader bis = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
                                        while ((line = bis.readLine()) != null) {
                                                if (isErrorStream) {
                                                        logger.error(line);
                                                } else {
                                                        logger.info(line);
                                                }
                                        }
                                } catch (IOException ioe) {
                                        throw new RuntimeException("Could not read ilastik output", ioe);
                                }
                        }
                };

                //        t.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(KNIPGateway.log()));
                t.start();
        }

        public static ImagePlus readFloatHdf5VolumeIntoImage(String filename, String dataset, String axesorder) {
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
                        if (flat_data[i] > maxGray)
                                maxGray = flat_data[i];
                }

                // configure options of image
                for (int c = 1; c <= dsConfig.numChannels; ++c) {
                        image.setC(c);
                        image.setDisplayRange(0, maxGray);
                }

                return image;
        }
}
