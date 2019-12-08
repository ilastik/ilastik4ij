package org.ilastik.ilastik4ij;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.logging.LogServiceWrapper;
import org.ilastik.ilastik4ij.executors.ObjectClassification;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;

import java.io.File;
import java.io.IOException;

import static org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.*;

public class ObjectClassificationTest
{
	public static < R extends RealType< R > > void main( String[] args ) throws IOException
	{
		final String ilastikPath = "/Applications/ilastik-1.3.3-OSX.app/Contents/MacOS/ilastik";
		final String rawInputImagePath = "/Users/tischer/Documents/tobias-kletter/ilastik-test/tubulin_dna_volume.zip";
		final String probabilitiesInputImagePath = "/Users/tischer/Documents/tobias-kletter/ilastik-test/tubulin_dna_volume-data_Probabilities.h5";
		final String ilastikProjectPath = "/Users/tischer/Documents/tobias-kletter/ilastik-test/tubulin_dna_volume_object_classification.ilp";

		// Run test
		//
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Open input images
		//
		final ImagePlus rawInputImp = IJ.openImage( rawInputImagePath );

		// open ilastik hdf5 output
		ImgPlus< R > probabilitiesInput = new Hdf5DataSetReader( probabilitiesInputImagePath, "exported_data", "tzyxc", new LogServiceWrapper( ij.log() ), ij.status() ).read();

		rawInputImp.show();
		ImageJFunctions.show( probabilitiesInput, "probabilities input" );

		// Classify pixels
		//

		final ObjectClassification prediction =
				new ObjectClassification(
						new File( ilastikPath ),
						new File( ilastikProjectPath ),
						new LogServiceWrapper( ij.log() ),
						ij.status(),
						4,
						10000 );

		final ImgPlus< R > predictedObjects =
				prediction.classifyObjects(
						ij.convert().convert( rawInputImp, ImgPlus.class ),
						probabilitiesInput,
						PixelClassificationType.Probabilities );

		ImageJFunctions.show( predictedObjects, "object classification" );
	}

}
