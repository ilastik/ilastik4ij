package org.ilastik.ilastik4ij.executors;

import ij.IJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.util.IlastikUtilities;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PixelClassificationPrediction extends AbstractIlastikExecutor {
    private final boolean isOutputSegmentation;

    public PixelClassificationPrediction(File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb, boolean isOutputSegmentation) {
        super(executableFilePath, projectFileName, logger, statusService, numThreads, maxRamMb);
        this.isOutputSegmentation = isOutputSegmentation;
    }

    public PixelClassificationPrediction(File executableFilePath, File projectFileName, boolean isOutputSegmentation) {
        this(executableFilePath, projectFileName, new DummyLoggerCallback(), null, -1, 4096, isOutputSegmentation);
    }

    private ImgPlus< ? > void getPrediction( ) {
        ImgPlus< ? > imgPlus = new Hdf5DataSetReader( tempFileOutput, "exported_data", "tzyxc", log, statusService).read();
        ImageJFunctions.show(imgPlus);
        if (chosenOutputType.equals("Segmentation")) {
            // apply LUT to the resulting segmentation
            IJ.run("glasbey_inverted");
        }
    }

    @Override
    protected Map<String, String> prepareTempFiles(ImgPlus<? extends RealType<?>> img) throws IOException {
        LinkedHashMap<String, String> tempFiles = new LinkedHashMap<>();

        tempFiles.put( tempFileRawInput, IlastikUtilities.getTemporaryFileName("_raw.h5"));
        tempFiles.put( tempFileOutput, IlastikUtilities.getTemporaryFileName("_out.h5") );

        int compressionLevel = 1;

        logger.info("Dumping raw input image to temporary file " + tempInFileName);
        new Hdf5DataSetWriter(img, tempInFileName, "data", compressionLevel, logger, statusService).write();

        logger.info("Saved files for training to " + tempInFileName + " and " + tempFileOutput
                + ". Use it to train an ilastik pixelClassificationProject now,"
                + " and make sure to select to copy the raw data into the project file in the data selection");

        return Collections.unmodifiableMap(tempFiles);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles) {


        List<String> commandLine = new ArrayList<>();
        commandLine.add(executableFilePath.getAbsolutePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempFiles.get( tempFileOutput ));
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        if (isOutputSegmentation) {
            commandLine.add("--export_source=\"Simple Segmentation\"");
        }
        commandLine.add(tempFiles.get( tempFileRawInput ));

        //TODO: move to object classification
//        if (tempFiles.containsKey(tempSegFilename)) {
//            commandLine.add("--segmentation_image=" + tempFiles.get(tempSegFilename));
//        } else {
//            if (!tempFiles.containsKey(tempProbFilename)) {
//                throw new IllegalStateException("No tempProbFilename provided");
//            }
//            commandLine.add("--prediction_maps=" + tempFiles.get(tempProbFilename));
//        }
        return commandLine;
    }
}
