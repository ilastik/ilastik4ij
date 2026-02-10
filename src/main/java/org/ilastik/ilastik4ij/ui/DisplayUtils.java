/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2026 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
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
