package org.ilastik.ilastik4ij.executors;

import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.util.IOUtils;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractIlastikExecutor {

    protected static final String rawInputTempFile = "tempFileRawInput";
    protected static final String outputTempFile = "tempFileOutput";
    protected static final String secondInputTempFile = "tempFileSegmentationOrProbabilitiesInput";

    private final int numThreads;
    private final int maxRamMb;

    protected final File executableFilePath;
    protected final File projectFileName;
    protected final LogService logService;
    protected final StatusService statusService;

    public enum PixelPredictionType {
        Segmentation,
        Probabilities
    }


    public AbstractIlastikExecutor(File executableFilePath, File projectFileName, LogService logService,
                                   StatusService statusService, int numThreads, int maxRamMb) {
        this.numThreads = numThreads;
        this.maxRamMb = maxRamMb;
        this.executableFilePath = executableFilePath;
        this.projectFileName = projectFileName;
        this.logService = logService;
        this.statusService = statusService;
    }

    protected abstract List<String> buildCommandLine(Map<String, String> tempFiles, PixelPredictionType pixelPredictionType);

    protected <T extends NativeType<T>> ImgPlus<T> executeIlastik(ImgPlus<? extends RealType<?>> rawInputImg,
                                                                  ImgPlus<? extends RealType<?>> secondInputImg,
                                                                  PixelPredictionType pixelPredictionType) throws IOException {
        final Map<String, String> tempFiles = prepareTempFiles(secondInputImg != null);
        try {

            stageInputFiles(rawInputImg, secondInputImg, tempFiles);

            List<String> commandLine = buildCommandLine(tempFiles, pixelPredictionType);

            executeCommandLine(commandLine);

            ImgPlus<T> outputImg = new Hdf5DataSetReader<T>(tempFiles.get(outputTempFile), "exported_data",
                    "tzyxc", logService, statusService).read();

            return outputImg;
        } finally {
            deleteTempFiles(tempFiles);
        }
    }

    private void stageInputFiles(ImgPlus<? extends RealType<?>> rawInputImg, ImgPlus<? extends RealType<?>> secondInputImg,
                                 Map<String, String> tempFiles) {
        int compressionLevel = 1;

        new Hdf5DataSetWriter(rawInputImg, tempFiles.get(rawInputTempFile), "data",
                compressionLevel, logService, statusService).write();

        if (secondInputImg != null) {
            new Hdf5DataSetWriter(secondInputImg, tempFiles.get(secondInputTempFile), "data",
                    compressionLevel, logService, statusService).write();
        }
    }

    private Map<String, String> prepareTempFiles(boolean hasSecondInputImg) throws IOException {
        LinkedHashMap<String, String> tempFiles = new LinkedHashMap<>();

        tempFiles.put(rawInputTempFile, IOUtils.getTemporaryFileName("_in_raw.h5"));
        tempFiles.put(outputTempFile, IOUtils.getTemporaryFileName("_out.h5"));

        if (hasSecondInputImg) {
            tempFiles.put(secondInputTempFile, IOUtils.getTemporaryFileName("_in_2nd.h5"));
        }

        logService.info("Temporary files: " + tempFiles);

        return Collections.unmodifiableMap(tempFiles);
    }

    private void deleteTempFiles(Map<String, String> tempFiles) {
        for (String tempFilePath : tempFiles.values()) {
            final File tempFile = new File(tempFilePath);
            if (tempFile.exists()) {
                tempFile.delete();
                logService.info("Deleted tmp file: " + tempFile);
            }
        }
    }

    private void executeCommandLine(List<String> commandLine) throws IOException {
        logService.info("Running ilastik headless command:");
        logService.info(commandLine.toString());

        ProcessBuilder pB = new ProcessBuilder(commandLine);
        configureProcessBuilderEnvironment(pB);

        // run ilastik
        final Process p = pB.start();

        // write ilastik output to log
        IOUtils.redirectOutputToLogService(p.getInputStream(), logService, false);
        IOUtils.redirectOutputToLogService(p.getErrorStream(), logService, true);

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            logService.warn("Execution got interrupted", e);
            p.destroy();
        }

        // 0 indicates successful execution
        if (p.exitValue() != 0) {
            logService.error("ilastik execution crashed");
            throw new RuntimeException("Execution of ilastik was not successful.");
        }

        logService.info("ilastik execution finished successfully!");
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
