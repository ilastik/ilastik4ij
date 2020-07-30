package org.ilastik.ilastik4ij;

import net.imagej.display.process.SingleInputPreprocessor;
import org.scijava.Priority;
import org.scijava.display.DisplayService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Optional;

@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class ActiveProjectPreprocessor extends SingleInputPreprocessor<Project> {
    @Parameter(required = false)
    public DisplayService displayService;

    public ActiveProjectPreprocessor() {
        super(Project.class);
    }

    @Override
    public Project getValue() {
        return Optional.ofNullable(displayService)
                .map(ds -> ds.getDisplaysOfType(ProjectDisplay.class))
                .flatMap(pds -> pds.stream().findFirst())
                .flatMap(pd -> pd.stream().findFirst())
                .orElse(null);
    }
}
