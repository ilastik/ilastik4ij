package org.ilastik.ilastik4ij.io;

import ch.systemsx.cisd.hdf5.*;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Hdf5 {
    private static final AxisType[] IMAGEJ_DEFAULT_AXES = {
            Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};

    public static final class DatasetDescription implements Comparable<DatasetDescription> {
        public final String path;
        public final NativeType<?> type;
        public final long[] shape;
        public final String axes;

        public DatasetDescription(String path, NativeType<?> type, long[] shape, String axes) {
            this.path = path;
            this.type = type;
            this.shape = shape;
            this.axes = axes;
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
                    "DatasetDescription{path='%s', type=%s, shape=%s, axes='%s'}",
                    path,
                    type,
                    Arrays.toString(shape), axes);
        }

        @Override
        public int compareTo(Hdf5.DatasetDescription o) {
            return path.compareTo(o.path);
        }
    }

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

                    NativeType<?> type = imglib2Type(hdf5Info.getTypeInformation());
                    if (type == null) {
                        continue;
                    }

                    long[] shape = reversed(hdf5Info.getDimensions());
                    if (!(2 <= shape.length && shape.length <= 5)) {
                        continue;
                    }

                    String axes;
                    try {
                        axes = reversed(parseAxes(reader.string().getAttr(path, "axistags")));
                    } catch (HDF5AttributeException ignored) {
                        axes = defaultAxes(shape.length);
                    }

                    result.add(new DatasetDescription(path, type, shape, axes));
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    public static <T extends NativeType<T> & RealType<T>> ImgPlus<T>
    readDataset(File file, String path) {
        try (IHDF5Reader reader = HDF5Factory.openForReading(file)) {
            HDF5DataSetInformation hdf5Info = reader.getDataSetInformation(path);

            T type = imglib2Type(hdf5Info.getTypeInformation());
            if (type == null) {
                throw new IllegalArgumentException(String.format(
                        "Unsupported dataset type %s",
                        hdf5Info.getTypeInformation()));
            }

            long[] shape = reversed(hdf5Info.getDimensions());
            if (!(2 <= shape.length && shape.length <= 5)) {
                throw new IllegalArgumentException(String.format(
                        "%d-dimensional datasets are not supported",
                        shape.length));
            }
            for (long dim : shape) {
                if (dim < 1) {
                    throw new IllegalArgumentException(String.format(
                            "Shape %s contains non-positive dimension",
                            Arrays.toString(shape)));
                }
            }

            if (product(shape) > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Datasets with 2^31 elements or more are not supported yet");
            }

            ArrayImg<T, ?> img = readArrayImg(type, reader, path, shape);
            String name = file.toPath()
                    .resolve(path.replaceFirst("/+", ""))
                    .toString()
                    .replace('\\', '/');
            AxisType[] axes = Arrays.copyOf(IMAGEJ_DEFAULT_AXES, shape.length);

            ImgPlus<T> imgPlus = new ImgPlus<>(img, name, axes);
            imgPlus.setValidBits(type.getBitsPerPixel());
            return imgPlus;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends NativeType<T>, A extends ArrayDataAccess<A>>
    ArrayImg<T, A> readArrayImg(Object type, IHDF5Reader r, String path, long[] shape) {
        if (type instanceof ByteType) {
            return (ArrayImg<T, A>) ArrayImgs.bytes(r.int8().readArray(path), shape);
        }
        if (type instanceof UnsignedByteType) {
            return (ArrayImg<T, A>) ArrayImgs.unsignedBytes(r.uint8().readArray(path), shape);
        }
        if (type instanceof ShortType) {
            return (ArrayImg<T, A>) ArrayImgs.shorts(r.int16().readArray(path), shape);
        }
        if (type instanceof UnsignedShortType) {
            return (ArrayImg<T, A>) ArrayImgs.unsignedShorts(r.uint16().readArray(path), shape);
        }
        if (type instanceof IntType) {
            return (ArrayImg<T, A>) ArrayImgs.ints(r.int32().readArray(path), shape);
        }
        if (type instanceof UnsignedIntType) {
            return (ArrayImg<T, A>) ArrayImgs.unsignedInts(r.uint32().readArray(path), shape);
        }
        if (type instanceof LongType) {
            return (ArrayImg<T, A>) ArrayImgs.longs(r.int64().readArray(path), shape);
        }
        if (type instanceof UnsignedLongType) {
            return (ArrayImg<T, A>) ArrayImgs.unsignedLongs(r.uint64().readArray(path), shape);
        }
        if (type instanceof FloatType) {
            return (ArrayImg<T, A>) ArrayImgs.floats(r.float32().readArray(path), shape);
        }
        if (type instanceof DoubleType) {
            return (ArrayImg<T, A>) ArrayImgs.doubles(r.float64().readArray(path), shape);
        }
        throw new IllegalStateException("Unexpected value: " + type);
    }

    @SuppressWarnings("unchecked")
    private static <T extends NativeType<T> & RealType<T>>
    T imglib2Type(HDF5DataTypeInformation hdf5TypeInfo) {
        HDF5DataClass dataClass = hdf5TypeInfo.getDataClass();
        int size = hdf5TypeInfo.getElementSize();
        boolean signed = hdf5TypeInfo.isSigned();

        switch (dataClass) {
            case INTEGER:
                switch (size) {
                    case 1:
                        return (T) (signed ? new ByteType() : new UnsignedByteType());
                    case 2:
                        return (T) (signed ? new ShortType() : new UnsignedShortType());
                    case 4:
                        return (T) (signed ? new IntType() : new UnsignedIntType());
                    case 8:
                        return (T) (signed ? new LongType() : new UnsignedLongType());
                    default:
                        return null;
                }
            case FLOAT:
                switch (size) {
                    case 4:
                        return (T) new FloatType();
                    case 8:
                        return (T) new DoubleType();
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static String parseAxes(String json) {
        try {
            JSONArray keys = new JSONObject(json).getJSONArray("axes");
            return IntStream.range(0, keys.length())
                    .mapToObj(i -> keys.getJSONObject(i).getString("key"))
                    .collect(Collectors.joining());
        } catch (JSONException ignored) {
            return "";
        }
    }

    private static String defaultAxes(int ndim) {
        switch (ndim) {
            case 5:
                return "cxyzt";
            case 4:
                return "cxyz";
            case 3:
                return "xyz";
            case 2:
                return "xy";
            default:
                return "";
        }
    }

    private static long[] reversed(long[] src) {
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[src.length - 1 - i];
        }
        return dst;
    }

    private static String reversed(String src) {
        return new StringBuilder(src).reverse().toString();
    }

    private static long product(long[] src) {
        long p = 1;
        for (long x : src) {
            p *= x;
        }
        return p;
    }

    private Hdf5() {
        throw new AssertionError();
    }
}
