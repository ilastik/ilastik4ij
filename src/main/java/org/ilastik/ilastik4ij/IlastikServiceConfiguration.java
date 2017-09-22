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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * 
 */
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Configure ilastik executable location")
public class IlastikServiceConfiguration implements Command {

	// needed services:
	@Parameter
	LogService log;

	@Parameter
	IlastikService ilastikService;

	// own parameters:
	@Parameter(label = "Path to ilastik executable")
	private String executableFilePath = "/Users/chaubold/Desktop/ilastik-1.2.0-OSX.app";

	@Parameter(label = "Number of Threads ilastik is allowed to use.\nNegative numbers means no restriction")
	private int numThreads = -1;

	@Parameter(min = "256", label="Maximum amount of RAM (in MB) that ilastik is allowed to use.")
	private int maxRamMb = 4096;

	/**
	 * Run method that calls ilastik
	 */
	@Override
	public void run() {
		ilastikService.setExecutableFilePath(executableFilePath);
		ilastikService.setNumThreads(numThreads);
		ilastikService.setMaxRamMb(maxRamMb);
	}
	
	/**
	 * A {@code main()} method for testing: starts up ImageJ with some image, and then invokes this command.
	 */
	public static void main(final String... args) {
		
		// Launch ImageJ as usual.
//		Context c = new Context(IlastikService.class);
		
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		
		final String filename = "/Users/chaubold/hci/data/divisionTestDataset/dataset_001.tif";
		Img<UnsignedShortType> img;
		try {	
			img = new ImgOpener().openImg( filename, new ArrayImgFactory< UnsignedShortType >(), new UnsignedShortType() );
			ij.ui().show(img);
			ij.command().run(IlastikServiceConfiguration.class, true);

		} catch (ImgIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
