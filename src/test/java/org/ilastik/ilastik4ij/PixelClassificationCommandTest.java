package org.ilastik.ilastik4ij;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.ui.PixelClassificationCommand;

import java.io.File;
import java.io.IOException;

public class PixelClassificationCommandTest
{
	public static < R extends RealType< R > > void main( String[] args ) throws IOException
	{
		// Configure paths
		//
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

		// Configure options
		//
		final IlastikOptions options = new IlastikOptions();
		options.setExecutableFile( new File( ilastikPath ) );

		// Classify pixels
		//
		final PixelClassificationCommand command = new PixelClassificationCommand();
		command.logService = ij.log();
		command.statusService = ij.status();
		command.optionsService = ij.options();
		command.ilastikOptions = options;
		command.chosenOutputType = "Probabilities";
		command.inputImage = ij.dataset().create( rawInput );
		command.projectFileName = new File( ilastikProjectPath );
		command.run();
	}

}
