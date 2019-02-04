/**
 * MIT License
 * <p>
 * Copyright (c) 2017 ilastik
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p>
 * Author: Carsten Haubold
 */
package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.util.IlastikUtilities;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Pixel Classification Prediction")
public class IlastikPixelClassificationPrediction implements Command {

    // needed services:
    @Parameter
    LogService log;

    @Parameter
    StatusService statusService;

    @Parameter
    OptionsService optionsService;

    // own parameters:
    @Parameter(label = "Save temporary file for training only, without prediction.")
    private Boolean saveOnly = false;

    @Parameter(label = "Trained ilastik project file")
    private File projectFileName;

    @Parameter(label = "Raw input image")
    private Dataset inputImage;

    @Parameter(label = "Output type", choices = {"Segmentation", "Probabilities"}, style = "radioButtonHorizontal")
    private String chosenOutputType = "Probabilities";

    @Parameter(type = ItemIO.OUTPUT)
    private Img<? extends NativeType<?>> predictions;

    private IlastikOptions ilastikOptions = null;

    /**
     * Run method that calls ilastik
     */
    @Override
    public void run() {
        ilastikOptions = optionsService.getOptions(IlastikOptions.class);

        if (ilastikOptions == null) {
            log.error("Could not find configured ilastik options!");
            return;
        }

        if (!ilastikOptions.isConfigured()) {
            log.error("ilastik service must be configured before use!");
            return;
        }

        String tempInFileName = null;
        String tempOutFileName = null;

        try {
            try {
                tempInFileName = IlastikUtilities.getTemporaryFileName("_raw.h5");
            } catch (IOException e) {
                log.error("Could not create a temporary file for sending raw data to ilastik");
                e.printStackTrace();
                return;
            }

            log.info("Dumping input image to temporary file " + tempInFileName);
            new Hdf5DataSetWriter(inputImage.getImgPlus(), tempInFileName, "data", 0, log, statusService).write();

            if (saveOnly) {
                log.info("Saved file for training to " + tempInFileName + ". Use it to train an ilastik pixelClassificationProject now,"
                        + " and make sure to select to copy the raw data into the project file in the data selection");
                return;
            }

            try {
                tempOutFileName = IlastikUtilities.getTemporaryFileName("_out" + chosenOutputType + ".h5");
            } catch (IOException e) {
                log.error("Could not create a temporary file for obtaining the results from ilastik");
                e.printStackTrace();
                return;
            }

            runIlastik(tempInFileName, tempOutFileName);
            log.info("Reading resulting " + chosenOutputType + " from " + tempOutFileName);

            predictions = new Hdf5DataSetReader(tempOutFileName, "exported_data", "tzyxc", log, statusService).read();
        } catch (final Exception e) {
            log.warn("something went wrong during processing ilastik pixel classification");
        } finally {
            if (!saveOnly) {
                log.info("Cleaning up");
                // get rid of temporary files
                if (tempInFileName != null)
                    new File(tempInFileName).delete();
                if (tempOutFileName != null)
                    new File(tempOutFileName).delete();
            }
        }
    }

    private void runIlastik(String tempInFileName, String tempOutFileName) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(ilastikOptions.getExecutableFilePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempOutFileName);
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        if (chosenOutputType.equals("Segmentation")) {
            commandLine.add("--export_source=Simple Segmentation");
        }
        commandLine.add(tempInFileName);

        log.info("Running ilastik headless command:");
        log.info(commandLine);

        ProcessBuilder pB = new ProcessBuilder(commandLine);
        ilastikOptions.configureProcessBuilderEnvironment(pB);

        // run ilastik
        Process p;
        try {
            p = pB.start();

            // write ilastik output to IJ log
            IlastikUtilities.redirectOutputToLogService(p.getInputStream(), log, false);
            IlastikUtilities.redirectOutputToLogService(p.getErrorStream(), log, true);

            try {
                p.waitFor();
            } catch (InterruptedException cee) {
                log.warn("Execution got interrupted");
                p.destroy();
            }

            // 0 indicates successful execution
            if (p.exitValue() != 0) {
                log.error("ilastik crashed");
                throw new IllegalStateException("Execution of ilastik was not successful.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("ilastik finished successfully!");
    }
}
