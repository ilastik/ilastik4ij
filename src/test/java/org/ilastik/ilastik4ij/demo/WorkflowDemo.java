package org.ilastik.ilastik4ij.demo;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.ilastik.ilastik4ij.ui.OptionsIlastik;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Show how to use the ilastik plugin.
 * <p>
 * The main methods of this class and it's subclasses contain minimum
 * working examples for some workflows, except for this class, which
 * just opens a dataset in GUI, so it can be used as a launcher for
 * interactive testing and exploration.
 * Commented lines of code show how you would do similar things outside
 * of these demos.
 * <p>
 * If you want to run a workflow without GUI, just omit all calls to
 * {@code ij.ui()}.
 * <p>
 * ilastik executable path can be configured either through the
 * {@code ilastik.path} system property, or just by temporarily
 * changing the constant. If you are doing the latter, make sure
 * that do <em>do not</em> accidentally commit this change.
 */
public abstract class WorkflowDemo {
    // Configure ilastik executable path.
//    protected static final String ILASTIK_PATH = "ilastik-executable-path-here";
    protected static final String ILASTIK_PATH = System.getProperty("ilastik.path");
    static {
        if (ILASTIK_PATH == null) {
            throw new RuntimeException("property ilastik.path is not set");
        }
    }

    public static void main(String[] args) throws Exception {
        // Start ImageJ.
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Configure options.
        OptionsIlastik options = ij.options().getOptions(OptionsIlastik.class);
//        options.setExecutableFile(new File("ilastik-executable-paths-here"));
        options.setExecutableFile(new File(ILASTIK_PATH));

        // Get raw data image.
//        Dataset rawData = ij.scifio().datasetIO().open("raw-data-image-path-here.tif");
        Dataset rawData = ij.scifio().datasetIO().open(
                tempResource("/2d_cells_apoptotic.tif").toString());
        ij.ui().show("Raw Input Data", rawData);
    }

    /**
     * Copy resource into a temporary file.
     *
     * @throws IllegalArgumentException resource does not exist
     */
    protected static Path tempResource(String name) throws IOException {
        String fileName = Paths.get(name).getFileName().toString();
        String prefix = fileName + '_';
        String extension = "";

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            prefix = fileName.substring(0, extensionIndex) + '_';
            extension = fileName.substring(extensionIndex);
        }

        try (InputStream resourceStream = WorkflowDemo.class.getResourceAsStream(name)) {
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource " + name + " does not exist");
            }

            Path tempFile = Files.createTempFile(prefix, extension);
            tempFile.toFile().deleteOnExit();

            Files.copy(new BufferedInputStream(resourceStream), tempFile,
                    StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}
