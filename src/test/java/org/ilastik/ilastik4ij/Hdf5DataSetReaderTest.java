/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ilastik.ilastik4ij;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.converter.RealUnsignedShortConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class Hdf5DataSetReaderTest {
    private static final List<AxisType> AXES = Arrays.asList(Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME);
    private static final String DATASET = "exported_data";
    private static Context context;
    private static LogService logService;
    private static StatusService statusService;
    private static DatasetIOService datasetIOService;

    private static File testH5;
    private static final String TEST_H5_RESOURCE = "src/test/resources/test.h5";
    private static final String TEST_JPG_RESOURCE = "src/test/resources/chocolate21.jpg";

    @BeforeClass
    public static void setUpClass() throws IOException {
        context = new ImageJ().getContext();
        logService = context.getService(LogService.class);
        statusService = context.getService(StatusService.class);
        datasetIOService = context.getService(DatasetIOService.class);
        testH5 = File.createTempFile("chocolate", "h5");
    }

    @AfterClass
    public static void tearDownClass() {
        testH5.deleteOnExit();
    }

    /**
     * Test of read method, of class Hdf5DataSetReader.
     */
    @Test
    public <T extends NativeType<T>> void testReadAxes() {
        ImgPlus<T> image = readFromHdf5(TEST_H5_RESOURCE, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 16", 16, image.getValidBits());
        assertEquals("DimX should be 4", 4, dims[0]);
        assertEquals("DimY should be 5", 5, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 6", 6, dims[3]);
        assertEquals("DimT should be 7", 7, dims[4]);

        image = readFromHdf5(TEST_H5_RESOURCE, DATASET, "ztyxc", logService, statusService);
        image.dimensions(dims);
        assertEquals("Bits should be 16", 16, image.getValidBits());
        assertEquals("DimX should be 4", 4, dims[0]);
        assertEquals("DimY should be 5", 5, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 7", 7, dims[3]);
        assertEquals("DimT should be 6", 6, dims[4]);

        image = readFromHdf5(TEST_H5_RESOURCE, "exported_data", "ztycx", logService, statusService);
        image.dimensions(dims);
        assertEquals("Bits should be 16", 16, image.getValidBits());
        assertEquals("DimX should be 3", 3, dims[0]);
        assertEquals("DimY should be 5", 5, dims[1]);
        assertEquals("DimC should be 4", 4, dims[2]);
        assertEquals("DimZ should be 7", 7, dims[3]);
        assertEquals("DimT should be 6", 6, dims[4]);
    }

    /**
     * Test of read method, of class Hdf5DataSetReader.
     */
    @Test
    public <T extends NativeType<T>> void testImageContents() {
        ImgPlus<T> image = readFromHdf5(TEST_H5_RESOURCE, DATASET, "tzyxc", logService, statusService);
        assertEquals("DimX", 0, AXES.indexOf(Axes.X));
        assertEquals("DimY", 1, AXES.indexOf(Axes.Y));
        assertEquals("DimC", 2, AXES.indexOf(Axes.CHANNEL));
        assertEquals("DimZ", 3, AXES.indexOf(Axes.Z));
        assertEquals("DimT", 4, AXES.indexOf(Axes.TIME));

        RandomAccess rai = image.randomAccess();
        rai.setPosition(1, AXES.indexOf(Axes.CHANNEL));
        rai.setPosition(0, AXES.indexOf(Axes.Y));
        rai.setPosition(0, AXES.indexOf(Axes.X));
        rai.setPosition(5, AXES.indexOf(Axes.Z));
        rai.setPosition(6, AXES.indexOf(Axes.TIME));
        UnsignedShortType f = (UnsignedShortType) rai.get();
        assertEquals("CHANNEL value should be 200", 200, f.get());
        rai.setPosition(5, AXES.indexOf(Axes.TIME));
        f = (UnsignedShortType) rai.get();
        assertEquals("CHANNEL value should be 0", 0, f.get());
        rai.setPosition(6, AXES.indexOf(Axes.TIME));
        rai.setPosition(4, AXES.indexOf(Axes.Z));
        f = (UnsignedShortType) rai.get();
        assertEquals("CHANNEL value should be 200", 200, f.get());
    }

    /**
     * Test of write method, specifically for 8 bit UnsignedByteType type image, of class Hdf5DataSetReader.
     */
    @Test
    public <T extends NativeType<T>> void testWriteHDF5Byte() throws Exception {
        Dataset input = datasetIOService.open(TEST_JPG_RESOURCE);
        ImgPlus<T> imgPlus = (ImgPlus<T>) input.getImgPlus();
        String h5Path = testH5.getPath();
        new Hdf5DataSetWriter<T>(imgPlus, h5Path, DATASET, 0, logService, statusService).write();
        // Loading file in tzyxc order
        ImgPlus<T> image = readFromHdf5(h5Path, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 8", 8, image.getValidBits());
        assertEquals("DimX should be 400", 400, dims[0]);
        assertEquals("DimY should be 289", 289, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 1", 1, dims[3]);
        assertEquals("DimT should be 1", 1, dims[4]);

        RandomAccess raiOut = image.randomAccess();
        raiOut.setPosition(0, AXES.indexOf(Axes.CHANNEL));
        raiOut.setPosition(80, AXES.indexOf(Axes.X));
        raiOut.setPosition(115, AXES.indexOf(Axes.Y));
        UnsignedByteType valOut = (UnsignedByteType) raiOut.get();

        RandomAccess raiIn = imgPlus.randomAccess();
        raiIn.setPosition(0, imgPlus.dimensionIndex(Axes.CHANNEL));
        raiIn.setPosition(80, imgPlus.dimensionIndex(Axes.X));
        raiIn.setPosition(115, imgPlus.dimensionIndex(Axes.Y));
        UnsignedByteType valIn = (UnsignedByteType) raiIn.get();

        assertEquals("Image content should be same.", valOut.get(), valIn.get());
    }

    /**
     * Test of write method, specifically for 8 bit ARGB type image, of class Hdf5DataSetReader.
     */
    @Test
    public void testWriteHDF5ARGB() throws Exception {
        String h5Path = testH5.getPath();
        Dataset input = datasetIOService.open(TEST_JPG_RESOURCE);
        ImgPlus<UnsignedByteType> inputImage = (ImgPlus<UnsignedByteType>) input.getImgPlus();
        final RandomAccessibleInterval<ARGBType> output = Converters.convert((RandomAccessibleInterval<UnsignedByteType>) inputImage, new RealARGBConverter<>(0, 255), new ARGBType());
        Img<ARGBType> imview = ImgView.wrap(output, inputImage.getImg().factory().imgFactory(new ARGBType()));
        AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        ImgPlus<ARGBType> imgrgb = new ImgPlus<>(imview, "", axes);
        Hdf5DataSetWriter<ARGBType> hdf5 = new Hdf5DataSetWriter<>(imgrgb, h5Path, DATASET, 0, logService, statusService);
        hdf5.write();
        // Loading file in tzyxc order
        ImgPlus<ARGBType> image = readFromHdf5(h5Path, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 8", 8, image.getValidBits());
        assertEquals("DimX should be 400", 400, dims[0]);
        assertEquals("DimY should be 289", 289, dims[1]);
        assertEquals("DimC should be 4", 4, dims[2]);
        assertEquals("DimZ should be 1", 1, dims[3]);
        assertEquals("DimT should be 1", 1, dims[4]);

        RandomAccess raiOut = image.randomAccess();
        raiOut.setPosition(1, AXES.indexOf(Axes.CHANNEL));
        raiOut.setPosition(80, AXES.indexOf(Axes.X));
        raiOut.setPosition(115, AXES.indexOf(Axes.Y));
        UnsignedByteType valout = (UnsignedByteType) raiOut.get();

        RandomAccess raiIn = imgrgb.randomAccess();
        raiIn.setPosition(0, imgrgb.dimensionIndex(Axes.CHANNEL));
        raiIn.setPosition(80, imgrgb.dimensionIndex(Axes.X));
        raiIn.setPosition(115, imgrgb.dimensionIndex(Axes.Y));
        ARGBType valIn = (ARGBType) raiIn.get();

        assertEquals("Image content should be same.", valout.get(), ARGBType.red(valIn.get()));

        raiOut.setPosition(0, AXES.indexOf(Axes.CHANNEL));
        raiOut.setPosition(80, AXES.indexOf(Axes.X));
        raiOut.setPosition(115, AXES.indexOf(Axes.Y));
        valout = (UnsignedByteType) raiOut.get();
        assertEquals("Alpha channel should be set to 255.", valout.get(), 255);
    }

    /**
     * Test of write method, specifically for 32 bit  Float type image, of class Hdf5DataSetReader.
     */
    @Test
    public void testWriteHDF5Float() throws Exception {
        String h5Path = testH5.getPath();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
        Dataset input = datasetIOService.open(TEST_JPG_RESOURCE);
        ImgPlus<UnsignedByteType> inputImage = (ImgPlus<UnsignedByteType>) input.getImgPlus();
        final RandomAccessibleInterval<FloatType> output = Converters.convert((RandomAccessibleInterval<UnsignedByteType>) inputImage, new RealFloatConverter<>(), new FloatType());
        Img<FloatType> imview = ImgView.wrap(output, inputImage.getImg().factory().imgFactory(new FloatType()));
        AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        ImgPlus<FloatType> imgrgb = new ImgPlus<>(imview, "", axes);
        Hdf5DataSetWriter<FloatType> hdf5 = new Hdf5DataSetWriter<>(imgrgb, h5Path, DATASET, 0, logService, statusService);
        hdf5.write();
        // Loading file in tzyxc order
        ImgPlus<FloatType> image = readFromHdf5(h5Path, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 32", 32, image.getValidBits());
        assertEquals("DimX should be 400", 400, dims[0]);
        assertEquals("DimY should be 289", 289, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 1", 1, dims[3]);
        assertEquals("DimT should be 1", 1, dims[4]);

        RandomAccess rai = image.randomAccess();
        rai.setPosition(1, AXES.indexOf(Axes.CHANNEL));
        rai.setPosition(80, AXES.indexOf(Axes.X));
        rai.setPosition(115, AXES.indexOf(Axes.Y));
        FloatType valOut = (FloatType) rai.get();

        RandomAccess rai2 = imgrgb.randomAccess();
        rai2.setPosition(1, imgrgb.dimensionIndex(Axes.CHANNEL));
        rai2.setPosition(80, imgrgb.dimensionIndex(Axes.X));
        rai2.setPosition(115, imgrgb.dimensionIndex(Axes.Y));
        FloatType valIn = (FloatType) rai2.get();
        assertEquals("Image content should be same.", valOut.get(), valIn.get(), 0);

    }

    /**
     * Test of write method, specifically for 16 bit Short type image, of class Hdf5DataSetReader.
     */
    @Test
    public void testWriteHDF5Short() throws Exception {
        String h5Path = testH5.getPath();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
        Dataset input = datasetIOService.open(TEST_JPG_RESOURCE);
        ImgPlus<UnsignedByteType> inputImage = (ImgPlus<UnsignedByteType>) input.getImgPlus();
        final RandomAccessibleInterval<UnsignedShortType> output = Converters.convert((RandomAccessibleInterval<UnsignedByteType>) inputImage, new RealUnsignedShortConverter<>(0, 255), new UnsignedShortType());
        Img<UnsignedShortType> imview = ImgView.wrap(output, inputImage.getImg().factory().imgFactory(new UnsignedShortType()));
        AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        ImgPlus<UnsignedShortType> imgrgb = new ImgPlus<>(imview, "", axes);
        Hdf5DataSetWriter<UnsignedShortType> hdf5 = new Hdf5DataSetWriter<>(imgrgb, h5Path, DATASET, 0, logService, statusService);
        hdf5.write();
        // Loading file in tzyxc order
        ImgPlus<UnsignedShortType> image = readFromHdf5(h5Path, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 16", 16, image.getValidBits());
        assertEquals("DimX should be 400", 400, dims[0]);
        assertEquals("DimY should be 289", 289, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 1", 1, dims[3]);
        assertEquals("DimT should be 1", 1, dims[4]);

        RandomAccess rai = image.randomAccess();
        rai.setPosition(1, AXES.indexOf(Axes.CHANNEL));
        rai.setPosition(80, AXES.indexOf(Axes.X));
        rai.setPosition(115, AXES.indexOf(Axes.Y));
        UnsignedShortType valOut = (UnsignedShortType) rai.get();

        RandomAccess rai2 = imgrgb.randomAccess();
        rai2.setPosition(1, imgrgb.dimensionIndex(Axes.CHANNEL));
        rai2.setPosition(80, imgrgb.dimensionIndex(Axes.X));
        rai2.setPosition(115, imgrgb.dimensionIndex(Axes.Y));
        UnsignedShortType valIn = (UnsignedShortType) rai2.get();
        assertEquals("Image content should be same.", valOut.get(), valIn.get());

    }

    /**
     * Test of write method, specifically for 32 bit  Integer type image, of class Hdf5DataSetReader.
     */
    @Test
    public void testWriteHDF5Int32() throws Exception {
        String h5Path = testH5.getPath();
        DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
        Dataset input = datasetIOService.open(TEST_JPG_RESOURCE);
        ImgPlus<UnsignedByteType> inputImage = (ImgPlus<UnsignedByteType>) input.getImgPlus();
        final RandomAccessibleInterval<UnsignedIntType> output = Converters.convert((RandomAccessibleInterval<UnsignedByteType>) inputImage, new RealUnsignedIntConverter<>(0, 255), new UnsignedIntType());
        Img<UnsignedIntType> imview = ImgView.wrap(output, inputImage.getImg().factory().imgFactory(new UnsignedIntType()));
        AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        ImgPlus<UnsignedIntType> imgrgb = new ImgPlus<>(imview, "", axes);
        Hdf5DataSetWriter<UnsignedIntType> hdf5 = new Hdf5DataSetWriter<>(imgrgb, h5Path, DATASET, 0, logService, statusService);
        hdf5.write();
        // Loading file in tzyxc order
        ImgPlus<UnsignedIntType> image = readFromHdf5(h5Path, DATASET, "tzyxc", logService, statusService);
        long[] dims = new long[5];
        image.dimensions(dims);
        assertEquals("Bits should be 32", 32, image.getValidBits());
        assertEquals("DimX should be 400", 400, dims[0]);
        assertEquals("DimY should be 289", 289, dims[1]);
        assertEquals("DimC should be 3", 3, dims[2]);
        assertEquals("DimZ should be 1", 1, dims[3]);
        assertEquals("DimT should be 1", 1, dims[4]);

        RandomAccess rai = image.randomAccess();
        rai.setPosition(1, AXES.indexOf(Axes.CHANNEL));
        rai.setPosition(80, AXES.indexOf(Axes.X));
        rai.setPosition(115, AXES.indexOf(Axes.Y));
        UnsignedIntType valOut = (UnsignedIntType) rai.get();

        RandomAccess rai2 = imgrgb.randomAccess();
        rai2.setPosition(1, imgrgb.dimensionIndex(Axes.CHANNEL));
        rai2.setPosition(80, imgrgb.dimensionIndex(Axes.X));
        rai2.setPosition(115, imgrgb.dimensionIndex(Axes.Y));
        UnsignedIntType valIn = (UnsignedIntType) rai2.get();
        assertEquals("Image content should be same.", valOut.get(), valIn.get());
    }

    private <T extends NativeType<T>> ImgPlus<T> readFromHdf5(String filename, String dataset, String axesOrder,
                                                              LogService logService, StatusService statusService) {
        return new Hdf5DataSetReader<T>(filename, dataset, axesOrder, logService, statusService).read();
    }

}
