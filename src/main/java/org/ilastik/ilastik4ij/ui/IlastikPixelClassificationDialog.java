package org.ilastik.ilastik4ij.ui;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import javax.print.DocFlavor;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

public class IlastikPixelClassificationDialog extends JDialog implements PropertyChangeListener {
    private static final Border VALID_BORDER = new JTextField().getBorder();
    private static final Border INVALID_BORDER = new LineBorder(Color.RED, 1);

    private class DatasetComboboxEntry {
        public final Dataset dataset;
        public final String title;

        public DatasetComboboxEntry(String title, Dataset dataset) {
            this.title = title;
            this.dataset = dataset;
        }

        @Override
        public String toString() {
            return this.title;
        }
    }
    private class OutputTypeComboboxEntry {
        public final String verboseName;
        public final String type;

        public OutputTypeComboboxEntry(String verboseName, String type) {
            this.verboseName = verboseName;
            this.type = type;
        }

        @Override
        public String toString() {
            return this.verboseName;
        }
    }

    private boolean cancelled = true;
    private final LogService logService;
    private final UIService uiService;
    private final DatasetService datasetService;
    private final IlastikPixelClassificationModel model;
    
    private final JPanel contentPanel = new JPanel();
    private final JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private final JLabel ilpPathLabel = new JLabel("Path:");
    private final JTextField ilpPath = new JTextField();
    private final JButton ilpPathBrowse = new JButton("Browse");
    
    private final JLabel outputTypeLabel = new JLabel("Output type:");
    private final JComboBox<String>  outputType = new JComboBox<>();

    private final JLabel predictionMaskLabel = new JLabel("Prediction Mask:");
    private final JComboBox<DatasetComboboxEntry>  predictionMask = new JComboBox<>();

    private final JButton predictBtn = new JButton("Predict");
    private final JButton cancelBtn = new JButton("Cancel");

    private void initializeComponentLayout() {
        getContentPane().setLayout(new BorderLayout());
        GroupLayout layout  = new GroupLayout(contentPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        contentPanel.setLayout(layout);
        getContentPane().add(contentPanel, BorderLayout.PAGE_START);
        getContentPane().add(controlPanel, BorderLayout.PAGE_END);
        controlPanel.add(cancelBtn);
        controlPanel.add(predictBtn);
        ilpPath.setMinimumSize(new Dimension(400, 20));

        layout.setHorizontalGroup(layout
                .createSequentialGroup()
                .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(ilpPathLabel)
                        .addComponent(outputTypeLabel)
                        .addComponent(predictionMaskLabel)
                )
                .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout
                                .createSequentialGroup()
                                .addComponent(ilpPath)
                                .addComponent(ilpPathBrowse)
                        )
                        .addComponent(outputType)
                        .addComponent(predictionMask)
                )
        );
        layout.setVerticalGroup(layout
                .createSequentialGroup()
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(ilpPathLabel)
                                .addComponent(ilpPath)
                                .addComponent(ilpPathBrowse)
                )
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(outputTypeLabel)
                                .addComponent(outputType)
                )
                .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(predictionMaskLabel)
                                .addComponent(predictionMask)
                )
        );
        layout.linkSize(SwingConstants.VERTICAL, ilpPathBrowse, ilpPath);
        setResizable(true);
        pack();
    }

    private void initIlpControl() {
        ilpPathBrowse.addActionListener(actionEvent -> {
            File parent = model.getIlastikProjectFile();
            File result = uiService.chooseFile(parent, FileWidget.OPEN_STYLE);
            if (result != null) {
                model.setIlastikProjectFile(result);
            }
        });

        ilpPath.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                model.setIlastikProjectFile(new File(ilpPath.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                model.setIlastikProjectFile(new File(ilpPath.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                model.setIlastikProjectFile(new File(ilpPath.getText()));
            }
        });
    }

    private void initOutputTypeControl() {
        outputType.addItem(UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES);
        outputType.addItem(UiConstants.PIXEL_PREDICTION_TYPE_SEGMENTATION);
        outputType.addActionListener(evt -> {
            String entry = (String)outputType.getSelectedItem();
            if (entry != null) {
                model.setOutputType(entry);
            }
        });
    }

    private void initPredictionTypeControl() {
        this.predictionMask.addItem(new DatasetComboboxEntry("<none>", null));
        for (Dataset ds : datasetService.getDatasets()) {
            this.predictionMask.addItem(new DatasetComboboxEntry(ds.getName(), ds));
        }
        predictionMask.addActionListener(evt -> {
            DatasetComboboxEntry entry = (DatasetComboboxEntry) predictionMask.getSelectedItem();
            if (entry != null) {
                model.setPredictionMask(entry.dataset);
            }
        });
    }
    public boolean wasCancelled() {
        return this.cancelled;
    }

    public IlastikPixelClassificationDialog(LogService logService, UIService uiService, DatasetService datasetService, IlastikPixelClassificationModel model) {
        this.setModalityType(ModalityType.APPLICATION_MODAL);  // Block until dialog is closed
        this.uiService = uiService;
        this.logService = logService;
        this.datasetService = datasetService;

        this.model = model;
        this.model.addPropertyChangeListener(this);

        this.initIlpControl();
        this.initOutputTypeControl();
        this.initPredictionTypeControl();

        this.cancelBtn.addActionListener(evt -> {
            this.dispose();
        });
        this.predictBtn.addActionListener(evt -> {
            if (model.isValid()) {
                cancelled = false;
                this.dispose();
            }
        });

        this.initializeComponentLayout();
    }

    private static Border getLineBorder(boolean valid) {
        if (valid) {
            return VALID_BORDER;
        } else {
            return INVALID_BORDER;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(IlastikPixelClassificationModel.PROPERTY_ILASTIK_PROJECT_FILE)) {
            File newProjectFile = (File) evt.getNewValue();
            if (newProjectFile != null && !this.ilpPath.equals(newProjectFile.getAbsolutePath())) {
                this.ilpPath.setText(newProjectFile.getAbsolutePath());
            }

            ilpPath.setBorder(getLineBorder(model.isValidIlastikProjectFile()));
        } else if (evt.getPropertyName().equals(IlastikPixelClassificationModel.PROPERTY_OUTPUT_TYPE)) {
            String newType = (String) evt.getNewValue();
            int selectedIdx = outputType.getSelectedIndex();
            for (int i = 0; i < outputType.getItemCount(); i++) {
                String entry = outputType.getItemAt(i);
                if (entry.equals(newType) && selectedIdx != i) {
                    outputType.setSelectedIndex(i);
                }
            }

        } else if (evt.getPropertyName().equals(IlastikPixelClassificationModel.PROPERTY_PREDICTION_MASK)) {
            Dataset newPredMask = (Dataset) evt.getNewValue();
            int selectedIdx = predictionMask.getSelectedIndex();
            if (newPredMask != null) {
                for (int i = 0; i < predictionMask.getItemCount(); i++) {
                    DatasetComboboxEntry entry = predictionMask.getItemAt(i);
                    if (newPredMask.getName().equals(entry.title) && selectedIdx != i) {
                        predictionMask.setSelectedIndex(i);
                    }
                }
            } else {
                if (selectedIdx != 0) {
                    outputType.setSelectedIndex(0);
                }
            }
        }
    }

}
