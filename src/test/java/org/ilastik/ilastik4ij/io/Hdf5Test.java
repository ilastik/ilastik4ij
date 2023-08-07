package org.ilastik.ilastik4ij.io;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.RealType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class Hdf5Test {
    @RunWith(Parameterized.class)
    public static class ListDatasetsTest {
        @Rule
        public TemporaryFolder temp = new TemporaryFolder();

        private final String resource;
        private final List<Hdf5.DatasetDescription> expected;
        private List<Hdf5.DatasetDescription> actual;

        @Parameters

        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"/test.h5", Collections.singletonList(
                            new Hdf5.DatasetDescription(
                                    "exported_data",
                                    Hdf5.DatasetType.UINT16,
                                    new long[]{3, 4, 5, 6, 7},
                                    new AxisType[]{Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z, Axes.TIME})
                    )},
                    {"/test_axes.h5", Arrays.asList(
                            new Hdf5.DatasetDescription(
                                    "dataset_without_axes",
                                    Hdf5.DatasetType.INT64,
                                    new long[]{64, 64},
                                    new AxisType[]{Axes.X, Axes.Y}),
                            new Hdf5.DatasetDescription(
                                    "exported_data",
                                    Hdf5.DatasetType.FLOAT32,
                                    new long[]{1, 256, 256, 256},
                                    new AxisType[]{Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z})
                    )}
            });
        }

        public ListDatasetsTest(String resource, List<Hdf5.DatasetDescription> expected) {
            this.resource = resource;
            this.expected = expected;
        }

        @Before
        public void setUp() {
            actual = Hdf5.datasets(fromResource(temp, resource));
        }

        @Test
        public void testDatasetDescription() {
            assertEquals(expected, actual);
        }
    }

    @RunWith(Parameterized.class)
    public static class ReadDatasetTest {
        @Rule
        public TemporaryFolder temp = new TemporaryFolder();

        private final String resource;
        private final String path;
        private final List<AxisType> axes;
        private final Hdf5.DatasetType type;
        private final long[] dims;
        private final Object[][] values;
        private ImgPlus<?> img;

        @Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"/test.h5",
                            "exported_data",
                            Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z, Axes.TIME),
                            Hdf5.DatasetType.UINT16,
                            new long[]{4, 5, 3, 6, 7},
                            new Object[][]{
                                    {1, 0, 0, 5, 6, 200},
                                    {1, 0, 0, 5, 5, 0},
                                    {1, 0, 0, 4, 6, 200}}
                    },
                    {"/test.h5",
                            "exported_data",
                            Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y, Axes.TIME, Axes.Z),
                            Hdf5.DatasetType.UINT16,
                            new long[]{4, 5, 3, 7, 6},
                            new Object[][]{}
                    },
                    {"/test.h5",
                            "exported_data",
                            Arrays.asList(Axes.X, Axes.CHANNEL, Axes.Y, Axes.TIME, Axes.Z),
                            Hdf5.DatasetType.UINT16,
                            new long[]{3, 5, 4, 7, 6},
                            new Object[][]{}
                    },
            });
        }

        public ReadDatasetTest(
                String resource,
                String path,
                List<AxisType> axes,
                Hdf5.DatasetType type,
                long[] dims,
                Object[][] values) {
            this.resource = resource;
            this.path = path;
            this.axes = axes;
            this.type = type;
            this.dims = dims;
            this.values = values;
        }

        @Before
        public void setUp() {
            img = Hdf5.readDataset(
                    fromResource(temp, resource), path, axes.toArray(new AxisType[0]));
        }

        @Test
        public void testSuffix() {
            String name = img.getName();
            String suffix = "/" + path;
            assertTrue(
                    String.format("Expected '%s' to end with '%s'", name, suffix),
                    name.endsWith(suffix));
        }

        @Test
        public void testAxes() {
            int n = img.numDimensions();
            List<AxisType> expected = Hdf5.DEFAULT_AXES.subList(0, n);
            List<AxisType> actual = IntStream.range(0, n)
                    .mapToObj(d -> img.axis(d).type())
                    .collect(Collectors.toList());
            assertEquals(expected, actual);
        }

        @Test
        public void testType() {
            assertEquals(type.imglib2Type().getClass(), img.firstElement().getClass());
        }

        @Test
        public void testDims() {
            assertArrayEquals(dims, img.dimensionsAsLongArray());
        }

        @Test
        public void testValues() {
            int n = img.numDimensions();
            for (Object[] idxAndVal : values) {
                assert idxAndVal.length == n + 1;
                long[] idx = Arrays.stream(idxAndVal)
                        .limit(n)
                        .mapToLong(x -> ((Number) x).longValue())
                        .toArray();
                // Double is the most common primitive type across supported types.
                double expected = ((Number) idxAndVal[n]).doubleValue();
                double actual = ((RealType<?>) getAt(img, idx, axes)).getRealDouble();
                // Compare exact values (delta == 0) because we only test reading.
                assertEquals(expected, actual, 0);
            }
        }
    }

    private static File fromResource(TemporaryFolder tempFolder, String resource) {
        try (InputStream in = Hdf5Test.class.getResourceAsStream(resource)) {
            Objects.requireNonNull(in);
            File tempFile = tempFolder.newFile(resource);
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T getAt(ImgPlus<T> img, long[] idx, List<AxisType> axes) {
        List<AxisType> imgAxes = IntStream.range(0, img.numDimensions())
                .mapToObj(d -> img.axis(d).type())
                .collect(Collectors.toList());
        long[] imgIdx = imgAxes.stream()
                .mapToLong(axis -> idx[axes.indexOf(axis)])
                .toArray();
        return img.getAt(imgIdx);
    }
}