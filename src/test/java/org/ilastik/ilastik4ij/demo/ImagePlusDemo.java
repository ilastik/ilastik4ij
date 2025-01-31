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

import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Shows how to convert from and to {@link ImagePlus}.
 */
public final class ImagePlusDemo {
    public static void main(String[] args) throws IOException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        fromImagePlus(ij);
        toImagePlus(ij);
    }

    private static <T extends NativeType<T> & RealType<T>> void fromImagePlus(ImageJ ij) {
        ImagePlus imagePlus = IJ.openImage(fromResource("/2d_cells_apoptotic.tif"));
        imagePlus.setTitle("ImagePlus");  // Change title just for the demonstration.
        imagePlus.show();

        ImgPlus<T> imgPlus = ImagePlusAdapter.wrapImgPlus(imagePlus);
        ij.ui().show("ImgPlus", imgPlus);

        DefaultDataset dataset = new DefaultDataset(ij.context(), imgPlus);
        ij.ui().show("Dataset", dataset);
    }

    private static <T extends NativeType<T> & RealType<T>> void toImagePlus(ImageJ ij) throws IOException {
        Dataset dataset = ij.scifio().datasetIO().open(fromResource("/2d_cells_apoptotic.tif"));
        ij.ui().show("Dataset", dataset);

        @SuppressWarnings("unchecked")
        ImgPlus<T> imgPlus = (ImgPlus<T>) dataset.getImgPlus();
        ij.ui().show("ImgPlus", imgPlus);

        ImagePlus imagePlus = ImageJFunctions.wrap(imgPlus, imgPlus.getName());
        imagePlus.setTitle("ImagePlus"); // Change title just for the demonstration.
        imagePlus.show();
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

    private ImagePlusDemo() {
        throw new AssertionError();
    }
}
