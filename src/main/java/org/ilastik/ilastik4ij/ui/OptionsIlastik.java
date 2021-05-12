package org.ilastik.ilastik4ij.ui;

import org.scijava.ItemVisibility;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;

@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public class OptionsIlastik extends OptionsPlugin {
    @Parameter(label = "ilastik executable path", style = FileWidget.OPEN_STYLE)
    private File executableFile;

    @Parameter(min = "-1", label = "maximum number of threads (\"-1\" means no restriction)")
    private int numThreads = -1;

    @Parameter(min = "256", label = "maximum amount of megabytes")
    private int maxRamMb = 4096;

    @SuppressWarnings("unused")
    @Parameter(visibility = ItemVisibility.MESSAGE)
    private final String examples = "<html>ilastik executable path examples:<dl>" +
            "<dt>Windows</dt>" +
            "<dd><tt>C:\\\\Program Files\\ilastik-1.3.3\\ilastik.exe</tt></dd>" +
            "<dt>macOS</dt>" +
            "<dd><tt>/Applications/ilastik-1.3.3-OSX.app</tt></dd>" +
            "<dt>Linux</dt>" +
            "<dd><tt>/opt/ilastik-1.3.3-Linux/run_ilastik.sh</tt></dd>" +
            "</dl></html>";

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
