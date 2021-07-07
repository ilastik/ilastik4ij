package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.executors.PixelClassification;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;

import static org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Pixel Classification Prediction")
public class IlastikPixelClassificationCommand extends DynamicCommand {

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

    @Parameter(label = "Output type", choices = {UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES, UiConstants.PIXEL_PREDICTION_TYPE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String pixelClassificationType;

    @Parameter(label = "Use Mask?", persist=false, initializer = "initUseMask")
    public boolean useMask=false;

    protected void initUseMask(){
        useMask = false;
        //resolveInput("useMask"); //this makes the input not be rendered -.-
    }

    @Parameter(
        label = "Prediction Mask",
        required = false,
        description = "An image with same dimensions as Raw Data, where the black pixels will be masked out of the predictions"
    )
    public Dataset predictionMask;

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
            logService.error("Pixel classification command failed", e);
            throw new RuntimeException(e);
        }
    }

    private void runClassification() throws IOException {
        final PixelClassification pixelClassification = new PixelClassification(ilastikOptions.getExecutableFile(),
                projectFileName, logService, statusService, ilastikOptions.getNumThreads(), ilastikOptions.getMaxRamMb());

        PixelPredictionType pixelPredictionType = PixelPredictionType.valueOf(pixelClassificationType);
        this.predictions = pixelClassification.classifyPixels(
            inputImage.getImgPlus(),
            useMask ? predictionMask.getImgPlus() : null,
            pixelPredictionType
        );

        // DisplayUtils.showOutput(uiService, predictions);
    }
}
