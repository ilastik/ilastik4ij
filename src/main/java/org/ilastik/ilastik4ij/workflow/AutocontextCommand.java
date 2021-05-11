package org.ilastik.ilastik4ij.workflow;

import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Autocontext Prediction")
public final class AutocontextCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(
            label = "Output type",
            choices = {PROBABILITIES, SEGMENTATION},
            style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
    public String outputType = PROBABILITIES;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        switch (outputType) {
            case PROBABILITIES:
                return Collections.singletonList("--export_source=Probabilities Stage 2");
            case SEGMENTATION:
                return Collections.singletonList("--export_source=Simple Segmentation Stage 2");
            default:
                throw new RuntimeException();
        }
    }
}
