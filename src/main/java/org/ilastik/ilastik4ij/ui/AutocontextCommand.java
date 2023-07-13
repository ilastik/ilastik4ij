package org.ilastik.ilastik4ij.ui;

import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Autocontext Prediction")
public final class AutocontextCommand<T extends NativeType<T>> extends CommandBase<T> {
    @Parameter(label = "Output type", choices = {ROLE_PROBABILITIES, ROLE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String AutocontextPredictionType;

    @Override
    protected List<String> workflowArgs() {
        if (ROLE_PROBABILITIES.equals(AutocontextPredictionType)) {
            return Collections.singletonList("--export_source=Probabilities Stage 2");
        }
        if (ROLE_SEGMENTATION.equals(AutocontextPredictionType)) {
            return Collections.singletonList("--export_source=Simple Segmentation Stage 2");
        }
        throw new IllegalStateException("Unexpected value: " + AutocontextPredictionType);
    }
}
