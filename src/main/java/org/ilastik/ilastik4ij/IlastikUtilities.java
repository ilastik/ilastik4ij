package org.ilastik.ilastik4ij;

import java.io.File;
import java.io.IOException;

public class IlastikUtilities {
	/**
	 * Utility method to obtain a unique filename
	 * @param extension
	 * @return
	 * @throws IOException
	 */
	public static String getTemporaryFileName(String extension) throws IOException
	{
		File f = File.createTempFile("ilastik4j", extension, null);
		String filename = f.getName();
		f.delete();
		return filename;
	}
}
