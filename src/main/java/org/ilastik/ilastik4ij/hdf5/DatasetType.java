package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.base.mdarray.*;
import ch.systemsx.cisd.hdf5.*;
import net.imglib2.Cursor;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.array.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Describes type of HDF5 dataset.
 */
public enum DatasetType {
    INT8(true, 1, true, new ByteType()),
    UINT8(true, 1, false, new UnsignedByteType()),
    INT16(true, 2, true, new ShortType()),
    UINT16(true, 2, false, new UnsignedShortType()),
    INT32(true, 4, true, new IntType()),
    UINT32(true, 4, false, new UnsignedIntType()),
    INT64(true, 8, true, new LongType()),
    UINT64(true, 8, false, new UnsignedLongType()),
    FLOAT32(false, 4, true, new FloatType()),
    FLOAT64(false, 8, true, new DoubleType()),
    ;

    /**
     * true for integers, false for floats.
     */
    final boolean integral;

    /**
     * Element size in bytes.
     */
    final int size;

    /**
     * true for integral signed types and floats, false for unsigned integral types.
     */
    final boolean signed;

    /**
     * The corresponding imglib2 type.
     */
    private final NativeType<?> imglib2Type;

    /**
     * Get dataset type from imglib2 type, if possible.
     */
    static <T extends NativeType<T>> Optional<DatasetType> ofImglib2(T imglib2Type) {
        return Arrays.stream(values())
                .filter(dt -> dt.imglib2Type.getClass() == imglib2Type.getClass())
                .findFirst();
    }

    /**
     * Get dataset type from HDF5 type information, if possible.
     */
    static Optional<DatasetType> ofHdf5(HDF5DataTypeInformation typeInfo) {
        HDF5DataClass dataClass = typeInfo.getDataClass();
        if (!(dataClass == HDF5DataClass.INTEGER || dataClass == HDF5DataClass.FLOAT)) {
            return Optional.empty();
        }

        boolean integral = dataClass == HDF5DataClass.INTEGER;
        int size = typeInfo.getElementSize();
        boolean signed = typeInfo.isSigned();

        return Arrays.stream(values())
                .filter(dt -> dt.integral == integral && dt.size == size && dt.signed == signed)
                .findFirst();
    }

    DatasetType(boolean integral, int size, boolean signed, NativeType<?> imglib2Type) {
        this.integral = integral;
        this.size = size;
        this.signed = signed;
        this.imglib2Type = imglib2Type;
    }

    /**
     * Create a new imglib2 type variable that matches this dataset type.
     */
    @SuppressWarnings("unchecked")
    <T extends NativeType<T>> T createVariable() {
        return (T) imglib2Type.createVariable();
    }

    /**
     * New native image instances should be linked to their corresponding primitive types.
     *
     * @see NativeTypeFactory
     */
    @SuppressWarnings("unchecked")
    <T extends NativeType<T>, A extends ArrayDataAccess<A>> void linkImglib2Type(
            NativeImg<T, A> nativeImg) {
        NativeTypeFactory<T, A> ntf = (NativeTypeFactory<T, A>) imglib2Type.getNativeTypeFactory();
        nativeImg.setLinkedType(ntf.createLinkedType(nativeImg));
    }

    /**
     * Read a block of data from HDF5 dataset as a primitive array wrapped in imglib2 container.
     * <p>
     * Dimension order is <em>row-major</em>.
     */
    @SuppressWarnings("unchecked")
    <A extends ArrayDataAccess<A>> A readBlock(
            IHDF5Reader reader, HDF5DataSet dataset, int[] blockDims, long[] offset) {
        switch (this) {
            case INT8:
                return (A) new ByteArray(reader.int8()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case UINT8:
                return (A) new ByteArray(reader.uint8()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case INT16:
                return (A) new ShortArray(reader.int16()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case UINT16:
                return (A) new ShortArray(reader.uint16()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case INT32:
                return (A) new IntArray(reader.int32()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case UINT32:
                return (A) new IntArray(reader.uint32()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case INT64:
                return (A) new LongArray(reader.int64()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case UINT64:
                return (A) new LongArray(reader.uint64()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case FLOAT32:
                return (A) new FloatArray(reader.float32()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            case FLOAT64:
                return (A) new DoubleArray(reader.float64()
                        .readMDArrayBlockWithOffset(dataset, blockDims, offset).getAsFlatArray());
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * Create and open a new HDF5 dataset.
     */
    HDF5DataSet createDataset(
            IHDF5Writer writer, String path, long[] dims, int[] blockDims, int compressionLevel) {
        HDF5IntStorageFeatures intFeatures =
                HDF5IntStorageFeatures.createDeflationDelete(compressionLevel);
        HDF5FloatStorageFeatures floatFeatures =
                HDF5FloatStorageFeatures.createDeflationDelete(compressionLevel);
        switch (this) {
            case INT8:
                return writer.int8().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case UINT8:
                return writer.uint8().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case INT16:
                return writer.int16().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case UINT16:
                return writer.uint16().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case INT32:
                return writer.int32().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case UINT32:
                return writer.uint32().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case INT64:
                return writer.int64().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case UINT64:
                return writer.uint64().createMDArrayAndOpen(path, dims, blockDims, intFeatures);
            case FLOAT32:
                return writer.float32().createMDArrayAndOpen(path, dims, blockDims, floatFeatures);
            case FLOAT64:
                return writer.float64().createMDArrayAndOpen(path, dims, blockDims, floatFeatures);
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * Write cursor contents into HDF5 dataset.
     * <p>
     * Dimension order is <em>row-major</em>.
     */
    @SuppressWarnings("unchecked")
    <T extends NativeType<T>> void writeCursor(
            Cursor<T> cursor, IHDF5Writer writer, HDF5DataSet dataset, long[] dims, long[] offset) {
        switch (this) {
            case INT8: {
                Cursor<ByteType> src = (Cursor<ByteType>) cursor;
                MDByteArray dst = new MDByteArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.int8().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case UINT8: {
                Cursor<UnsignedByteType> src = (Cursor<UnsignedByteType>) cursor;
                MDByteArray dst = new MDByteArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set((byte) src.next().get(), i);
                }
                writer.uint8().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case INT16: {
                Cursor<ShortType> src = (Cursor<ShortType>) cursor;
                MDShortArray dst = new MDShortArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.int16().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case UINT16: {
                Cursor<UnsignedShortType> src = (Cursor<UnsignedShortType>) cursor;
                MDShortArray dst = new MDShortArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set((short) src.next().get(), i);
                }
                writer.uint16().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case INT32: {
                Cursor<IntType> src = (Cursor<IntType>) cursor;
                MDIntArray dst = new MDIntArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.int32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case UINT32: {
                Cursor<UnsignedIntType> src = (Cursor<UnsignedIntType>) cursor;
                MDIntArray dst = new MDIntArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set((int) src.next().get(), i);
                }
                writer.uint32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case INT64: {
                Cursor<LongType> src = (Cursor<LongType>) cursor;
                MDLongArray dst = new MDLongArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.int64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case UINT64: {
                Cursor<UnsignedLongType> src = (Cursor<UnsignedLongType>) cursor;
                MDLongArray dst = new MDLongArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.uint64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case FLOAT32: {
                Cursor<FloatType> src = (Cursor<FloatType>) cursor;
                MDFloatArray dst = new MDFloatArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.float32().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            case FLOAT64: {
                Cursor<DoubleType> src = (Cursor<DoubleType>) cursor;
                MDDoubleArray dst = new MDDoubleArray(dims);
                for (int i = 0; i < dst.size(); i++) {
                    dst.set(src.next().get(), i);
                }
                writer.float64().writeMDArrayBlockWithOffset(dataset, dst, offset);
                return;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
