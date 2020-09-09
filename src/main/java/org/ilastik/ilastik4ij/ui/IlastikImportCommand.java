package org.ilastik.ilastik4ij.ui;

import ij.Macro;
import ij.plugin.frame.Recorder;
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
import java.util.Objects;

@Plugin(type = Command.class, menuPath = "Plugins>ilastik>Import HDF5")
public class IlastikImportCommand implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private UIService uiService;

    public void run() {
        IlastikImportModel importModel = new IlastikImportModel(logService);
        IlastikImportMacroOptionsParser.ParseResult options = IlastikImportMacroOptionsParser.parseOptions(Macro.getOptions());

        importModel.setPath(options.path);
        importModel.setDatasetPath(options.datasetName);
        importModel.setAxisTags(options.axisOrder);

        if (!importModel.isValid()) {
            IlastikImportDialog dialog = new IlastikImportDialog(importModel, logService, uiService);
            importModel.fireInitialProperties();
            dialog.setVisible(true);
            if (dialog.wasCancelled()) {
                logService.info("Cancel loading HDF5 file!");
                return;
            }
        }

        this.loadDataset(importModel.getPath(), importModel.getDatasetPath(), importModel.getAxisTags());

        if (Recorder.record) {
            Recorder.recordOption("select", importModel.getPath());
            Recorder.recordOption("datasetname", importModel.getDatasetPath());
            Recorder.recordOption("axisorder", importModel.getAxisTags());
        }

    }

    private <T extends RealType<T> & NativeType<T>> void loadDataset(String hdf5FilePath, String datasetName, String axisOrder) {
        Objects.requireNonNull(hdf5FilePath);
        Objects.requireNonNull(datasetName);
        Objects.requireNonNull(axisOrder);
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
