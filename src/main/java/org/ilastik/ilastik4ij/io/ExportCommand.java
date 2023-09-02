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
import java.util.List;
import java.util.function.LongConsumer;

import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Export HDF5")
public final class ExportCommand<T extends NativeType<T> & RealType<T>> extends ContextCommand {
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
            persist = false,
            required = false,
            description = "Row-major axes (last axis varies fastest)")
    public final String axisOrder = "tzyxc";

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
        List<AxisType> axes = toImagejAxes(reversed(axisOrder));

        long totalBytes = img.size() * img.firstElement().getBitsPerPixel() / 8;
        if (totalBytes >> 20 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image is too large to export");
        }
        int totalMegabytes = (int) (totalBytes >> 20);
        LongConsumer updateStatusBar = (bytes) ->
                statusService.showStatus((int) (bytes >> 20), totalMegabytes, "Exporting to HDF5");

        logger.info("Starting dataset export to " + exportPath);
        long startTime = System.nanoTime();
        Hdf5.writeDataset(exportPath, datasetName, img, compressionLevel, axes, updateStatusBar);
        statusService.clearStatus();
        logger.info(String.format(
                "Finished dataset export in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
    }
}
