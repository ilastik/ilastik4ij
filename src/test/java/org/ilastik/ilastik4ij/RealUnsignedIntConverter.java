package org.ilastik.ilastik4ij;

import net.imglib2.converter.Converter;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

/**
 * Unofficial Converter class to convert images to 32 bit UnsignedIntType.
 * The constant 4294967295 used is simply (2^32-1) => Max value of 32 bit Integer.
 */
public class RealUnsignedIntConverter<R extends RealType<R>> extends AbstractLinearRange implements Converter<R, UnsignedIntType> {
    private static final long MAX_VAL = 4294967295L;

    public RealUnsignedIntConverter() {
        super();
    }

    public RealUnsignedIntConverter(final double min, final double max) {
        super(min, max);
    }

    @Override
    public void convert(final R input, final UnsignedIntType output) {
        final double a = input.getRealDouble();
        output.set(Math.min(MAX_VAL, roundUp(Math.max(0, ((a - min) / scale * MAX_VAL)))));
    }

    private long roundUp(final double a) {
        return (long) (a + 0.5);
    }
}