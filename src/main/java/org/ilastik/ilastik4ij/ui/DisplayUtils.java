package org.ilastik.ilastik4ij.ui;

import ij.IJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.ui.UIService;

public class DisplayUtils {
    public static final String GLASBEY_INVERTED = "glasbey_inverted";

    public static void applyLUT(String lutName) {
        try {
            IJ.run(lutName);
        } catch (Exception e) {
            throw new UnsupportedOperationException("LUT not found: " + lutName);
        }
    }

    public static void applyGlasbeyLUT() {
        applyLUT(GLASBEY_INVERTED);
    }

    public static void showOutput(UIService uiService, ImgPlus<?> predictions) {
        if (!uiService.isHeadless()) {
            ImageJFunctions.show((ImgPlus) predictions);
        }
    }
}
