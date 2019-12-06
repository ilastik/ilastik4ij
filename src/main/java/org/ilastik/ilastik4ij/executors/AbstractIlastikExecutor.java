package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.util.IlastikUtilities;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractIlastikExecutor {
    protected static final String tempInRawFileName = "tempInRawFileName";
    protected static final String tempOutFileName = "tempOutFileName";
    protected static final String tempProbFilename = "tempProbFilename";
    protected static final String tempSegFilename = "tempSegFilename";

    private final int numThreads;
    private final int maxRamMb;

    protected final File executableFilePath;
    protected final File projectFileName;
    protected final LoggerCallback logger;
    protected final StatusService statusService;


    public AbstractIlastikExecutor(File executableFilePath, File projectFileName, LoggerCallback logger, StatusService statusService, int numThreads, int maxRamMb) {
        this.numThreads = numThreads;
        this.maxRamMb = maxRamMb;
        this.executableFilePath = executableFilePath;
        this.projectFileName = projectFileName;
        this.logger = logger;
        this.statusService = statusService;
    }

    public abstract Map<String, String> prepareTempFiles(ImgPlus<? extends RealType<?>> img) throws IOException;

    public abstract List<String> buildCommandLine(Map<String, String> tempFiles);


    public void runIlastik(ImgPlus<? extends RealType<?>> img) throws IOException {
        List<String> commandLine = buildCommandLine(prepareTempFiles(img));

        logger.info("Running ilastik headless command:");
        logger.info(commandLine.toString());

        ProcessBuilder pB = new ProcessBuilder(commandLine);
        configureProcessBuilderEnvironment(pB);

        // run ilastik
        final Process p = pB.start();

        // write ilastik output to IJ log
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
