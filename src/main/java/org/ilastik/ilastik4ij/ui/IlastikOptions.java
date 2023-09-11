package org.ilastik.ilastik4ij.ui;

import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.PlatformUtils;
import org.scijava.widget.FileWidget;

import java.io.File;

import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public final class IlastikOptions extends OptionsPlugin {
    @Parameter(label = "Path to ilastik executable", style = FileWidget.OPEN_STYLE)
    public File executableFile;

    @Parameter(label = "Number of Threads ilastik is allowed to use. Negative numbers means no restriction")
    public int numThreads = -1;

    @Parameter(min = "256", label = "Maximum amount of RAM (in MB) that ilastik is allowed to use.")
    public int maxRamMb = 4096;

    @Parameter(visibility = MESSAGE)
    private String examplePath = "";

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
            getInfo().getMutableInput("examplePath", String.class).setLabel("Example path");
            examplePath = "<pre>" + path + "</pre>";
        }
    }
}
