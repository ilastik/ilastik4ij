package org.ilastik.ilastik4ij.ui;

import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.util.PlatformUtils;
import org.scijava.widget.FileWidget;

import java.io.File;


/**
 * The ilastik options let you configure where your ilastik installation is,
 * and how many processors and RAM it is allowed to use.
 * <p>
 * Because of the way option plugins work in ImageJ, there is always just one instance
 * of this class, that can be requested by every plugin to get these values of
 * shared configuration.
 */
@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public class IlastikOptions extends OptionsPlugin {
    private static final String ILASTIK_PATH_WIN = "C:\\\\Program Files\\ilastik-1.3.3\\ilastik.exe";
    private static final String ILASTIK_PATH_LINUX = "/opt/ilastik-1.3.3-Linux/run_ilastik.sh";
    private static final String ILASTIK_PATH_MACOS = "/Applications/ilastik-1.3.3-OSX.app/Contents/MacOS/ilastik";

    @Parameter(label = "Path to ilastik executable", style = FileWidget.OPEN_STYLE)
    private File executableFile = new File("/opt/ilastik/run_ilastik.sh");

    @Parameter(label = "Number of Threads ilastik is allowed to use.\nNegative numbers means no restriction")
    private int numThreads = -1;

    @Parameter(min = "256", label = "Maximum amount of RAM (in MB) that ilastik is allowed to use.")
    private int maxRamMb = 4096;

	@Override
	public void initialize() {
        String executableLocation = null;

        if (PlatformUtils.isWindows()) {
            executableLocation = ILASTIK_PATH_WIN;
        } else if (PlatformUtils.isLinux()) {
            executableLocation = ILASTIK_PATH_LINUX;
        } else if (PlatformUtils.isMac()) {
            executableLocation = ILASTIK_PATH_MACOS;
            getExecutableFileItem().setWidgetStyle(FileWidget.DIRECTORY_STYLE);
        }

        if (executableLocation != null) {
            getExecutableFileItem().setLabel("Path to ilastik executable, e.g. " + executableLocation);
        }
	}

	private final MutableModuleItem<File> getExecutableFileItem(){
        final MutableModuleItem<File> executableFileItem = getInfo().getMutableInput("executableFile", File.class);
        return executableFileItem;
    }

    public File getExecutableFile() {
        return executableFile;
    }

    public int getMaxRamMb() {
        return maxRamMb;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setExecutableFile(File executableFile) {
        this.executableFile = executableFile;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void setMaxRamMb(int maxRamMb) {
        this.maxRamMb = maxRamMb;
    }
}
