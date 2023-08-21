package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.*;
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

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ilastik.ilastik4ij.util.ImgUtils.*;

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
        Deque<String> stack = new ArrayDeque<>();
        stack.push("/");

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            while (!stack.isEmpty()) {
                String path = stack.pop();

                if (reader.object().isGroup(path)) {
                    stack.addAll(reader.object().getAllGroupMembers(path));
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
     * If not null, callback will be invoked each time a block is about to be read.
     * The callback accepts an index of the current block and the total number of blocks.
     * The callback will be called at least once.
     */
    public static <T extends NativeType<T>> ImgPlus<T> readDataset(
            File file, String path, List<AxisType> axes, BiConsumer<Long, Long> callback) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        if (axes != null && !(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
            throw new IllegalArgumentException("Axes must contain X and Y");
        }
        if (callback == null) {
            callback = (a, b) -> {
            };
        }

        DatasetType type;
        Img<T> img;

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            HDF5DataSetInformation info = reader.getDataSetInformation(path);

            long[] dims = reversed(info.getDimensions());
            if (!(2 <= dims.length && dims.length <= 5)) {
                throw new IllegalArgumentException(dims.length + "D datasets are not supported");
            }
            if (axes != null && axes.size() != dims.length) {
                throw new IllegalArgumentException("Requested axes don't match dataset dimensions");
            }

            HDF5DataTypeInformation typeInfo = info.getTypeInformation();
            type = DatasetType.ofHdf5(typeInfo).orElseThrow(() ->
                    new IllegalArgumentException("Unsupported dataset type " + typeInfo));

            int[] blockDims = info.getStorageLayout() == HDF5StorageLayout.CHUNKED ?
                    reversed(info.tryGetChunkSizes()) : largeBlockDims(dims);

            try (HDF5DataSet dataset = reader.object().openDataSet(path)) {
                if (IntStream.range(0, dims.length).allMatch(i -> dims[i] == blockDims[i])) {
                    callback.accept(0L, 1L);
                    img = readArrayImg(type, reader, dataset, dims);
                } else {
                    img = readCellImg(type, reader, dataset, dims, blockDims, callback);
                }
            }
        }

        String name = file.toPath()
                .resolve(path.replaceFirst("/+", ""))
                .toString()
                .replace('\\', '/');

        if (axes == null) {
            axes = DEFAULT_AXES.subList(0, img.numDimensions());
        } else {
            List<AxisType> srcAxes = axes;
            axes = DEFAULT_AXES.stream().filter(srcAxes::contains).collect(Collectors.toList());
            img = transformDims(img, srcAxes, axes);
        }

        ImgPlus<T> imgPlus = new ImgPlus<>(img, name, axes.toArray(new AxisType[0]));
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
     * <p>
     * If not null, callback will be invoked each time a block is about to be written.
     * The callback accepts an index of the current block and the total number of blocks.
     * The callback will be called at least once.
     */
    public static <T extends NativeType<T>> void writeDataset(
            File file,
            String path,
            ImgPlus<T> img,
            int compressionLevel,
            List<AxisType> axes,
            BiConsumer<Long, Long> callback) {

        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        Objects.requireNonNull(img);
        if (compressionLevel < 0) {
            throw new IllegalArgumentException("Compression level cannot be negative");
        }
        if (callback == null) {
            callback = (a, b) -> {
            };
        }

        if (!(2 <= img.numDimensions() && img.numDimensions() <= 5)) {
            throw new IllegalArgumentException(
                    img.numDimensions() + "D datasets are not supported");
        }

        T imglib2Type = img.firstElement();
        if (imglib2Type.getClass() == ARGBType.class) {
            @SuppressWarnings("unchecked")
            ImgPlus<ARGBType> argbImg = (ImgPlus<ARGBType>) img;
            writeDataset(file, path, argbToMultiChannel(argbImg), compressionLevel, axes, callback);
            return;
        }

        Img<T> data = axes == null ? img.getImg() : transformDims(img, axesOf(img), axes);
        if (axes == null) {
            axes = axesOf(img);
        }

        DatasetType type = DatasetType.ofImglib2(imglib2Type).orElseThrow(() ->
                new IllegalArgumentException("Unsupported image type " + imglib2Type.getClass()));

        long[] dims = data.dimensionsAsLongArray();
        IterableInterval<RandomAccessibleInterval<T>> grid =
                Views.flatIterable(Views.tiles(data, largeBlockDims(dims)));
        Cursor<RandomAccessibleInterval<T>> gridCursor = grid.cursor();
        long gridSize = grid.size();

        try (IHDF5Writer writer = HDF5Factory.open(file);
             HDF5DataSet dataset = type.createDataset(
                     writer,
                     path,
                     reversed(dims),
                     reversed(smallBlockDims(dims, axes)),
                     compressionLevel)) {

            long gridIndex = 0;
            while (gridCursor.hasNext()) {
                callback.accept(gridIndex++, gridSize);
                RandomAccessibleInterval<T> block = gridCursor.next();
                Cursor<T> blockCursor = Views.flatIterable(block).cursor();
                long[] currBlockDims = reversed(block.dimensionsAsLongArray());
                long[] offset = reversed(block.minAsLongArray());
                type.writeCursor(blockCursor, writer, dataset, currBlockDims, offset);
            }
        }
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
            BiConsumer<Long, Long> callback) {

        // Grid is an ND array of blocks (cells), which are themselves ND arrays of pixels.
        CellGrid grid = new CellGrid(dims, blockDims);
        List<Cell<A>> cells = new ArrayList<>();
        for (GridCoordinates.Block block : new GridCoordinates(grid)) {
            callback.accept(block.index, block.count);
            A data = type.readBlock(reader, dataset, reversed(block.dims), reversed(block.offset));
            cells.add(new Cell<>(block.dims, block.offset, data));
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
}
