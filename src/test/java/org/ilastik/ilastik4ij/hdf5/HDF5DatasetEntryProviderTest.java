package org.ilastik.ilastik4ij.hdf5;

import junit.framework.TestCase;
import net.imagej.ImageJ;
import org.scijava.Context;
import org.scijava.log.LogService;

import java.util.Vector;

public class HDF5DatasetEntryProviderTest extends TestCase {
    private Context context;
    private LogService logService;

    private static final String TEST_H5_RESOURCE = "src/test/resources/test.h5";
    private static final String TEST_AXES_H5_RESOURCE = "src/test/resources/test_axes.h5";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ImageJ().getContext();
        logService = context.getService(LogService.class);
    }

    public void testFindAvailableDatasetsNoAxes() {
        HDF5DatasetEntryProvider provider = new HDF5DatasetEntryProvider(logService);
        Vector<HDF5DatasetEntryProvider.DatasetEntry> infos = provider.findAvailableDatasets(TEST_H5_RESOURCE);
        assertEquals(1, infos.size());

        HDF5DatasetEntryProvider.DatasetEntry info = infos.get(0);
        assertEquals("/exported_data", info.path);
        assertEquals("/exported_data: (7, 6, 5, 4, 3) uint16", info.verboseName);
        assertEquals("tzyxc", info.axisTags);
    }

    public void testFindAvailableDatasetsWithAxes() {
        HDF5DatasetEntryProvider provider = new HDF5DatasetEntryProvider(logService);
        Vector<HDF5DatasetEntryProvider.DatasetEntry> infos = provider.findAvailableDatasets(TEST_AXES_H5_RESOURCE);
        assertEquals(1, infos.size());

        HDF5DatasetEntryProvider.DatasetEntry info = infos.get(0);
        assertEquals("/exported_data", info.path);
        assertEquals("/exported_data: (256, 256, 256, 1) float32", info.verboseName);
        assertEquals("zyxc", info.axisTags);
    }
}