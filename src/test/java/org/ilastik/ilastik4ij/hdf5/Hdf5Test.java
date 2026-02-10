/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2026 N/A
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

    static ImgPlus<UnsignedByteType> sampleImg;

    @BeforeAll
    static void setUpClass() throws IOException {
        sourceHdf5 = copyResource("/test.h5", tempDir).toFile();
        sampleImg = new ImageJ()
                .scifio()
                .datasetIO()
                .open(copyResource("/chocolate21.jpg", tempDir).toString())
                .typedImg(new UnsignedByteType());
    }

    @ParameterizedTest
    @MethodSource
    void testReadAxes(long[] expected, String axes) {
        long[] actual = readDataset(axes).dimensionsAsLongArray();
        assertDimsEquals(expected, actual);
    }

    static Stream<Arguments> testReadAxes() {
        return Stream.of(
                arguments(new long[]{4, 5, 3, 6, 7}, "cxyzt"),
                arguments(new long[]{4, 5, 3, 7, 6}, "cxytz"),
                arguments(new long[]{3, 5, 4, 7, 6}, "xcytz"));
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
        long[] actual = readWrittenDataset(new UnsignedByteType()).dimensionsAsLongArray();
        assertDimsEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource
    <T extends NativeType<T> & RealType<T>> void testWrittenContents(
            Class<T> typeClass, long[] index) throws Exception {
        // Intentionally compare doubles for the exact match: source contents are integers,
        // and double is the most common real type.
        long[] dstIndex = Arrays.copyOf(index, DEFAULT_AXES.size());
        double expected = sampleImg.getAt(index).getRealDouble();
        double actual = readWrittenDataset(typeClass.newInstance()).getAt(dstIndex).getRealDouble();
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

    private static <T extends NativeType<T> & RealType<T>> ImgPlus<T> readWrittenDataset(T type) {
        File file = tempDir.resolve("chocolate21.h5").toFile();
        ImgPlus<T> converted = new ImgPlus<>(
                ImgView.wrap(RealTypeConverters.convert(sampleImg, type)), sampleImg);
        Hdf5.writeDataset(file, "/exported_data", converted, 0, DEFAULT_AXES);
        return Hdf5.readDataset(file, "/exported_data");
    }

    private static void assertDimsEquals(long[] expected, long[] actual) {
        assertArrayEquals(expected, actual, () -> String.format(
                "Array contents differ: expected %s, actual %s",
                Arrays.toString(expected),
                Arrays.toString(actual)));
    }
}
