package org.ilastik.ilastik4ij.ui;

import ij.IJ;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Vector;


class IlastikImportDialog extends JDialog implements PropertyChangeListener {
    private final Border VALID_BORDER = new JTextField().getBorder();
    private final Border INVALID_BORDER = new LineBorder(Color.RED, 1);

    private final JPanel contentPanel = new JPanel();
    private final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private final JLabel hdf5PathLabel = new JLabel("Path:");
    private final JTextField hdf5Path = new JTextField();
    private final JButton hdf5PathBrowse = new JButton("Browse");
    private final JLabel datasetNameLabel = new JLabel("Dataset:");
    private final JLabel axisTagsLabel = new JLabel("Axis tags:");
    private final JTextField axisTags = new JTextField();
    private final JButton importBtn = new JButton("Import");
    private final JButton cancelBtn = new JButton("Cancel");
    private final JComboBox<String> datasetName = new JComboBox<>();
    private final JCheckBox applyLut = new JCheckBox();
    private final JLabel applyLutLabel = new JLabel("Apply LUT:");
    private final DefaultComboBoxModel<String> datasetNameModel = new DefaultComboBoxModel<>();
    private final LogService logService;
    private final UIService uiService;
    private final IlastikImportModel model;
    private boolean cancelled = false;
    private Vector<HDF5DatasetEntryProvider.DatasetEntry> datasetEntries = new Vector<>();

    private void intializeComponentsLayout() {
        getContentPane().setLayout(new BorderLayout());
        GroupLayout layout  = new GroupLayout(contentPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        contentPanel.setLayout(layout);
        getContentPane().add(contentPanel, BorderLayout.PAGE_START);
        getContentPane().add(controlPanel, BorderLayout.PAGE_END);
        controlPanel.add(cancelBtn);
        controlPanel.add(importBtn);

        layout.setHorizontalGroup(layout
                .createSequentialGroup()
                .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(hdf5PathLabel)
                        .addComponent(datasetNameLabel)
                        .addComponent(axisTagsLabel)
                        .addComponent(applyLutLabel)
                )
                .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout
                                .createSequentialGroup()
                                .addComponent(hdf5Path)
                                .addComponent(hdf5PathBrowse)
                        )
                        .addComponent(datasetName)
                        .addComponent(axisTags)
                        .addComponent(applyLut)
                )
        );
        layout.setVerticalGroup(layout
                .createSequentialGroup()
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(hdf5PathLabel)
                                .addComponent(hdf5Path)
                                .addComponent(hdf5PathBrowse)
                )
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(datasetNameLabel)
                                .addComponent(datasetName)
                )
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(axisTagsLabel)
                                .addComponent(axisTags)
                )
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(applyLutLabel)
                                .addComponent(applyLut)
                )
        );
        layout.linkSize(SwingConstants.VERTICAL, hdf5PathBrowse, hdf5Path, axisTags, datasetName);
        contentPanel.setPreferredSize(new Dimension(600, contentPanel.getPreferredSize().height));
    }

    private void updateDatasets() {
        try {
            hdf5Path.setBorder(new JTextField().getBorder());
            datasetNameModel.removeAllElements();
            HDF5DatasetEntryProvider infoProvider = new HDF5DatasetEntryProvider(hdf5Path.getText(), logService);
            datasetEntries = infoProvider.findAvailableDatasets();
            for (HDF5DatasetEntryProvider.DatasetEntry info : datasetEntries) {
                datasetNameModel.addElement(info.verboseName);
            }
        } catch (Exception e) {
            hdf5Path.setBorder(new LineBorder(Color.RED, 1));
        }
    }

    public void setDatasetNames(Vector<String> names) {
        datasetNameModel.removeAllElements();
        for (String name : names) {
            datasetNameModel.addElement(name);
        }
    }
    public boolean wasCancelled() {
        return this.cancelled;
    }

    public IlastikImportDialog(IlastikImportModel model, LogService logService, UIService uiService) {
        this.setModalityType(ModalityType.APPLICATION_MODAL);  // Block until dialog is closed
        this.uiService = uiService;
        this.logService = logService;
        this.model = model;

        setTitle("Import HDF5");
        setLocationRelativeTo(null);

        datasetName.setModel(datasetNameModel);
        this.model.addPropertyChangeListener(this);

        cancelBtn.addActionListener(actionEvent -> {
            cancelled = true;
            dispose();
        });

        importBtn.addActionListener(actionEvent -> {
            if (model.isValid()) {
                dispose();
            }
        });

        hdf5PathBrowse.addActionListener(actionEvent -> {
            File parent = new File(hdf5Path.getText());
            File result = uiService.chooseFile(parent, FileWidget.OPEN_STYLE);
            if (result != null) {
                model.setPath(result.getAbsolutePath());
            }
        });

        hdf5Path.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                model.setPath(hdf5Path.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                model.setPath(hdf5Path.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                model.setPath(hdf5Path.getText());
            }
        });

        datasetName.addActionListener(actionEvent -> {
            int idx = datasetName.getSelectedIndex();
            if (idx != -1) {
                String axisTags = model.getAxisTagsForDataset(idx);
                model.setDatasetIdx(idx);
            }
        });

        axisTags.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                model.setAxisTags(axisTags.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                model.setAxisTags(axisTags.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                model.setAxisTags(axisTags.getText());
            }
        });

        intializeComponentsLayout();
        setResizable(true);
        pack();
    }

    private boolean isValidAxisOrder(int rank, String axisTags) {
        if (axisTags.length() != rank) {
            IJ.error(String.format("Incorrect axis order '%s' for dataset of rank %s", axisTags, rank));
            return false;
        }
        return true;
    }

    private Border getLineBorder(boolean valid) {
        if (valid) {
            return VALID_BORDER;
        } else {
            return INVALID_BORDER;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(IlastikImportModel.PROPERTY_PATH)) {
            String newPath = (String) evt.getNewValue();
            if (!this.hdf5Path.getText().equals(newPath)) {
                this.hdf5Path.setText(newPath);
            }
            Vector<String> datasets = this.model.getAvailableDatasetNames();
            this.setDatasetNames(datasets);

        } else if (evt.getPropertyName().equals(IlastikImportModel.PROPERTY_DATASET_IDX)) {
            int newIdx  = (int) evt.getNewValue();
            datasetName.setSelectedIndex(newIdx);
            String axisTags = model.getAxisTagsForDataset(newIdx);
            model.setAxisTags(axisTags);

        } else if (evt.getPropertyName().equals(IlastikImportModel.PROPERTY_AXIS_TAGS)) {
            String newTags  = (String) evt.getNewValue();
            if (!this.axisTags.getText().equals(newTags)) {
                this.axisTags.setText(newTags);
            }
        }

        this.hdf5Path.setBorder(getLineBorder(this.model.isPathValid()));
        this.axisTags.setBorder(getLineBorder(this.model.isAxisTagsValid()));
    }
}
