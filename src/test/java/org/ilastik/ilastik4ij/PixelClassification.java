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
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.executors.LogServiceWrapper;

import java.io.File;
import java.io.IOException;

public class PixelClassification
{
	public static < R extends RealType< R > > void main( String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();

		// Create input image
		//
		final ImagePlus imagePlus = IJ.openImage( "/Users/tischer/Documents/fiji-plugin-morphometry/src/test/resources/test-data/spindle/SpindleVolumeTest01.zip" );
		final RandomAccessibleInterval< R > raiXYCZ = ImageJFunctions.wrapReal( imagePlus );
		DatasetService datasetService = ij.dataset();
		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z };
		ImgPlus< R > imgPlus = new ImgPlus( datasetService.create( raiXYCZ  ), "image", axisTypes );

		ij.ui().show( imgPlus );

		// Classify pixels with ilastik
		//
		final File ilastikApp = new File( "/Applications/ilastik-1.3.3-OSX.app" );
		final File ilastikProject = new File( "/Users/tischer/Documents/tobias-kletter/20191206_DNA_Segmentation_2Ch.ilp" );
		final org.ilastik.ilastik4ij.executors.PixelClassification prediction = new org.ilastik.ilastik4ij.executors.PixelClassification( ilastikApp, ilastikProject,  new LogServiceWrapper( ij.log() ), ij.status(), 4, 10000 );
		final ImgPlus< ? > classifiedPixels = prediction.classifyPixels( imgPlus, org.ilastik.ilastik4ij.executors.PixelClassification.OutputType.Probabilities );

		ij.ui().show( classifiedPixels );
	}
}
