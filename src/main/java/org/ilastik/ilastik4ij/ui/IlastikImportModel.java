package org.ilastik.ilastik4ij.ui;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider.DatasetEntry;
import org.scijava.log.LogService;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

class IlastikImportModel {
    public static final String PROPERTY_PATH = "path";
    public static final String PROPERTY_DATASET_IDX = "datasetIdx";
    public static final String PROPERTY_AXIS_TAGS = "axisTags";

    private String path = "";
    private int datasetIdx = -1;
    private String axisTags = "";

    private boolean isPathValid = false;

    private List<HDF5DatasetEntryProvider.DatasetEntry> availableDatasets = new Vector<>();
    private final LogService logService;
    private final PropertyChangeSupport propertyChangeSupport;
    private final HDF5DatasetEntryProvider entryProvider;

    public IlastikImportModel(LogService logService) {
        this.logService = logService;
        propertyChangeSupport = new PropertyChangeSupport(this);
        entryProvider = new HDF5DatasetEntryProvider(logService);
    }

    public int getDatasetIdx() {
        return datasetIdx;
    }

    public boolean isValid() {
        return this.isPathValid && this.isDatasetIdxValid() && this.isAxisTagsValid();
    }

    public String getPath() {
        return path;
    }

    public boolean isPathValid() {
        return this.isPathValid;
    }

    public void setPath(String path) {
        if (this.path.equals(path)) {
            return;
        }

        this.isPathValid = true;
        String oldPath = this.path;
        this.path = path;

        try {
            availableDatasets = entryProvider.findAvailableDatasets(path);
        } catch (HDF5Exception e) {
            availableDatasets = new Vector<>();
            this.isPathValid = false;
            this.setDatasetIdx(-1);
        }

        firePropertyChange(PROPERTY_PATH, oldPath, path);
    }

    public void setDatasetPath(String path) {
        if (!this.isPathValid || availableDatasets.isEmpty()) {
            return;
        }

        int idx = 0;
        for (DatasetEntry entry : availableDatasets) {
            if (entry.path.equals(path)) {
                this.datasetIdx = idx;
            }
            idx++;
        }
    }

    public String getDatasetPath() {
        if (isDatasetIdxValid()) {
            return this.availableDatasets.get(this.datasetIdx).path;
        } else {
            return "";
        }
    }

    public void setDatasetIdx(int idx) {
        if (this.datasetIdx == idx) {
            return;
        }
        int oldIdx = this.datasetIdx;
        this.datasetIdx = idx;

        firePropertyChange(PROPERTY_DATASET_IDX, oldIdx, idx);
    }

    public boolean isDatasetIdxValid() {
        return this.datasetIdx >= 0 && this.datasetIdx < this.availableDatasets.size();
    }

    public String getAxisTagsForDataset(int idx) {
        if (idx >= 0 && idx < this.availableDatasets.size()) {
            return this.availableDatasets.get(idx).axisTags;
        } else {
            return "";
        }
    }

    public List<String> getAvailableDatasetNames() {
        return availableDatasets.stream().map(e -> e.verboseName).collect(Collectors.toList());
    }

    public String getAxisTags() {
        return axisTags;
    }

    public void setAxisTags(String axisTags) {
        if (this.axisTags.equals(axisTags)) {
            return;
        }
        String oldValue = this.axisTags;
        this.axisTags = axisTags;

        firePropertyChange(PROPERTY_AXIS_TAGS, oldValue, axisTags);
    }

    public boolean isAxisTagsValid() {
        if (isDatasetIdxValid()) {
            return this.availableDatasets.get(this.datasetIdx).rank == axisTags.length();
        } else {
            return false;
        }
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

    public void fireInitialProperties() {
        firePropertyChange(IlastikImportModel.PROPERTY_PATH, null, this.path);
        firePropertyChange(IlastikImportModel.PROPERTY_DATASET_IDX, null, this.datasetIdx);
        firePropertyChange(IlastikImportModel.PROPERTY_AXIS_TAGS, null, this.axisTags);
    }
}

