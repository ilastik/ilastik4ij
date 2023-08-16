package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.util.StatusBar;
import org.ilastik.ilastik4ij.util.Subprocess;
import org.ilastik.ilastik4ij.util.TempDir;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.util.PlatformUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Base class for all commands that run ilastik in a subprocess.
 * <p>
 * Instead of {@link #run()}, derived classes can implement {@link #workflowArgs()}
 * and {@link #workflowInputs()}, which are used to build workflow-specific
 * command-line arguments.
 * <p>
 * Subclasses should be named as {@code [MyWorkflowName]Command}
 * because workflow names are derived from their corresponding class names.
 */
public abstract class WorkflowCommand<T extends NativeType<T>> extends ContextCommand {
    public static final String ROLE_PROBABILITIES = "Probabilities";
    public static final String ROLE_SEGMENTATION = "Segmentation";

    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;

    @Parameter(label = "Raw input image")
    public Dataset inputImage;

    @Parameter(type = ItemIO.OUTPUT)
    public ImgPlus<T> predictions;

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private OptionsService optionsService;

    @Override
    public final void run() {
        try (StatusBar status = new StatusBar(statusService, 300)) {
            try (TempDir tempDir = new TempDir("ilastik4ij_")) {
                Path inputDir = Files.createDirectory(tempDir.path.resolve("inputs"));
                Path outputDir = Files.createDirectory(tempDir.path.resolve("outputs"));
                runImpl(status, inputDir, outputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Workflow-specific command-line arguments.
     */
    protected List<String> workflowArgs() {
        return Collections.emptyList();
    }

    /**
     * Workflow-specific input datasets.
     * Keys are corresponding command-line arguments without leading dashes.
     * Values are input datasets.
     */
    protected Map<String, Dataset> workflowInputs() {
        return Collections.emptyMap();
    }

    private void runImpl(StatusBar status, Path inputDir, Path outputDir) {
        Logger logger = logService.subLogger(getClass().getCanonicalName(), LogLevel.INFO);

        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);
        opts.load();

        List<String> args = commonSubprocessArgs(opts, projectFileName);
        args.addAll(workflowArgs());

        Map<String, Dataset> inputs = new HashMap<>(workflowInputs());
        inputs.put("raw_data", inputImage);

        status.withProgress("Writing inputs", inputs.entrySet(), (entry) -> {
            Path inputPath = inputDir.resolve(entry.getKey() + ".h5");
            args.add(String.format("--%s=%s", entry.getKey(), inputPath));

            @SuppressWarnings("unchecked")
            ImgPlus<T> inputImg = (ImgPlus<T>) entry.getValue().getImgPlus();

            logger.info(String.format("Write input '%s' starting", inputPath));
            new Hdf5DataSetWriter<>(
                    inputImg, inputPath.toString(), "data", 1, logger, statusService).write();
            logger.info(String.format("Write input '%s' finished", inputPath));
        });

        Path outputPath = outputDir.resolve("predictions.h5");
        args.add("--output_filename_format=" + outputPath);

        Map<String, String> env = subprocessEnv(opts);
        logger.info(String.format(
                "Subprocess starting with arguments %s and environment %s", args, env));
        status.withSpinner("Running " + workflowName(), () -> {
            // ilastik prints warnings to stderr, but we don't want them to be displayed as errors.
            // Therefore, use Logger#info for both stdout and stderr.
            // Fatal errors should still result in a non-zero exit code.
            int exitCode = new Subprocess(args, env, logger::info).getAsInt();
            if (exitCode != 0) {
                throw new RuntimeException("Subprocess failed with exit code " + exitCode);
            }
        });
        logger.info("Subprocess finished");

        status.withSpinner("Reading output", () ->
                predictions = new Hdf5DataSetReader<T>(
                        outputPath.toString(),
                        "exported_data",
                        "tzyxc",
                        logger,
                        statusService).read());
    }

    private String workflowName() {
        List<String> words = Arrays.asList(getClass().getSimpleName().split("(?=\\p{Lu})"));
        return String.join(" ", words.subList(0, words.size() - 1));
    }

    private static List<String> commonSubprocessArgs(IlastikOptions opts, File projectFileName) {
        Path executable = opts.executableFile.toPath().toAbsolutePath();
        // On macOS, if an app bundle path was provided, get an actual executable path from it.
        if (PlatformUtils.isMac() && executable.toString().endsWith(".app")) {
            executable = executable.resolve("Contents").resolve("MacOS").resolve("ilastik");
        }
        return new ArrayList<>(Arrays.asList(
                executable.toString(),
                "--headless",
                "--project=" + projectFileName.getAbsolutePath(),
                "--output_format=hdf5",
                "--output_axis_order=tzyxc",
                "--input_axes=tzyxc",
                "--readonly=1",
                "--output_internal_path=exported_data"));
    }

    private static Map<String, String> subprocessEnv(IlastikOptions opts) {
        Map<String, String> env = new HashMap<>();
        if (opts.numThreads >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(opts.numThreads));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(opts.maxRamMb));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");
        return env;
    }
}
