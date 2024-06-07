/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2024 N/A
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
package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Tracking")
public final class TrackingCommand<T extends NativeType<T> & RealType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Pixel Probability or Segmentation image")
    public Dataset inputProbOrSegImage;

    @Parameter(label = "Second Input Type", choices = {ROLE_PROBABILITIES, ROLE_SEGMENTATION}, style = "radioButtonHorizontal")
    public String secondInputType = ROLE_PROBABILITIES;

    @Override
    protected List<String> workflowArgs() {
        return Collections.singletonList("--export_source=Tracking-Result");
    }

    @Override
    protected Map<String, Dataset> workflowInputs() {
        if (ROLE_PROBABILITIES.equals(secondInputType)) {
            return Collections.singletonMap("prediction_maps", inputProbOrSegImage);
        }
        if (ROLE_SEGMENTATION.equals(secondInputType)) {
            return Collections.singletonMap("binary_image", inputProbOrSegImage);
        }
        throw new IllegalStateException("Unexpected value: " + secondInputType);
    }
}
