package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.*;
import ncsa.hdf.hdf5lib.exceptions.HDF5AttributeException;
import org.ilastik.ilastik4ij.util.Hdf5Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.scijava.log.LogService;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class HDF5DatasetEntryProvider {
    static class InvalidAxisTagsException extends Exception {}
    private final String path;
    private final LogService logService;

    public HDF5DatasetEntryProvider(String path, LogService logService) {
        this.path = path;
        this.logService = logService;
    }

    public Vector<DatasetEntry> findAvailableDatasets() {
        return this.findAvailableDatasets("/");
    }

    private DatasetEntry getDatasetInfo(String internalPath, IHDF5Reader reader) {
        HDF5DataSetInformation hdf5DatasetInfo = reader.object().getDataSetInformation(internalPath);
        int dsRank = hdf5DatasetInfo.getRank();
        String axisTags = defaultAxisOrder(dsRank);

        try {
            String axisTagsJSON = reader.string().getAttr(internalPath, "axistags");
            axisTags = parseAxisTags(axisTagsJSON);
            logService.debug("Detected axistags " + axisTags + " in dataset " + internalPath);
        } catch (HDF5AttributeException e) {
            logService.debug("No axistags attribute in dataset");
        } catch (HDF5DatasetEntryProvider.InvalidAxisTagsException e) {
            logService.debug("Invalid axistags attribute in dataset");
        }

        return new DatasetEntry(internalPath, dsRank, axisTags, makeVerboseName(internalPath, hdf5DatasetInfo));
    }

    private String makeVerboseName(String internalPath, HDF5DataSetInformation dsInfo) {
        String shape = Arrays.stream(dsInfo.getDimensions())
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        String dtype = Hdf5Utils.getTypeInfo(dsInfo);
        return String.format("%s: (%s) %s", internalPath, shape, dtype);
    }

    private String defaultAxisOrder(int rank) {
        // Uses ilastik default axis order,
        // see https://github.com/ilastik/ilastik/blob/a1bb868b0a8d43ac3c89e681cc89d43be9591ff7/lazyflow/utility/helpers.py#L107
        switch (rank) {
            case 5:
                return "tzyxc";
            case 4:
                return "zyxc";
            case 3:
                return "zyx";
            default:
                return "yx";
        }
    }

    private Vector<DatasetEntry> findAvailableDatasets(String intenalPath) {
        Vector<DatasetEntry> result = new Vector<>();

        try (IHDF5Reader reader = HDF5Factory.openForReading(path)) {
            HDF5LinkInformation link = reader.object().getLinkInformation(intenalPath);
            List<HDF5LinkInformation> members = reader.object().getGroupMemberInformation(link.getPath(), true);

            for (HDF5LinkInformation linkInfo : members) {
                switch (linkInfo.getType()) {
                    case DATASET:
                        DatasetEntry datasetEntry = getDatasetInfo(linkInfo.getPath(), reader);
                        result.add(datasetEntry);
                        break;
                    case GROUP:
                        result.addAll(findAvailableDatasets(linkInfo.getPath()));
                        break;
                }
            }
        }

        return result;
    }

    private static String parseAxisTags(String jsonString) throws HDF5DatasetEntryProvider.InvalidAxisTagsException {
        JSONObject axisObject = new JSONObject(jsonString);
        JSONArray axesArray = axisObject.optJSONArray("axes");
        StringBuilder axisTags = new StringBuilder();

        if (axesArray == null) {
            throw new HDF5DatasetEntryProvider.InvalidAxisTagsException();
        }

        for (int i = 0; i < axesArray.length(); i++) {
            JSONObject axisEntry = axesArray.optJSONObject(i);
            if (axisEntry == null) {
                throw new HDF5DatasetEntryProvider.InvalidAxisTagsException();
            }

            String axisTag = axisEntry.optString("key");

            if (axisTag == null) {
                throw new HDF5DatasetEntryProvider.InvalidAxisTagsException();
            }

            axisTags.append(axisTag);
        }

        return axisTags.toString();
    }

    public static class DatasetEntry {
        public final String path;
        public final String axisTags;
        public final String name;
        public final int rank;

        public DatasetEntry(String path, int rank, String axisTags, String name) {
            this.path = path;
            this.rank = rank;
            this.axisTags = axisTags;
            this.name = name;
        }

    }
}
