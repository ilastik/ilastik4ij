package org.ilastik.ilastik4ij;

import ij.ImagePlus;
import org.junit.Test;
import ij.IJ;
import static org.junit.Assert.assertEquals;

public class ImportMacroRegressionTests {
    private static final String TEST_MACRO_RESOURCE = "src/test/resources/import-1.7.3.ijm";

    @Test
    public void testHeadlessRun() {
        IJ.runMacroFile(TEST_MACRO_RESOURCE);
        ImagePlus img = IJ.getImage();
        int[] imgDims = img.getDimensions();
        assertEquals("DimX should be 1344", 1344, imgDims[0]);
        assertEquals("DimY should be 1024", 1024, imgDims[1]);
        assertEquals("DimC should be 1", 1, imgDims[2]);
        assertEquals("DimZ should be 1", 1, imgDims[3]);
        assertEquals("DimT should be 1", 1, imgDims[4]);
    }
}
