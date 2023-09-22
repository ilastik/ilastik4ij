/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2023 N/A
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

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import hdf.hdf5lib.exceptions.HDF5AttributeException;
import net.imagej.axis.AxisType;
import org.ilastik.ilastik4ij.util.ImgUtils;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.ilastik.ilastik4ij.util.ImgUtils.guessAxes;
import static org.ilastik.ilastik4ij.util.ImgUtils.parseAxes;

/**
 * Metadata for HDF5 dataset.
 */
public final class DatasetDescription {
    /**
     * Internal dataset path in a file.
     */
    public final String path;

    /**
     * Type of the dataset.
     */
    public final DatasetType type;

    /**
     * Dataset dimensions in the <em>column-major</em> order.
     */
    public final long[] dims;

    /**
     * Dimension axes.
     */
    public final List<AxisType> axes;

    /**
     * Whether {@link #axes} are read by {@link ImgUtils#parseAxes}
     * or inferred with {@link ImgUtils#guessAxes}.
     */
    public boolean axesGuessed;

    /**
     * Try to get dataset description for HDF5 dataset.
     */
    static Optional<DatasetDescription> ofHdf5(IHDF5Reader reader, String path) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(path);

        HDF5DataSetInformation info = reader.object().getDataSetInformation(path);

        Optional<DatasetType> type = DatasetType.ofHdf5(info.getTypeInformation());
        if (!type.isPresent()) {
            return Optional.empty();
        }

        long[] dims = ImgUtils.reversed(info.getDimensions());
        if (!(2 <= dims.length && dims.length <= 5)) {
            return Optional.empty();
        }

        List<AxisType> axes;
        boolean axesGuessed;
        try {
            axes = parseAxes(reader.string().getAttr(path, "axistags"));
            axesGuessed = false;
        } catch (HDF5AttributeException | JSONException ignored) {
            axes = guessAxes(dims);
            axesGuessed = true;
        }

        path = "/" + path.replaceFirst("^/+", "");
        return Optional.of(new DatasetDescription(path, type.get(), dims, axes, axesGuessed));
    }

    public DatasetDescription(
            String path, DatasetType type, long[] dims, List<AxisType> axes, boolean axesGuessed) {
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
        this.dims = Objects.requireNonNull(dims).clone();
        this.axes = new ArrayList<>(Objects.requireNonNull(axes));
        this.axesGuessed = axesGuessed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetDescription that = (DatasetDescription) o;
        return Objects.equals(path, that.path) &&
                type == that.type &&
                Arrays.equals(dims, that.dims) &&
                Objects.equals(axes, that.axes) &&
                axesGuessed == that.axesGuessed;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path, type, axes, axesGuessed);
        result = 31 * result + Arrays.hashCode(dims);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "DatasetDescription{path='%s', type=%s, dims=%s, axes=%s, axesGuessed=%s}",
                path,
                type,
                Arrays.toString(dims),
                axes,
                axesGuessed);
    }
}
