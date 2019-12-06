package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.util.IlastikUtilities;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractIlastikExecutor {

    protected static final String tempFileRawInput = "tempFileRawInput";
    protected static final String tempFileOutput = "tempFileOutput";
    protected static final String tempFileSecondInput = "tempFileSegmentationOrProbabilitiesInput";

    private final int numThreads;
    private final int maxRamMb;

    protected final File executableFilePath;
    protected final File projectFileName;
    protected final LoggerCallback logger;
    protected final StatusService statusService;

    enum SecondInputType
    {
        None,
        Segmentation,
        Probability
    }

    public AbstractIlastikExecutor(File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb) {
        this.numThreads = numThreads;
        this.maxRamMb = maxRamMb;
        this.executableFilePath = executableFilePath;
        this.projectFileName = projectFileName;
        this.logger = logger;
        this.statusService = statusService;
    }

    protected abstract List<String> buildCommandLine(Map<String, String> tempFiles, SecondInputType secondInputType );

    public void runIlastik( ImgPlus<? extends RealType<?>> img, SecondInputType secondInputType ) throws IOException {

        final Map< String, String > tempFiles = prepareTempFiles( secondInputType );

        List<String> commandLine = buildCommandLine( tempFiles, secondInputType );

        executeCommandLine( commandLine );

        ImgPlus< ? > imgPlus = new Hdf5DataSetReader(tempFiles.get( tempFileOutput ), "exported_data", "tzyxc", logger, statusService).read();

        deleteTempFiles( tempFiles );
    }

    private Map<String, String> prepareTempFiles(SecondInputType secondInputType) throws IOException
    {
        LinkedHashMap<String, String> tempFiles = new LinkedHashMap<>();

        tempFiles.put( tempFileRawInput, IlastikUtilities.getTemporaryFileName("_in_raw.h5"));
        tempFiles.put( tempFileOutput, IlastikUtilities.getTemporaryFileName("_out.h5") );

        if ( ! secondInputType.equals( SecondInputType.None ) )
        {
            tempFiles.put( tempFileSecondInput, IlastikUtilities.getTemporaryFileName("_in_2nd.h5") );
        }

        return tempFiles;
    }

    private void deleteTempFiles( Map< String, String > tempFiles )
    {
        logger.info("Deleting temporary files...");

        for( String tempFilePath : tempFiles.keySet() )
        {
            final File tempFile = new File( tempFilePath );
            if ( tempFile.exists() ) tempFile.delete();
        }

        logger.info( "...done." );
    }

    private void executeCommandLine( List< String > commandLine ) throws IOException
    {
        logger.info("Running ilastik headless command:");
        logger.info(commandLine.toString());

        ProcessBuilder pB = new ProcessBuilder(commandLine);
        configureProcessBuilderEnvironment(pB);

        // run ilastik
        final Process p = pB.start();

        // write ilastik output to log
        IlastikUtilities.redirectOutputToLogService(p.getInputStream(), logger, false);
        IlastikUtilities.redirectOutputToLogService(p.getErrorStream(), logger, true);

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            logger.warn("Execution got interrupted");
            p.destroy();
        }

        // 0 indicates successful execution
        if (p.exitValue() != 0) {
            logger.error("ilastik crashed");
            throw new RuntimeException("Execution of ilastik was not successful.");
        }

        logger.info("ilastik finished successfully!");
    }

    private void configureProcessBuilderEnvironment(ProcessBuilder pb) {
        final Map<String, String> env = pb.environment();
        if (this.numThreads >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(this.numThreads));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(this.maxRamMb));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");
    }
}
