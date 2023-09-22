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
package org.ilastik.ilastik4ij.util;

import net.imglib2.img.cell.CellGrid;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Iterate over offset and block (cell) dimensions of a grid in the flat, column-major order.
 * <p>
 * Each step yields {@link Block} that contains offset and dimensions of the current block.
 * Yielded block dimensions could be smaller than default block dimensions if the current block
 * is the last one across some dimension.
 * Offset and dimensions are reused between iterations, therefore they should not be mutated.
 */
public class GridCoordinates implements Iterable<GridCoordinates.Block> {
    private final long[] imageDims;
    private final long[] gridDims;
    private final int[] blockDims;

    /**
     * Container for block offset and block dimensions.
     */
    public static final class Block {
        public final long[] offset;
        public final int[] dims;

        public Block(long[] offset, int[] dims) {
            this.offset = offset;
            this.dims = dims;
        }
    }

    private final class EntryIterator implements Iterator<Block> {
        private final long[] offset = imageDims.clone();
        private final int[] dims = new int[imageDims.length];
        private final long count = Arrays.stream(gridDims).reduce(1, (l, r) -> l * r);
        private long index;

        @Override
        public boolean hasNext() {
            return index < count;
        }

        @Override
        public Block next() {
            index++;

            for (int i = 0; i < dims.length; i++) {
                offset[i] += blockDims[i];
                if (offset[i] < imageDims[i]) {
                    break;
                }
                offset[i] = 0;
            }

            for (int i = 0; i < dims.length; i++) {
                dims[i] = (int) Math.min(blockDims[i], imageDims[i] - offset[i]);
            }

            return new Block(offset, dims);
        }
    }

    public GridCoordinates(CellGrid grid) {
        Objects.requireNonNull(grid);
        imageDims = grid.getImgDimensions();
        gridDims = grid.getGridDimensions();
        blockDims = grid.getCellDimensions();
    }

    @Override
    public Iterator<Block> iterator() {
        return new EntryIterator();
    }
}
