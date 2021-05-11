package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.util.Subprocesses;
import org.ilastik.ilastik4ij.util.TempDirCloseable;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.util.PlatformUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class WorkflowCommand<T extends NativeType<T>> extends ContextCommand {
    public static final String PROBABILITIES = "Probabilities";
    public static final String SEGMENTATION = "Segmentation";

    @Parameter
    public LogService logService;

    @Parameter
    public StatusService statusService;

    @Parameter
    public OptionsService optionsService;

    @Parameter(label = "Trained ilastik project file")
    public File project;

    @Parameter(label = "Raw input image")
    public Dataset rawData;

    @Parameter(type = ItemIO.OUTPUT)
    public ImgPlus<T> predictions;

    @Override
    public final void run() {
        try {
            try (TempDirCloseable temp = new TempDirCloseable("ilastik4ij_")) {
                runWithTempDir(temp.dir);
            }
        } catch (Exception e) {
            logService.error("Failed to execute ilastik " + getClass().getSimpleName(), e);
            throw new RuntimeException(e);
        }
    }

    private void runWithTempDir(Path tempDir) throws Exception {
        IlastikOptions options = optionsService.getOptions(IlastikOptions.class);

        // On MacOS user can select an app bundle, but we need the actual executable.
        Path executablePath = options.getExecutableFile().toPath();
        if (PlatformUtils.isMac() && executablePath.toString().endsWith(".app")) {
            executablePath = executablePath.resolve(Paths.get("Contents", "MacOS", "ilastik"));
        }

        Path rawDataPath = tempDir.resolve("rawData.h5");
        writeHdf5(rawDataPath, rawData);

        Path predictionsPath = tempDir.resolve("predictions.h5");

        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(
                executablePath.toString(),
                "--project=" + project,
                "--headless",
                "--readonly=1",
                "--raw_data=" + rawDataPath,
                "--input_axes=tzyxc",
                "--output_filename_format=" + predictionsPath,
                "--output_axis_order=tzyxc",
                "--output_format=hdf5",
                "--output_internal_path=exported_data"));
        cmd.addAll(workflowArgs(tempDir));

        Map<String, String> env = new HashMap<>();
        if (options.getNumThreads() >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(options.getNumThreads()));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(options.getMaxRamMb()));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");

        Subprocesses.run(cmd, env, logService::info, logService::error);

        predictions = new Hdf5DataSetReader<T>(
                predictionsPath.toString(),
                "exported_data",
                "tzyxc",
                logService,
                statusService).read();
    }

    @SuppressWarnings("unchecked")
    public final void writeHdf5(Path path, Dataset dataset) {
        new Hdf5DataSetWriter<>(
                (ImgPlus<T>) dataset.getImgPlus(),
                path.toString(),
                "data",
                1,
                logService,
                statusService).write();
    }

    public abstract List<String> workflowArgs(Path tempDir);
}
