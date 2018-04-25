package org.ilastik.ilastik4ij;

import net.imglib2.converter.Converter;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

/**
 * @author Ashis Ravindran
 * <p>
 * Unofficial Converter class to convert images to 32 bit UnsignedIntType.
 * The constant 2147483647 used is simply (2^32-1) => Max value of 32 bit Integer.
 */
public class RealUnsignedIntConverter<R extends RealType<R>> extends AbstractLinearRange implements Converter<R, UnsignedIntType> {
    public RealUnsignedIntConverter() {
        super();
    }

    public RealUnsignedIntConverter(final double min, final double max) {
        super(min, max);
    }

    @Override
    public void convert(final R input, final UnsignedIntType output) {
        final double a = input.getRealDouble();
        output.set(Math.min(2147483647, roundPositive(Math.max(0, ((a - min) / scale * 2147483647)))));
    }
}