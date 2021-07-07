package org.ilastik.ilastik4ij;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.util.Subprocesses;
import org.ilastik.ilastik4ij.util.TempDir;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.DefaultMutableModuleItem;
import org.scijava.module.ModuleItem;
import org.scijava.module.MutableModuleItem;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(
        type = Command.class,
        menuPath = "Plugins>ilastik>Run ilastik Workflow",
        headless = true)
public class Workflow extends DynamicCommand implements Initializable {
    private static final String AXIS_ORDER = "tzyxc";
    private static final String OUTPUT_INTERNAL_PATH = "exported_data";
    private static final String OUTPUT_FILENAME_FORMAT = "_output.h5";
    private static final String LOCALE = "en_US.UTF-8";

    @Parameter
    public LogService logService;

    @Parameter
    public StatusService statusService;

    @Parameter
    public OptionsService optionsService;

    @Parameter(label = "Trained ilastik Project File")
    public Project project;

    // NOTE: Cannot dynamically add outputs yet due to the bug in scijava-common: https://github.com/scijava/scijava-common/pull/392.
    @Parameter(type = ItemIO.OUTPUT)
    public Dataset output;

    private List<ModuleItem<?>> argInputs;

    @Override
    public void initialize() {
        List<ModuleItem<?>> items = new ArrayList<>();

        items.add(datasetItem("raw_data", "Raw Input Image"));
        items.add(exportSourceItem(project.type.exportSources));

        items.forEach(this::addInput);
        argInputs = Collections.unmodifiableList(items);
    }

    private ModuleItem<?> datasetItem(String name, String label) {
        MutableModuleItem<?> item = new DefaultMutableModuleItem<>(
                this, name, Dataset.class);
        item.setLabel(label);
        return item;
    }

    private ModuleItem<?> exportSourceItem(List<String> choices) {
        MutableModuleItem<String> item = new DefaultMutableModuleItem<>(
                this, "export_source", String.class);
        item.setLabel("Export Source");
        item.setChoices(choices);
        item.setWidgetStyle("radioButtonHorizontal");
        return item;
    }

    @Override
    public void run() {
        try (TempDir tempDir = new TempDir("ilastik4ij_")) {

            List<String> args = createArgs(tempDir.path);
            Map<String, String> env = getEnv();

            logService.info("ilastik args: " + args);
            logService.info("ilastik env: " + env);
            Subprocesses.run(args, env, logService::info, logService::error);

            output = readHdf5(tempDir.path.resolve(OUTPUT_FILENAME_FORMAT).toString());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> createArgs(Path tempDirPath) {
        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);

        Stream<String> initialArgs = Stream.of(
                opts.getExecutableFile().getAbsolutePath(),
                "--headless",
                "--readonly",
                "--input_axes", AXIS_ORDER,
                "--output_axis_order", AXIS_ORDER,
                "--output_format", "hdf5",
                "--output_internal_path", OUTPUT_INTERNAL_PATH,
                "--output_filename_format", tempDirPath.resolve(OUTPUT_FILENAME_FORMAT).toString(),
                "--project", project.path.toString());

        Stream<String> inputArgs = argInputs.stream()
                .flatMap(item -> Stream.of(
                        "--" + item.getName(), moduleItemValue(item, tempDirPath)));

        return Collections.unmodifiableList(
                Stream.concat(initialArgs, inputArgs)
                        .collect(Collectors.toList()));
    }

    private String moduleItemValue(ModuleItem<?> item, Path tempDirPath) {
        if (item.getType().isAssignableFrom(Dataset.class)) {
            String filename = tempDirPath.resolve(item.getName() + ".h5").toString();
            writeHdf5((Dataset) item.getValue(this), filename);
            return filename;
        }

        // TODO: Support tables by writing them into a CSV file.

        return item.getValue(this).toString();
    }

    private Map<String, String> getEnv() {
        IlastikOptions opts = optionsService.getOptions(IlastikOptions.class);
        Map<String, String> env = new HashMap<>();

        if (opts.getNumThreads() >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(opts.getNumThreads()));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(opts.getMaxRamMb()));
        env.put("LANG", LOCALE);
        env.put("LC_ALL", LOCALE);
        env.put("LC_CTYPE", LOCALE);

        return Collections.unmodifiableMap(env);
    }

    private Dataset readHdf5(String filename) {
        ImgPlus imgPlus = new Hdf5DataSetReader(
                filename,
                OUTPUT_INTERNAL_PATH,
                AXIS_ORDER,
                logService,
                statusService).read();
        return new DefaultDataset(context(), imgPlus);
    }

    private void writeHdf5(Dataset dataset, String filename) {
        new Hdf5DataSetWriter(
                dataset.getImgPlus(),
                filename,
                "data",
                1,
                logService,
                statusService).write();
    }
}
