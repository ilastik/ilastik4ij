package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.ui.IlastikPixelClassificationCommand;
import org.ilastik.ilastik4ij.ui.UiConstants;

import java.io.File;
import java.io.IOException;

public class PixelClassificationCommandDemo {
    public static void main(String[] args) throws IOException {
        // Configure paths
        //
        final String ilastikPath = "/opt/ilastik-1.3.3post1-Linux/run_ilastik.sh";
        final String inputImagePath = "/home/adrian/workspace/ilastik4ij/src/test/resources/2d_cells_apoptotic.tif";
        final String ilastikProjectPath = "/home/adrian/workspace/ilastik4ij/src/test/resources/pixel_class_2d_cells_apoptotic.ilp";

        // Run test
        //
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Open input image
        //
        final Dataset inputDataset = ij.scifio().datasetIO().open(inputImagePath);
        ij.ui().show(inputDataset);

        // Configure options
        //
        final IlastikOptions options = new IlastikOptions();
        options.setExecutableFile(new File(ilastikPath));

        // Classify pixels
        //
        final IlastikPixelClassificationCommand command = new IlastikPixelClassificationCommand();
        command.logService = ij.log();
        command.statusService = ij.status();
        command.uiService = ij.ui();
        command.optionsService = ij.options();
        command.ilastikOptions = options;
        /*
        command.pixelClassificationType = UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES;
        command.inputImage = inputDataset;
        command.projectFileName = new File(ilastikProjectPath);
         */
        command.run();
    }
}
