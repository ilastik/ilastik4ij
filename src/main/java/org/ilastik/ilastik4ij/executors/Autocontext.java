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

public class Autocontext extends AbstractIlastikExecutor {

    public Autocontext(File executableFilePath, File projectFileName, LogService logService,
                               StatusService statusService, int numThreads, int maxRamMb) {
        super(executableFilePath, projectFileName, logService, statusService, numThreads, maxRamMb);
    }

    public <T extends NativeType<T>> ImgPlus<T> classifyPixels(ImgPlus<? extends RealType<?>> rawInputImg,
                                                               PixelPredictionType pixelPredictionType) throws IOException {
        return executeIlastik(rawInputImg, null, pixelPredictionType);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelPredictionType pixelPredictionType) {
        List<String> commandLine = getBaseCommand();
        if (pixelPredictionType == PixelPredictionType.Segmentation) {
            commandLine.add("--export_source=Simple Segmentation Stage 2");
        }
        else if (pixelPredictionType == PixelPredictionType.Probabilities) {
            commandLine.add("--export_source=Probabilities Stage 2");
        }
        commandLine.add("--raw_data=" + tempFiles.get(rawInputTempFile));
        commandLine.add("--output_filename_format=" + tempFiles.get(outputTempFile));

        return commandLine;
    }
}
