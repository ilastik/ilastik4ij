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

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.converter.ColorChannelOrder;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.ilastik.ilastik4ij.util.ImgUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static org.ilastik.ilastik4ij.util.ImgUtils.DEFAULT_AXES;
import static org.ilastik.ilastik4ij.util.ImgUtils.axesOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class Hdf5Test {
    @TempDir
    static Path tempDir;

    static File sourceHdf5;

    static File sourceHdf5WithMeta;

    static ImgPlus<UnsignedByteType> sampleImg;

    static double[] sampleCalibrations = new double[]{0.148, 5.00000023, 1};

    static String[] sampleUnits = new String[]{"meters", "ü☺µ", ""};

    static ImgPlus<UnsignedByteType> sampleImgWithMeta;

    @BeforeAll
    static void setUpClass() throws IOException {
        sourceHdf5 = copyResource("/test.h5", tempDir).toFile();
        sourceHdf5WithMeta = copyResource("/test_axes.h5", tempDir).toFile();
        sampleImg = new ImageJ()
                .scifio()
                .datasetIO()
                .open(copyResource("/chocolate21.jpg", tempDir).toString())
                .typedImg(new UnsignedByteType());
        sampleImgWithMeta = new ImgPlus<>(sampleImg, sampleImg.getName(), new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL}, sampleCalibrations, sampleUnits);
    }

    @ParameterizedTest
    @MethodSource
    void testReadAxes(long[] expected, String axes) {
        long[] actual = readDataset(axes).dimensionsAsLongArray();
        assertLongsEqual(expected, actual);
    }

    static Stream<Arguments> testReadAxes() {
        // Output axes are always ImageJ default (xyczt)
        // Data axes are volumina default (tzyxc [7, 6, 5, 4, 3]), but readDataset takes axes in reverse order
        return Stream.of(
                arguments(new long[]{4, 5, 3, 6, 7}, "cxyzt"), // Correctly maps data axes to ImageJ
                arguments(new long[]{4, 5, 3, 7, 6}, "cxytz"), // Equivalent to user entering "ztyxc" in the plugin
                arguments(new long[]{3, 5, 4, 7, 6}, "xcytz")); // Flip t<>z and x<>c
    }

    @ParameterizedTest
    @MethodSource
    void testReadAxisMetadata(String axes, long[] expectedShape, double[] expectedResolutions, String[] expectedUnits) {
        ImgPlus<UnsignedShortType> read = readDatasetWithMeta(axes);
        PixelSize sizeMeta = getPixelSize(read);
        assertLongsEqual(expectedShape, read.dimensionsAsLongArray());
        assertResolutionsEqual(expectedResolutions, sizeMeta.resolution);
        assertArrayEqualsFormatted(expectedUnits, sizeMeta.unit);
    }

    static Stream<Arguments> testReadAxisMetadata() {
        double[] imagejDefaultResolutions = {1, 1, 1, 1, 1};
        String[] imagejDefaultUnits = {null, null, null, null, null};
        return Stream.of(
                arguments(null, new long[]{4, 5, 3, 6, 7}, new double[]{0.5, 12.0, 1.0, 3.0, 2.3}, new String[]{"µm", "☺", "", "über", "seconds"}),
                arguments("cxyzt", new long[]{4, 5, 3, 6, 7}, new double[]{0.5, 12.0, 1.0, 3.0, 2.3}, new String[]{"µm", "☺", "", "über", "seconds"}),
                arguments("cxytz", new long[]{4, 5, 3, 7, 6}, imagejDefaultResolutions, imagejDefaultUnits),
                arguments("xcytz", new long[]{3, 5, 4, 7, 6}, imagejDefaultResolutions, imagejDefaultUnits));
    }

    @Test
    void testAxisOrder() {
        assertEquals(DEFAULT_AXES, axesOf(readDataset("cxyzt")));
    }

    @ParameterizedTest
    @MethodSource
    void testImageContents(int expected, String axes, long[] position) {
        int actual = readDataset(axes).getAt(position).get();
        assertEquals(expected, actual);
    }

    static Stream<Arguments> testImageContents() {
        return Stream.of(
                arguments(200, "cxyzt", new long[]{0, 0, 1, 5, 6}),
                arguments(0, "cxyzt", new long[]{0, 0, 1, 5, 5}),
                arguments(200, "cxyzt", new long[]{0, 0, 1, 4, 6}));
    }

    @Test
    void testWrittenDims() {
        long[] expected = {400, 289, 3, 1, 1};
        long[] actual = writeSampleAndReadBack(new UnsignedByteType()).dimensionsAsLongArray();
        assertLongsEqual(expected, actual);
    }

    @ParameterizedTest
    @MethodSource
    <T extends NativeType<T> & RealType<T>> void testWrittenContents(
            Class<T> typeClass, long[] index) throws Exception {
        // Intentionally compare doubles for the exact match: source contents are integers,
        // and double is the most common real type.
        long[] dstIndex = Arrays.copyOf(index, DEFAULT_AXES.size());
        double expected = sampleImg.getAt(index).getRealDouble();
        double actual = writeSampleAndReadBack(typeClass.newInstance()).getAt(dstIndex).getRealDouble();
        assertEquals(expected, actual);
    }

    @Test
    void testWrittenContentsARGB() {
        List<AxisType> axes = axesOf(sampleImg);
        assertEquals(axes.remove(axes.size() - 1), Axes.CHANNEL);

        ImgPlus<ARGBType> argbImg = new ImgPlus<>(
                ImgView.wrap(Converters.mergeARGB(sampleImg, ColorChannelOrder.RGB)),
                sampleImg.getName(),
                axes.toArray(new AxisType[0]));

        File file = tempDir.resolve("chocolate21.h5").toFile();
        Hdf5.writeDataset(file, "/exported_data", argbImg, 0, DEFAULT_AXES);
        ImgPlus<? extends RealType<?>> img = Hdf5.readDataset(file, "/exported_data");

        // Red is channel #0 in input image, but channel #1 in output image ([R]GB vs. A[R]GB).
        double expected = sampleImg.getAt(80, 115, 0).getRealDouble();
        double actual = img.getAt(80, 115, 1, 0, 0).getRealDouble();
        assertEquals(expected, actual);
    }

    static Stream<Arguments> testWrittenContents() {
        long[] srcPos = {80, 115, 0};
        return Stream.of(
                arguments(UnsignedByteType.class, srcPos),
                arguments(UnsignedShortType.class, srcPos),
                arguments(UnsignedIntType.class, srcPos),
                arguments(FloatType.class, srcPos));
    }

    @Test
    void testWrittenMetadata() {
        long[] expectedShape = new long[]{400, 289, 3, 1, 1};
        double[] expectedCalibrations = DoubleStream.concat(
                        Arrays.stream(sampleCalibrations),
                        DoubleStream.of(1.0, 1.0)
                )
                .toArray();
        String[] expectedUnits = Stream.concat(
                        Arrays.stream(sampleUnits),
                        Stream.of("", "")
                )
                .toArray(String[]::new);

        ImgPlus<UnsignedByteType> read = writeSampleWithMetaAndReadBack();
        PixelSize sizeMeta = getPixelSize(read);

        // Note Hdf5.readDataset will ignore all metadata if there is any error
        // like mismatching number of resolution numbers and unit strings.
        assertLongsEqual(expectedShape, read.dimensionsAsLongArray());
        assertResolutionsEqual(expectedCalibrations, sizeMeta.resolution);
        assertArrayEqualsFormatted(expectedUnits, sizeMeta.unit);
    }

    private static <T extends NativeType<T> & RealType<T>> PixelSize getPixelSize(ImgPlus<T> img) {
        double[] resolutions = new double[img.numDimensions()];
        String[] units = new String[img.numDimensions()];
        for (int i = 0; i < img.numDimensions(); i++) {
            resolutions[i] = img.axis(i).calibratedValue(1.0);
            units[i] = img.axis(i).unit();
        }
        return new PixelSize(resolutions, units);
    }

    private static class PixelSize {
        public final double[] resolution;
        public final String[] unit;

        public PixelSize(double[] resolution, String[] unit) {
            this.resolution = resolution;
            this.unit = unit;
        }
    }

    private static Path copyResource(String resourcePath, Path dstDir) {
        try (InputStream in = Hdf5Test.class.getResourceAsStream(resourcePath)) {
            Objects.requireNonNull(in);
            Path path = dstDir.resolve(resourcePath.replaceFirst("^/+", ""));
            Files.createDirectories(path.getParent());
            Files.copy(in, path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ImgPlus<UnsignedShortType> readDataset(String axes) {
        return Hdf5.readDataset(sourceHdf5, "/exported_data", ImgUtils.toImagejAxes(axes));
    }

    private static ImgPlus<UnsignedShortType> readDatasetWithMeta(String axes) {
        return Hdf5.readDataset(sourceHdf5WithMeta, "/test_with_meta", axes == null ? null : ImgUtils.toImagejAxes(axes));
    }

    private static <T extends NativeType<T> & RealType<T>> ImgPlus<T> writeSampleAndReadBack(T type) {
        File file = tempDir.resolve("chocolate21.h5").toFile();
        ImgPlus<T> converted = new ImgPlus<>(
                ImgView.wrap(RealTypeConverters.convert(sampleImg, type)), sampleImg);
        Hdf5.writeDataset(file, "/exported_data", converted, 0, DEFAULT_AXES);
        return Hdf5.readDataset(file, "/exported_data");
    }

    private static ImgPlus<UnsignedByteType> writeSampleWithMetaAndReadBack() {
        File file = tempDir.resolve("chocolate21.h5").toFile();
        Hdf5.writeDataset(file, "/exported_data", sampleImgWithMeta, 0, DEFAULT_AXES);
        return Hdf5.readDataset(file, "/exported_data", ImgUtils.toImagejAxes("xyczt"));
    }

    private static void assertLongsEqual(long[] expected, long[] actual) {
        assertArrayEquals(expected, actual, () -> String.format(
                "Array contents differ: expected %s, actual %s",
                Arrays.toString(expected),
                Arrays.toString(actual)));
    }

    private static void assertResolutionsEqual(double[] expected, double[] actual) {
        // test_axes.h5/test_with_axes actually contains 12.000048000192 for y resolution, so 1e-4 tolerance needed.
        // The inaccuracy comes from FIJI, which saves this imprecise value in the tif metadata when the user enters 12.
        double toleratedDelta = 1e-4;
        assertArrayEquals(expected, actual, toleratedDelta, () -> String.format(
                "Array contents differ: expected %s, actual %s",
                Arrays.toString(expected),
                Arrays.toString(actual)));
    }

    private static <T> void assertArrayEqualsFormatted(T[] expected, T[] actual) {
        assertArrayEquals(expected, actual, () -> String.format(
                "Array contents differ: expected %s, actual %s",
                Arrays.toString(expected),
                Arrays.toString(actual)));
    }
}
