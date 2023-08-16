package org.ilastik.ilastik4ij.util;

import net.imglib2.img.cell.CellGrid;

import java.util.Arrays;
import java.util.Iterator;

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
        private final long size = Arrays.stream(gridDims).reduce(1, (l, r) -> l * r);
        private long curr;

        @Override
        public boolean hasNext() {
            return curr < size;
        }

        @Override
        public Block next() {
            curr++;

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
        imageDims = grid.getImgDimensions();
        gridDims = grid.getGridDimensions();
        blockDims = grid.getCellDimensions();
    }

    @Override
    public Iterator<Block> iterator() {
        return new EntryIterator();
    }
}
