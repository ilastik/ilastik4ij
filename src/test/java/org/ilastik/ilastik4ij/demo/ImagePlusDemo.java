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
