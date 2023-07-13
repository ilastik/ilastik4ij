package org.ilastik.ilastik4ij.cmd;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.util.PlatformUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

/**
 * Base class for all commands that run ilastik in a subprocess.
 * <p>
 * Instead of {@link #run()}, derived classes can implement {@link #workflowArgs()}
 * and {@link #workflowInputs()}, which are used to build workflow-specific
 * command-line arguments.
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

    private final String logPrefix = "ilastik4ij: " + getClass().getSimpleName();

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
        Options opts = optionsService.getOptions(Options.class);
        opts.load();

        Path executable = opts.executableFile.toPath().toAbsolutePath().normalize();
        if (PlatformUtils.isMac() && executable.toString().endsWith(".app")) {
            executable = executable.resolve(Paths.get("Contents", "MacOS", "ilastik"));
        }

        List<String> args = new ArrayList<>(Arrays.asList(
                executable.toString(),
                "--headless",
                "--project=" + projectFileName.getAbsolutePath(),
                "--output_format=hdf5",
                "--output_axis_order=tzyxc",
                "--input_axes=tzyxc",
                "--readonly=1",
                "--output_internal_path=exported_data"));
        args.addAll(workflowArgs());

        Map<String, String> env = new HashMap<>();
        if (opts.numThreads >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(opts.numThreads));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(opts.maxRamMb));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");

        try {
            try (TempDirCloseable temp = new TempDirCloseable()) {
                Path inputs = temp.dir.resolve("inputs");
                Files.createDirectory(inputs);

                args.add(writeInput("raw_data", temp.dir.resolve("raw_data.h5"), inputImage));
                workflowInputs().forEach((name, dataset) ->
                        args.add(writeInput(name, inputs.resolve(name + ".h5"), dataset)));

                Path output = temp.dir.resolve("predictions.h5");
                args.add("--output_filename_format=" + output);

                ProcessBuilder pb = new ProcessBuilder(args);
                pb.environment().putAll(env);

                logService.info(String.format("%s: starting", logPrefix));
                logService.info(String.format("%s: arguments = %s", logPrefix, args));
                logService.info(String.format("%s: environment = %s", logPrefix, env));

                Process proc = pb.start();
                redirectInputStream(proc.getInputStream(), logService::info);
                redirectInputStream(proc.getErrorStream(), logService::error);
                if (proc.waitFor() != 0) {
                    throw new RuntimeException("non-zero exit code " + proc.exitValue());
                }

                logService.info(String.format("%s: finished", logPrefix));
                predictions = readOutput(output);
            }

        } catch (IOException | InterruptedException e) {
            logService.error(String.format("%s: failed", logPrefix), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Write dataset to file; return command-line argument for the input.
     */
    private String writeInput(String name, Path path, Dataset dataset) {
        String prefix = String.format("%s: write %s to %s", logPrefix, name, path);
        logService.info(prefix + " starting");
        new Hdf5DataSetWriter(
                dataset.getImgPlus(), path.toString(), "data", 1, logService, statusService).write();
        logService.info(prefix + " finished");
        return String.format("--%s=%s", name, path);
    }

    /**
     * Read dataset from file.
     */
    private ImgPlus<T> readOutput(Path path) {
        String prefix = String.format("%s: read from %s", logPrefix, path);
        logService.info(prefix + " starting");
        ImgPlus<T> output = new Hdf5DataSetReader<T>(
                path.toString(), "exported_data", "tzyxc", logService, statusService).read();
        logService.info(prefix + " finished");
        return output;
    }

    /**
     * Spawn a new thread that reads lines from input and sends them into sink.
     */
    private static void redirectInputStream(InputStream is, Consumer<String> sink) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.lines().forEachOrdered(sink);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Create temporary directory that exists within a try-with-resources block.
     */
    private static final class TempDirCloseable implements AutoCloseable {
        public final Path dir;

        public TempDirCloseable() throws IOException {
            dir = Files.createTempDirectory("ilastik4ij_");
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
