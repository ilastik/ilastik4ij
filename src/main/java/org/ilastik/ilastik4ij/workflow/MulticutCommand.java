package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Multicut")
public final class MulticutCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Boundary Prediction Image")
    public Dataset boundaryPredictionImage;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        Path boundaryPredictionImagePath = tempDir.resolve("boundaryPredictionImage.h5");
        writeHdf5(boundaryPredictionImagePath, boundaryPredictionImage);

        return Arrays.asList(
                "--export_source=Multicut Segmentation",
                "--probabilities=" + boundaryPredictionImagePath);
    }
}
