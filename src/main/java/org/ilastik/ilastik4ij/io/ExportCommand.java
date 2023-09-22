/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2023 N/A
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

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.LongConsumer;

import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.totalMegabytes;
import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Export HDF5")
public final class ExportCommand<T extends NativeType<T> & RealType<T>> extends ContextCommand {
    private static final String AXIS_ORDER = "tzyxc";

    @Parameter(label = "Image to save")
    public Dataset input;

    @Parameter(label = "Export path")
    public File exportPath;

    @Parameter(label = "Dataset name")
    public String datasetName = "/data";

    @Parameter(label = "Compression level", min = "0", max = "9")
    public int compressionLevel = 0;

    @Parameter(
            label = "Axes",
            description = "Row-major axes (last axis varies fastest)",
            visibility = MESSAGE)
    private String axisOrder = String.format("<i>%s</i>", AXIS_ORDER);

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Override
    public void run() {
        Logger logger = logService.subLogger(getClass().getName());

        if (!exportPath.getPath().endsWith(".h5")) {
            throw new IllegalArgumentException("HDF5 export file must have '.h5' suffix");
        }

        @SuppressWarnings("unchecked")
        ImgPlus<T> img = (ImgPlus<T>) input.getImgPlus();
        List<AxisType> axes = toImagejAxes(reversed(AXIS_ORDER));

        int total = totalMegabytes(Collections.singletonList(img));
        LongConsumer updateStatusBar = (bytes) ->
                statusService.showStatus((int) (bytes >> 20), total, "Exporting to HDF5");

        logger.info("Starting dataset export to " + exportPath);
        long startTime = System.nanoTime();
        Hdf5.writeDataset(exportPath, datasetName, img, compressionLevel, axes, updateStatusBar);
        statusService.clearStatus();
        logger.info(String.format(
                "Finished dataset export in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
    }
}
