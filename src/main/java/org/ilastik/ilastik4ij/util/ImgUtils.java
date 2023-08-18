package org.ilastik.ilastik4ij.util;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilities for dimensions and axes.
 */
public final class ImgUtils {
    /**
     * Supported ImageJ axes in the default order.
     */
    public static final List<AxisType> DEFAULT_AXES = Collections.unmodifiableList(Arrays.asList(
            Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME));

    /**
     * Supported axes in the character format, in the same order as {@link #DEFAULT_AXES}.
     */
    public static final String DEFAULT_STRING_AXES = "xyczt";

    /**
     * Convert axes from {@link #DEFAULT_AXES}
     * to string with chars from {@link #DEFAULT_STRING_AXES}.
     */
    public static String toStringAxes(List<AxisType> axes) {
        Objects.requireNonNull(axes);
        return axes.stream().map(axis -> {
            int i = DEFAULT_AXES.indexOf(axis);
            if (i < 0) {
                throw new IllegalArgumentException("Unsupported axis " + axis);
            }
            return String.valueOf(DEFAULT_STRING_AXES.charAt(i));
        }).collect(Collectors.joining());
    }

    /**
     * Convert string with chars from {@link #DEFAULT_STRING_AXES}
     * to axes from {@link #DEFAULT_AXES}.
     */
    public static List<AxisType> toImagejAxes(String axes) {
        Objects.requireNonNull(axes);
        return axes.chars().mapToObj(c -> {
            int i = DEFAULT_STRING_AXES.indexOf(c);
            if (i < 0) {
                throw new IllegalArgumentException("Unsupported axis " + c);
            }
            return DEFAULT_AXES.get(i);
        }).collect(Collectors.toList());
    }

    /**
     * Return a new reversed array.
     */
    public static int[] reversed(int[] a) {
        Objects.requireNonNull(a);
        int[] b = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[a.length - 1 - i];
        }
        return b;
    }

    /**
     * Return a new reversed array.
     */
    public static long[] reversed(long[] a) {
        Objects.requireNonNull(a);
        long[] b = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[a.length - 1 - i];
        }
        return b;
    }

    /**
     * Return a reversed string.
     */
    public static String reversed(String s) {
        return new StringBuilder(Objects.requireNonNull(s)).reverse().toString();
    }

    /**
     * Pick small, reasonable block dimensions from image dimensions and it's axes.
     * <p>
     * Typically used for determining on-disk block size.
     */
    public static int[] smallBlockDims(long[] dims, List<AxisType> axes) {
        Objects.requireNonNull(dims);
        Objects.requireNonNull(axes);
        if (dims.length != axes.size()) {
            throw new IllegalArgumentException("Dimension and axis count must be the same");
        }

        int x = axes.indexOf(Axes.X);
        int y = axes.indexOf(Axes.Y);
        int z = axes.indexOf(Axes.Z);
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Axes X or Y not found");
        }

        int[] blockDims = new int[dims.length];
        Arrays.fill(blockDims, 1);

        if (z < 0 || dims[z] == 1) {
            int n = 128;
            blockDims[x] = n;
            blockDims[y] = n;
        } else {
            int n = 32;
            blockDims[x] = n;
            blockDims[y] = n;
            blockDims[z] = n;
        }

        return blockDims;
    }

    /**
     * Pick the largest possible optimal block dimensions from image dimensions.
     * <p>
     * Element count in the result does not exceed {@link Integer#MAX_VALUE}.
     * <p>
     * Typically used for determining in-memory block size for some operations.
     */
    public static int[] largeBlockDims(long[] dims) {
        Objects.requireNonNull(dims);

        int[] blockDims = new int[dims.length];
        Arrays.fill(blockDims, 1);
        long elementCount = 1;

        for (int i = 0; i < dims.length; i++) {
            long dim = dims[i];

            // Halve the current dimension until block element count fits into int.
            while (elementCount * dim > Integer.MAX_VALUE) {
                // Round up odd values to avoid tiny tail blocks.
                dim = dim / 2 + dim % 2;
            }

            // Everything still fits into int because of the loop condition above.
            elementCount *= dim;
            blockDims[i] = (int) dim;

            if (2 * elementCount > Integer.MAX_VALUE) {
                // Block dimensions cannot be enlarged further.
                break;
            }
        }

        return blockDims;
    }

    /**
     * Extract axes from {@link ImgPlus}.
     */
    public static List<AxisType> axesOf(ImgPlus<?> img) {
        Objects.requireNonNull(img);
        return IntStream.range(0, img.numDimensions())
                .mapToObj(d -> img.axis(d).type())
                .collect(Collectors.toList());
    }

    /**
     * Guess axes from image dimensions.
     */
    public static List<AxisType> guessAxes(long[] dims) {
        Objects.requireNonNull(dims);
        // The corresponding default axis ordering logic from ilastik:
        // https://github.com/ilastik/ilastik/blob/414b6e15a2802ed923ec832776e0f33b1c7d30ae/lazyflow/utility/helpers.py#L76
        switch (dims.length) {
            case 2:
                return new ArrayList<>(Arrays.asList(Axes.X, Axes.Y));
            case 3:
                // Heuristic for 2D multi-channel data.
                return dims[0] <= 4 ?
                        new ArrayList<>(Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y)) :
                        new ArrayList<>(Arrays.asList(Axes.X, Axes.Y, Axes.Z));
            case 4:
                return new ArrayList<>(Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z));
            case 5:
                return new ArrayList<>(
                        Arrays.asList(Axes.CHANNEL, Axes.X, Axes.Y, Axes.Z, Axes.TIME));
            default:
                throw new IllegalStateException("Can't guess axes for dimensions other than 2D-5D");
        }
    }

    /**
     * {@link #transformDims} to {@link #DEFAULT_AXES}.
     */
    public static <T extends Type<T>> Img<T> transformDims(Img<T> img, List<AxisType> srcAxes) {
        return transformDims(img, srcAxes, DEFAULT_AXES);
    }

    /**
     * Change dimensions of the image with the given source axes to match destination axes.
     * <p>
     * Existing axes are transposed to match the new order, new axes are inserted as singletons.
     */
    public static <T extends Type<T>> Img<T> transformDims(
            Img<T> img, List<AxisType> srcAxes, List<AxisType> dstAxes) {

        Objects.requireNonNull(img);
        Objects.requireNonNull(srcAxes);
        Objects.requireNonNull(dstAxes);
        if (img.numDimensions() != srcAxes.size()) {
            throw new IllegalArgumentException(
                    "Number of image dimensions and number of source axes must be the same");
        }

        RandomAccessibleInterval<T> view = img;
        srcAxes = new ArrayList<>(srcAxes);

        for (AxisType axis : dstAxes) {
            if (!srcAxes.contains(axis)) {
                view = Views.addDimension(view, 0, 0);
                srcAxes.add(axis);
            }
        }

        if (srcAxes.size() != dstAxes.size()) {
            throw new IllegalArgumentException(
                    "Some source axes are not listed in destination axes");
        }

        for (int dst = 0; dst < dstAxes.size(); dst++) {
            AxisType axis = dstAxes.get(dst);
            int src = srcAxes.indexOf(axis);
            if (src != dst) {
                Collections.swap(srcAxes, src, dst);
                view = Views.permute(view, src, dst);
            }
        }

        return ImgView.wrap(view, img.factory());
    }

    /**
     * Parse axes from JSON string.
     * <p>
     * JSON string {@code {"axes": [{"key": "y"}, {"key": "x}]}} produces axes {@code XY}.
     * Note the reversed axis order.
     *
     * @throws JSONException if JSON is malformed/invalid, or if axes are invalid.
     */
    public static List<AxisType> parseAxes(String json) {
        Objects.requireNonNull(json);

        List<AxisType> axes = new ArrayList<>();
        JSONArray arr = new JSONObject(json).getJSONArray("axes");

        for (int d = 0; d < arr.length(); d++) {
            String s = arr.getJSONObject(d).getString("key");
            if (s.length() != 1) {
                throw new JSONException(String.format("Invalid axis '%s'", s));
            }

            int i = DEFAULT_STRING_AXES.indexOf(s.charAt(0));
            if (i < 0) {
                throw new JSONException(String.format("Unknown axis '%s'", s));
            }

            axes.add(DEFAULT_AXES.get(i));
        }

        Collections.reverse(axes);
        return axes;
    }

    /**
     * Treat alpha, red, green, and blue values in {@link ARGBType} image
     * as a separate, last channel dimension.
     */
    public static ImgPlus<UnsignedByteType> argbToMultiChannel(ImgPlus<ARGBType> img) {
        Objects.requireNonNull(img);

        List<AxisType> axes = axesOf(img);
        if (axes.contains(Axes.CHANNEL)) {
            throw new IllegalArgumentException(
                    "Cannot handle ARGBType images with channel dimension");
        }

        Img<UnsignedByteType> multiChannelImg = ImgView.wrap(Converters.argbChannels(img));
        axes.add(Axes.CHANNEL);
        return new ImgPlus<>(multiChannelImg, img.getName(), axes.toArray(new AxisType[0]));
    }

    private ImgUtils() {
        throw new AssertionError();
    }
}
