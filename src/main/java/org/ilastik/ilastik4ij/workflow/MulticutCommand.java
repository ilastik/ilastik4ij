package org.ilastik.ilastik4ij.workflow;

import net.imagej.Dataset;
import net.imglib2.type.NativeType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Multicut")
public final class MulticutCommand<T extends NativeType<T>> extends WorkflowCommand<T> {
    @Parameter(label = "Boundary prediction image")
    public Dataset input2;

    @Override
    public List<String> workflowArgs(Path tempDir) {
        Path input2Path = tempDir.resolve("input2.h5");
        writeHdf5(input2Path, input2);

        return Arrays.asList(
                "--export_source=Multicut Segmentation",
                "--probabilities=" + input2Path);
    }
}
