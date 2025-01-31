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
