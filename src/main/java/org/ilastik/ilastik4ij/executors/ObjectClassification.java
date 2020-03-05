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

public class ObjectClassification extends AbstractIlastikExecutor {

    public ObjectClassification(File executableFilePath, File projectFileName, LogService logService,
                                StatusService statusService, int numThreads, int maxRamMb) {
        super(executableFilePath, projectFileName, logService, statusService, numThreads, maxRamMb);
    }

    public <T extends NativeType<T>> ImgPlus<T> classifyObjects(ImgPlus<? extends RealType<?>> rawInputImg,
                                                                ImgPlus<? extends RealType<?>> probOrSegInputImg,
                                                                PixelPredictionType secondInputType) throws IOException {
        return executeIlastik(rawInputImg, probOrSegInputImg, secondInputType);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelPredictionType secondInputType) {
        List<String> commandLine = new ArrayList<>(this.baseCommand);
        commandLine.add("--raw_data=" + tempFiles.get(rawInputTempFile));

        if (secondInputType == PixelPredictionType.Segmentation) {
            commandLine.add("--segmentation_image=" + tempFiles.get(secondInputTempFile));
        } else {
            commandLine.add("--prediction_maps=" + tempFiles.get(secondInputTempFile));
        }

        commandLine.add("--output_filename_format=" + tempFiles.get(outputTempFile));

        return commandLine;
    }
}
