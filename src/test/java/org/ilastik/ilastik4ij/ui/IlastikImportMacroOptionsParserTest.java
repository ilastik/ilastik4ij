package org.ilastik.ilastik4ij.ui;

import junit.framework.TestCase;

public class IlastikImportMacroOptionsParserTest extends TestCase {

    public void testParseOptionsNull() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions(null);
        assertEquals("", result.path);
        assertEquals("", result.axisOrder);
        assertEquals("", result.datasetName);
    }

    public void testParseOptionsEmptyString() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions("");
        assertEquals("", result.path);
        assertEquals("", result.axisOrder);
        assertEquals("", result.datasetName);
    }

    public void testParseOptionsPath() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions("select=../test");
        assertEquals("../test", result.path);
    }

    public void testParseOptionsAxes() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions("axisorder=xyc");
        assertEquals("xyc", result.axisOrder);
    }

    public void testParseOptionsDatasetName() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions("datasetname=/testDataset");
        assertEquals("/testDataset", result.datasetName);
    }

    public void testParseOptionsDatasetNameLegacy() {
        IlastikImportMacroOptionsParser.ParseResult result = IlastikImportMacroOptionsParser.parseOptions("datasetname=[/test_image: (17, 37, 127, 137, 7) uint8]");
        assertEquals("/test_image", result.datasetName);
    }
}