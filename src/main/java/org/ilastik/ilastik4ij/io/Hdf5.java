package org.ilastik.ilastik4ij.io;

import ch.systemsx.cisd.hdf5.*;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import net.imglib2.type.NativeType;
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

    @SuppressWarnings("unchecked")
    private static <T extends NativeType<T>> T imglib2Type(HDF5DataTypeInformation hdf5TypeInfo) {
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

    private Hdf5() {
        throw new AssertionError();
    }
}
