package org.ilastik.ilastik4ij.ui;

import org.scijava.module.MutableModuleItem;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.PlatformUtils;
import org.scijava.widget.FileWidget;

import java.io.File;

@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public final class IlastikOptions extends OptionsPlugin {
    @Parameter(label = "Path to ilastik executable", style = FileWidget.OPEN_STYLE)
    public File executableFile = new File("/opt/ilastik/run_ilastik.sh");

    @Parameter(label = "Number of Threads ilastik is allowed to use.\nNegative numbers means no restriction")
    public int numThreads = -1;

    @Parameter(min = "256", label = "Maximum amount of RAM (in MB) that ilastik is allowed to use.")
    public int maxRamMb = 4096;

    @Override
    public void initialize() {
        MutableModuleItem<File> item = getInfo().getMutableInput("executableFile", File.class);
        String path = "";

        if (PlatformUtils.isWindows()) {
            path = "C:\\\\Program Files\\ilastik-1.3.3\\ilastik.exe";
        } else if (PlatformUtils.isLinux()) {
            path = "/opt/ilastik-1.3.3-Linux/run_ilastik.sh";
        } else if (PlatformUtils.isMac()) {
            path = "/Applications/ilastik-1.3.3-OSX.app/Contents/MacOS/ilastik";
            item.setWidgetStyle(FileWidget.DIRECTORY_STYLE);
        }

        if (!path.isEmpty()) {
            item.setLabel(item.getLabel() + ", e.g. " + path);
        }
    }
}
