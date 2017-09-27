/**
 * MIT License
 *
 * Copyright (c) 2017 ilastik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Carsten Haubold
 */
package org.ilastik.ilastik4ij;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.options.OptionsPlugin;


/**
 * The ilastik options let you configure where your ilastik installation is,
 * and how many processors and RAM it is allowed to use.
 *
 * Because of the way option plugins work in ImageJ, there is always just one instance
 * of this class, that can be requested by every plugin to get these values of
 * shared configuration.
 */
@Plugin(type = OptionsPlugin.class, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public class IlastikOptions extends OptionsPlugin {

    @Parameter
    LogService log;

    // own parameters:
    @Parameter(label = "Path to ilastik executable")
    private File executableFilePath = new File("/Users/chaubold/Desktop/ilastik-1.2.0-OSX.app");

    @Parameter(label = "Number of Threads ilastik is allowed to use.\nNegative numbers means no restriction")
    private int numThreads = -1;

    @Parameter(min = "256", label="Maximum amount of RAM (in MB) that ilastik is allowed to use.")
    private int maxRamMb = 4096;
    
    private static String getOS() {
        return System.getProperty("os.name", "generic").toLowerCase();
    }

    public String getExecutableFilePath() {
        final String os = getOS();
        String macExtension = "";
        // On Mac OS X we must call the program within the app to be able to add arguments
        if (os.contains("mac") || os.contains("darwin")) 
        {
            macExtension = "/Contents/MacOS/ilastik";
        }
        return executableFilePath.getAbsolutePath().concat(macExtension);
    }

    public int getMaxRamMb() {
        return maxRamMb;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setExecutableFilePath(String executableFilePath) {
        this.executableFilePath = new File(executableFilePath);
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void setMaxRamMb(int maxRamMb) {
        this.maxRamMb = maxRamMb;
    }

    /**
     * As soon as all parameters above have been set, the service is properly
     * configured
     * @return True if properly configured
     */
    public Boolean isConfigured() {
        Path p = Paths.get(getExecutableFilePath());
        return Files.exists(p) && maxRamMb > 0;
    }

    public void configureProcessBuilderEnvironment(ProcessBuilder pb) {
        final Map<String, String> env = pb.environment();
        if (numThreads >= 0) {
            env.put("LAZYFLOW_THREADS", String.valueOf(numThreads));
        }
        env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(maxRamMb));
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("LC_CTYPE", "en_US.UTF-8");
    }
}
