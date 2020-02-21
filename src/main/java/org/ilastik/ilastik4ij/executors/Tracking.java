package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tracking extends AbstractIlastikExecutor {

    public Tracking(File executableFilePath, File projectFileName, LogService logService, StatusService statusService, int numThreads, int maxRamMb) {
        super(executableFilePath, projectFileName, logService, statusService, numThreads, maxRamMb);
    }

    public <T extends NativeType<T>> ImgPlus<T> trackObjects(ImgPlus<? extends RealType<?>> rawInputImg, ImgPlus<? extends RealType<?>> secondInputImg,
                                                             PixelPredictionType pixelPredictionType) throws IOException {
        return executeIlastik(rawInputImg, secondInputImg, pixelPredictionType);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelPredictionType pixelPredictionType) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(executableFilePath.getAbsolutePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempFiles.get(outputTempFile));
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        commandLine.add("--export_source=Tracking-Result");
        commandLine.add("--input_axes=tzyxc");
        commandLine.add("--raw_data=" + tempFiles.get(rawInputTempFile));

        if (pixelPredictionType == PixelPredictionType.Segmentation) {
            commandLine.add("--binary_image=" + tempFiles.get(secondInputTempFile));
        } else {
            commandLine.add("--prediction_maps=" + tempFiles.get(secondInputTempFile));
        }

        return commandLine;
    }
}
