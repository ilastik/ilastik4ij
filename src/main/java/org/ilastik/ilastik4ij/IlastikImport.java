/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.util.DatasetInfo;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author chaubold
 */
@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ilastik>Import HDF5")
public class IlastikImport implements Command, ActionListener {
    private static final String SELECT_DATASET = "selectDataset";
    private static final String CANCEL_DATASET_SELECTION = "cancelDatasetSelection";
    private static final String LOAD_RAW = "loadDataset";
    private static final String LOAD_LUT = "loadLUT";
    private static final String CANCEL_AXES_ORDER_CONFIGURATION = "cancelAxesOrderConfiguration";

    @Parameter
    private LogService log;
    @Parameter
    private StatusService statusService;
    @Parameter
    private ThreadService threadService;

    // plugin parameters
    @Parameter(label = "HDF5 file exported from ilastik")
    private File hdf5FileName;

    private String fullFileName;
    private Map<String, HDF5DataSetInformation> datasets;
    private IHDF5Reader reader;
    private JComboBox<String> dataSetBox;
    private JComboBox<String> dimBox;
    private JFrame frameSelectAxisOrdering;
    private JFrame frameSelectDataset;
    private String datasetPath;

    private final Lock lock = new ReentrantLock();
    private final Condition finishedCondition = lock.newCondition();
    private boolean notFinished = true;

    @Override
    public void run() {
        try {
            this.fullFileName = hdf5FileName.getAbsolutePath();
            this.reader = HDF5Factory.openForReading(fullFileName);
            this.datasets = findAvailableDatasets(reader, "/");
            if (this.datasets.isEmpty()) {
                IJ.error(String.format("Could not find any datasets inside '%s'", this.fullFileName));
            }
            if (isSingleDataset()) {
                showAxesorderInputDialog();
            } else {
                showDatasetSelectionDialog(reader);
            }

        } catch (Exception err) {
            IJ.error(String.format("Error while opening '%s': %s", this.fullFileName, err.getMessage()));
        } catch (OutOfMemoryError o) {
            IJ.outOfMemory("OOM while loading HDF5");
        }

        // we need wait inside the run() method until for the HDF5 import to finish (in a separate thread)
        // otherwise we won't be able to display the resulting image
        waitForCompletion();

        log.info("Done loading HDF5 file!");
    }

    private void waitForCompletion() {
        // wait for notFinished to become false
        lock.lock();
        try {
            while (notFinished)
                finishedCondition.await();
        } catch (InterruptedException ex) {
            log.warn("Execution of HDF5 loading got interrupted");
        } finally {
            lock.unlock();
        }
    }

    private void signalCompletion() {
        lock.lock();
        try {
            notFinished = false;
            finishedCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    private <T extends RealType<T> & NativeType<T>> void loadDataset(boolean applyLUT) {
        String dimensionOrder = (String) dimBox.getSelectedItem();
        dimensionOrder = dimensionOrder.toLowerCase();

        Instant start = Instant.now();

        ImgPlus<T> imgPlus = new Hdf5DataSetReader(fullFileName, datasetPath, dimensionOrder, log, statusService).read();
        ImageJFunctions.show(imgPlus);
        if (applyLUT) {
            IJ.run("glasbey_inverted");
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info("Loading HDF5 dataset took: " + timeElapsed);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String actionCommand = event.getActionCommand();
        switch (actionCommand) {
            case SELECT_DATASET:
                frameSelectDataset.dispose();
                showAxesorderInputDialog();
                break;
            case CANCEL_DATASET_SELECTION:
                frameSelectDataset.dispose();
                signalCompletion();
                break;
            case LOAD_RAW:
                if (isValidAxisOrder()) {
                    frameSelectAxisOrdering.dispose();
                    threadService.run(() -> {
                        loadDataset(false);
                        signalCompletion();
                    });
                }
                break;
            case LOAD_LUT:
                if (isValidAxisOrder()) {
                    frameSelectAxisOrdering.dispose();
                    threadService.run(() -> {
                        loadDataset(true);
                        signalCompletion();
                    });
                }
                break;
            case CANCEL_AXES_ORDER_CONFIGURATION:
                frameSelectAxisOrdering.dispose();
                signalCompletion();
                break;
        }
    }

    private boolean isSingleDataset() {
        return datasets.size() == 1;
    }

    private Map<String, HDF5DataSetInformation> findAvailableDatasets(IHDF5Reader reader, String path) {
        HDF5LinkInformation link = reader.object().getLinkInformation(path);
        List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);

        Map<String, HDF5DataSetInformation> result = new LinkedHashMap<>();
        for (HDF5LinkInformation info : members) {
            log.info(info.getPath() + ": " + info.getType());
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

    private void showAxesorderInputDialog() {
        String boxInfo;

        frameSelectAxisOrdering = new JFrame();

        JLabel datasetLabel = new JLabel();
        JLabel taskLabel = new JLabel("Please enter the meaning of those axes:");
        JButton k2 = new JButton("Cancel");
        k2.setActionCommand(CANCEL_AXES_ORDER_CONFIGURATION);
        k2.addActionListener(this);

        if (!isSingleDataset()) {
            boxInfo = (String) dataSetBox.getSelectedItem();
            datasetPath = DatasetInfo.parsePath(boxInfo);
        } else {
            datasetPath = datasets.keySet().iterator().next();
        }
        HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(datasetPath);

        String shape = Arrays.stream(dsInfo.getDimensions())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        String datasetDescription = String.format("Found dataset with dimensions: (%s)", shape);
        datasetLabel.setText(datasetDescription);

        Vector<String> dimExamples = new Vector<>();
        switch (dsInfo.getRank()) {
            case 5:
                dimExamples.add("tzyxc");
                dimExamples.add("txyzc");
                break;
            case 4:
                dimExamples.add("xyzc");
                dimExamples.add("txyz");
                dimExamples.add("txyc");
                break;
            case 3:
                dimExamples.add("xyc");
                dimExamples.add("xyz");
                dimExamples.add("txy");
                break;
            default:
                dimExamples.add("xy");
                dimExamples.add("yx");
                break;
        }

        this.dimBox = new JComboBox<>(dimExamples);
        dimBox.setEditable(true);
        dimBox.addActionListener(this);

        JButton l1 = new JButton("Load");
        l1.setActionCommand(LOAD_RAW);
        l1.addActionListener(this);
        JButton l2 = new JButton("Load and apply LUT");
        l2.setActionCommand(LOAD_LUT);
        l2.addActionListener(this);

        // layout frame:
        frameSelectAxisOrdering.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        frameSelectAxisOrdering.getContentPane().add(datasetLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        frameSelectAxisOrdering.getContentPane().add(taskLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        frameSelectAxisOrdering.getContentPane().add(dimBox, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        frameSelectAxisOrdering.getContentPane().add(l1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        frameSelectAxisOrdering.getContentPane().add(l2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        frameSelectAxisOrdering.getContentPane().add(k2, c);

        frameSelectAxisOrdering.setResizable(false);
        frameSelectAxisOrdering.setLocationRelativeTo(null);
        frameSelectAxisOrdering.pack();
        frameSelectAxisOrdering.setVisible(true);
    }

    private void showDatasetSelectionDialog(IHDF5Reader reader) {
        frameSelectDataset = new JFrame();
        JButton b1 = new JButton("Select");
        b1.setActionCommand(SELECT_DATASET);
        b1.addActionListener(this);
        JButton b2 = new JButton("Cancel");
        b2.setActionCommand(CANCEL_DATASET_SELECTION);
        b2.addActionListener(this);

        this.dataSetBox = new JComboBox<>();

        for (Map.Entry<String, HDF5DataSetInformation> entry : this.datasets.entrySet()) {
            dataSetBox.addItem(DatasetInfo.name(entry.getKey(), entry.getValue()));
        }

        dataSetBox.addActionListener(this);

        frameSelectDataset.getContentPane().add(dataSetBox, BorderLayout.PAGE_START);
        frameSelectDataset.getContentPane().add(b1, BorderLayout.LINE_START);
        frameSelectDataset.getContentPane().add(b2, BorderLayout.LINE_END);
        frameSelectDataset.setResizable(false);
        frameSelectDataset.setLocationRelativeTo(null);
        frameSelectDataset.pack();
        frameSelectDataset.setVisible(true);
    }

    private boolean isValidAxisOrder() {
        String dimensionOrder = (String) dimBox.getSelectedItem();
        HDF5DataSetInformation dsInfo = this.datasets.get(datasetPath);
        if (dimensionOrder.length() != dsInfo.getRank()) {
            IJ.error(String.format("Incorrect axis order '%s' for dataset '%s' of rank %s", dimensionOrder, datasetPath, dsInfo.getRank()));
            return false;
        }
        return true;
    }
}
