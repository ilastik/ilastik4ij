package org.ilastik.ilastik4ij;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class TestHelpers
{
	public static < R extends RealType< R > > ImgPlus< R > openImg( String inputImagePath, DatasetService dataset )
	{
		final ImagePlus imagePlus = IJ.openImage( inputImagePath );
		final RandomAccessibleInterval< R > raiXYCZ = ImageJFunctions.wrapReal( imagePlus );
		final RandomAccessibleInterval< R > raiXYCZT = Views.addDimension( raiXYCZ, 0, 0 );
		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME };
		return ( ImgPlus< R > ) new ImgPlus( dataset.create( raiXYCZT ), "image", axisTypes );
	}
}
