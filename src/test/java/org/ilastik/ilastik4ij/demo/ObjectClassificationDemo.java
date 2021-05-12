package org.ilastik.ilastik4ij.demo;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.OptionsIlastik;
import org.ilastik.ilastik4ij.workflow.ObjectClassificationCommand;

import java.io.File;

public final class ObjectClassificationDemo extends WorkflowDemo {
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
        File project = tempResource("/obj_class_2d_cells_apoptotic.ilp").toFile();

        // Get raw data image.
//        Dataset rawData = ij.scifio().datasetIO().open("raw-data-image-path-here.tif");
        Dataset rawData = ij.scifio().datasetIO().open(
                tempResource("/2d_cells_apoptotic.tif").toString());
        ij.ui().show("Raw Input Data", rawData);

        // Get probability map.
//        Dataset probabilityMap = ij.scifio().datasetIO().open("probability-map-path-here.tif");
        Dataset probabilityMap = ij.scifio().datasetIO().open(
                tempResource("/2d_cells_apoptotic_1channel-data_Probabilities.tif").toString());
        ij.ui().show("Probability Map", probabilityMap);

        // Configure and run the command.
        ObjectClassificationCommand<?> cmd = new ObjectClassificationCommand<>();
        cmd.setContext(ij.context());
        cmd.project = project;
        cmd.rawData = rawData;
        cmd.input2 = probabilityMap;
        cmd.input2Type = ObjectClassificationCommand.PROBABILITIES;
        cmd.run();
        ij.ui().show("Predictions", cmd.predictions);
    }

    private ObjectClassificationDemo() {
    }
}
