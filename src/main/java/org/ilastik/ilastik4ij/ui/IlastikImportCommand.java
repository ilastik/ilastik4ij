package org.ilastik.ilastik4ij.ui;

import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;

interface DatasetLoader {
    public <T extends RealType<T> & NativeType<T>> void loadDataset(String hdf5FilePath, String datasetName, String axisOrder);
}

@Plugin(type = Command.class, menuPath = "Plugins>ilastik>Import HDF5")
public class IlastikImportCommand implements Command, DatasetLoader  {
    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private UIService uiService;

    private static IlastikImportDialog dialog = null;
    
    public void run() {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null) {
                dialog = new IlastikImportDialog(logService, uiService, this);
            }
            dialog.setVisible(true);
        });
        logService.info("Done loading HDF5 file!");
    }

    public <T extends RealType<T> & NativeType<T>> void loadDataset(String hdf5FilePath, String datasetName, String axisOrder) {
        assert hdf5FilePath != null;
        assert datasetName != null;
        assert axisOrder != null;
        axisOrder = axisOrder.toLowerCase();

        Instant start = Instant.now();

        ImgPlus<T> imgPlus = new Hdf5DataSetReader<T>(hdf5FilePath, datasetName,
                axisOrder, logService, statusService).read();
        ImageJFunctions.show(imgPlus);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logService.info("Loading HDF5 dataset took: " + timeElapsed + "ms");
    }


}
