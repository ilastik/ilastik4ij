/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij;

import java.awt.event.ActionListener;
import java.io.File;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import net.imagej.Dataset;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriterFromImgPlus;
import org.scijava.ItemVisibility;

/**
 *
 * @author chaubold
 */
@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ilastik>Export HDF5")
public class IlastikExport implements Command {

    // needed services:
    @Parameter
    LogService log;

    @Parameter
    DatasetService datasetService;
    
    @Parameter
    OptionsService optionsService;

    // plugin parameters
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private String message = "Be careful to properly configure axes.\nScrollabale axes"
					+ " time frames, channels and z-slices can be easily mistaken."
                    + "\nFor changing their order go to"
					+ " Image -> Properties";
    
    @Parameter(label = "HDF5 file where to export to", style="save")
    private File hdf5FileName;
    
    @Parameter(label = "Image to save")
    private Dataset input;

    @Parameter(label = "Compression level (0-9)", style="spinner", min = "0", max="9",
               description = "The best setting depends on the kind of data you are saving."
                             + " Segmentations can be compressed well (-> select 9), "
                             + "but raw data is best saved without compression (->0) for faster access.")
    private int compressionLevel = 0;
    
    @Override
    public void run() {
        new Hdf5DataSetWriterFromImgPlus(input.getImgPlus(), hdf5FileName.getAbsolutePath(), "data", compressionLevel, log).write();
    }
    
}
