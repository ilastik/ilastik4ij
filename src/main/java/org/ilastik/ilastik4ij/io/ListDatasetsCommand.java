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

        datasets = new DefaultGenericTable(4, descriptions.size());
        datasets.setColumnHeader(0, "Path");
        datasets.setColumnHeader(1, "Type");
        datasets.setColumnHeader(2, "Dimensions");
        datasets.setColumnHeader(3, "Axes");

        for (int i = 0; i < descriptions.size(); i++) {
            DatasetDescription dd = descriptions.get(i);
            datasets.set(0, i, dd.path);
            datasets.set(1, i, dd.type.toString().toLowerCase());
            datasets.set(2, i, Arrays.stream(dd.dims).boxed().collect(Collectors.toList()));
            datasets.set(3, i, dd.axes);
        }
    }
}
