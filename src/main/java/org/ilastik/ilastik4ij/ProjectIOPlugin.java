package org.ilastik.ilastik4ij;

import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;

import java.nio.file.Paths;

@Plugin(type = IOPlugin.class)
public class ProjectIOPlugin extends AbstractIOPlugin<Project> {
    @Override
    public Class<Project> getDataType() {
        return Project.class;
    }

    @Override
    public boolean supportsOpen(String source) {
        return source.endsWith(".ilp");
    }

    @Override
    public Project open(String source) {
        return new Project(Paths.get(source));
    }
}
