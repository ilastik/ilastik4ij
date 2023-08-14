package org.ilastik.ilastik4ij.io;

import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetWriter;
import org.openjdk.jmh.annotations.*;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 0, jvmArgs = {"-Xmx4g"})
@Warmup(iterations = 1)
@Measurement(iterations = 1)
public class Hdf5Benchmark {
    public ImgPlus<UnsignedByteType> img;
    public ImageJ ij;
    public LogService log;
    public StatusService status;

    public long[] dims;
    public AxisType[] axes;
    public String stringAxes;

    @Setup
    public void setup() {
        // 240.0 MB
//         dims = new long[]{256, 256, 3, 256, 5};

        dims = new long[]{256, 256, 3, 256, 1};

//        dims = new long[]{4096, 4096, 3, 1, 1};

        axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};
        stringAxes = "tzcyx"; // Reverse order!
        img = new ImgPlus<>(ArrayImgs.unsignedBytes(dims), "mydata", axes);
        ij = new ImageJ();
        log = ij.log();
        status = ij.status();
    }

    @Benchmark
    public void measureWriteOld() {
        new Hdf5DataSetWriter<>(img, "output.h5", "/data", 0, log, status).write();
    }

    @Benchmark
    public void measureWriteNew() {
        Hdf5.writeDataset(new File("output.h5"), "/data", img, 0);
    }

//    @Benchmark
//    public Object measureReadOld() {
//        return new Hdf5DataSetReader<>("large.h5", "/data", stringAxes, log, status).read();
//    }

//    @Benchmark
//    public Object measureReadNew() {
//        return Hdf5.readDataset(new File("large.h5"), "/data", Arrays.asList(axes));
//    }
}
