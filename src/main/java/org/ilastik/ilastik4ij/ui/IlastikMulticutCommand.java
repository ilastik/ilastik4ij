package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.executors.Multicut;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Multicut")
public class IlastikMulticutCommand implements Command {

    @Parameter
    public LogService logService;

    @Parameter
    public StatusService statusService;

    @Parameter
    public OptionsService optionsService;

    @Parameter
    public UIService uiService;

    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;

    @Parameter(label = "Raw input image")
    public Dataset inputImage;

    @Parameter(label = "Boundary Prediction Image")
    public Dataset boundaryPredictionImage;

    @Parameter(type = ItemIO.OUTPUT)
    private ImgPlus<? extends NativeType<?>> predictions;

    public IlastikOptions ilastikOptions;

    /**
     * Run method that calls ilastik
     */
    @Override
    public void run() {

        if (ilastikOptions == null)
            ilastikOptions = optionsService.getOptions(IlastikOptions.class);

        try {
            runClassification();
        } catch (IOException e) {
            logService.error("Multicut command failed", e);
            throw new RuntimeException(e);
        }
    }

    private void runClassification() throws IOException {
        final Multicut multicut = new Multicut(ilastikOptions.getExecutableFile(), projectFileName, logService, statusService, ilastikOptions.getNumThreads(), ilastikOptions.getMaxRamMb());

        this.predictions = multicut.runMulticut(inputImage.getImgPlus(), boundaryPredictionImage.getImgPlus());
    }
}
