package org.ilastik.ilastik4ij.ui;

import ij.IJ;

public class DisplayUtils
{
	public static void applyGlasbeyLUT()
	{
		try
		{
			IJ.run("glasbey_inverted");
		} catch ( Exception e )
		{
			throw new UnsupportedOperationException( "LUT not found: " + "glasbey_inverted"	 );
		}
	}
}
