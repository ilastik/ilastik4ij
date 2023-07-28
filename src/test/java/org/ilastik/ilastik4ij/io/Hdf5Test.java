package org.ilastik.ilastik4ij.io;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
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
        private final String expectedSuffix;
        private final long[] expectedDimensions;
        private final AxisType[] expectedAxes;
        private final int expectedBits;
        private final NativeType<?> expectedType;
        private final Object[][] expectedPosAndVals;
        private ImgPlus<?> actual;

        @Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"/test.h5",
                            "/exported_data",
                            new long[]{3, 4, 5, 6, 7},
                            new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME},
                            16,
                            new UnsignedShortType(),
                            new Object[][]{
                                    {1, 0, 0, 5, 6, 200},
                                    {1, 0, 0, 5, 5, 0},
                                    {1, 0, 0, 4, 6, 200},
                            },
                    },
                    {"/test_axes.h5",
                            "/exported_data",
                            new long[]{1, 256, 256, 256},
                            new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z},
                            32,
                            new FloatType(),
                            new Object[][]{},
                    },
                    {"/test_axes.h5",
                            "/dataset_without_axes",
                            new long[]{64, 64},
                            new AxisType[]{Axes.X, Axes.Y},
                            64,
                            new LongType(),
                            new Object[][]{},
                    },
            });
        }

        public ReadDatasetTest(
                String resource,
                String path,
                long[] shape,
                AxisType[] axes,
                int bits,
                NativeType<?> type,
                Object[][] posAndVals) {
            this.resource = resource;
            this.path = path;
            expectedSuffix = path;
            expectedDimensions = shape;
            expectedAxes = axes;
            expectedBits = bits;
            expectedType = type;
            expectedPosAndVals = posAndVals;
        }

        @Before
        public void setUp() {
            actual = Hdf5.readDataset(fromResource(temp, resource), path);
        }

        @Test
        public void testDimensions() {
            assertArrayEquals(expectedDimensions, actual.dimensionsAsLongArray());
        }

        @Test
        public void testAxes() {
            AxisType[] actualAxes = new AxisType[actual.numDimensions()];
            IntStream.range(0, actualAxes.length)
                    .forEach(i -> actualAxes[i] = actual.axis(i).type());
            assertArrayEquals(expectedAxes, actualAxes);
        }

        @Test
        public void testBits() {
            assertEquals(expectedBits, actual.getValidBits());
        }

        @Test
        public void testNameSuffix() {
            assertTrue(actual.getName().endsWith(expectedSuffix));
        }

        @Test
        public void testImgClass() {
            assertEquals(actual.getImg().getClass(), ArrayImg.class);
        }

        @Test
        public void testElementClass() {
            assertEquals(actual.firstElement().getClass(), expectedType.getClass());
        }

        @Test
        public void testValues() {
            int n = expectedDimensions.length;

            for (Object[] posAndVal : expectedPosAndVals) {
                long[] pos = Arrays.stream(posAndVal)
                        .limit(n)
                        .mapToLong(d -> ((Number) d).longValue())
                        .toArray();

                Object expectedVal = posAndVal[n];
                Object actualVal = actual.getAt(pos);

                // Compare strings because there is no simple way to get Number from NativeType.
                assertEquals(
                        String.format(
                                "Expected value at %s to be %s, actual is %s",
                                Arrays.toString(pos),
                                expectedVal,
                                actualVal),
                        expectedVal.toString(),
                        actualVal.toString());
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
}