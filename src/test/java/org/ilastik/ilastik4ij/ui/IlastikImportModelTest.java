package org.ilastik.ilastik4ij.ui;

import junit.framework.TestCase;
import org.ilastik.ilastik4ij.hdf5.DatasetEntryProvider;
import org.ilastik.ilastik4ij.hdf5.DatasetEntryProvider.DatasetEntry;
import org.scijava.Context;
import org.scijava.log.LogService;

import java.util.ArrayList;
import java.util.List;

public class IlastikImportModelTest extends TestCase {
    private IlastikImportModel model;

    class StubEntryProvider implements DatasetEntryProvider {

        @Override
        public List<DatasetEntry> findAvailableDatasets(String path) {
            List<DatasetEntry> result = new ArrayList<>();
            result.add(new DatasetEntry("/firstDataset", 3, "xyc", "/firstDataset"));
            result.add(new DatasetEntry("/secondDataset", 4, "xyzc", "/secondDataset"));
            return result;
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Context context = new Context();
        LogService logService = context.getService(LogService.class);
        model = new IlastikImportModel(logService, new StubEntryProvider());
        model.setPath("/valid");
        assertTrue(model.isPathValid());
    }


    public void testSettingDataset() {
        model.setDatasetPath("/firstDataset");

        assertTrue(model.isDatasetIdxValid());
        assertEquals(0, model.getDatasetIdx());

        model.setDatasetPath("/secondDataset");

        assertTrue(model.isDatasetIdxValid());
        assertEquals(1, model.getDatasetIdx());

        model.setDatasetPath("/myRandomDataset");

        assertFalse(model.isDatasetIdxValid());
        assertEquals(-1, model.getDatasetIdx());
    }

    public void testSettingDatasetAxisTags() {
        model.setDatasetPath("/firstDataset");

        model.setAxisTags("cyx");
        assertTrue(model.isAxisTagsValid());

        model.setAxisTags("yx");
        assertFalse(model.isAxisTagsValid());

        model.setAxisTags("xy");
        assertFalse(model.isAxisTagsValid());

        model.setAxisTags("zy");
        assertFalse(model.isAxisTagsValid());

        model.setAxisTags("xyc");
        assertTrue(model.isAxisTagsValid());
    }

    public void testSettingDatasetAxisTagsWithDuplicateAxes() {
        model.setDatasetPath("/firstDataset");

        model.setAxisTags("ccx");
        assertFalse(model.isAxisTagsValid());
    }

    public void testSettingDatasetAxisTagsWithTooManyAxes() {
        model.setDatasetPath("/firstDataset");

        model.setAxisTags("xyzctf");
        assertFalse(model.isAxisTagsValid());
    }

    public void testSettingDatasetAxisTagsWithEmptyAxes() {
        model.setDatasetPath("/firstDataset");

        model.setAxisTags("");
        assertFalse(model.isAxisTagsValid());
    }
}