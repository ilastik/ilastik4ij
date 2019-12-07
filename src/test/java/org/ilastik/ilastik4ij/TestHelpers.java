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

public abstract class TestHelpers
{
	public static < R extends RealType< R > > ImgPlus< R > openImg( String inputImagePath, ImageJ ij )
	{
		final ImagePlus imagePlus = IJ.openImage( inputImagePath );
		final RandomAccessibleInterval< R > raiXYCZ = ImageJFunctions.wrapReal( imagePlus );
		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z };
		return ( ImgPlus< R > ) new ImgPlus( ij.dataset().create( raiXYCZ ), "image", axisTypes );
	}
}
