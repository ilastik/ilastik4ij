package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.util.PlatformUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private static final String SPINNER_CHARS = "|/-\\";

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private OptionsService optionsService;

    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;

    @Parameter(label = "Raw input image")
    public Dataset inputImage;

    @Parameter(type = ItemIO.OUTPUT)
    public ImgPlus<T> predictions;

    private Logger logger;
    private ScheduledExecutorService spinnerPool;

    /**
     * Workflow-specific command-line arguments.
     */
    protected List<String> workflowArgs() {
        return Collections.emptyList();
    }

    /**
     * Workflow-specific input datasets.
     * <p>
     * Keys are corresponding command-line arguments without leading dashes.
     * Values are input datasets.
     */
    protected Map<String, Dataset> workflowInputs() {
        return Collections.emptyMap();
    }

    @Override
    public final void run() {
        logger = logService.subLogger(getClass().getCanonicalName(), LogLevel.INFO);
        spinnerPool = Executors.newScheduledThreadPool(1);

        try {
            try (TempDir temp = new TempDir("ilastik4ij_")) {
                runWithTempDirs(
                        Files.createDirectory(temp.dir.resolve("inputs")),
                        Files.createDirectory(temp.dir.resolve("outputs")));
            }

        } catch (Exception e) {
            statusService.clearStatus();
            logger.error("ilastik run failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Write inputs, run subprocess, read outputs.
     */
    private void runWithTempDirs(Path inputDir, Path outputDir) throws Exception {
        ProcessBuilder pb = initialProcessBuilder();

        Map<String, Dataset> inputs = new HashMap<>(workflowInputs());
        inputs.put("raw_data", inputImage);
        pb.command().addAll(writeInputs(inputs, inputDir));

        Path outputPath = outputDir.resolve("predictions.h5");
        pb.command().add("--output_filename_format=" + outputPath);

        runSubprocess(pb);

        logger.info(String.format("read from %s starting", outputPath));
        try (Spinner ignored = new Spinner("Reading predictions")) {
            predictions = new Hdf5DataSetReader<T>(
                    outputPath.toString(), "exported_data", "tzyxc", logger, statusService).read();
        }
        logger.info("read finished");
    }

    /**
     * Create {@link ProcessBuilder} with configured command-line arguments and environment variables,
     * but without input and output parameters.
     */
    private ProcessBuilder initialProcessBuilder() {
        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);
        opts.load();

        Path exe = opts.executableFile.toPath().toAbsolutePath();
        if (PlatformUtils.isMac() && exe.toString().endsWith(".app")) {
            // On macOS, we need an actual executable path, not an app bundle path.
            exe = exe.resolve("Contents").resolve("MacOS").resolve("ilastik");
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(exe.toString(),
                "--headless",
                "--project=" + projectFileName.getAbsolutePath(),
                "--output_format=hdf5",
                "--output_axis_order=tzyxc",
                "--input_axes=tzyxc",
                "--readonly=1",
                "--output_internal_path=exported_data");
        pb.command().addAll(workflowArgs());

        Map<String, String> env = pb.environment();
        if (opts.numThreads >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(opts.numThreads));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(opts.maxRamMb));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");

        return pb;
    }

    /**
     * Write inputs into temporary files, return command-line arguments for these inputs.
     */
    private List<String> writeInputs(Map<String, Dataset> inputs, Path dir) {
        // We need a counter, but the lambda below requires an immutable reference.
        final int[] progress = {0};

        return inputs.keySet().stream().map(name -> {
            @SuppressWarnings("unchecked")
            ImgPlus<T> img = (ImgPlus<T>) inputs.get(name).getImgPlus();
            Path path = dir.resolve(name + ".h5");

            logger.info(String.format("write %s to %s starting", name, path));
            statusService.showStatus(progress[0]++, inputs.size(), "Writing inputs");

            new Hdf5DataSetWriter<>(img, path.toString(), "data", 1, logger, statusService).write();

            logger.info("write finished");
            return String.format("--%s=%s", name, path);

        }).collect(Collectors.toList());
    }

    /**
     * Run ilastik subprocess and wait for it to finish.
     */
    private void runSubprocess(ProcessBuilder pb) throws Exception {
        logger.info(String.format(
                "ilastik run starting with arguments %s and environment %s",
                pb.command(),
                pb.environment()));

        Process process = pb.start();
        redirectInputStream(process.getInputStream(), logger::info);
        // ilastik prints warnings to stderr, but we don't want them to be displayed as errors.
        // Fatal errors should result in a non-zero exit code.
        redirectInputStream(process.getErrorStream(), logger::info);

        // Get "My Workflow" string from a MyWorkflowCommand class.
        String[] words = getClass().getSimpleName().split("(?=\\p{Lu})");
        String workflowName = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));

        try (Spinner ignored = new Spinner("Running " + workflowName)) {
            if (process.waitFor() != 0) {
                throw new RuntimeException("exit code " + process.exitValue());
            }
        }

        logger.info("ilastik run finished");
    }

    /**
     * During try-with-resources block, show message with a ticking spinner in the status bar.
     */
    private final class Spinner implements AutoCloseable {
        private final String message;
        private final ScheduledFuture<?> future;
        private int i = 0;

        public Spinner(String message) {
            this.message = message;
            future = spinnerPool.scheduleWithFixedDelay(this::update, 0, 300, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            future.cancel(true);
        }

        private void update() {
            statusService.showStatus(message + " " + SPINNER_CHARS.charAt(i));
            i = (i + 1) % SPINNER_CHARS.length();
        }
    }

    /**
     * Start {@link Thread} that reads UTF-8 lines from source and sends them to destination.
     */
    private static void redirectInputStream(InputStream src, Consumer<String> dst) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(src, StandardCharsets.UTF_8))) {
                reader.lines().forEachOrdered(dst);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).start();
    }

    /**
     * Create temporary directory that exists within a try-with-resources block.
     */
    private static final class TempDir implements AutoCloseable {
        public final Path dir;

        public TempDir(String prefix) throws IOException {
            dir = Files.createTempDirectory(prefix);
        }

        @Override
        public void close() throws IOException {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
