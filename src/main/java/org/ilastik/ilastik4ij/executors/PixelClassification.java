package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PixelClassification extends AbstractIlastikExecutor {

    public PixelClassification( File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb )
    {
        super( executableFilePath, projectFileName, logger, statusService, numThreads, maxRamMb );
    }

    public <T extends NativeType<T>> ImgPlus<T> classifyPixels( ImgPlus<? extends RealType<?>> rawInputImg, PixelClassificationType pixelClassificationType ) throws IOException
    {
        return executeIlastik(rawInputImg, null, pixelClassificationType);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, PixelClassificationType pixelClassificationType ) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(executableFilePath.getAbsolutePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempFiles.get( outputTempFile ));
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        if (pixelClassificationType.equals( PixelClassificationType.Segmentation )) {
            commandLine.add("--export_source=Simple Segmentation");
        }
        commandLine.add(tempFiles.get( rawInputTempFile ));

        return commandLine;
    }
}
