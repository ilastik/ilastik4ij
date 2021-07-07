package org.ilastik.ilastik4ij;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>ilastik>Open ilastik Project", headless = true)
public class OpenProject extends ContextCommand {
    @Parameter(required = false)
    public DisplayService displayService;

    @Parameter
    public File file;

    @Override
    public void run() {
        if (displayService != null) {
            displayService.createDisplay(new Project(file));
        }
    }
}
