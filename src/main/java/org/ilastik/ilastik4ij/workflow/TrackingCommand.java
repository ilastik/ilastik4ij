package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ChoiceWidget;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Tracking")
public final class TrackingCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Pixel Probability or Segmentation image")
    public Dataset inputProbOrSegImage;

    @Parameter(
            label = "Second Input Type",
            choices = {PROBABILITIES, SEGMENTATION},
            style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
    public String secondInputType = PROBABILITIES;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        Path inputProbOrSegImagePath = tempDir.resolve("inputProbOrSegImage.h5");
        writeHdf5(inputProbOrSegImagePath, inputProbOrSegImage);

        List<String> args = new ArrayList<>();
        args.add("--export_source=Tracking-Result");

        switch (secondInputType) {
            case PROBABILITIES:
                args.add("--prediction_maps=" + inputProbOrSegImagePath);
                break;
            case SEGMENTATION:
                args.add("--binary_image=" + inputProbOrSegImagePath);
                break;
            default:
                throw new RuntimeException();
        }

        return args;
    }
}
