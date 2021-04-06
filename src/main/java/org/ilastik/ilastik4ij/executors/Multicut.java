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

public class Multicut extends AbstractIlastikExecutor {

    public Multicut(File executableFilePath, File projectFileName, LogService logService,
                                StatusService statusService, int numThreads, int maxRamMb) {
        super(executableFilePath, projectFileName, logService, statusService, numThreads, maxRamMb);
    }

    public <T extends NativeType<T>> ImgPlus<T> runMulticut(ImgPlus<? extends RealType<?>> rawInputImg,
                                                            ImgPlus<? extends RealType<?>> probOrSegInputImg
                                                            ) throws IOException {
        // null specified here to satisfy execteIlastik interface...
        return executeIlastik(rawInputImg, probOrSegInputImg, null);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelPredictionType pixelPredictionType) {

        List<String> commandLine = getBaseCommand();
        commandLine.add("--raw_data=" + tempFiles.get(rawInputTempFile));
        commandLine.add("--probabilities=" + tempFiles.get(secondInputTempFile));
        commandLine.add("--output_filename_format=" + tempFiles.get(outputTempFile));
        commandLine.add("--export_source=Multicut Segmentation");

        return commandLine;
    }
}
