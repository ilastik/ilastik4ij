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
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.util.ComboBoxDimensions;
import org.ilastik.ilastik4ij.util.IlastikBoxModel;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author chaubold
 */
@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ilastik>Import HDF5")
public class IlastikImport implements Command, ActionListener {

    // needed services:
    @Parameter
    LogService log;

    @Parameter
    DatasetService datasetService;
    
    @Parameter
    OptionsService optionsService;

    // plugin parameters
    @Parameter(label = "HDF5 file exported from ilastik")
    private File hdf5FileName;
    
    @Parameter(type = ItemIO.OUTPUT)
    private ImgPlus output;

    // private variables
    private String fullFileName;
    private List<String> datasetList;
    private IHDF5Reader reader;
    private JComboBox dataSetBox;
    private JComboBox dimBox;
    private boolean isList;
    private JFrame frameSelectAxisOrdering;
    private JFrame frameSelectDataset;
    private String dimensionOrder;
    private String datasetPath;
    
    private final Lock lock = new ReentrantLock();
    private final Condition finishedCondition = lock.newCondition();
    private boolean isFinished = false;

    @Override
    public void run() {
        try {
            fullFileName = hdf5FileName.getAbsolutePath();
            this.reader = HDF5Factory.openForReading(fullFileName);
            this.datasetList = new ArrayList<String>();
            String path = "/";
            findAvailableDatasets(reader, path);
            if (datasetList.size() == 1) {
                this.isList = false;
                showAxesorderInputDialog();
            } else {
                showDatasetSelectionDialog(reader, datasetList);
                this.isList = true;
            }

        } catch (HDF5Exception err) {
            IJ.error("Error while opening '" + fullFileName + err);
        } catch (Exception err) {
            IJ.error("Error while opening '" + fullFileName + err);
        } catch (OutOfMemoryError o) {
            IJ.outOfMemory("Load HDF5");
        }
        
        // wait for isFinished to become true
        lock.lock();
        try {
            while(!isFinished)
                finishedCondition.await();
        } catch (InterruptedException ex) {
            log.warn("Execution of HDF5 loading got interrupted");
        } finally {
            lock.unlock();
        }
        
        log.info("Done loading HDF5 file!");
    }

    private void findAvailableDatasets(IHDF5Reader reader, String path) {
        //	    path inside HDF5
        HDF5LinkInformation link = reader.object().getLinkInformation(path);

        List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);

        for (HDF5LinkInformation info : members) {
            log.info(info.getPath() + ": " + info.getType());
            switch (info.getType()) {
                case DATASET:
                    datasetList.add(info.getPath());

                case SOFT_LINK:
                    break;
                case GROUP:
                    path = info.getPath();
                    findAvailableDatasets(reader, path);

                default:
                    break;
            }
        }

    }

    private void showAxesorderInputDialog() {
        String boxInfo;
        int rank = 0;
        String[] dimExamples = new String[20];

        frameSelectAxisOrdering = new JFrame();

        JLabel datasetLabel = new JLabel(); 
        JLabel taskLabel = new JLabel("Please enter the meaning of those axes:");
        JButton k2 = new JButton("Cancel");
        k2.setActionCommand("cancelAxesOrderConfiguration");
        k2.addActionListener(this);

        if (this.isList) {
            boxInfo = (String) dataSetBox.getSelectedItem();
            String[] parts = boxInfo.split(":");
            datasetPath = parts[1].replaceAll("\\s+", "");
//			log.info(boxInfo);
        } else {
            datasetPath = datasetList.get(0);
//			log.info(path);
        }
        HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(datasetPath);
        rank = dsInfo.getRank();
        
        String datasetDescription = "Found dataset with dimensions: (";
        long[] dims = dsInfo.getDimensions();
        boolean isFirst = true;
        for(long dim : dims)
        {
            if(isFirst)
            {
                datasetDescription = datasetDescription + String.valueOf(dim);
                isFirst = false;
            }
            else
                datasetDescription = datasetDescription + ", " + String.valueOf(dim);
        }
        
        datasetDescription = datasetDescription + ")";
        datasetLabel.setText(datasetDescription);

        switch (rank) {
            case 5:
                dimExamples[0] = "txyzc";
                break;
            case 4:
                dimExamples[0] = "xyzc";
                dimExamples[1] = "txyz";
                dimExamples[2] = "txyc";
                break;
            case 3:
                dimExamples[0] = "xyc";
                dimExamples[1] = "xyz";
                break;
            default:
                dimExamples[0] = "xy";
                break;
        }

        this.dimBox = new JComboBox(dimExamples);
        dimBox.setEditable(true);
        dimBox.addActionListener(this);
        
        JButton l1 = new JButton("Load Raw");
        l1.setActionCommand("Load Raw");
        l1.addActionListener(this);
        JButton l2 = new JButton("Load and apply LUT");
        l2.setActionCommand("Load LUT");
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

    private void showDatasetSelectionDialog(IHDF5Reader reader, List<String> datasetList) {
        frameSelectDataset = new JFrame();
        JButton b1 = new JButton("Select");
        b1.setActionCommand("selectDataset");
        b1.addActionListener(this);
        JButton b2 = new JButton("Cancel");
        b2.setActionCommand("cancelDatasetSelection");
        b2.addActionListener(this);

        String[] dataSets = new String[datasetList.size()];
        dataSets = datasetList.toArray(dataSets);

        this.dataSetBox = new JComboBox(new IlastikBoxModel());

        for (int i = 0; i < datasetList.size(); i++) {

            if (reader.object().getDataSetInformation(dataSets[i]).getRank() == 5) {

                dataSetBox.addItem(new ComboBoxDimensions(dataSets[i], "+"));
            } else {
                dataSetBox.addItem(new ComboBoxDimensions(dataSets[i], "-"));
            }

        }

        //this.dataSetBox = new JComboBox(dataSets);
        //	    dataSetBox.setSelectedIndex(0);
        dataSetBox.addActionListener(this);

        frameSelectDataset.getContentPane().add(dataSetBox, BorderLayout.PAGE_START);
        frameSelectDataset.getContentPane().add(b1, BorderLayout.LINE_START);
        frameSelectDataset.getContentPane().add(b2, BorderLayout.LINE_END);
        frameSelectDataset.setResizable(false);
        frameSelectDataset.setLocationRelativeTo(null);
        frameSelectDataset.pack();
        frameSelectDataset.setVisible(true);

    }

    private void signalFinished(){
        lock.lock();
        try{
            isFinished = true;
            finishedCondition.signal();
        } finally
        {
            lock.unlock();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("selectDataset")) {
            showAxesorderInputDialog();
        } else if (event.getActionCommand().equals("cancelDatasetSelection")) {
            frameSelectDataset.dispose();
            signalFinished();
        } else if (event.getActionCommand().equals("Load Raw")) {
            dimensionOrder = (String) dimBox.getSelectedItem();
            frameSelectAxisOrdering.dispose();
            output = new Hdf5DataSetReader(fullFileName, datasetPath, dimensionOrder, log, datasetService).read();
            signalFinished();
        } else if (event.getActionCommand().equals("Load LUT")) {
            dimensionOrder = (String) dimBox.getSelectedItem();
            frameSelectAxisOrdering.dispose();
            output = new Hdf5DataSetReader(fullFileName, datasetPath, dimensionOrder, log, datasetService).read();
            IJ.run("3-3-2 RGB"); // Applies the lookup table
            signalFinished();
        } else if (event.getActionCommand().equals("cancelAxesOrderConfiguration")) {
            frameSelectAxisOrdering.dispose();
            signalFinished();
        }
    }

}
