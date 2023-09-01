package org.ilastik.ilastik4ij.io;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.function.LongConsumer;

import static org.ilastik.ilastik4ij.util.ImgUtils.DEFAULT_STRING_AXES;
import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes;

@Plugin(
        type = Command.class,
        headless = true, menuPath = "Plugins>ilastik>Export HDF5",
        attrs = @Attr(name = "resolve-optional"))
public final class ExportCommand<T extends NativeType<T> & RealType<T>> extends ContextCommand {
    @Parameter(label = "Image to save")
    public Dataset input;

    @Parameter(label = "Export path")
    public File exportPath;

    // Reverse axis order from column-major to row-major for backwards compatibility.
    // The default axis order here is kept for backwards compatibility.
    @Parameter(
            label = "Axis order",
            description = "Row-major axes (last axis varies fastest)",
            required = false)
    public String axisOrder = reversed("cxyzt");

    @Parameter(label = "Dataset name")
    public String datasetName = "/data";

    @Parameter(label = "Compression level", min = "0", max = "9")
    public int compressionLevel = 0;

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
        axisOrder = axisOrder.toLowerCase(Locale.ROOT);
        if (axisOrder.indexOf('x') < 0 || axisOrder.indexOf('y') < 0) {
            throw new IllegalArgumentException("Axes 'x' and 'y' are mandatory");
        }
        if (axisOrder.chars().anyMatch(c -> DEFAULT_STRING_AXES.indexOf(c) < 0)) {
            throw new IllegalArgumentException(String.format(
                    "One or more axes in '%s' are unsupported; supported axes: '%s'",
                    axisOrder,
                    reversed(DEFAULT_STRING_AXES)));
        }

        @SuppressWarnings("unchecked")
        ImgPlus<T> img = (ImgPlus<T>) input.getImgPlus();

        long totalBytes = img.size() * img.firstElement().getBitsPerPixel() / 8;
        if (totalBytes >> 20 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image is too large to export");
        }
        int totalMegabytes = (int) (totalBytes >> 20);
        LongConsumer updateStatusBar = (bytes) ->
                statusService.showStatus((int) (bytes >> 20), totalMegabytes, "Exporting to HDF5");

        logger.info("Starting dataset export to " + exportPath);
        long startTime = System.nanoTime();
        Hdf5.writeDataset(
                exportPath,
                datasetName,
                img,
                compressionLevel,
                // Reverse axis order back from row-major to column-major.
                toImagejAxes(reversed(axisOrder)),
                updateStatusBar);
        statusService.clearStatus();
        logger.info(String.format(
                "Finished dataset export in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
    }
}
