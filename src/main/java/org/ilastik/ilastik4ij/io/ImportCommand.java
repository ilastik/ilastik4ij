/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2025 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
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
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.toStringAxes;
import static org.scijava.ItemIO.OUTPUT;
import static org.scijava.ItemVisibility.MESSAGE;
import static org.scijava.widget.ChoiceWidget.LIST_BOX_STYLE;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Import HDF5")
public final class ImportCommand<T extends NativeType<T> & RealType<T>> extends DynamicCommand {
    private static final String NOT_SELECTED = "<i>Select file and dataset...</i>";

    @Parameter(label = "HDF5 file", persist = false, callback = "selectChanged")
    public File select;

    @SuppressWarnings("unused")
    private void selectChanged() {
        List<String> choices = Collections.singletonList(" ");
        Map<String, DatasetDescription> datasets = Collections.emptyMap();

        try {
            List<DatasetDescription> desc = Hdf5.datasets(select);
            if (!desc.isEmpty()) {
                choices = desc.stream().map(dd -> dd.path).collect(Collectors.toList());
                datasets = desc.stream().collect(Collectors.toMap(dd -> dd.path, dd -> dd));
            }
        } catch (Exception ignored) {
            // Can happen while user is modifying the file name.
            return;
        }

        getInfo().getMutableInput("datasetName", String.class).setChoices(choices);
        this.datasets = datasets;
    }

    // Non-empty choices and " " are ImageJ workarounds.
    @Parameter(
            label = "Dataset name",
            persist = false,
            callback = "datasetNameChanged",
            style = LIST_BOX_STYLE,
            choices = {" "})
    public String datasetName = " ";

    @SuppressWarnings("unused")
    private void datasetNameChanged() {
        DatasetDescription dd = datasetName.equals(" ") ? null : datasets.get(datasetName);
        if (dd != null) {
            type = dd.type.toString().toLowerCase();
            // Show dimensions and axes in the row-major order for backwards compatibility.
            dimensions = Arrays.toString(reversed(dd.dims));
            axisOrder = reversed(toStringAxes(dd.axes));
            pixelSize = dd.formatPixelSize().isEmpty() ? "(no pixel size metadata)" : dd.formatPixelSize();
        } else {
            type = NOT_SELECTED;
            dimensions = NOT_SELECTED;
            axisOrder = "";
            pixelSize = NOT_SELECTED;
        }
    }

    @Parameter(label = "Type", persist = false, required = false, visibility = MESSAGE)
    private String type = NOT_SELECTED;

    @Parameter(label = "Dimensions", persist = false, required = false, visibility = MESSAGE)
    private String dimensions = NOT_SELECTED;

    @Parameter(label = "Pixel size", persist = false, required = false, visibility = MESSAGE)
    private String pixelSize = NOT_SELECTED;

    @Parameter(
            label = "Axes",
            persist = false,
            required = false,
            description = "Row-major axes (last axis varies fastest)")
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
                // Reverse axis order back from row-major to column-major.
                output = Hdf5.readDataset(select, datasetName, toImagejAxes(reversed(axisOrder)));
                logger.info(String.format(
                        "Import dataset finished in %.3f seconds",
                        (System.nanoTime() - startTime) / 1e9));
            });
        }
    }
}
