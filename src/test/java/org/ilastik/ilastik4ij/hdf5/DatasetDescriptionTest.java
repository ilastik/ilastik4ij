/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2025 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.ilastik.ilastik4ij.ResourceLoader.copyResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatasetDescriptionTest {
    @TempDir
    static Path tempDir;

    static File sourceHdf5;

    @BeforeAll
    static void setUpClass() {
        sourceHdf5 = copyResource("/test_axes.h5", tempDir).toFile();
    }

    @Test
    void testOfHdf5ReadsAxisTags() {
        try (IHDF5Reader reader = HDF5Factory.openForReading(sourceHdf5)) {
            DatasetDescription dd = DatasetDescription.ofHdf5(reader, "/exported_data").orElseThrow(() -> new IllegalStateException("test data not setup"));
            List<AxisType> expectedAxes = Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z);
            assertFalse(dd.axesGuessed);
            assertEquals(expectedAxes, dd.axes);
        }
    }
}
