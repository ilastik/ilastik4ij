package org.ilastik.ilastik4ij.ui;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Export HDF5")
public class IlastikExportCommand implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Parameter(label = "Image to save")
    private Dataset input;

    @Override
    public void run() {
        SaveDialog sd = new SaveDialog("Select export path", "", ".h5");
        String hdf5FilePath = Paths.get(sd.getDirectory(), sd.getFileName()).toString();

        GenericDialog gd = new GenericDialog("Export to HDF5");
        gd.addMessage("Axis order of the exported data set: 'TZYXC'");
        gd.addStringField("ExportPath", hdf5FilePath, 100);
        gd.addStringField("DatasetName", "data");
        gd.addStringField("CompressionLevel", "0");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        hdf5FilePath = gd.getNextString();
        String datasetName = gd.getNextString();
        int compressionLevel = Integer.parseInt(gd.getNextString());

        if (!hdf5FilePath.endsWith(".h5")) {
            IJ.error("Error: HDF5 export file must have '.h5' suffix");
        } else {
            saveImage(hdf5FilePath, datasetName, compressionLevel);
        }
    }

    private <T extends RealType<T> & NativeType<T>> void saveImage(String hdf5FilePath, String datasetName, int compressionLevel) {
        Instant start = Instant.now();

        @SuppressWarnings("unchecked")
        ImgPlus<T> imgPlus = (ImgPlus<T>) input.getImgPlus();
        new Hdf5DataSetWriter<>(imgPlus, hdf5FilePath, datasetName,
                compressionLevel, logService, statusService).write();

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logService.info("Exporting image to HDF5 dataset took: " + timeElapsed);
    }

}
