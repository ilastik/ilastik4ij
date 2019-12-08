package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.logging.LoggerCallback;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tracking extends AbstractIlastikExecutor {

    public Tracking( File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb )
    {
        super( executableFilePath, projectFileName, logger, statusService, numThreads, maxRamMb );
    }

    public ImgPlus< ? > trackObjects( ImgPlus<? extends RealType<?>> rawInputImg, ImgPlus<? extends RealType<?>> secondInputImg, PixelClassificationType pixelClassificationType ) throws IOException
    {
        return executeIlastik( rawInputImg, secondInputImg, pixelClassificationType );
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelClassificationType pixelClassificationType ) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(executableFilePath.getAbsolutePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempFiles.get(outputTempFile));
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        commandLine.add("--export_source=Tracking-Result");
        commandLine.add("--raw_data=" + tempFiles.get(rawInputTempFile));

        if ( pixelClassificationType.equals( PixelClassificationType.Segmentation)) {
            commandLine.add("--segmentation_image=" + tempFiles.get(secondInputTempFile));
        } else {
            commandLine.add("--prediction_maps=" + tempFiles.get(secondInputTempFile));
        }

        return commandLine;
    }
}
