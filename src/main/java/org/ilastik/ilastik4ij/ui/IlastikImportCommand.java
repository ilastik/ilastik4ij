/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij.ui;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.util.Hdf5Utils;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ilastik>Import HDF5")
public class IlastikImportCommand implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private StatusService statusService;

    @Override
    public void run() {
        OpenDialog od = new OpenDialog("Select HDF5 file", "", "");
        String hdf5FilePath = od.getPath();

        try (IHDF5Reader reader = HDF5Factory.openForReading(hdf5FilePath)) {
            Map<String, HDF5DataSetInformation> datasets = findAvailableDatasets(reader, "/");
            List<String> choices = datasets.entrySet()
                    .stream()
                    .map(e -> Hdf5Utils.dropdownName(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            GenericDialog gd = new GenericDialog("Select dataset name and axis order");
            String firstChoice = choices.get(0);
            gd.addChoice("DatasetName", choices.toArray(new String[0]), firstChoice);
            int rank = datasets.get(Hdf5Utils.parseDataset(firstChoice)).getRank();
            gd.addStringField("AxisOrder", defaultAxisOrder(rank));
            gd.addCheckbox("ApplyLUT", false);

            gd.showDialog();
            if (gd.wasCanceled()) return;

            String datasetName = Hdf5Utils.parseDataset(gd.getNextChoice());
            rank = datasets.get(datasetName).getRank();
            String axisOrder = gd.getNextString();
            boolean applyLUT = gd.getNextBoolean();
            if (isValidAxisOrder(rank, axisOrder)) {
                loadDataset(hdf5FilePath, datasetName, axisOrder);
                if (applyLUT) {
                    DisplayUtils.applyGlasbeyLUT();
                }
            }
        }
        logService.info("Done loading HDF5 file!");
    }

    private String defaultAxisOrder(int rank) {
        switch (rank) {
            case 5:
                return "tzyxc";
            case 4:
                return "txyc";
            case 3:
                return "zyx";
            default:
                return "xy";
        }
    }

    private <T extends RealType<T> & NativeType<T>> void loadDataset(String hdf5FilePath, String datasetName, String axisOrder) {
        assert hdf5FilePath != null;
        assert datasetName != null;
        assert axisOrder != null;
        axisOrder = axisOrder.toLowerCase();

        Instant start = Instant.now();

        ImgPlus<T> imgPlus = new Hdf5DataSetReader<T>(hdf5FilePath, datasetName,
                axisOrder, logService, statusService).read();
        ImageJFunctions.show(imgPlus);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        logService.info("Loading HDF5 dataset took: " + timeElapsed);
    }

    private Map<String, HDF5DataSetInformation> findAvailableDatasets(IHDF5Reader reader, String path) {
        HDF5LinkInformation link = reader.object().getLinkInformation(path);
        List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);

        Map<String, HDF5DataSetInformation> result = new LinkedHashMap<>();
        for (HDF5LinkInformation info : members) {
            logService.info(info.getPath() + ": " + info.getType());
            switch (info.getType()) {
                case DATASET:
                    result.put(info.getPath(), reader.object().getDataSetInformation(info.getPath()));
                    break;
                case GROUP:
                    result.putAll(findAvailableDatasets(reader, info.getPath()));
                    break;
            }
        }

        return result;
    }


    private boolean isValidAxisOrder(int rank, String dimensionOrder) {
        if (dimensionOrder.length() != rank) {
            IJ.error(String.format("Incorrect axis order '%s' for dataset of rank %s", dimensionOrder, rank));
            return false;
        }
        return true;
    }
}
