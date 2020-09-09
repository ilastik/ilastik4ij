package org.ilastik.ilastik4ij.ui;

import ij.Macro;

import javax.crypto.Mac;

public class IlastikImportMacroOptionsParser {
    static class ParseResult {
        public final String path;
        public final String datasetName;
        public final String axisOrder;

        ParseResult(String path, String datasetName, String axisOrder) {
            this.path = path;
            this.datasetName = datasetName;
            this.axisOrder = axisOrder;
        }
    }

    static private String parseDatasetName(String name) {
        int idx = name.lastIndexOf(": (");
        if (idx != -1) {
            return name.substring(0, idx);
        } else {
            return name;
        }
    }

    static public ParseResult parseOptions(String options) {
        if (options == null) {
            return new ParseResult("", "", "");
        }
        String path = Macro.getValue(options, "select", "");
        String axisTags = Macro.getValue(options, "axisorder", "");
        String datasetName = Macro.getValue(options, "datasetname", "");
        datasetName = parseDatasetName(datasetName);
        return new ParseResult(path, datasetName, axisTags);
    }
}
