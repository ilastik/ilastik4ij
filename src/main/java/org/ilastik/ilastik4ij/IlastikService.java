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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.ImageJService;

/**
 * The ilastik service lets you configure where your ilastik installation is,
 * and how many processors and RAM it is allowed to use.
 * 
 * Because of the way services work in ImageJ, there is always just one instance of this class, 
 * that can be requested by every plugin to get these values of shared configuration. 
 */
@Plugin(type = Service.class)
public class IlastikService extends AbstractService implements ImageJService {

	@Parameter
	LogService log;
	
	// 
	private String executableFilePath = "/Users/chaubold/Desktop/ilastik-1.2.0-OSX.app/Contents/MacOS/ilastik";
	private int numThreads = -1;
	private int maxRamMb = 4096;

	@Override
	public void initialize() {
		log.info("IlastikService is being initialized");
	}
    
	private static String getOS() {
        return System.getProperty("os.name", "generic").toLowerCase();
    }

	public String getExecutableFilePath() {
		final String os = getOS();
        String macExtension = "";
        // On Mac OS X we must call the program within the app to be able to add
        // arguments
        if ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)) {
            macExtension = "/Contents/MacOS/ilastik";
        }
        return executableFilePath.concat(macExtension);
	}
	
	public int getMaxRamMb() {
		return maxRamMb;
	}
	
	public int getNumThreads() {
		return numThreads;
	}
	
	public void setExecutableFilePath(String executableFilePath) {
		this.executableFilePath = executableFilePath;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	public void setMaxRamMb(int maxRamMb) {
		this.maxRamMb = maxRamMb;
	}
	
	/**
	 * As soon as all parameters above have been set, the service is properly configured
	 */
	public Boolean isConfigured() {
		Path p = Paths.get(getExecutableFilePath());
		return Files.exists(p) && maxRamMb > 0;
	}
	
	public void configureProcessBuilderEnvironment(ProcessBuilder pb)
	{
		final Map<String, String> env = pb.environment();
		if(numThreads >= 0)
		{
			env.put("LAZYFLOW_THREADS", String.valueOf(numThreads));
		}
		env.put("LAZYFLOW_TOTAL_RAM_MB", String.valueOf(maxRamMb));
	}
}
