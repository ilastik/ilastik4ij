package org.ilastik.ilastik4ij.ui;

import ij.IJ;

public class DisplayUtils
{
	public static final String GLASBEY_INVERTED = "glasbey_inverted";

	public static void applyLUT(String lutName)
	{
		try
		{
			IJ.run( lutName );
		} catch ( Exception e )
		{
			throw new UnsupportedOperationException("LUT not found: " + lutName);
		}
	}

	public static void applyGlasbeyLUT() {
		applyLUT( GLASBEY_INVERTED );
	}
}
