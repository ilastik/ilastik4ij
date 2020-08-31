package org.ilastik.ilastik4ij.ui;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import com.sun.istack.internal.Nullable;
import ij.IJ;
import org.apache.commons.lang.StringUtils;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider;
import org.ilastik.ilastik4ij.hdf5.HDF5DatasetEntryProvider.DatasetEntry;
import org.scijava.log.LogService;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Observable;
import java.util.Vector;

class IlastikImportModel extends Observable {
    private String path = "";
    private boolean isPathValid = false;

    private int datasetIdx = -1;

    private String axisTags = "";
    private boolean isAxisTagsValid = false;

    private boolean applyLut = false;
    private Vector<HDF5DatasetEntryProvider.DatasetEntry> availableDatasets = new Vector<>();
    private DatasetLoader loader;
    private LogService logService;

    public IlastikImportModel() {
    }

    public IlastikImportModel setDatasetLoader(DatasetLoader loader) {
        this.loader = loader;
        return this;
    }

    public IlastikImportModel setLogService(LogService logService) {
        this.logService = logService;
        return this;
    }

    public int getDatasetIdx() {
        return this.datasetIdx;
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

    @Nullable
    private DatasetEntry findDatasetByName(String name) {
        for (DatasetEntry entry : this.availableDatasets) {
            if (name.equals(entry.verboseName)) {
                return entry;
            }
        }
        return null;
    }

    public void setPath(String path) {
        if (this.path.equals(path)) {
            return;
        }

        this.isPathValid = true;
        this.path = path;

        try {
            HDF5DatasetEntryProvider infoProvider = new HDF5DatasetEntryProvider(path, logService);
            availableDatasets = infoProvider.findAvailableDatasets();
        } catch (Exception e) {
            availableDatasets = new Vector<>();
            this.isPathValid = false;
            this.setDatasetIdx(-1);
        }

        setChanged();
        notifyObservers();
    }

    public void setDatasetPath(String path) {
        if (!this.isPathValid || availableDatasets.size() == 0) {
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
        this.datasetIdx = idx;

        setChanged();
        notifyObservers();
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

    public Vector<String> getAvailableDatasetNames() {
        Vector<String> result = new Vector<>();

        for (DatasetEntry entry : this.availableDatasets) {
            result.add(entry.verboseName);
        }

        return result;
    }

    public String getAxisTags() {
        return axisTags;
    }

    public void setAxisTags(String axisTags) {
        if (this.axisTags.equals(axisTags)) {
            return;
        }
        this.axisTags = axisTags;

        setChanged();
        notifyObservers();
    }

    public boolean isAxisTagsValid() {
        if (isDatasetIdxValid()) {
            return this.availableDatasets.get(this.datasetIdx).rank == axisTags.length();
        } else {
            return false;
        }
    }
}

