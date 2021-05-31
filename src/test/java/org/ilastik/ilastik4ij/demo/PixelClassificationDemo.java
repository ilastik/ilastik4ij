package org.ilastik.ilastik4ij.demo;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.OptionsIlastik;
import org.ilastik.ilastik4ij.workflow.PixelClassificationCommand;

import java.io.File;

public final class PixelClassificationDemo extends WorkflowDemo {
    public static void main(String[] args) throws Exception {
        // Start ImageJ.
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Configure options.
        OptionsIlastik options = ij.options().getOptions(OptionsIlastik.class);
//        options.setExecutableFile(new File("ilastik-executable-paths-here"));
        options.setExecutableFile(new File(ILASTIK_PATH));

        // Get ilastik project file.
//        File project = new File("ilastik-project-file-path-here.ilp");
        File project = tempResource("/pixel_class_2d_cells_apoptotic.ilp").toFile();

        // Get raw data image.
//        Dataset rawData = ij.scifio().datasetIO().open("raw-data-image-path-here.tif");
        Dataset rawData = ij.scifio().datasetIO().open(
                tempResource("/2d_cells_apoptotic.tif").toString());
        ij.ui().show("Raw Input Data", rawData);

        // Configure and run the command.
        PixelClassificationCommand<?> cmd = new PixelClassificationCommand<>();
        cmd.setContext(ij.context());
        cmd.projectFileName = project;
        cmd.inputImage = rawData;
        cmd.pixelClassificationType = PixelClassificationCommand.PROBABILITIES;
        cmd.run();
        ij.ui().show("Predictions", cmd.predictions);
    }

    private PixelClassificationDemo() {
    }
}
