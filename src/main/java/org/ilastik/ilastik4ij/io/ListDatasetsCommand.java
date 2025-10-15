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
package org.ilastik.ilastik4ij.io;

import org.ilastik.ilastik4ij.hdf5.DatasetDescription;
import org.ilastik.ilastik4ij.hdf5.Hdf5;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.scijava.ItemIO.OUTPUT;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>List HDF5 Datasets")
public final class ListDatasetsCommand extends ContextCommand {
    @Parameter(label = "HDF5 File")
    public File file;

    @Parameter(label = "Datasets", type = OUTPUT)
    public GenericTable datasets;

    @Override
    public void run() {
        List<DatasetDescription> descriptions = Hdf5.datasets(file);

        datasets = new DefaultGenericTable(5, descriptions.size());
        datasets.setColumnHeader(0, "Path");
        datasets.setColumnHeader(1, "Type");
        datasets.setColumnHeader(2, "Dimensions");
        datasets.setColumnHeader(3, "Axes");
        datasets.setColumnHeader(4, "Pixel size");

        for (int i = 0; i < descriptions.size(); i++) {
            DatasetDescription dd = descriptions.get(i);
            datasets.set(0, i, dd.path);
            datasets.set(1, i, dd.type.toString().toLowerCase());
            datasets.set(2, i, Arrays.stream(dd.dims).boxed().collect(Collectors.toList()));
            datasets.set(3, i, dd.axes);
            datasets.set(4, i, dd.formatPixelSize());
        }
    }
}
