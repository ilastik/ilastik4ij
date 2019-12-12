package org.ilastik.ilastik4ij;

import net.imagej.ImageJ;
import io.scif.services.DatasetIOService;
import java.io.IOException;
import net.imagej.Dataset;
import org.ilastik.ilastik4ij.ui.IlastikObjectClassificationCommand;
import org.ilastik.ilastik4ij.ui.IlastikPixelClassificationCommand;
import org.ilastik.ilastik4ij.ui.IlastikTrackingCommand;
import org.scijava.Context;

public class WorkflowDemos {
    public static void testPixelClassificationWorkflow() {
        // Launch ImageJ as usual.
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        Context context = ij.getContext();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);

        try {
            Dataset input = datasetIOService.open("example/2d_cells_apoptotic_1channel.tiff");
            ij.ui().show(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ij.command().run(IlastikPixelClassificationCommand.class, true);
    }

    public static void testObjectClassificationWorkflow() {
		// Launch ImageJ as usual.
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        Context context = ij.getContext();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);

        try {
            Dataset input = datasetIOService.open("example/2d_cells_apoptotic_1channel.tiff");
            ij.ui().show(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ij.command().run(IlastikObjectClassificationCommand.class, true);
    }

    public static void testTrackingWorkflow() {
    	// Launch ImageJ as usual.
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        Context context = ij.getContext();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);

        try {
            Dataset input = datasetIOService.open("example/CTC-FluoSim-06-RAW.tif");
            ij.ui().show(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Dataset input = datasetIOService.open("example/CTC-FluoSim-06-SEG.tif");
            ij.ui().show(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ij.command().run(IlastikTrackingCommand.class, true);
    }

    public static void main(String... args)
    {
        testTrackingWorkflow();
        testPixelClassificationWorkflow();
        testObjectClassificationWorkflow();
    }
}
