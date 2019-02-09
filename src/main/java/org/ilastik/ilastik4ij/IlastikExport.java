/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij;

import ij.IJ;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

/**
 * @author chaubold
 */
@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ilastik>Export HDF5")
public class IlastikExport implements Command {
    @Parameter
    private LogService log;
    @Parameter
    private StatusService statusService;
    @Parameter
    private DatasetService datasetService;

    // plugin parameters
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String message = "Axis order of the exported dataset: 'TZYXC'." +
            " Carefully configure scrollabale axes:"
            + " time frames, channels and z-slices can be easily mistaken."
            + " For changing their order go to"
            + " Image -> Properties.";

    @Parameter(label = "Image to save")
    private Dataset input;

    @Parameter(label = "HDF5 file where to export to", style = "save")
    private File hdf5FileName;

    @Parameter(label = "Dataset name", style = "text field")
    private String dataset = "data";

    @Parameter(label = "Compression level (0-9)", style = "spinner", min = "0", max = "9",
            description = "The best setting depends on the kind of data you are saving."
                    + " Segmentations can be compressed well (-> select 9), "
                    + "but raw data is best saved without compression (->0) for faster access.")
    private int compressionLevel = 0;


    @Override
    public void run() {
        String filename = hdf5FileName.getAbsolutePath();
        if (!filename.endsWith(".h5")) {
            IJ.error("Error: HDF5 export file must have '.h5' suffix");
        } else {
            saveImage(filename);
        }
    }

    private <T extends RealType<T>> void saveImage(String filename) {
        Instant start = Instant.now();

        @SuppressWarnings("unchecked")
        ImgPlus<T> imgPlus = (ImgPlus<T>) input.getImgPlus();
        new Hdf5DataSetWriter<>(imgPlus, filename, dataset, compressionLevel, log, statusService).write();

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info("Exporting image to HDF5 dataset took: " + timeElapsed);
    }

}
