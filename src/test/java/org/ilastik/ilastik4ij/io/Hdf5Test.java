package org.ilastik.ilastik4ij.io;

import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Hdf5Test {
    private final List<Hdf5.DatasetDescription> expected;
    private final List<Hdf5.DatasetDescription> actual;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"/test.h5", Collections.singletonList(
                        new Hdf5.DatasetDescription(
                                "exported_data",
                                new UnsignedShortType(),
                                new long[]{3, 4, 5, 6, 7},
                                "cxyzt"))
                },
                {"/test_axes.h5", Arrays.asList(
                        new Hdf5.DatasetDescription(
                                "dataset_without_axes",
                                new LongType(),
                                new long[]{64, 64},
                                "xy"),
                        new Hdf5.DatasetDescription(
                                "exported_data",
                                new FloatType(),
                                new long[]{1, 256, 256, 256},
                                "cxyz"))
                },
        });
    }

    public Hdf5Test(String resource, List<Hdf5.DatasetDescription> expected) throws IOException {
        Path target = Files.createTempFile("", resource.replace('/', '-'));
        try (InputStream in = Hdf5Test.class.getResourceAsStream(resource)) {
            Files.copy(Objects.requireNonNull(in), target, StandardCopyOption.REPLACE_EXISTING);
        }
        this.expected = expected;
        actual = Hdf5.datasets(new File(target.toString()));
    }

    @Test
    public void testDatasets() {
        assertEquals(expected, actual);
    }
}