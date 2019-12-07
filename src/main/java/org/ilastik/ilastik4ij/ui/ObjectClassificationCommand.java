/**
 * MIT License
 * <p>
 * Copyright (c) 2017 ilastik
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p>
 * Author: Carsten Haubold
 */
package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.ilastik.ilastik4ij.executors.LogServiceWrapper;
import org.ilastik.ilastik4ij.executors.ObjectClassification;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

import static org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.*;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Run Object Classification Prediction")
public class ObjectClassificationCommand implements Command {

    @Parameter
	public LogService logService;

    @Parameter
	public StatusService statusService;

    @Parameter
	public OptionsService optionsService;

    // own parameters:
//    @Parameter(label = "Save temporary file for training only, without prediction.")
//    private Boolean saveOnly = false;

    @Parameter(label = "Trained ilastik project file")
    public File projectFileName;

    @Parameter(label = "Raw input image")
	public Dataset inputImage;

	@Parameter(label = "Pixel Probability or Segmentation image")
	public Dataset inputProbOrSegImage;

	@Parameter(label = "Second Input Type", choices = {"Segmentation", "Probabilities"}, style = "radioButtonHorizontal")
	public String secondInputType = "Probabilities";

	public IlastikOptions ilastikOptions;

	/**
     * Run method that calls ilastik
     */
    @Override
    public void run() {

    	if ( ilastikOptions == null )
			ilastikOptions = optionsService.getOptions(IlastikOptions.class);

		try
		{
			runClassification();
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
    }

	private void runClassification() throws IOException
	{
		final ObjectClassification objectClassification = new ObjectClassification( ilastikOptions.getExecutableFile(), projectFileName, new LogServiceWrapper( logService ), statusService, ilastikOptions.getNumThreads(), ilastikOptions.getMaxRamMb() );

		final PixelClassificationType pixelClassificationType = PixelClassificationType.valueOf( secondInputType );

		final ImgPlus< ? > classifiedObjects = objectClassification.classifyObjects( inputImage.getImgPlus(), inputProbOrSegImage.getImgPlus(), pixelClassificationType );

		ImageJFunctions.show( ( ImgPlus ) classifiedObjects );

		DisplayUtils.applyGlasbeyLUT();
	}

}
