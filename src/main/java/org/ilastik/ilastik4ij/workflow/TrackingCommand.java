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

        List<String> args = new ArrayList<>();
        args.add("--export_source=Tracking-Result");

        switch (input2Type) {
            case PROBABILITIES:
                args.add("--prediction_maps=" + input2Path);
                break;
            case SEGMENTATION:
                args.add("--binary_image=" + input2Path);
                break;
            default:
                throw new RuntimeException();
        }

        return args;
    }
}
