package org.ilastik.ilastik4ij.ui;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import org.scijava.log.LogService;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Vector;

public class IlastikPixelClassificationModel {
    public static final String PROPERTY_ILASTIK_PROJECT_FILE = "ilastikProjectFile";
    public static final String PROPERTY_OUTPUT_TYPE = "outputType";
    public static final String PROPERTY_RAW_INPUT = "rawInput";
    public static final String PROPERTY_PREDICTION_MASK = "predictionMask";

    private final LogService logService;
    private final PropertyChangeSupport propertyChangeSupport;

    private File ilastikProjectFile = null;
    private Dataset rawInput = null;
    private Dataset predictionMask = null;

    private String outputType = UiConstants.PIXEL_PREDICTION_TYPE_PROBABILITIES;

    public IlastikPixelClassificationModel(LogService logService) {
        this.logService = logService;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public File getIlastikProjectFile() {
        return this.ilastikProjectFile;
    }

    public void setIlastikProjectFile(File path) {
        File oldValue = this.ilastikProjectFile;
        this.ilastikProjectFile = path;
        firePropertyChange(PROPERTY_ILASTIK_PROJECT_FILE, oldValue, path);
    }
    
    public void setRawInput(Dataset dataset) {
        Dataset oldValue = this.rawInput;
        this.rawInput = dataset;
        firePropertyChange(PROPERTY_RAW_INPUT, oldValue, dataset);
    }
    
    public Dataset getRawInput() {
        return this.rawInput;
    }

    public void setPredictionMask(Dataset dataset) {
        Dataset oldValue = this.predictionMask;
        this.predictionMask = dataset;
        firePropertyChange(PROPERTY_PREDICTION_MASK, oldValue, dataset);
    }

    public Dataset getPredictionMask() {
        return this.predictionMask;
    }

    public void setOutputType(String type) {
        String oldValue = this.outputType;
        this.outputType = type;
        firePropertyChange(PROPERTY_OUTPUT_TYPE, oldValue, type);
    }

    public String getOutputType() {
        return this.outputType;
    }

    public void fireInitialProperties() {
        firePropertyChange(PROPERTY_ILASTIK_PROJECT_FILE, null, this.ilastikProjectFile);
        firePropertyChange(PROPERTY_OUTPUT_TYPE, null, this.outputType);
        firePropertyChange(PROPERTY_PREDICTION_MASK, null, this.predictionMask);
        firePropertyChange(PROPERTY_RAW_INPUT, null, this.rawInput);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}
