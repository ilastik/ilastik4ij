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

import ch.systemsx.cisd.hdf5.HDF5DataSet;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Fraction;
import net.imglib2.view.Views;
import org.ilastik.ilastik4ij.util.GridCoordinates;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ilastik.ilastik4ij.util.ImgUtils.DEFAULT_AXES;
import static org.ilastik.ilastik4ij.util.ImgUtils.argbToMultiChannel;
import static org.ilastik.ilastik4ij.util.ImgUtils.axesOf;
import static org.ilastik.ilastik4ij.util.ImgUtils.axesToJSON;
import static org.ilastik.ilastik4ij.util.ImgUtils.inputBlockDims;
import static org.ilastik.ilastik4ij.util.ImgUtils.outputDims;
import static org.ilastik.ilastik4ij.util.ImgUtils.parseAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.parseResolutionsMatchingAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.parseUnitsMatchingAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.reversed;
import static org.ilastik.ilastik4ij.util.ImgUtils.taggedResolutionsOf;
import static org.ilastik.ilastik4ij.util.ImgUtils.taggedUnitsOf;
import static org.ilastik.ilastik4ij.util.ImgUtils.transformDims;
import static org.ilastik.ilastik4ij.util.ImgUtils.unitsToJSON;

/**
 * Read/write/list HDF5 datasets.
 */
public final class Hdf5 {
    /**
     * Return descriptions of supported datasets in an HDF5 file, sorted by their paths.
     */
    public static List<DatasetDescription> datasets(File file) {
        Objects.requireNonNull(file);
        List<DatasetDescription> result = new ArrayList<>();

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            Deque<String> stack = new ArrayDeque<>(reader.object().getAllGroupMembers("/"));

            while (!stack.isEmpty()) {
                String path = stack.pop();

                if (reader.object().isGroup(path)) {
                    reader.object().getAllGroupMembers(path).stream()
                            .map(subPath -> path + "/" + subPath)
                            .forEach(stack::add);
                } else if (reader.object().isDataSet(path)) {
                    DatasetDescription.ofHdf5(reader, path).ifPresent(result::add);
                }
            }
        }

        result.sort(Comparator.comparing(dd -> dd.path));
        return result;
    }

    /**
     * {@link #readDataset} with {@link org.ilastik.ilastik4ij.util.ImgUtils#DEFAULT_AXES}
     * and without callback.
     */
    public static <T extends NativeType<T>> ImgPlus<T> readDataset(File file, String path) {
        return readDataset(file, path, null, null);
    }

    /**
     * {@link #readDataset} without callback.
     */
    public static <T extends NativeType<T>> ImgPlus<T> readDataset(
            File file, String path, List<AxisType> axes) {
        return readDataset(file, path, axes, null);
    }

    /**
     * Read HDF5 dataset contents.
     * <p>
     * Only 2D-5D datasets with types enumerated in {@link DatasetType} are supported.
     * <p>
     * If not null, callback will be invoked between block writes (N + 1 invocations for N blocks).
     * The callback accepts the total number of bytes written so far.
     * This is useful for progress reporting.
     */
    public static <T extends NativeType<T>> ImgPlus<T> readDataset(
            File file, String path, List<AxisType> axes, LongConsumer callback) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        if (axes != null && !(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
            throw new IllegalArgumentException("Axes must contain X and Y");
        }
        if (callback == null) {
            callback = (a) -> {
            };
        }

        DatasetType type;
        Img<T> img;
        String axistagsJson;
        String axisunitsJson;

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            HDF5DataSetInformation info = reader.getDataSetInformation(path);

            HDF5DataTypeInformation typeInfo = info.getTypeInformation();
            type = DatasetType.ofHdf5(typeInfo).orElseThrow(() ->
                    new IllegalArgumentException("Unsupported dataset type " + typeInfo));

            img = loadImg(reader, path, type, info, axes, callback);

            try {
                axistagsJson = reader.string().getAttr(path, "axistags");
                axisunitsJson = reader.string().getAttr(path, "axis_units");
            } catch (HDF5AttributeException ignored) {
                axistagsJson = "";
                axisunitsJson = "";
            }
        }

        if (axes == null && !axistagsJson.isEmpty()) {
            try {
                List<AxisType> storedAxes = parseAxes(axistagsJson);
                axes = storedAxes.isEmpty() ? null : storedAxes;
            } catch (JSONException ignored) {
                // axis metadata unhelpful, proceed with null
            }
        }

        String name = file.toPath()
                .resolve(path.replaceFirst("^/+", ""))
                .toString()
                .replace('\\', '/');

        double[] resolutions = null;
        String[] units = null;
        List<AxisType> imagejAxes = DEFAULT_AXES.subList(0, img.numDimensions());
        if (axes != null) {
            imagejAxes = DEFAULT_AXES.stream().filter(axes::contains).collect(Collectors.toList());
            img = transformDims(img, axes, imagejAxes);

            boolean axesMismatchDatasetMeta = axistagsJson.isEmpty() || !axes.equals(parseAxes(axistagsJson));
            if (!axesMismatchDatasetMeta) {
                // Pixel size metadata only make sense if user isn't forcing a reinterpretation of the dataset's axes.
                resolutions = parseResolutionsMatchingAxes(axistagsJson, imagejAxes).stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray();
                units = parseUnitsMatchingAxes(axisunitsJson, imagejAxes).toArray(new String[0]);
            }
        }

        ImgPlus<T> imgPlus = new ImgPlus<>(img, name, imagejAxes.toArray(new AxisType[0]), resolutions, units);
        imgPlus.setValidBits(8 * type.size);
        return imgPlus;
    }

    /**
     * {@link #writeDataset} without compression, axis reordering and callback.
     */
    public static <T extends NativeType<T>> void writeDataset(
            File file, String path, ImgPlus<T> img) {
        writeDataset(file, path, img, 0, null, null);
    }

    /**
     * {@link #writeDataset} without axis reordering and callback.
     */
    public static <T extends NativeType<T>> void writeDataset(
            File file, String path, ImgPlus<T> img, int compressionLevel) {
        writeDataset(file, path, img, compressionLevel, null, null);
    }

    /**
     * {@link #writeDataset} without callback.
     */
    public static <T extends NativeType<T>> void writeDataset(
            File file, String path, ImgPlus<T> img, int compressionLevel, List<AxisType> axes) {
        writeDataset(file, path, img, compressionLevel, axes, null);
    }

    /**
     * Write image contents to HDF5 dataset, creating/overwriting file and dataset if needed.
     * <p>
     * Only 2D-5D datasets with types enumerated in {@link DatasetType} are supported.
     * As a special case, {@link ARGBType} is supported too, but its use is discouraged.
     * <p>
     * if axes are specified, image will be written in the specified axis order.
     * <p>
     * If not null, callback will be invoked between block writes (N + 1 invocations for N blocks).
     * The callback accepts the total number of bytes written so far.
     * This is useful for progress reporting.
     */
    public static <T extends NativeType<T>> void writeDataset(
            File file,
            String path,
            ImgPlus<T> img,
            int compressionLevel,
            List<AxisType> axes,
            LongConsumer callback) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        Objects.requireNonNull(img);
        if (compressionLevel < 0) {
            throw new IllegalArgumentException("Compression level cannot be negative");
        }
        if (callback == null) {
            callback = (a) -> {
            };
        }

        if (!(2 <= img.numDimensions() && img.numDimensions() <= 5)) {
            throw new IllegalArgumentException(
                    img.numDimensions() + "D datasets are not supported");
        }

        T imglib2Type = img.firstElement();
        if (imglib2Type.getClass() == ARGBType.class) {
            Logger.getLogger(Hdf5.class.getName()).warning("Writing ARGBType images is deprecated");
            @SuppressWarnings("unchecked")
            ImgPlus<ARGBType> argbImg = (ImgPlus<ARGBType>) img;
            writeDataset(file, path, argbToMultiChannel(argbImg), compressionLevel, axes, callback);
            return;
        }

        Map<AxisType, Double> taggedResolutions = taggedResolutionsOf(img);
        Map<AxisType, String> taggedUnits = taggedUnitsOf(img);

        Img<T> data = axes == null ? img.getImg() : transformDims(img, axesOf(img), axes);
        if (axes == null) {
            axes = axesOf(img);
        }

        DatasetType type = DatasetType.ofImglib2(imglib2Type).orElseThrow(() ->
                new IllegalArgumentException("Unsupported image type " + imglib2Type.getClass()));

        long[] dims = data.dimensionsAsLongArray();
        int[] chunkDims = new int[dims.length];
        int[] blockDims = new int[dims.length];
        outputDims(dims, axes, type, chunkDims, blockDims);

        IterableInterval<RandomAccessibleInterval<T>> grid =
                Views.flatIterable(Views.tiles(data, blockDims));
        Cursor<RandomAccessibleInterval<T>> gridCursor = grid.cursor();
        long bytes = 0;

        try (IHDF5Writer writer = HDF5Factory.open(file);
             HDF5DataSet dataset = type.createDataset(
                     writer,
                     path,
                     reversed(dims),
                     reversed(chunkDims),
                     compressionLevel)) {

            // Add axes tags
            writer.string().setAttr(dataset.getDataSetPath(), "axistags", axesToJSON(axes, taggedResolutions));
            if (!taggedUnits.isEmpty()) {
                writer.string().setAttr(dataset.getDataSetPath(), "axis_units", escapeUnicode(unitsToJSON(axes, taggedUnits)));
            }

            callback.accept(0L);

            while (gridCursor.hasNext()) {
                RandomAccessibleInterval<T> block = gridCursor.next();
                Cursor<T> blockCursor = Views.flatIterable(block).cursor();
                long[] currBlockDims = reversed(block.dimensionsAsLongArray());
                long[] offset = reversed(block.minAsLongArray());
                type.writeCursor(blockCursor, writer, dataset, currBlockDims, offset);

                bytes += Arrays.stream(currBlockDims).reduce(type.size, (l, r) -> l * r);
                callback.accept(bytes);
            }
        }
    }

    private static <T extends NativeType<T>> Img<T> loadImg(IHDF5Reader reader, String path, DatasetType type, HDF5DataSetInformation info, List<AxisType> axes, LongConsumer callback) {
        long[] dims = reversed(info.getDimensions());
        if (!(2 <= dims.length && dims.length <= 5)) {
            throw new IllegalArgumentException(dims.length + "D datasets are not supported");
        }
        if (axes != null && axes.size() != dims.length) {
            throw new IllegalArgumentException("Requested axes don't match dataset dimensions");
        }

        int[] blockDims = inputBlockDims(dims,
                info.tryGetChunkSizes() != null ? reversed(info.tryGetChunkSizes()) : null);

        Img<T> img;
        try (HDF5DataSet dataset = reader.object().openDataSet(path)) {
            callback.accept(0L);
            if (IntStream.range(0, dims.length).allMatch(i -> dims[i] == blockDims[i])) {
                img = readArrayImg(type, reader, dataset, dims);
                callback.accept(Arrays.stream(dims).reduce(type.size, (l, r) -> l * r));
            } else {
                img = readCellImg(type, reader, dataset, dims, blockDims, callback);
            }
        }
        return img;
    }

    private static <T extends NativeType<T>, A extends ArrayDataAccess<A>>
    ArrayImg<T, A> readArrayImg(
            DatasetType type, IHDF5Reader reader, HDF5DataSet dataset, long[] dims) {

        int[] blockDims = Arrays.stream(reversed(dims)).mapToInt(d -> (int) d).toArray();
        A block = type.readBlock(reader, dataset, blockDims, new long[dims.length]);

        ArrayImg<T, A> arrayImg = new ArrayImg<>(block, dims, new Fraction());
        type.linkImglib2Type(arrayImg);
        return arrayImg;
    }

    private static <T extends NativeType<T>, A extends ArrayDataAccess<A>>
    CellImg<T, A> readCellImg(
            DatasetType type,
            IHDF5Reader reader,
            HDF5DataSet dataset,
            long[] dims,
            int[] blockDims,
            LongConsumer callback) {

        // Grid is an ND array of blocks (cells), which are themselves ND arrays of pixels.
        CellGrid grid = new CellGrid(dims, blockDims);
        List<Cell<A>> cells = new ArrayList<>();
        long bytes = 0;

        for (GridCoordinates.Block block : new GridCoordinates(grid)) {
            A data = type.readBlock(reader, dataset, reversed(block.dims), reversed(block.offset));
            cells.add(new Cell<>(block.dims, block.offset, data));
            bytes += Arrays.stream(block.dims).reduce(type.size, (l, r) -> l * r);
            callback.accept(bytes);
        }

        CellImgFactory<T> factory = new CellImgFactory<T>(type.createVariable(), blockDims);
        ListImg<Cell<A>> imgOfCells = new ListImg<>(cells, grid.getGridDimensions());
        CellImg<T, A> cellImg = new CellImg<>(factory, grid, imgOfCells, new Fraction());
        type.linkImglib2Type(cellImg);
        return cellImg;
    }

    private Hdf5() {
        throw new AssertionError();
    }

    /**
     * IHDF5StringWriter doesn't escape unicode, so we have to do it manually.
     * If more complex stuff specifically for JSON starts being needed at some point, should consider
     * switching to com.google.code.gson instead of org.json.
     */
    private static String escapeUnicode(String input) {
        StringBuilder b = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 127) {
                b.append(String.format("\\u%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
