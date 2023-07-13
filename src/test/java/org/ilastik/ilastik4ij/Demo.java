package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.ObjectClassificationCommand;
import org.ilastik.ilastik4ij.ui.PixelClassificationCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class Demo {
    public static void main(String[] args) throws Exception {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // If you want to change ilastik options, e.g. executable path:
        // Options opts = ij.options().getOptions(Options.class);
        // opts.load();
        // opts.executableFile = new File("/path/to/your/local/ilastik/executable");
        // opts.save();

        pixelClassification(ij);
        objectClassification(ij);
    }

    @SuppressWarnings("unused")
    private static void pixelClassification(ImageJ ij) throws Exception {
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
    private static void objectClassification(ImageJ ij) throws Exception {
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
    private static String fromResource(String resourcePath) throws IOException {
        Path target = Files.createTempFile("", resourcePath.replace('/', '-'));
        try (InputStream in = Demo.class.getResourceAsStream(resourcePath)) {
            Files.copy(Objects.requireNonNull(in), target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target.toString();
    }

    private Demo() {
    }
}
