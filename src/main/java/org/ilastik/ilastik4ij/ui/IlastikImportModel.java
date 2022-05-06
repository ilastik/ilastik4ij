package org.ilastik.ilastik4ij.ui;

import hdf.hdf5lib.exceptions.HDF5Exception;
import org.ilastik.ilastik4ij.hdf5.DatasetEntryProvider;
import org.ilastik.ilastik4ij.hdf5.DatasetEntryProvider.DatasetEntry;
import org.scijava.log.LogService;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.stream.Collectors;

class IlastikImportModel {
    public static final char[] VALID_SORTED_AXIS_TAGS = {'c', 't', 'x', 'y', 'z'};
    public static final String PROPERTY_PATH = "path";
    public static final String PROPERTY_DATASET_IDX = "datasetIdx";
    public static final String PROPERTY_AXIS_TAGS = "axisTags";

    private String path = "";
    private int datasetIdx = -1;
    private String axisTags = "";

    private boolean isPathValid = false;

    private List<DatasetEntry> availableDatasets = new ArrayList<>();
    private final LogService logService;
    private final PropertyChangeSupport propertyChangeSupport;
    private final DatasetEntryProvider entryProvider;

    public IlastikImportModel(LogService logService, DatasetEntryProvider provider) {
        this.logService = logService;
        propertyChangeSupport = new PropertyChangeSupport(this);
        entryProvider = provider;
    }

    public int getDatasetIdx() {
        return datasetIdx;
    }

    public boolean isValid() {
        return isPathValid && isDatasetIdxValid() && isAxisTagsValid();
    }

    public String getPath() {
        return path;
    }

    public boolean isPathValid() {
        return isPathValid;
    }

    public void setPath(String path) {
        if (this.path.equals(path)) {
            return;
        }

        isPathValid = true;
        String oldPath = this.path;
        this.path = path;

        try {
            availableDatasets = entryProvider.findAvailableDatasets(path);
        } catch (DatasetEntryProvider.ReadException e) {
            availableDatasets = new ArrayList<>();
            isPathValid = false;
            setDatasetIdx(-1);
        }

        firePropertyChange(PROPERTY_PATH, oldPath, path);
    }

    public void setDatasetPath(String path) {
        if (!isPathValid || availableDatasets.isEmpty()) {
            return;
        }

        int cur = 0;
        int foundIdx = -1;

        for (DatasetEntry entry : availableDatasets) {
            if (entry.path.equals(path)) {
                foundIdx = cur;
            }
            cur++;
        }

        datasetIdx = foundIdx;
    }

    public String getDatasetPath() {
        if (isDatasetIdxValid()) {
            return availableDatasets.get(datasetIdx).path;
        } else {
            return "";
        }
    }

    public void setDatasetIdx(int idx) {
        if (datasetIdx == idx) {
            return;
        }
        int oldIdx = datasetIdx;
        datasetIdx = idx;

        firePropertyChange(PROPERTY_DATASET_IDX, oldIdx, idx);
    }

    public boolean isDatasetIdxValid() {
        return datasetIdx >= 0 && datasetIdx < availableDatasets.size();
    }

    public String getAxisTagsForDataset(int idx) {
        if (idx >= 0 && idx < availableDatasets.size()) {
            return availableDatasets.get(idx).axisTags;
        } else {
            return "";
        }
    }

    public List<String> getAvailableDatasetNames() {
        return availableDatasets.stream().map(Object::toString).collect(Collectors.toList());
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
            if (axisTags.length() > 5) {
                return false;
            };

            char[] normalizedTags = axisTags.toLowerCase().toCharArray();
            Arrays.sort(normalizedTags);
            int count = 0;
            int cur = 0;

            for (char c : normalizedTags) {
                while (cur < VALID_SORTED_AXIS_TAGS.length) {
                    char check = VALID_SORTED_AXIS_TAGS[cur];
                    cur++;
                    if (c == check) {
                        count++;
                        break;
                    }
                }
            }

            return availableDatasets.get(datasetIdx).shape.length == count;
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
        firePropertyChange(IlastikImportModel.PROPERTY_PATH, null, path);
        firePropertyChange(IlastikImportModel.PROPERTY_DATASET_IDX, null, datasetIdx);
        firePropertyChange(IlastikImportModel.PROPERTY_AXIS_TAGS, null, axisTags);
    }
}

