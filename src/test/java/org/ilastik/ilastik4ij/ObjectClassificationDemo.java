package org.ilastik.ilastik4ij;

import io.scif.io.ByteArrayHandle;
import io.scif.io.IRandomAccess;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.ilastik.ilastik4ij.executors.ObjectClassification;
import org.ilastik.ilastik4ij.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;

public class ObjectClassificationDemo {
    public static void main(String[] args) throws IOException {
        final String ilastikPath = "/opt/ilastik-1.3.3post1-Linux/run_ilastik.sh";
        final String inputImagePath = "/2d_cells_apoptotic.tif";
        final String inputSegmPath = "/2d_cells_apoptotic_1channel-data_Probabilities.tif";
        final String ilastikProjectPath = "/obj_class_2d_cells_apoptotic.ilp";

        // Open ImageJ
        //
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();


        // Open input image
        //
        final InputStream inputFileStream = PixelClassificationDemo.class.getResourceAsStream(inputImagePath);
        final ByteBuffer bb1 = ByteBuffer.allocate(inputFileStream.available());
        while (inputFileStream.available() > 0) {
            bb1.put((byte) inputFileStream.read());
        }
        final IRandomAccess ira1 = new ByteArrayHandle(bb1);
        ij.scifio().location().mapFile("rawInputFile", ira1);
        final Dataset inputDataset = ij.scifio().datasetIO().open("rawInputFile");

        // Open segmentation image
        //
        final InputStream segmFileStream = PixelClassificationDemo.class.getResourceAsStream(inputSegmPath);
        final ByteBuffer bb2 = ByteBuffer.allocate(segmFileStream.available());
        while (segmFileStream.available() > 0) {
            bb2.put((byte) segmFileStream.read());
        }
        final IRandomAccess ira2 = new ByteArrayHandle(bb2);
        ij.scifio().location().mapFile("segmFile", ira2);
        final Dataset segmDataset = ij.scifio().datasetIO().open("segmFile");

        ij.ui().show(inputDataset);

        // Copy project file to tmp
        //
        InputStream projectFileStream = PixelClassificationDemo.class.getResourceAsStream(ilastikProjectPath);
        Path tmpIlastikProjectFile = Paths.get(IOUtils.getTemporaryFileName("obj_class.ilp"));
        Files.copy(projectFileStream, tmpIlastikProjectFile);

        // Classify pixels
        //
        final ObjectClassification prediction = new ObjectClassification(
                new File(ilastikPath),
                tmpIlastikProjectFile.toFile(),
                ij.log(),
                ij.status(),
                4,
                1024
        );

        final ImgPlus classifiedObjects = prediction.classifyObjects(inputDataset.getImgPlus(), segmDataset.getImgPlus(),
                PixelPredictionType.Probabilities);

        ImageJFunctions.show(classifiedObjects, "Classified objects");
    }

}
