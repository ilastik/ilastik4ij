package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
public abstract class CommandBase<T extends NativeType<T>> extends ContextCommand {
    public static final String ROLE_PROBABILITIES = "Probabilities";
    public static final String ROLE_SEGMENTATION = "Segmentation";

    @Parameter
    public LogService logService;

    @Parameter
    public StatusService statusService;

    @Parameter
    public OptionsService optionsService;

    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;

    @Parameter(label = "Raw input image")
    public Dataset inputImage;

    @Parameter(type = ItemIO.OUTPUT)
    public ImgPlus<T> predictions;

    /**
     * Workflow-specific command-line arguments.
     */
    protected List<String> workflowArgs() {
        return Collections.emptyList();
    }

    /**
     * Workflow inputs.
     * <p>
     * Keys are corresponding command-line arguments without leading dashes.
     * Values are input datasets.
     */
    protected Map<String, Dataset> workflowInputs() {
        return Collections.emptyMap();
    }

    @Override
    public final void run() {
        Logger logger = logService.subLogger(getClass().getCanonicalName(), LogLevel.INFO);

        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);
        opts.load();

        Path executable = opts.executableFile.toPath().toAbsolutePath();
        if (PlatformUtils.isMac() && executable.toString().endsWith(".app")) {
            executable = executable.resolve("Contents").resolve("MacOS").resolve("ilastik");
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(executable.toString(),
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

        try {
            try (TempDir temp = new TempDir("ilastik4ij_")) {
                Path inputDir = Files.createDirectory(temp.dir.resolve("inputs"));
                Path outputDir = Files.createDirectory(temp.dir.resolve("outputs"));

                int counter = 0;
                HashMap<String, Dataset> inputs = new HashMap<>(workflowInputs());
                inputs.put("raw_data", inputImage);
                for (String name : inputs.keySet()) {
                    Dataset dataset = inputs.get(name);
                    Path inputPath = inputDir.resolve(name + ".h5");
                    statusService.showStatus(++counter, inputs.size(), "Exporting inputs");
                    logger.info(String.format("write %s to %s starting", name, inputPath));
                    new Hdf5DataSetWriter(
                            dataset.getImgPlus(), inputPath.toString(), "data", 1, logger, statusService).write();
                    logger.info("write finished");
                    pb.command().add(String.format("--%s=%s", name, inputPath));
                }

                Path outputPath = outputDir.resolve("predictions.h5");
                pb.command().add("--output_filename_format=" + outputPath);

                logger.info(String.format("ilastik run starting with arguments %s and environment %s",
                        pb.command(),
                        pb.environment()));
                Process process = pb.start();
                redirectInputStream(process.getInputStream(), logger::info);
                redirectInputStream(process.getErrorStream(), logger::info);

                List<Character> spinnerChars = Arrays.asList('|', '/', '-', '\\');

                String[] words = getClass().getSimpleName().split("(?=\\p{Lu})");
                String workflowName = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                Consumer<Character> updateStatus = (spinner) ->
                    statusService.showStatus(String.format("Running %s %c", workflowName, spinner));

                try (Cycler<Character> ignored = new Cycler<>(300, spinnerChars, updateStatus)) {
                    if (process.waitFor() != 0) {
                        throw new RuntimeException("non-zero exit code " + process.exitValue());
                    }
                }
                logger.info("ilastik run finished");

                statusService.showStatus("Importing outputs");
                logger.info(String.format("read from %s starting", outputPath));
                predictions = new Hdf5DataSetReader<T>(
                        outputPath.toString(), "exported_data", "tzyxc", logger, statusService).read();
                logger.info("read finished");
            }

        } catch (Exception e) {
            statusService.clearStatus();
            logger.error("ilastik run failed", e);
            throw new RuntimeException(e);
        }
    }

    private static void redirectInputStream(InputStream src, Consumer<String> dst) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(src, StandardCharsets.UTF_8))) {
                reader.lines().forEachOrdered(dst);
            } catch (IOException e) {
                throw new RuntimeException(e);
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

    /**
     * During try-with-resources block, periodically call sink with list items.
     */
    private static final class Cycler<T> implements AutoCloseable {
        private final List<T> items;
        private final Consumer<T> sink;
        private final ScheduledExecutorService pool;
        private int i = 0;

        public Cycler(long periodMillis, List<T> items, Consumer<T> sink) {
            this.items = new ArrayList<>(items);
            this.sink = sink;
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleWithFixedDelay(this::update, 0, periodMillis, TimeUnit.MILLISECONDS);
        }

        private void update() {
            sink.accept(items.get(i));
            i = (i + 1) % items.size();
        }

        @Override
        public void close() {
            pool.shutdown();
        }
    }
}
