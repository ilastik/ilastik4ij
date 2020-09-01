package org.ilastik.ilastik4ij.ui;

import ij.Macro;
import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.DatasetService;
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
    public DatasetService datasetService;

    @Parameter
    public UIService uiService;

    @Parameter(label = "Raw input image")
    public Dataset inputImage;

    /*
    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;


    @Parameter(label = "Output type", choices = {UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES, UiConstants.PIXEL_PREDICTION_TYPE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String pixelClassificationType;

    @Parameter(label = "Use Mask?", persist=false, initializer = "initUseMask")
    public boolean useMask=false;

    protected void initUseMask(){
        useMask = false;
        //resolveInput("useMask"); //this makes the input not be rendered -.-
    }
    */


    /*
    @Parameter(
        label = "Prediction Mask",
        required = false,
        description = "An image with same dimensions as Raw Data, where the black pixels will be masked out of the predictions"
    )
    public Dataset predictionMask;

    */
    @Parameter(type = ItemIO.OUTPUT)
    private ImgPlus<? extends NativeType<?>> predictions;

    public IlastikOptions ilastikOptions;

    private IlastikPixelClassificationModel createModel() {
        IlastikPixelClassificationModel model = new IlastikPixelClassificationModel(logService);
        model.setRawInput(inputImage);

        String macroOptions = Macro.getOptions();

        if (macroOptions != null) {
            String projectFilePath = Macro.getValue(macroOptions, "projectfilename", "");
            model.setIlastikProjectFile(new File(projectFilePath));
            model.setOutputType(Macro.getValue(macroOptions, "pixelclassificationtype", ""));

            String predictionMaskName = Macro.getValue(macroOptions, "predictionmask", "");
            for (Dataset ds : datasetService.getDatasets()) {
                if (ds.getName().equals(predictionMaskName)) {
                    model.setPredictionMask(ds);
                    break;
                }
            }
        }

        return model;
    }

    @Override
    public void run() {
        if (ilastikOptions == null)
            ilastikOptions = optionsService.getOptions(IlastikOptions.class);

        IlastikPixelClassificationModel model = createModel();
        IlastikPixelClassificationDialog dialog = new IlastikPixelClassificationDialog(logService, uiService, datasetService, model);
        model.fireInitialProperties();
        dialog.setVisible(true);
        if (dialog.wasCancelled()) {
            return;
        }

        try {
            runClassification(model);
        } catch (IOException e) {
            logService.error("Pixel classification command failed", e);
            throw new RuntimeException(e);
       }
    }

    private void runClassification(IlastikPixelClassificationModel model) throws IOException {
        final PixelClassification pixelClassification = new PixelClassification(ilastikOptions.getExecutableFile(),
                model.getIlastikProjectFile(), logService, statusService, ilastikOptions.getNumThreads(), ilastikOptions.getMaxRamMb());

        PixelPredictionType pixelPredictionType = PixelPredictionType.valueOf(model.getOutputType());

        ImgPlus predMaskImg = null;
        if (model.getPredictionMask() != null) {
            predMaskImg = model.getPredictionMask().getImgPlus();
        }

        this.predictions = pixelClassification.classifyPixels(
            inputImage.getImgPlus(),
            predMaskImg,
            pixelPredictionType
        );
    }
}
