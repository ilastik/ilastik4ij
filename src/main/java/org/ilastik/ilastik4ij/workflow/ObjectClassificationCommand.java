package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.util.CsvTables;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.GenericTable;
import org.scijava.widget.ChoiceWidget;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Object Classification Prediction")
public final class ObjectClassificationCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Pixel Probability or Segmentation image")
    public Dataset inputProbOrSegImage;

    @Parameter(
            label = "Second Input Type",
            choices = {PROBABILITIES, SEGMENTATION},
            style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
    public String secondInputType = PROBABILITIES;

    @Parameter(label = "Open object features table")
    public boolean needTable = false;

    @Parameter(type = ItemIO.OUTPUT)
    public GenericTable objectFeatures;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        Path inputProbOrSegImagePath = tempDir.resolve("inputProbOrSegImage.h5");
        writeHdf5(inputProbOrSegImagePath, inputProbOrSegImage);

        List<String> args = new ArrayList<>();

        switch (secondInputType) {
            case PROBABILITIES:
                args.add("--prediction_maps=" + inputProbOrSegImagePath);
                break;
            case SEGMENTATION:
                args.add("--segmentation_image=" + inputProbOrSegImagePath);
                break;
            default:
                throw new RuntimeException();
        }

        if (needTable) {
            args.add("--table_filename=" + tempDir.resolve("features.csv"));
        }

        return args;
    }

    @Override
    public void workflowDidRun(Path tempDir) throws Exception {
        if (needTable) {
            objectFeatures = CsvTables.read(Files.newBufferedReader(
                    tempDir.resolve("features_table.csv")));
        }
    }
}
