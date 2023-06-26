package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;
import org.ilastik.ilastik4ij.executors.PixelClassification;
import org.ilastik.ilastik4ij.util.IOUtils;
import org.scijava.io.location.BytesLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PixelClassificationDemo {
    public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws IOException {
        final String ilastikPath = "/opt/ilastik-1.3.3post1-Linux/run_ilastik.sh";
        final String inputImagePath = "/2d_cells_apoptotic.tif";
        final String ilastikProjectPath = "/pixel_class_2d_cells_apoptotic.ilp";

        // Open ImageJ
        //
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();


        // Open input image
        //
        InputStream inputFileStream = PixelClassificationDemo.class.getResourceAsStream(inputImagePath);
        ByteBuffer byteBuffer = ByteBuffer.allocate(inputFileStream.available());
        while (inputFileStream.available() > 0) {
            byteBuffer.put((byte) inputFileStream.read());
        }
        final Dataset inputDataset = ij.scifio().datasetIO().open(
                new BytesLocation(byteBuffer.array(), "rawInputFile"));

        ij.ui().show(inputDataset);

        // Copy project file to tmp
        //
        InputStream projectFileStream = PixelClassificationDemo.class.getResourceAsStream(ilastikProjectPath);
        Path tmpIlastikProjectFile = Paths.get(IOUtils.getTemporaryFileName("pixel_class.ilp"));
        Files.copy(projectFileStream, tmpIlastikProjectFile);

        // Classify pixels
        //
        final PixelClassification prediction = new PixelClassification(
                new File(ilastikPath),
                tmpIlastikProjectFile.toFile(),
                ij.log(),
                ij.status(),
                4,
                1024
        );

        final ImgPlus<T> classifiedPixels = prediction.classifyPixels(inputDataset.getImgPlus(), PixelPredictionType.Probabilities);

        ImageJFunctions.show(classifiedPixels, "Probability maps");
    }

}
