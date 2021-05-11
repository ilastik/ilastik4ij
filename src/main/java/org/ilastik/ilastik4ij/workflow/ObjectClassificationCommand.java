package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Object Classification Prediction")
public final class ObjectClassificationCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Pixel probability or segmentation image")
    public Dataset input2;

    @Parameter(
            label = "Second input type",
            choices = {PROBABILITIES, SEGMENTATION},
            style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
    public String input2Type = PROBABILITIES;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        Path input2Path = tempDir.resolve("input2.h5");
        writeHdf5(input2Path, input2);

        switch (input2Type) {
            case PROBABILITIES:
                return Collections.singletonList("--prediction_maps=" + input2Path);
            case SEGMENTATION:
                return Collections.singletonList("--segmentation_image=" + input2Path);
            default:
                throw new RuntimeException();
        }
    }
}
