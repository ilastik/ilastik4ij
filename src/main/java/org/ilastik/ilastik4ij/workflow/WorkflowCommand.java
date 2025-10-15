/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2025 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.util.ImgUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

import static org.ilastik.ilastik4ij.util.ImgUtils.DEFAULT_AXES;
import static org.ilastik.ilastik4ij.util.ImgUtils.DEFAULT_STRING_AXES;
import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.taggedResolutionsOf;
import static org.ilastik.ilastik4ij.util.ImgUtils.taggedUnitsOf;

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
public abstract class WorkflowCommand<T extends NativeType<T> & RealType<T>> extends ContextCommand {
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
        try (StatusBar statusBar = new StatusBar(statusService, 300)) {
            try (TempDir tempDir = new TempDir("ilastik4ij_")) {
                Path inputDir = Files.createDirectory(tempDir.path.resolve("inputs"));
                Path outputDir = Files.createDirectory(tempDir.path.resolve("outputs"));
                runImpl(statusBar, inputDir, outputDir);
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

    private void runImpl(StatusBar statusBar, Path inputDir, Path outputDir) {
        Logger logger = logService.subLogger(getClass().getCanonicalName(), LogLevel.INFO);
        long startTime;

        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);
        opts.load();

        List<String> args = commonSubprocessArgs(opts, projectFileName);
        args.addAll(workflowArgs());

        Map<String, Dataset> inputs = new HashMap<>(workflowInputs());
        inputs.put("raw_data", inputImage);

        int total = ImgUtils.totalMegabytes(new ArrayList<>(inputs.values()));
        LongConsumer updateStatusBar = (size) ->
                statusBar.service.showStatus((int) (size >> 20), total, "Writing inputs");
        startTime = System.nanoTime();

        for (String inputName : inputs.keySet()) {
            Path inputPath = inputDir.resolve(inputName + ".h5");
            args.add(String.format("--%s=%s", inputName, inputPath));

            @SuppressWarnings("unchecked")
            ImgPlus<T> inputImg = (ImgPlus<T>) inputs.get(inputName).getImgPlus();

            Map<AxisType, Double> res = taggedResolutionsOf(inputImg);
            Map<AxisType, String> units = taggedUnitsOf(inputImg);
            logger.info(String.format("Input has resolutions='%s' and units='%s'", res.values(), units.values()));

            logger.info(String.format("Write input '%s' starting", inputPath));
            Hdf5.writeDataset(
                    inputPath.toFile(), "data", inputImg, 1, DEFAULT_AXES, updateStatusBar);
            logger.info(String.format("Write input '%s' finished", inputPath));
        }
        statusBar.service.clearStatus();
        logger.info(String.format(
                "Write inputs finished in %.3f seconds", 1e-9 * (System.nanoTime() - startTime)));

        Path outputPath = outputDir.resolve("predictions.h5");
        args.add("--output_filename_format=" + outputPath);

        Map<String, String> env = subprocessEnv(opts);
        startTime = System.nanoTime();
        logger.info(String.format(
                "Subprocess starting with arguments %s and environment %s", args, env));
        statusBar.withSpinner("Running " + workflowName(), () -> {
            // ilastik prints warnings to stderr, but we don't want them to be displayed as errors.
            // Therefore, use Logger#info for both stdout and stderr.
            // Fatal errors should still result in a non-zero exit code.
            int exitCode = new Subprocess(args, env, logger::info).getAsInt();
            if (exitCode != 0) {
                throw new RuntimeException("Subprocess failed with exit code " + exitCode);
            }
        });
        logger.info(String.format(
                "Subprocess finished in %.3f seconds", 1e-9 * (System.nanoTime() - startTime)));

        logger.info("Read output starting");
        startTime = System.nanoTime();
        statusBar.withSpinner("Reading output", () ->
                predictions = Hdf5.readDataset(outputPath.toFile(), "exported_data"));
        logger.info(String.format(
                "Read output finished in %.3f seconds", 1e-9 * (System.nanoTime() - startTime)));
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
        String axes = reversed(DEFAULT_STRING_AXES);
        return new ArrayList<>(Arrays.asList(
                executable.toString(),
                "--headless",
                "--project=" + projectFileName.getAbsolutePath(),
                "--output_format=hdf5",
                "--output_axis_order=" + axes,
                "--ignore_training_axistags",
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
