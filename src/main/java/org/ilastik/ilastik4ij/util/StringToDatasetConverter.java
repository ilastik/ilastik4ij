package org.ilastik.ilastik4ij.util;

import ij.ImagePlus;
import net.imagej.Dataset;
import org.scijava.Priority;
import org.scijava.convert.AbstractDelegateConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Extra converter needed for IJ1 macro scripting:
 *
 * Converts a String (provided by an IJ1 macro script) to a Dataset by delegating
 * the conversion to a String to ImagePlus converter chained to an
 * {@link ImagePlus} to {@link Dataset} converter
 *
 * This converter should be removed once this issue : https://github.com/imagej/imagej-legacy/issues/246
 * is resolved
 *
 * See also :
 * https://forum.image.sc/t/plugin-with-two-datasets-parameters-will-always-pop-up-gui-in-macro-runs/36637/11 *
 * https://forum.image.sc/t/object-classification-using-the-ilastik-plugin-for-fiji/32997
 *
 * @author Nicolas Chiaruttini, Jan Eglinger
 *
 */

@Plugin(type = Converter.class, priority = Priority.VERY_HIGH) // don't exaggerate priorities!
public class StringToDatasetConverter extends
        AbstractDelegateConverter<String, ImagePlus, Dataset>
{

    @Override
    public Class<Dataset> getOutputType() {
        return Dataset.class;
    }

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    protected Class<ImagePlus> getDelegateType() {
        return ImagePlus.class;
    }
}
