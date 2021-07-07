package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;

import java.io.File;
import java.util.concurrent.Future;

public class WorkflowJavaDemo {
    public static void main(String[] args) throws Exception {
        String ilastikPath = System.getProperty("ilastik.path");
        if (ilastikPath == null) {
            throw new RuntimeException("System property ilastik.path is not configured");
        }

        ImageJ ij = new ImageJ();
        ij.launch(args);

        IlastikOptions opts = ij.options().getOptions(IlastikOptions.class);
        opts.setExecutableFile(new File(ilastikPath));

        Project project = (Project) ij.io().open("/pixel_class_2d_cells_apoptotic.ilp");
        Dataset rawData = ij.scifio().datasetIO().open(resourcePath("/2d_cells_apoptotic.tif"));

        CommandInfo workflow = ij.command().getCommand(Workflow.class);
        Future<Module> future = ij.module().run(workflow, true,
                "project", project,
                "raw_data", rawData,
                "export_source", "Probabilities");

        Dataset output = (Dataset) future.get().getOutput("output");
        assert output != null;
    }

    private static String resourcePath(String path) {
        return WorkflowJavaDemo.class.getResource(path).getPath();
    }
}
