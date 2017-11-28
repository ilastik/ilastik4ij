
package org.ilastik.ilastik4ij;

import net.imagej.ImgPlus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import io.scif.config.SCIFIOConfig;
import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;
import net.imagej.Dataset;
import net.imglib2.RandomAccess;
import net.imagej.ImageJ;
import org.scijava.Context;
import java.io.File;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import ij.ImagePlus;
import ij.process.ImageConverter;  

import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriterFromImgPlus;

//import net.imglib2.img.display.imagej.ImageJFunctions;
/**
 *
 * @author Ashis
 */
public class Hdf5DataSetReaderTest {
    
    
    public Hdf5DataSetReader hdf5Reader;
    final ImageJ ij = new ImageJ();
    public Context context = ij.getContext();
    public DatasetService ds = context.getService(DatasetService.class);
    public LogService log = context.getService(LogService.class);
    public String filename = "src/test/java/org/ilastik/ilastik4ij/test.h5";
    public Hdf5DataSetReaderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownClass() {
        
         System.out.println("In tear down class");
         new File("src/test/java/org/ilastik/ilastik4ij/chocolate.h5").delete();
         System.out.println("File deleted");
        
    }
    
    @Before
    public void setUp() {
                System.out.println("In SetUp");
        
    }
    
    @After
    public void tearDown() {
        System.out.println("In tear down");
    }

    /**
     * Test of read method, of class Hdf5DataSetReader.
     */
    @Test
    public void testReadAxes() {
        System.out.println("Loading file in tzyxc order");
        hdf5Reader = new Hdf5DataSetReader(filename, "exported_data", "tzyxc", log, ds); //
        ImgPlus image= hdf5Reader.read();
        assertEquals("Bits should be 16",image.getValidBits(),16);
        assertEquals("DimX should be 4",4,image.getImg().dimension(0));
        assertEquals("DimY should be 5",5,image.getImg().dimension(1));
        assertEquals("DimC should be 3",3,image.getImg().dimension(2));
        assertEquals("DimZ should be 6",6,image.getImg().dimension(3));
        assertEquals("DimT should be 7",7,image.getImg().dimension(4));
        
        System.out.println("Loading file in ztyxc order");
        hdf5Reader = new Hdf5DataSetReader(filename, "exported_data", "ztyxc", log, ds); //
        image= hdf5Reader.read();
        assertEquals("Bits should be 16",image.getValidBits(),16);
        assertEquals("DimX should be 4",4,image.getImg().dimension(0));
        assertEquals("DimY should be 5",5,image.getImg().dimension(1));
        assertEquals("DimC should be 3",3,image.getImg().dimension(2));
        assertEquals("DimZ should be 7",7,image.getImg().dimension(3));
        assertEquals("DimT should be 6",6,image.getImg().dimension(4));
                        
        System.out.println("Loading file in ztycx order");
        hdf5Reader = new Hdf5DataSetReader(filename, "exported_data", "ztycx", log, ds); //
        image= hdf5Reader.read();
        assertEquals("Bits should be 16",image.getValidBits(),16);
        assertEquals("DimX should be 3",3,image.getImg().dimension(0));
        assertEquals("DimY should be 5",5,image.getImg().dimension(1));
        assertEquals("DimC should be 4",4,image.getImg().dimension(2));
        assertEquals("DimZ should be 7",7,image.getImg().dimension(3));
        assertEquals("DimT should be 6",6,image.getImg().dimension(4));
    }
    
    @Test
    public void testImageContents(){
        System.out.println("Loading file in tzyxc order");
        hdf5Reader = new Hdf5DataSetReader(filename, "exported_data", "tzyxc", log, ds); 
        ImgPlus image= hdf5Reader.read();
        assertEquals("DimX",0,image.dimensionIndex(Axes.X));
        assertEquals("DimY",1,image.dimensionIndex(Axes.Y));
        assertEquals("DimC",2,image.dimensionIndex(Axes.CHANNEL));
        assertEquals("DimZ",3,image.dimensionIndex(Axes.Z));
        assertEquals("DimT",4,image.dimensionIndex(Axes.TIME));
        
        RandomAccess rai = image.randomAccess();
        rai.setPosition(1,image.dimensionIndex(Axes.CHANNEL));
        rai.setPosition(0,image.dimensionIndex(Axes.Y));
        rai.setPosition(0,image.dimensionIndex(Axes.X));
        rai.setPosition(5,image.dimensionIndex(Axes.Z));
        rai.setPosition(6,image.dimensionIndex(Axes.TIME));
        UnsignedShortType f = (UnsignedShortType)rai.get();
        assertEquals("CHANNEL value should be 200", 200, f.get());
        rai.setPosition(5,image.dimensionIndex(Axes.TIME));
        f = (UnsignedShortType)rai.get();
        assertEquals("CHANNEL value should be 0", 0, f.get());
        rai.setPosition(6,image.dimensionIndex(Axes.TIME));
        rai.setPosition(4,image.dimensionIndex(Axes.Z));
        f = (UnsignedShortType)rai.get();
        assertEquals("CHANNEL value should be 200", 200, f.get());
    }
    
    @Test
    public void testWriteHDF5Postive() throws Exception{
       String filename_HDF5 ="src/test/java/org/ilastik/ilastik4ij/chocolate.h5";
       DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
       Dataset input = datasetIOService.open("src/test/java/org/ilastik/ilastik4ij/chocolate21.jpg");
       new Hdf5DataSetWriterFromImgPlus(input.getImgPlus(),filename_HDF5 , "exported_data", 0, log).write();
       System.out.println("Loading file in tzyxc order");
       hdf5Reader = new Hdf5DataSetReader(filename_HDF5, "exported_data", "tzyxc", log, ds); //
       ImgPlus image= hdf5Reader.read();
       assertEquals("Bits should be 8",image.getValidBits(),8);
       assertEquals("DimX should be 400",400,image.getImg().dimension(0));
       assertEquals("DimY should be 289",289,image.getImg().dimension(1));
       assertEquals("DimC should be 3",3,image.getImg().dimension(2));
       assertEquals("DimZ should be 1",1,image.getImg().dimension(3));
       assertEquals("DimT should be 1",1,image.getImg().dimension(4));
    }
    
    /*
    @Test
    public void testWriteHDF5Negative() throws Exception{
       String filename_HDF5 ="C:/Users/user/Documents/UNI_HEIDELBERG/Uni_job/java_work/chocolate.h5";
       String filename_JPG="C:/Users/user/Documents/UNI_HEIDELBERG/Uni_job/java_work/chocolate21.jpg"
       DatasetIOService datasetIOService = context.getService(DatasetIOService.class);
       Dataset input = datasetIOService.open(filename_JPG);
       //ImgPlus<UnsignedShortType> img = (ImgPlus<UnsignedShortType>) input.getImgPlus();  
       ImageConverter ic = new ImageConverter(input.getImgPlus());
       ImageConverter ic = new ImageConverter(imp);
       ImagePlus imp = ImageJFunctions.
       IJ.run(imp, "8-bit", "");
       //input.firstElement()
       ARGBType argbRAI = new ARGBType(0) ;
       ImgPlus imgrgb= new ImgPlus (input);
       System.out.println(input.getType());
       System.out.println(input.getTypeLabelLong());
       RealARGBConverter con= new RealARGBConverter<>();
       //con.convert(input., imgrgb);
        RandomAccess ubyteRAI = input.getImgPlus().randomAccess(imgrgb);
       IntegerType f = (IntegerType)ubyteRAI.get();
       ImagePlus im = ImageJFunctions.wrapR 
       //Converters.convert( input, new RealARGBConverter<Integer>());
       //Hdf5DataSetWriterFromImgPlus hdf5=new Hdf5DataSetWriterFromImgPlus(input.getImgPlus(),filename_HDF5 , "data", 0, log);
       Hdf5DataSetWriterFromImgPlus hdf5=new Hdf5DataSetWriterFromImgPlus(imgrgb,filename_HDF5 , "data", 0, log);
       hdf5.write();
       System.out.println("Loading file in tzyxc order");
       hdf5Reader = new Hdf5DataSetReader(filename_HDF5, "exported_data", "tzyxc", log, ds); //
       ImgPlus image= hdf5Reader.read();
       assertEquals("Bits should be 8",image.getValidBits(),8);
       assertEquals("DimX should be 400",400,image.getImg().dimension(0));
       assertEquals("DimY should be 289",289,image.getImg().dimension(1));
       assertEquals("DimC should be 3",3,image.getImg().dimension(2));
       assertEquals("DimZ should be 1",1,image.getImg().dimension(3));
       assertEquals("DimT should be 1",1,image.getImg().dimension(4));
    }
    */
}
