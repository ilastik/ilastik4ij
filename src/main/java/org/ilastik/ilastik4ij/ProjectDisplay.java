package org.ilastik.ilastik4ij;

import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class ProjectDisplay extends AbstractDisplay<Project> {
    public ProjectDisplay() {
        super(Project.class);
    }
}
