package org.ilastik.ilastik4ij.io;

import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.DatasetDescription;
import org.ilastik.ilastik4ij.hdf5.Hdf5;
import org.ilastik.ilastik4ij.util.StatusBar;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.toStringAxes;
import static org.scijava.ItemIO.OUTPUT;
import static org.scijava.ItemVisibility.MESSAGE;
import static org.scijava.widget.ChoiceWidget.LIST_BOX_STYLE;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Import HDF5")
public final class ImportCommand<T extends NativeType<T> & RealType<T>> extends DynamicCommand {
    private static final String NOT_SELECTED = "<i>Select file and dataset...</i>";

    @Parameter(label = "HDF5 file", persist = false, callback = "fileChanged")
    public File select;

    @SuppressWarnings("unused")
    private void fileChanged() {
        MutableModuleItem<String> item = getInfo().getMutableInput("datasetName", String.class);
        try {
            List<DatasetDescription> descriptions = Hdf5.datasets(select);
            item.setChoices(descriptions.stream().map(dd -> dd.path).collect(Collectors.toList()));
            datasets = descriptions.stream().collect(Collectors.toMap(dd -> dd.path, dd -> dd));
        } catch (Exception ignored) {
            item.setChoices(Collections.singletonList(" "));
            datasets = Collections.emptyMap();
        }
    }

    // Non-empty choices and " " are ImageJ workarounds.
    @Parameter(
            label = "Dataset name",
            persist = false,
            callback = "datasetChanged",
            style = LIST_BOX_STYLE,
            choices = {" "})
    public String datasetName = " ";

    @SuppressWarnings("unused")
    private void datasetChanged() {
        DatasetDescription dd = datasets.get(datasetName);
        type = dd != null ? dd.type.toString().toLowerCase() : NOT_SELECTED;
        dimensions = dd != null ? Arrays.toString(dd.dims) : NOT_SELECTED;
        axisOrder = dd != null ? toStringAxes(dd.axes) : "";
    }

    @Parameter(label = "Type", persist = false, visibility = MESSAGE)
    private String type = NOT_SELECTED;

    @Parameter(label = "Dimensions", persist = false, visibility = MESSAGE)
    private String dimensions = NOT_SELECTED;

    @Parameter(label = "Axes", persist = false)
    public String axisOrder = "";

    @Parameter(label = "Output image", persist = false, type = OUTPUT)
    public ImgPlus<T> output;

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    private Map<String, DatasetDescription> datasets = Collections.emptyMap();

    @Override
    public void run() {
        Logger logger = logService.subLogger(getClass().getName());
        logger.info(String.format(
                "Import dataset %s/%s starting",
                select,
                datasetName.replaceFirst("/+", "")));

        try (StatusBar statusBar = new StatusBar(statusService, 300)) {
            statusBar.withSpinner("Importing dataset", () -> {
                long startTime = System.nanoTime();
                output = Hdf5.readDataset(select, datasetName, toImagejAxes(axisOrder));
                logger.info(String.format(
                        "Import dataset finished in %.3f seconds",
                        (System.nanoTime() - startTime) / 1e9));
            });
        }
    }
}
