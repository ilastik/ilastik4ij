package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Multicut")
public final class MulticutCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Boundary Prediction Image")
    public Dataset boundaryPredictionImage;

    @Override
    protected List<String> workflowArgs() {
        return Collections.singletonList("--export_source=Multicut Segmentation");
    }

    @Override
    protected Map<String, Dataset> workflowInputs() {
        return Collections.singletonMap("probabilities", boundaryPredictionImage);
    }
}
