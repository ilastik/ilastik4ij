package org.ilastik.ilastik4ij.io;

import ch.systemsx.cisd.base.mdarray.*;
import ch.systemsx.cisd.hdf5.*;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Hdf5 {
    /**
     * Default ImageJ axis order.
     */
    public static final List<AxisType> DEFAULT_AXES = Collections.unmodifiableList(Arrays.asList(
            Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME));
    private static final String DEFAULT_JSON_AXES = "xyczt";

    /**
     * Element type of an HDF5 dataset.
     */
    public enum DatasetType {
        INT8(new ByteType()),
        UINT8(new UnsignedByteType()),
        INT16(new ShortType()),
        UINT16(new UnsignedShortType()),
        INT32(new IntType()),
        UINT32(new UnsignedIntType()),
        INT64(new LongType()),
        UINT64(new UnsignedByteType()),
        FLOAT32(new FloatType()),
        FLOAT64(new DoubleType());

        private final Type<?> type;

        /**
         * Get supported DatasetType from HDF5 type information.
         */
        private static Optional<DatasetType> fromHdf5(HDF5DataTypeInformation typeInfo) {
            switch (typeInfo.getDataClass()) {
                case INTEGER:
                    switch (typeInfo.getElementSize()) {
                        case 1:
                            return Optional.of(typeInfo.isSigned() ? INT8 : UINT8);
                        case 2:
                            return Optional.of(typeInfo.isSigned() ? INT16 : UINT16);
                        case 4:
                            return Optional.of(typeInfo.isSigned() ? INT32 : UINT32);
                        case 8:
                            return Optional.of(typeInfo.isSigned() ? INT64 : UINT64);
                        default:
                            return Optional.empty();
                    }
                case FLOAT:
                    switch (typeInfo.getElementSize()) {
                        case 4:
                            return Optional.of(FLOAT32);
                        case 8:
                            return Optional.of(FLOAT64);
                        default:
                            return Optional.empty();
                    }
                default:
                    return Optional.empty();
            }
        }

        private static <T extends NativeType<T>> Optional<DatasetType> fromType(T type) {
            return Arrays.stream(values())
                    .filter(dt -> dt.type.getClass() == type.getClass())
                    .findFirst();
        }

        DatasetType(Type<?> type) {
            if (!(type instanceof NativeType & type instanceof RealType)) {
                throw new IllegalStateException(
                        type.getClass() + " should be an instance of NativeType and RealType");
            }
            this.type = type;
        }

        /**
         * The corresponding imglib2 type.
         */
        @SuppressWarnings("unchecked")
        public <T extends NativeType<T>> T imglib2Type() {
            return (T) type.createVariable();
        }

        /**
         * Size in bits of a single element of this type.
         */
        public int bits() {
            return ((RealType<?>) type).getBitsPerPixel();
        }

        @SuppressWarnings("unchecked")
        private <T extends NativeType<T>, A extends ArrayDataAccess<A>> ArrayImg<T, A> readArrayImg(
                IHDF5Reader reader, HDF5DataSet dataset, int[] dims, long[] index) {
            switch (this) {
                case INT8: {
                    MDByteArray block = reader.int8().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.bytes(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case UINT8: {
                    MDByteArray block = reader.uint8().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.unsignedBytes(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case INT16: {
                    MDShortArray block = reader.int16().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.shorts(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case UINT16: {
                    MDShortArray block = reader.uint16().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.unsignedShorts(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case INT32: {
                    MDIntArray block = reader.int32().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.ints(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case UINT32: {
                    MDIntArray block = reader.uint32().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.unsignedInts(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case INT64: {
                    MDLongArray block = reader.int64().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.longs(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case UINT64: {
                    MDLongArray block = reader.uint64().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.unsignedLongs(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case FLOAT32: {
                    MDFloatArray block = reader.float32().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.floats(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                case FLOAT64: {
                    MDDoubleArray block = reader.float64().readMDArrayBlock(dataset, dims, index);
                    return (ArrayImg<T, A>) ArrayImgs.doubles(
                            block.getAsFlatArray(), reversed(block.longDimensions()));
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }

        private <T extends NativeType<T>, A extends ArrayDataAccess<A>> CellImg<T, A> readCellImg(
                IHDF5Reader reader, HDF5DataSet dataset, long[] dims, int[] blockDims) {

            // Column-major grid for the result.
            CellGrid grid = new CellGrid(reversed(dims), reversed(blockDims));

            // These need to be filled in for creating Cell instances.
            long[] cellMin = new long[dims.length];
            int[] cellDims = new int[dims.length];

            // Row-major grid and multi-index for reading.
            long[] gridDims = reversed(grid.getGridDimensions());
            long[] index = new long[dims.length];

            long blockCount = Arrays.stream(gridDims).reduce(1, (l, r) -> l * r);
            List<Cell<A>> cells = new ArrayList<>();

            for (int b = 0; b < blockCount; b++) {
                // Increment multi-index.
                for (int i = dims.length - 1; i >= 0; i--) {
                    index[i]++;
                    if (index[i] < gridDims[i]) {
                        break;
                    }
                    index[i] = 0;
                }

                ArrayImg<T, A> arrayImg = readArrayImg(reader, dataset, blockDims, index);
                grid.getCellDimensions(reversed(index), cellMin, cellDims);
                // For supported types, ArrayImg#update returns its container unchanged.
                // It's hacky, but we want to reuse readArrayImg instead of reimplementing
                // static methods from ArrayImgs.
                cells.add(new Cell<>(cellDims, cellMin, arrayImg.update(null)));
            }

            T type = imglib2Type();
            CellImg<T, A> img = new CellImg<>(
                    new CellImgFactory<>(type, grid.getCellDimensions()),
                    grid,
                    new ListImg<>(cells, grid.getGridDimensions()),
                    type.getEntitiesPerPixel());
            img.setLinkedType(type);
            return img;
        }

        private HDF5DataSet openDataset(
                String path,
                IHDF5Writer writer,
                long[] dims,
                int[] blockDims,
                int compressionLevel) {
            switch (this) {
                case INT8:
                    return writer.int8().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case UINT8:
                    return writer.uint8().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case INT16:
                    return writer.int16().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case UINT16:
                    return writer.uint16().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case INT32:
                    return writer.int32().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case UINT32:
                    return writer.uint32().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case INT64:
                    return writer.int64().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case UINT64:
                    return writer.uint64().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel));
                case FLOAT32:
                    return writer.float32().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5FloatStorageFeatures.createDeflationDelete(compressionLevel));
                case FLOAT64:
                    return writer.float64().createMDArrayAndOpen(path, dims, blockDims,
                            HDF5FloatStorageFeatures.createDeflationDelete(compressionLevel));
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }

        private <T extends NativeType<T>> void writeBlock(
                IHDF5Writer writer, HDF5DataSet dataset, RandomAccessibleInterval<T> block) {

            IterableInterval<T> interval = Views.flatIterable(block);
            Cursor<T> cursor = interval.cursor();
            int size = Math.toIntExact(interval.size());

            int[] blockDims = Arrays.stream(reversed(interval.dimensionsAsLongArray()))
                    .mapToInt(Math::toIntExact)
                    .toArray();
            long[] offset = reversed(interval.minAsLongArray());

            switch (this) {
                case INT8: {
                    MDByteArray dst = new MDByteArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((ByteType) cursor.next()).get(), i);
                    }
                    writer.int8().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case UINT8: {
                    MDByteArray dst = new MDByteArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set((byte) ((UnsignedByteType) cursor.next()).get(), i);
                    }
                    writer.uint8().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case INT16: {
                    MDShortArray dst = new MDShortArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((ShortType) cursor.next()).get(), i);
                    }
                    writer.int16().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case UINT16: {
                    MDShortArray dst = new MDShortArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set((short) ((UnsignedShortType) cursor.next()).get(), i);
                    }
                    writer.uint16().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case INT32: {
                    MDIntArray dst = new MDIntArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((IntType) cursor.next()).get(), i);
                    }
                    writer.int32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case UINT32: {
                    MDIntArray dst = new MDIntArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set((int) ((UnsignedIntType) cursor.next()).get(), i);
                    }
                    writer.uint32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case INT64: {
                    MDLongArray dst = new MDLongArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((LongType) cursor.next()).get(), i);
                    }
                    writer.int64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case UINT64: {
                    MDLongArray dst = new MDLongArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        // Large unsigned long values might not fit into signed long.
                        dst.set(((UnsignedLongType) cursor.next()).get(), i);
                    }
                    writer.uint64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case FLOAT32: {
                    MDFloatArray dst = new MDFloatArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((FloatType) cursor.next()).get(), i);
                    }
                    writer.float32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                case FLOAT64: {
                    MDDoubleArray dst = new MDDoubleArray(blockDims);
                    for (int i = 0; i < size; i++) {
                        dst.set(((DoubleType) cursor.next()).get(), i);
                    }
                    writer.float64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    /**
     * HDF5 dataset metadata.
     */
    public static final class DatasetDescription implements Comparable<DatasetDescription> {
        /**
         * Internal dataset path in an HDF5 file.
         */
        public final String path;

        /**
         * Element type of the dataset.
         */
        public final DatasetType type;

        private final long[] dims;
        private final AxisType[] axes;

        public DatasetDescription(String path, DatasetType type, long[] dims, AxisType[] axes) {
            this.path = path;
            this.type = type;
            this.dims = dims.clone();
            this.axes = axes.clone();
        }

        /**
         * Column-major dataset dimensions.
         */
        public long[] dims() {
            return dims.clone();
        }

        /**
         * Axis order for the dataset.
         */
        public AxisType[] axes() {
            return axes.clone();
        }

        public long dim(int i) {
            return dims[i];
        }

        public AxisType axis(int i) {
            return axes[i];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DatasetDescription that = (DatasetDescription) o;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public String toString() {
            return String.format(
                    "DatasetDescription{path='%s', type=%s, dims=%s, axes=%s}",
                    path,
                    type,
                    Arrays.toString(dims),
                    Arrays.toString(axes));
        }

        @Override
        public int compareTo(Hdf5.DatasetDescription o) {
            return path.compareTo(o.path);
        }
    }

    /**
     * Return sorted list of descriptions for supported datasets in an HDF5 file.
     */
    public static List<DatasetDescription> datasets(File file) {
        List<DatasetDescription> result = new ArrayList<>();

        Deque<String> stack = new ArrayDeque<>();
        stack.push("/");

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            while (!stack.isEmpty()) {
                String path = stack.pop();

                if (reader.object().isGroup(path)) {
                    stack.addAll(reader.object().getAllGroupMembers(path));

                } else if (reader.object().isDataSet(path)) {
                    HDF5DataSetInformation hdf5Info = reader.object().getDataSetInformation(path);
                    HDF5DataTypeInformation typeInfo = hdf5Info.getTypeInformation();

                    Optional<DatasetType> type = DatasetType.fromHdf5(typeInfo);
                    if (!type.isPresent()) {
                        continue;
                    }

                    long[] dims = reversed(hdf5Info.getDimensions());
                    if (!(2 <= dims.length && dims.length <= 5)) {
                        continue;
                    }

                    AxisType[] axes;
                    try {
                        axes = parseAxes(reader.string().getAttr(path, "axistags"));
                    } catch (HDF5AttributeException | JSONException ignored) {
                        axes = defaultAxes(dims);
                    }

                    result.add(new DatasetDescription(path, type.get(), dims, axes));
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    public static <T extends NativeType<T>> ImgPlus<T> readDataset(
            File file, String path, AxisType... axes) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        validateAxes(axes);

        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            if (!reader.exists(path)) {
                throw new IllegalArgumentException(String.format(
                        "Dataset '%s' not found in HDF5 file '%s'", path, file));
            }

            HDF5DataSetInformation hdf5Info = reader.getDataSetInformation(path);
            HDF5DataTypeInformation typeInfo = hdf5Info.getTypeInformation();

            DatasetType type = DatasetType.fromHdf5(typeInfo).orElseThrow(() ->
                    new IllegalArgumentException("Unsupported dataset type " + typeInfo));

            long[] dims = hdf5Info.getDimensions();
            if (axes.length != dims.length) {
                throw new IllegalArgumentException(String.format(
                        "Expected %dD dataset, got %dD", axes.length, dims.length));
            }
            if (Arrays.stream(dims).anyMatch(d -> d < 1)) {
                throw new IllegalArgumentException(
                        "Expected all dimensions to be positive, got " + Arrays.toString(dims));
            }

            int[] blockDims = hdf5Info.getStorageLayout() == HDF5StorageLayout.CHUNKED ?
                    hdf5Info.tryGetChunkSizes() : computeBlockDims(dims);

            Img<T> img;
            try (HDF5DataSet dataset = reader.object().openDataSet(path)) {
                if (IntStream.range(0, dims.length).allMatch(i -> dims[i] == blockDims[i])) {
                    img = type.readArrayImg(reader, dataset, blockDims, new long[dims.length]);
                } else {
                    img = type.readCellImg(reader, dataset, dims, blockDims);
                }
            }
            img = normalizeAxes(img, axes);
            axes = DEFAULT_AXES.subList(0, axes.length).toArray(new AxisType[0]);

            String name = file.toPath()
                    .resolve(path.replaceFirst("/+", ""))
                    .toString()
                    .replace('\\', '/');
            ImgPlus<T> imgPlus = new ImgPlus<>(img, name, axes);
            imgPlus.setValidBits(type.bits());
            return imgPlus;
        }
    }

    private static int[] outputBlockDims(ImgPlus<?> img) {
        int n = img.numDimensions();
        List<AxisType> axes = imgPlusAxes(img);
        int[] blockDims = new int[n];
        Arrays.fill(blockDims, 1);
        if (!(axes.contains(Axes.X) && axes.contains(Axes.Y))) {
            throw new IllegalArgumentException("Expected axes X and Y to be present, got " + axes);
        }
        if (axes.contains(Axes.Z)) {
            blockDims[axes.indexOf(Axes.X)] = 64;
            blockDims[axes.indexOf(Axes.Y)] = 64;
            blockDims[axes.indexOf(Axes.Z)] = 64;
        } else {
            blockDims[axes.indexOf(Axes.X)] = 512;
            blockDims[axes.indexOf(Axes.Y)] = 512;
        }
        return blockDims;
    }

    private static List<AxisType> imgPlusAxes(ImgPlus<?> img) {
        return IntStream.range(0, img.numDimensions())
                .mapToObj(d -> img.axis(d).type())
                .collect(Collectors.toList());
    }

    public static <T extends NativeType<T>> void writeDataset(
            File file, String path, ImgPlus<T> img) {
        writeDataset(file, path, img, 0);
    }

    public static <T extends NativeType<T>> void writeDataset(
            File file, String path, ImgPlus<T> img, int compressionLevel) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(path);
        Objects.requireNonNull(img);

        T imglib2Type = img.firstElement();
        if (imglib2Type.getClass() == ARGBType.class) {
            // ARGBType is a special, uncommon case.
            // Create a multichannel view from it, and call ourselves again
            // with a different type T.
            @SuppressWarnings("unchecked")
            Img<ARGBType> argbImg = (Img<ARGBType>) img;
            Img<UnsignedByteType> multiChannelImg = ImgView.wrap(Converters.argbChannels(argbImg));
            AxisType[] axes = Stream.concat(imgPlusAxes(img).stream(), Stream.of(Axes.CHANNEL))
                    .toArray(AxisType[]::new);
            writeDataset(file, path, new ImgPlus<>(multiChannelImg, img.getName(), axes));
            return;
        }

        DatasetType type = DatasetType.fromType(imglib2Type).orElseThrow(() ->
                new IllegalArgumentException("Unsupported " + imglib2Type.getClass()));

        long[] dims = img.dimensionsAsLongArray();
        int[] blockDims = outputBlockDims(img);
        Cursor<RandomAccessibleInterval<T>> blockCursor =
                Views.flatIterable(Views.tiles(img, blockDims)).cursor();

        try (IHDF5Writer writer = HDF5Factory.open(file)) {
            HDF5IntStorageFeatures.createDeflationDelete(compressionLevel);
            try (HDF5DataSet dataset = type.openDataset(
                    path, writer, reversed(dims), reversed(blockDims), compressionLevel)) {
                while (blockCursor.hasNext()) {
                    type.writeBlock(writer, dataset, blockCursor.next());
                }
            }
        }
    }

    private static AxisType[] validateAxes(AxisType[] axes) {
        Objects.requireNonNull(axes);
        if (!(2 <= axes.length && axes.length <= 5)) {
            throw new IllegalArgumentException(String.format(
                    "Only 2D-5D datasets are supported, but %dD is requested", axes.length));
        }

        boolean[] used = new boolean[DEFAULT_AXES.size()];
        for (AxisType axis : axes) {
            int i = DEFAULT_AXES.indexOf(axis);
            if (i < 0) {
                throw new IllegalArgumentException(String.format(
                        "Unsupported axis %s in axes %s", axis, Arrays.toString(axes)));
            }
            if (used[i]) {
                throw new IllegalArgumentException(String.format(
                        "Duplicate axis %s in axes %s", axis, Arrays.toString(axes)));
            }
            used[i] = true;
        }

        if (!(used[0] && used[1])) {
            throw new IllegalArgumentException(
                    "X and Y are not present in axes " + Arrays.toString(axes));
        }

        return axes;
    }

    private static AxisType[] parseAxes(String json) {
        JSONArray keys = new JSONObject(json).getJSONArray("axes");
        int n = keys.length();
        AxisType[] axes = new AxisType[n];

        for (int i = 0; i < n; i++) {
            String axis = keys.getJSONObject(n - 1 - i).getString("key");
            if (axis.length() != 1) {
                throw new JSONException(String.format("Invalid axis '%s'", axis));
            }

            int j = DEFAULT_JSON_AXES.indexOf(axis.charAt(0));
            if (j < 0) {
                throw new JSONException(String.format("Unknown axis '%s'", axis));
            }

            axes[i] = DEFAULT_AXES.get(j);
        }

        return axes;
    }

    private static AxisType[] defaultAxes(long[] dims) {
        // The corresponding default axis ordering logic from ilastik:
        // https://github.com/ilastik/ilastik/blob/414b6e15a2802ed923ec832776e0f33b1c7d30ae/lazyflow/utility/helpers.py#L76
        switch (dims.length) {
            case 5:
                return new AxisType[]{Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z, Axes.TIME};
            case 4:
                return new AxisType[]{Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z};
            case 3:
                // Heuristic for 2D multi-channel data.
                return dims[0] <= 4 ?
                        new AxisType[]{Axes.CHANNEL, Axes.X, Axes.Y} :
                        new AxisType[]{Axes.X, Axes.Y, Axes.Z};
            case 2:
                return new AxisType[]{Axes.X, Axes.Y};
            default:
                throw new IllegalStateException("Unexpected dims.length: " + dims.length);
        }
    }

    private static <T extends Type<T>> Img<T> normalizeAxes(Img<T> img, AxisType[] axes) {
        RandomAccessibleInterval<T> view = img;
        axes = axes.clone();

        for (int dst = 0; dst < axes.length; dst++) {
            int src = DEFAULT_AXES.indexOf(axes[dst]);
            if (src != dst) {
                AxisType tmp = axes[src];
                axes[src] = axes[dst];
                axes[dst] = tmp;
                view = Views.permute(view, src, dst);
            }
        }

        // Compare references and skip wrapping if no changes were made.
        return view != img ? ImgView.wrap(view, img.factory()) : img;
    }

    private static int[] computeBlockDims(long[] dims) {
        int[] blockDims = new int[dims.length];
        Arrays.fill(blockDims, 1);
        long n = 1;

        for (int i = dims.length - 1; i >= 0; i--) {
            // Halve the current dimension until block element count fits into a raw array.
            // Avoid tiny tail blocks by rounding up when d/2 is odd.
            long d = dims[i];
            while (n * d >> 31 > 0) {
                d = (d >> 1) + (d & 1);
            }

            // These will fit into int because of the loop above.
            n *= d;
            blockDims[i] = (int) d;

            // Can't enlarge block anymore because even 2*n won't fit into int.
            if (n >> 30 > 0) {
                break;
            }
        }

        return blockDims;
    }

    private static int[] reversed(int[] a) {
        return IntStream.range(0, a.length).map(i -> a[a.length - 1 - i]).toArray();
    }

    private static long[] reversed(long[] a) {
        return IntStream.range(0, a.length).mapToLong(i -> a[a.length - 1 - i]).toArray();
    }

    private Hdf5() {
        throw new AssertionError();
    }
}
