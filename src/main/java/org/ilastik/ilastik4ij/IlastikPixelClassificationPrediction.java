/**
MIT License

Copyright (c) 2017 ilastik

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Author: Carsten Haubold
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
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Pixel Classification Prediction")
public class IlastikPixelClassificationPrediction<T extends RealType<T>> implements Command {

	// needed services:
	@Parameter
	OpService ops;

	@Parameter
	LogService log;

	@Parameter
	IlastikService ilastikService;

	// own parameters:
	@Parameter(label = "Save temporary file for training only, without prediction.")
	private Boolean saveOnly = false;
	
	@Parameter(label = "Trained ilastik project file")
	private String projectFileName = "/Users/chaubold/hci/data/divisionTestDataset/pc_test.ilp";

	@Parameter
	ImgPlus<T> inputImage;

	@Parameter(type = ItemIO.OUTPUT)
	ImgPlus<FloatType> predictions;

	/**
	 * Run method that calls ilastik
	 */
	@Override
	public void run() {
		if(!ilastikService.isConfigured())
		{
			log.error("ilastik service must be configured before use!");
		}
		
//		String tempInFileName = "/tmp/blaIn.tif";
		String tempInFileName = "/Users/chaubold/hci/data/divisionTestDataset/dataset_003.tif";
		log.info("Dumping input image to temporary file " + tempInFileName);

		if(saveOnly)
		{
			log.info("Saved file for training to " + tempInFileName 
					+ ". Use it to train an ilastik pixelClassificationProject now,"
					+ " and make sure to select to copy the raw data into the project file in the data selection");
			return;
		}
		
		String tempOutFileName;
		try {
			tempOutFileName = IlastikUtilities.getTemporaryFileName(".tif");
		} catch (IOException e) {
			log.error("Could not create a temporary file for obtaining the results from ilastik");
			e.printStackTrace();
			return;
		}
		
		runIlastik(tempInFileName, tempOutFileName);
		log.info("Reading resulting probabilities from " + tempOutFileName);
		
		try {
			predictions = new ImgPlus<FloatType>(new ImgOpener().openImg(tempOutFileName, new ArrayImgFactory< FloatType >(), new FloatType()));
		} catch (ImgIOException e) {
			log.error("Wasn't able to read ilastik result");
			e.printStackTrace();
		}
	}

	private void runIlastik(String tempInFileName, String tempOutFileName) {
		List<String> commandLine = new ArrayList<>();
		commandLine.add(ilastikService.getExecutableFilePath()); 
		commandLine.add("--headless");
		commandLine.add("--project=" + projectFileName); 
		commandLine.add("--output_filename_format=" + tempOutFileName);
		commandLine.add("--output_format=multipage tiff");
		commandLine.add(tempInFileName);
		
		log.info("Running ilastik headless command:");
		log.info(commandLine);
		
		ProcessBuilder pB = new ProcessBuilder(commandLine);
		ilastikService.configureProcessBuilderEnvironment(pB);

        // run ilastik
        Process p;
		try {
			p = pB.start();
			
	        // write ilastik output to IJ log
	        redirectOutputToLogService(p.getInputStream(), log, false);
	        redirectOutputToLogService(p.getErrorStream(), log, true);

	        try {
	            p.waitFor();
	        } catch (InterruptedException cee) {
	            log.warn("Execution got interrupted");
	            p.destroy();
	        }

	        // 0 indicates successful execution
			
			if (p.exitValue() != 0) {
				log.error("ilastik crashed");
	            throw new IllegalStateException("Execution of ilastik was not successful.");
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("ilastik finished successfully!");
	}
	
    /**
     * Redirect an input stream to the log service (used for command line output)
     *
     * @param in input stream
     * @param logService
     * @throws IOException
     */
    private void redirectOutputToLogService(final InputStream in, final LogService logger, final Boolean isErrorStream) {

        Thread t = new Thread() {
            @Override
            public void run() {

                String line;

                try (BufferedReader bis = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
                    while ((line = bis.readLine()) != null) {
                        if (isErrorStream) {
                            logger.error(line);
                        } else {
                            logger.info(line);
                        }
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException("Could not read ilastik output", ioe);
                }
            }
        };

//        t.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(KNIPGateway.log()));
        t.start();
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
			ij.command().run(IlastikPixelClassificationPrediction.class, true);

		} catch (ImgIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
