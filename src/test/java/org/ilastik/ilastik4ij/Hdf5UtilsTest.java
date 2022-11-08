package org.ilastik.ilastik4ij;

import org.ilastik.ilastik4ij.util.Hdf5Utils;
import org.ilastik.ilastik4ij.util.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class Hdf5UtilsTest {
    @Test
    public void testParseAxisOrder() throws IOException {
        InputStream projectFileStream = Hdf5UtilsTest.class.getResourceAsStream("/pixel_class_2d_cells_apoptotic.ilp");
        Path tmpIlastikProjectFile = Paths.get(IOUtils.getTemporaryFileName(".ilp"));
        Files.copy(projectFileStream, tmpIlastikProjectFile);

        String axisOrder = Hdf5Utils.parseAxisOrder(tmpIlastikProjectFile.toString());

        assertEquals("tzyxc", axisOrder);
    }
}
