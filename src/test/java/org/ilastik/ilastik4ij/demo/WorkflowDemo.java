/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2025 N/A
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
package org.ilastik.ilastik4ij.demo;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.workflow.ObjectClassificationCommand;
import org.ilastik.ilastik4ij.workflow.PixelClassificationCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Shows how to run ilastik workflows.
 */
public final class WorkflowDemo {
    public static void main(String[] args) throws Exception {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // If you want to change ilastik options, e.g. executable path:
        // IlastikOptions opts = ij.options().getOptions(IlastikOptions.class);
        // opts.load();
        // opts.executableFile = new File("/path/to/your/local/ilastik/executable");
        // opts.save();

        pixelClassification(ij);
        objectClassification(ij);
    }

    @SuppressWarnings("unused")
    private static void pixelClassification(ImageJ ij) throws IOException {
        Dataset inputImage = ij.scifio().datasetIO().open(fromResource("/2d_cells_apoptotic.tif"));
        ij.ui().show(inputImage);

        PixelClassificationCommand<?> cmd = new PixelClassificationCommand<>();
        cmd.setContext(ij.context());
        cmd.projectFileName = new File(fromResource("/pixel_class_2d_cells_apoptotic.ilp"));
        cmd.inputImage = inputImage;
        cmd.pixelClassificationType = PixelClassificationCommand.ROLE_SEGMENTATION;
        cmd.run();

        ij.ui().show(cmd.predictions);
    }

    @SuppressWarnings("unused")
    private static void objectClassification(ImageJ ij) throws IOException {
        Dataset inputImage = ij.scifio().datasetIO().open(fromResource("/2d_cells_apoptotic.tif"));
        ij.ui().show(inputImage);

        Dataset inputProbImage = ij.scifio().datasetIO().open(fromResource("/2d_cells_apoptotic_1channel-data_Probabilities.tif"));
        ij.ui().show(inputProbImage);

        ObjectClassificationCommand<?> cmd = new ObjectClassificationCommand<>();
        cmd.setContext(ij.context());
        cmd.projectFileName = new File(fromResource("/obj_class_2d_cells_apoptotic.ilp"));
        cmd.inputImage = inputImage;
        cmd.inputProbOrSegImage = inputProbImage;
        cmd.secondInputType = ObjectClassificationCommand.ROLE_PROBABILITIES;
        cmd.run();

        ij.ui().show(cmd.predictions);
    }

    /**
     * Copy resource to a temporary file and return the file path.
     * <p>
     * This function is used here just for the demonstration purposes.
     * If you read files from a local disk, just use the path directly.
     */
    private static String fromResource(String resourcePath) {
        try (InputStream in = WorkflowDemo.class.getResourceAsStream(resourcePath)) {
            Path target = Files.createTempFile("", resourcePath.replace('/', '-'));
            Files.copy(Objects.requireNonNull(in), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private WorkflowDemo() {
        throw new AssertionError();
    }
}
