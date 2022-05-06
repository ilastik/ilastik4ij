package org.ilastik.ilastik4ij.hdf5;

import junit.framework.TestCase;
import net.imagej.ImageJ;
import org.scijava.Context;
import org.scijava.log.LogService;

import java.util.List;
import java.util.Vector;

public class HDF5DatasetEntryProviderTest extends TestCase {
    private Context context;
    private LogService logService;

    private static final String TEST_H5_RESOURCE = "src/test/resources/test.h5";
    private static final String TEST_AXES_H5_RESOURCE = "src/test/resources/test_axes.h5";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new Context();
        logService = context.getService(LogService.class);
    }

    public void testFindAvailableDatasetsNoAxes() {
        HDF5DatasetEntryProvider provider = new HDF5DatasetEntryProvider(logService);
        List<HDF5DatasetEntryProvider.DatasetEntry> infos = provider.findAvailableDatasets(TEST_H5_RESOURCE);
        assertEquals(1, infos.size());

        HDF5DatasetEntryProvider.DatasetEntry info = infos.get(0);
        assertEquals("/exported_data", info.path);
        assertEquals("tzyxc", info.axisTags);
        assertEquals("/exported_data: (7, 6, 5, 4, 3) uint16", info.toString());
    }

    public void testFindAvailableDatasets() {
        HDF5DatasetEntryProvider provider = new HDF5DatasetEntryProvider(logService);
        List<HDF5DatasetEntryProvider.DatasetEntry> infos = provider.findAvailableDatasets(TEST_AXES_H5_RESOURCE);
        assertEquals(2, infos.size());

        HDF5DatasetEntryProvider.DatasetEntry info = infos.get(0);
        assertEquals("/dataset_without_axes", info.path);
        assertEquals("yx", info.axisTags);
        assertEquals("/dataset_without_axes: (64, 64) int64", info.toString());

        HDF5DatasetEntryProvider.DatasetEntry info2 = infos.get(1);
        assertEquals("/exported_data", info2.path);
        assertEquals("zyxc", info2.axisTags);
        assertEquals("/exported_data: (256, 256, 256, 1) float32", info2.toString());
    }
}