package org.ilastik.ilastik4ij.ui;

import org.scijava.ItemVisibility;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.PlatformUtils;
import org.scijava.widget.FileWidget;

import java.io.File;

@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public class OptionsIlastik extends OptionsPlugin {
    private static final String EXAMPLES_VALUE;

    static {
        String path = null;
        if (PlatformUtils.isWindows()) {
            path = "C:\\\\Program Files\\ilastik-1.3.3\\ilastik.exe";
        } else if (PlatformUtils.isMac()) {
            path = "/Applications/ilastik-1.3.3-OSX.app";
        } else if (PlatformUtils.isLinux()) {
            path = "/opt/ilastik-1.3.3-Linux/run_ilastik.sh";
        }
        EXAMPLES_VALUE = path != null ?
                String.format("<html>Executable path example: <tt>%s</tt></html>", path) :
                null;
    }

    @Parameter(label = "Path to ilastik executable", style = FileWidget.OPEN_STYLE)
    private File executableFile;

    @Parameter(min = "-1", label = "Number of Threads ilastik is allowed to use.\nNegative numbers means no restriction")
    private int numThreads = -1;

    @Parameter(min = "256", label = "Maximum amount of RAM (in MB) that ilastik is allowed to use.")
    private int maxRamMb = 4096;

    @SuppressWarnings("unused")
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private final String examples = EXAMPLES_VALUE;

    public File getExecutableFile() {
        return executableFile;
    }

    public void setExecutableFile(File executableFile) {
        this.executableFile = executableFile;
    }

    public int getMaxRamMb() {
        return maxRamMb;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setMaxRamMb(int maxRamMb) {
        this.maxRamMb = maxRamMb;
    }
}
