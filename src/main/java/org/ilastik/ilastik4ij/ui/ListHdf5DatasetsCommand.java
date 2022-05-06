package org.ilastik.ilastik4ij.ui;

import org.ilastik.ilastik4ij.hdf5.DatasetEntryProvider;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>List HDF5 Datasets")
public class ListHdf5DatasetsCommand extends ContextCommand {
    @Parameter
    public File file;

    @SuppressWarnings("unused")
    @Parameter
    private LogService logService;

    @Parameter(type = ItemIO.OUTPUT)
    public GenericTable datasets;

    @Override
    public void run() {
        List<DatasetEntryProvider.DatasetEntry> entries = new HDF5DatasetEntryProvider(logService).findAvailableDatasets(file.toString());

        datasets = new DefaultGenericTable(4, entries.size());
        datasets.setColumnHeader(0, "Path");
        datasets.setColumnHeader(1, "Shape");
        datasets.setColumnHeader(2, "Data Type");
        datasets.setColumnHeader(3, "Axis Tags");

        for (int i = 0; i < entries.size(); i++) {
            DatasetEntryProvider.DatasetEntry entry = entries.get(i);

            String shapeString = Arrays.stream(entry.shape)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(","));

            datasets.set(0, i, entry.path);
            datasets.set(1, i, shapeString);
            datasets.set(2, i, entry.dtype);
            datasets.set(3, i, entry.axisTags);
        }
    }
}
