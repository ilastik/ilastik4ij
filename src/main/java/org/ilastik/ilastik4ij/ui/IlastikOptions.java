/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2026 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
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
            path = "C:\\Program Files\\ilastik-1.4.0\\ilastik.exe";
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
