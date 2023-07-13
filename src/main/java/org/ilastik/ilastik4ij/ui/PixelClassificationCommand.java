package org.ilastik.ilastik4ij.ui;

import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Pixel Classification Prediction")
public final class PixelClassificationCommand<T extends NativeType<T>> extends CommandBase<T> {
    @Parameter(label = "Output type", choices = {ROLE_PROBABILITIES, ROLE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String pixelClassificationType;

    @Override
    protected List<String> workflowArgs() {
        if (ROLE_PROBABILITIES.equals(pixelClassificationType)) {
            return Collections.singletonList("--export_source=Probabilities");
        }
        if (ROLE_SEGMENTATION.equals(pixelClassificationType)) {
            return Collections.singletonList("--export_source=Simple Segmentation");
        }
        throw new IllegalStateException("Unexpected value: " + pixelClassificationType);
    }
}
