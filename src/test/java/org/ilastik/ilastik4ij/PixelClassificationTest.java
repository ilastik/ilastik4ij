package org.ilastik.ilastik4ij;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.executors.LogServiceWrapper;
import org.ilastik.ilastik4ij.executors.PixelClassification;

import java.io.File;
import java.io.IOException;

public class PixelClassificationTest
{
	public static < R extends RealType< R > > void main( String[] args ) throws IOException
	{
		final String ilastikPath = "/Applications/ilastik-1.3.3-OSX.app/Contents/MacOS/ilastik";
		final String inputImagePath = "/Users/tischer/Documents/tobias-kletter/ilastik-test/tubulin_dna_volume.zip";
		final String ilastikProjectPath = "/Users/tischer/Documents/tobias-kletter/ilastik-test/tubulin_dna_volume_pixel_classification.ilp";


		// Run test
		//

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Open input image
		//
		ImgPlus< R > rawInput = TestHelpers.openImg( inputImagePath, ij.dataset() );

		ImageJFunctions.show( rawInput, "raw input" );

		// Classify pixels
		//
		final File ilastikApp = new File( ilastikPath );
		final File ilastikProject = new File( ilastikProjectPath );
		final PixelClassification prediction = new PixelClassification( ilastikApp, ilastikProject,  new LogServiceWrapper( ij.log() ), ij.status(), 4, 10000 );
		final ImgPlus< R > classifiedPixels = (ImgPlus)  prediction.classifyPixels( rawInput, PixelClassification.OutputType.Segmentation );

		ImageJFunctions.show( classifiedPixels, "segmentation" );
	}

}
