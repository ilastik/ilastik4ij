package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectClassification extends AbstractIlastikExecutor {

    public ObjectClassification( File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb )
    {
        super( executableFilePath, projectFileName, logger, statusService, numThreads, maxRamMb );
    }

    public ImgPlus< ? > classifyObjects( ImgPlus<? extends RealType<?>> rawInputImg, ImgPlus<? extends RealType<?>> secondInputImg, SecondInputType secondInputType ) throws IOException
    {
        return executeIlastik( rawInputImg, secondInputImg, secondInputType);
    }

    @Override
    protected List<String> buildCommandLine(Map<String, String> tempFiles, SecondInputType secondInputType) {

        // TODO: Adapt to object classification

//        commandLine.add(executableFilePath.getAbsolutePath());
//        commandLine.add("--headless");
//        commandLine.add("--project=" + projectFileName.getAbsolutePath());
//        commandLine.add("--output_filename_format=" + tempFiles.get( tempFileOutput ));
//        commandLine.add("--output_format=hdf5");
//        commandLine.add("--output_axis_order=tzyxc");
//        if (outputType.equals( OutputType.Segmentation )) {
//            commandLine.add("--export_source=\"Simple Segmentation\"");
//        }
//        commandLine.add(tempFiles.get( rawInputTempFile ));

        List<String> commandLine = new ArrayList<>();
        commandLine.add(executableFilePath.getAbsolutePath());
        commandLine.add("--headless");
        commandLine.add("--project=" + projectFileName.getAbsolutePath());
        commandLine.add("--output_filename_format=" + tempFiles.get( outputTempFile ));
        commandLine.add("--output_format=hdf5");
        commandLine.add("--output_axis_order=tzyxc");
        commandLine.add("--raw_data=" + tempFiles.get( rawInputTempFile ));

        if (secondInputType.equals("Segmentation")) {
            commandLine.add("--segmentation_image=" + tempFiles.get( secondInputTempFile ));
        } else {
            commandLine.add("--prediction_maps=" + tempFiles.get( secondInputTempFile ));
        }

        return commandLine;
    }
}
