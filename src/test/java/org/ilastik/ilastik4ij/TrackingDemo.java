package org.ilastik.ilastik4ij;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor;
import org.ilastik.ilastik4ij.executors.Tracking;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;

import java.io.File;
import java.io.IOException;

public class TrackingDemo {
    public static <T extends RealType<T>> void main(String[] args) throws IOException {
        final String ilastikPath = "/opt/ilastik-1.3.3post1-Linux/run_ilastik.sh";
        final String ilastikProjectPath = "/home/adrian/MyProject_tracking.ilp";
        final String inputImagePath = "/home/adrian/workspace/ilastik-datasets/mitocheck_2d+t/mitocheck_94570_2D+t_01-53_5D.h5";
        final String inputProbabMaps = "/home/adrian/workspace/ilastik-datasets/mitocheck_2d+t/mitocheck_94570_2D+t_01-53_export_5D.h5";

        // Open ImageJ
        //
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();


        // Open input image
        final ImgPlus<T> rawImage = new Hdf5DataSetReader(inputImagePath, "data",
                "tzyxc", ij.log(), ij.status()).read();

        // Open pmaps image
        //
        final ImgPlus<T> pmapsImage = new Hdf5DataSetReader(inputProbabMaps, "data",
                "tzyxc", ij.log(), ij.status()).read();

        ij.ui().show(rawImage);

        // Track objects
        //
        final Tracking prediction = new Tracking(
                new File(ilastikPath),
                new File(ilastikProjectPath),
                ij.log(),
                ij.status(),
                4,
                1024
        );

        final ImgPlus classifiedObjects = prediction.trackObjects(rawImage, pmapsImage,
                AbstractIlastikExecutor.PixelPredictionType.Probabilities);

        ImageJFunctions.show(classifiedObjects, "Tracked objects");
    }
}
