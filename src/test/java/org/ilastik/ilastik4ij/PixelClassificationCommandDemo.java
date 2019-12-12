package org.ilastik.ilastik4ij;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.ui.IlastikPixelClassificationCommand;
import org.ilastik.ilastik4ij.ui.UiConstants;
import org.scijava.Context;

import java.io.File;
import java.io.IOException;

public class PixelClassificationCommandDemo {
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
		Context context = ij.getContext();
		DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
		final Dataset inputDataset = datasetIOService.open( inputImagePath );

		// Configure options
		//
		final IlastikOptions options = new IlastikOptions();
		options.setExecutableFile( new File( ilastikPath ) );

		// Classify pixels
		//
		final IlastikPixelClassificationCommand command = new IlastikPixelClassificationCommand();
		command.logService = ij.log();
		command.statusService = ij.status();
		command.uiService = ij.ui();
		command.optionsService = ij.options();
		command.ilastikOptions = options;
		command.pixelClassificationType = UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES;
		command.inputImage = inputDataset;
		command.projectFileName = new File( ilastikProjectPath );
		command.run();
	}
}
