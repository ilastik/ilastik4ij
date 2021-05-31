package org.ilastik.ilastik4ij.workflow;

import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Pixel Classification Prediction")
public final class PixelClassificationCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(
            label = "Output type",
            choices = {PROBABILITIES, SEGMENTATION},
            style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
    public String pixelClassificationType = PROBABILITIES;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        switch (pixelClassificationType) {
            case PROBABILITIES:
                return Collections.singletonList("--export_source=Probabilities");
            case SEGMENTATION:
                return Collections.singletonList("--export_source=Simple Segmentation");
            default:
                throw new RuntimeException();
        }
    }
}
