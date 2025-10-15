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
package org.ilastik.ilastik4ij.util;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.IterableRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.ilastik.ilastik4ij.hdf5.DatasetType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public static final double IMAGEJ_DEFAULT_RESOLUTION = 1.0;

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
                throw new IllegalArgumentException(String.format("Unsupported axis '%c'", c));
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
     * Compute chunk and block dimensions suitable for writing.
     * <p>
     * Chunk dimensions are used for HDF5 chunking.
     * Block dimensions are used for tiling input dataset.
     */
    public static void outputDims(long[] dims,
                                  List<AxisType> axes,
                                  DatasetType type,
                                  int[] chunkDims,
                                  int[] blockDims) {
        int x = axes.indexOf(Axes.X);
        int y = axes.indexOf(Axes.Y);
        int z = axes.indexOf(Axes.Z);
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Axes X or Y not found");
        }

        int maxBytes = 256 * (1 << 20);
        int maxElements = maxBytes / type.size;

        boolean is3d = z >= 0 && dims[z] > 1;
        int chunkDim = is3d ? 32 : 128;
        int blockDim = (int) (is3d ? Math.cbrt(maxElements) : Math.sqrt(maxElements));

        Arrays.fill(chunkDims, 1);
        Arrays.fill(blockDims, 1);

        chunkDims[x] = (int) Math.min(dims[x], chunkDim);
        blockDims[x] = (int) Math.min(dims[x], blockDim);

        chunkDims[y] = (int) Math.min(dims[y], chunkDim);
        blockDims[y] = (int) Math.min(dims[y], blockDim);

        if (is3d) {
            chunkDims[z] = (int) Math.min(dims[z], chunkDim);
            blockDims[z] = (int) Math.min(dims[z], blockDim);
        }
    }

    /**
     * Compute block dimensions suitable for reading.
     */
    public static int[] inputBlockDims(long[] dims, int[] chunkDims) {
        Objects.requireNonNull(dims);

        int[] blockDims;
        if (chunkDims == null) {
            blockDims = new int[dims.length];
            Arrays.fill(blockDims, 1);
        } else {
            blockDims = chunkDims.clone();
        }

        long elementCount = Arrays.stream(blockDims).reduce(1, (l, r) -> l * r);

        for (int i = 0; i < dims.length; i++) {
            // Block element count assuming the current dimension is reduced to 1.
            long elementCount1 = elementCount / blockDims[i];

            long dim = dims[i];

            // Halve the current dimension until block element count fits into int.
            while (elementCount1 * dim > Integer.MAX_VALUE) {
                // Round up odd values to avoid tiny tail blocks.
                dim = dim / 2 + dim % 2;
            }

            // Everything still fits into int because of the loop condition above.
            elementCount = elementCount1 * dim;
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
     * Extract Axis : Resolution map from {@link ImgPlus}.
     *
     * There is no way to directly retrieve the value originally passed to the ImgPlus constructor,
     * so instead we ask it to calculate the axis value at 1px.
     * https://forum.image.sc/t/how-to-get-calibration-info-in-imagej2/38151
     */
    public static Map<AxisType, Double> taggedResolutionsOf(ImgPlus<?> img) {
        Objects.requireNonNull(img);
        return IntStream.range(0, img.numDimensions())
                .collect(
                        HashMap::new,
                        (map, d) -> map.put(img.axis(d).type(), img.axis(d).calibratedValue(1.0)),
                        HashMap::putAll
                );
    }

    /**
     * Extract Axis : Unit map from {@link ImgPlus}.
     */
    public static Map<AxisType, String> taggedUnitsOf(ImgPlus<?> img) {
        Objects.requireNonNull(img);
        return IntStream.range(0, img.numDimensions())
                .collect(
                        HashMap::new,
                        (map, d) -> map.put(img.axis(d).type(), img.axis(d).unit()),
                        HashMap::putAll
                );
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
     * JSON string {@code {"axes": [{"key": "y"}, {"key": "x"}]}} produces axes {@code XY}.
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
     * Parse resolutions for a given set of axes from a vigra.AxisTags JSON string.
     * <p>
     * Expected JSON string like {@code {
     *  "axes": [{"key": "x", "resolution": 25.0}, {"key": "y", "resolution": 12.0}]
     * } }.
     * Pads with {@link #IMAGEJ_DEFAULT_RESOLUTION} for requested `axes` that had no json entry (or 0.0).
     *
     * @throws JSONException in nonsense cases (malformed json)
     */
    public static List<Double> parseResolutionsMatchingAxes(String json, List<AxisType> axes) {
        Objects.requireNonNull(json);
        Objects.requireNonNull(axes);
        List<AxisType> storedAxes;
        try {
            storedAxes = parseAxes(json);
        } catch(JSONException e) {
            throw new JSONException("should have ensured parseAxes(json) is fine before calling this", e);
        }
        Collections.reverse(storedAxes);  // Undo inversion from parseAxes to match order in json string
        Map<AxisType, Double> resolutions = new HashMap<>();
        JSONArray arr = new JSONObject(json).getJSONArray("axes");
        if (storedAxes.size() != arr.length()) {
            throw new JSONException("impossible - parseAxes should parse the same json property");
        }
        for (int d = 0; d < arr.length(); d++) {
            try {
                double storedRes = arr.getJSONObject(d).getDouble("resolution");
                resolutions.put(storedAxes.get(d), storedRes == 0.0 ? IMAGEJ_DEFAULT_RESOLUTION : storedRes);
            } catch (JSONException ignored) {
                // no valid meta for this axis
            }
        }
        return axes.stream()
                .mapToDouble(axis -> resolutions.getOrDefault(axis, IMAGEJ_DEFAULT_RESOLUTION))
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Parse resolutions for a given set of axes from an ilastik-style axis_units JSON string.
     * <p>
     * Expected JSON string like {@code {"x": "nm", "t": "hours"}}.
     * Pads with "" for requested `axes` that had no json entry.
     * <p>
     * Returns empty list if json invalid.
     */
    public static List<String> parseUnitsMatchingAxes(String json, List<AxisType> axes) {
        Objects.requireNonNull(json);
        Objects.requireNonNull(axes);

        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch(JSONException ignored) {
            return Collections.emptyList();
        }
        Map<String, String> storedUnits = obj.keySet().stream()
                .collect(Collectors.toMap(
                        key -> key,
                        obj::getString
                ));

        return axes.stream()
                .map(axis -> storedUnits.getOrDefault(axisToVigraKey(axis), ""))
                .collect(Collectors.toList());
    }

    public static String axesToJSON(List<AxisType> axes, Map<AxisType, Double> taggedResolutions) {
        JSONArray jsonAxesArray = new JSONArray();
        List<AxisType> axesForSerialization = new ArrayList<>(axes);
        Collections.reverse(axesForSerialization);

        for (AxisType axis : axesForSerialization) {
            JSONObject jsonAxis = new JSONObject();
            String vigraKey = axisToVigraKey(axis);
            int vigraType = axisToVigraType(axis);

            jsonAxis.put("key", vigraKey);
            if (taggedResolutions.containsKey(axis)) {
                jsonAxis.put("resolution", taggedResolutions.get(axis));
            }
            jsonAxis.put("typeFlags", vigraType);
            jsonAxesArray.put(jsonAxis);
        }

        JSONObject root = new JSONObject();
        root.put("axes", jsonAxesArray);

        return root.toString();
    }

    public static String unitsToJSON(List<AxisType> axes, Map<AxisType, String> taggedUnits) {
        if (taggedUnits.isEmpty()) {
            return "{}";
        }
        List<AxisType> axesForSerialization = new ArrayList<>(axes);
        Collections.reverse(axesForSerialization);
        JSONObject units = new JSONObject();
        for (AxisType axis : axesForSerialization) {
            String vigraKey = axisToVigraKey(axis);
            units.put(vigraKey, taggedUnits.getOrDefault(axis, ""));
        }
        return units.toString();
    }

    private static String axisToVigraKey(AxisType axis) {
        String label = axis.getLabel().toLowerCase();
        if (axis.isSpatial()) {
            return label;
        } else if (label.equals("time")) {
            return "t";
        } else if (label.equals("channel")) {
            return "c";
        } else {
            throw new IllegalArgumentException(
                    String.format("Unknown axis type found with label %s.", label));
        }
    }

    private static int axisToVigraType(AxisType axis) {
        String label = axis.getLabel().toLowerCase();
        if (axis.isSpatial()) {
            return 2;
        } else if (label.equals("time")) {
            return 8;
        } else if (label.equals("channel")) {
            return 1;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unknown axis type found with label %s.", label));
        }
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

    /**
     * Return the total size of all images in megabytes.
     */
    public static int totalMegabytes(List<IterableRealInterval<? extends RealType<?>>> imgs) {
        return Math.toIntExact(imgs.stream()
                .mapToLong(img -> img.firstElement().getBitsPerPixel() / 8)
                .sum());
    }

    private ImgUtils() {
        throw new AssertionError();
    }
}
