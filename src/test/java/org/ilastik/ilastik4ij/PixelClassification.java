package org.ilastik.ilastik4ij;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.ilastik.ilastik4ij.executors.PixelClassificationPrediction;

import java.io.File;
import java.io.IOException;

public class PixelClassification
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();

		// Create input image
		//
		final ImagePlus imagePlus = IJ.openImage( "/Users/tischer/Documents/fiji-plugin-morphometry/src/test/resources/test-data/spindle/SpindleVolumeTest01.zip" );
		final RandomAccessibleInterval< ? > raiXYCZ = ImageJFunctions.wrapReal( imagePlus );
		DatasetService datasetService = ij.dataset();
		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z };
		ImgPlus imgPlus = new ImgPlus( datasetService.create( raiXYCZ  ), "image", axisTypes );

		ij.ui().show( imgPlus );

		// Run ilastik
		//
		final File ilastikApp = new File( "/Applications/ilastik-1.3.3-OSX.app" );
		final File ilastikProject = new File( "/Users/tischer/Documents/tobias-kletter/20191206_DNA_Segmentation_2Ch.ilp" );
		final PixelClassificationPrediction prediction = new PixelClassificationPrediction( ilastikApp, ilastikProject, true );
		prediction.runIlastik( imgPlus );

	}
}
