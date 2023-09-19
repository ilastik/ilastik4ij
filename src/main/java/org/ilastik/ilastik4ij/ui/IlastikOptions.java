package org.ilastik.ilastik4ij.ui;

import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.PlatformUtils;
import org.scijava.widget.FileWidget;

import java.io.File;

import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(
        type = OptionsPlugin.class,
        menuPath = "Plugins>ilastik>Configure ilastik executable location")
public final class IlastikOptions extends OptionsPlugin {
    @Parameter(
            label = "Executable path",
            description = "Path to ilastik executable",
            style = FileWidget.OPEN_STYLE)
    public File executableFile;

    @Parameter(label = "Example path", visibility = MESSAGE)
    private String examplePath = "";

    @Parameter(
            label = "Threads (-1 for all)",
            description = "Number of threads that ilastik is allowed to use, " +
                    "or \"-1\" for all available threads")
    public int numThreads = -1;

    @Parameter(
            label = "Memory (MiB)",
            min = "256",
            description = "Maximum amount of RAM (in megabytes) that ilastik is allowed to use")
    public int maxRamMb = 4096;

    @Override
    public void initialize() {
        String path = "";
        if (PlatformUtils.isWindows()) {
            path = "C:\\\\Program Files\\ilastik-1.4.0\\ilastik.exe";
        } else if (PlatformUtils.isLinux()) {
            path = "/opt/ilastik-1.4.0-Linux/run_ilastik.sh";
        } else if (PlatformUtils.isMac()) {
            path = "/Applications/ilastik-1.4.0-OSX.app/Contents/MacOS/ilastik";
        }

        if (!path.isEmpty()) {
            examplePath = "<pre>" + path + "</pre>";
        }
    }
}
