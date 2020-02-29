package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.executors.Tracking;
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

import static org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Tracking")
public class IlastikTrackingCommand implements Command {

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

    @Parameter(label = "Pixel Probability or Segmentation image")
    public Dataset inputProbOrSegImage;

    @Parameter(label = "Second Input Type", choices = {UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES, UiConstants.PIXEL_PREDICTION_TYPE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String secondInputType = UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES;

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
            runTracking();
        } catch (IOException e) {
            logService.error("Tracking command failed", e);
            throw new RuntimeException(e);
        }
    }

    private void runTracking() throws IOException {
        final Tracking tracking = new Tracking(ilastikOptions.getExecutableFile(), projectFileName, logService,
                statusService, ilastikOptions.getNumThreads(), ilastikOptions.getMaxRamMb());

        this.predictions = tracking.trackObjects(inputImage.getImgPlus(), inputProbOrSegImage.getImgPlus(),
                PixelPredictionType.valueOf(secondInputType));

        DisplayUtils.showOutput(uiService, predictions);
    }
}
