package org.ilastik.ilastik4ij;

import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dset_extent;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dwrite;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5F.H5Fclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5F.H5Fcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pset_chunk;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pset_deflate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Screate_simple;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_hyperslab;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5F_ACC_TRUNC;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DATASET_CREATE;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5S_ALL;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT16;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;

import org.scijava.log.LogService;

import ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import static java.lang.Integer.min;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;

public class Hdf5DataSetWriter {
	private ImagePlus image;
	private ImageStack stack;
	
	private int nFrames;
	private int nChannels;
	private int nLevs;
	private int nRows;
	private int nCols;

	private LogService log;
	private String filename;
	private String dataset;
	private int compressionLevel;
	
	private int file_id = -1;
	private int dataspace_id = -1;
	private int dataset_id = -1;
	private int dcpl_id = -1 ;
	private long[] maxdims = { 
		HDF5Constants.H5S_UNLIMITED, 
		HDF5Constants.H5S_UNLIMITED, 
		HDF5Constants.H5S_UNLIMITED, 
		HDF5Constants.H5S_UNLIMITED,
		HDF5Constants.H5S_UNLIMITED 
	};

	public Hdf5DataSetWriter(ImagePlus image, String filename, String dataset, int compressionLevel, LogService log)
	{
		this.image     = image;
		this.stack     = image.getStack();
		this.nFrames   = image.getNFrames();
		this.nChannels = image.getNChannels();
		this.nLevs     = image.getNSlices();
		this.nRows     = image.getHeight();
		this.nCols     = image.getWidth();
		this.filename  = filename;
		this.dataset   = dataset;
		this.compressionLevel = compressionLevel;
		this.log       = log;
	}
	
	public void write() 
	{
		long[] chunk_dims = {1, 
            min(nLevs, 256),
            min(nRows, 256), 
            min(nCols, 256), 
            1
        };
		log.info("Export Dimensions in tzyxc: " + String.valueOf(nFrames) + "x" + String.valueOf(nLevs) + "x" 
				+ String.valueOf(nRows) + "x" + String.valueOf(nCols) + "x" + String.valueOf(nChannels));

		try
		{
			try {
				file_id = H5Fcreate(filename, H5F_ACC_TRUNC, H5P_DEFAULT, H5P_DEFAULT);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try{
				dcpl_id = H5Pcreate(H5P_DATASET_CREATE);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try{
				H5Pset_chunk(dcpl_id, 5, chunk_dims);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try{
				H5Pset_deflate(dcpl_id, compressionLevel);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			int imgColorType = image.getType();
			if (imgColorType == ImagePlus.GRAY8 || imgColorType == ImagePlus.COLOR_256 )
			{	
                log.info("Writing uint 8");
				writeIndividualChannels(H5T_NATIVE_UINT8);
			}
			else if (imgColorType == ImagePlus.GRAY16) 
			{
                log.info("Writing uint 16");
				writeIndividualChannels(H5T_NATIVE_UINT16);
			}
			else if (imgColorType == ImagePlus.GRAY32)
			{
                log.info("Writing uint 32");
				writeIndividualChannels(H5T_NATIVE_FLOAT);
			} 
			else if (imgColorType == ImagePlus.COLOR_RGB){
                log.info("Writing RGB to 3 uint8 channels");
				writeRGB();
			}
			else {
				log.error("Type Not handled yet!");
			}

			try {
				if (dataspace_id >= 0)    
				{
					H5Sclose(dataspace_id);
					dataspace_id = -1;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try {
				H5Pclose(dcpl_id);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try {    
				if (dataset_id >= 0)
					H5Dclose(dataset_id);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try {
				if (file_id >= 0)
					H5Fclose(file_id);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (HDF5Exception err) 
		{
			log.error("Error while saving '" + filename + "':\n" + err);
		} 
		catch (Exception err) 
		{
			log.error("Error while saving '" + filename + "':\n" + err);
		} 
		catch (OutOfMemoryError o) 
		{
			log.error("Out of Memory Error while saving '" + filename + "':\n" + o);
		}
	}

	private void writeRGB()
	{
		long[] channelDimsRGB = null;
		channelDimsRGB = new long[5];
		channelDimsRGB[0] = nFrames; //t
		channelDimsRGB[1] = nLevs ; //z
		channelDimsRGB[2] = nRows; //y
		channelDimsRGB[3] = nCols; //x
		channelDimsRGB[4] = 3;
		
		long[] color_iniDims = new long[5];
		color_iniDims[0] = 1;
		color_iniDims[1] = 1;
		color_iniDims[2] = nRows;
		color_iniDims[3] = nCols;
		color_iniDims[4] = 3;

		try {
			dataspace_id = H5Screate_simple(5, color_iniDims, maxdims);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if ((file_id >= 0) && (dataspace_id >= 0))
				dataset_id =  H5Dcreate(file_id, dataset, H5T_NATIVE_UINT8, dataspace_id, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		for (int t=0; t<=nFrames; t++){
			for (int z=0; z<nLevs; z++) {
				int stackIndex = image.getStackIndex(1, z + 1, t + 1);
				ColorProcessor cp = (ColorProcessor)(stack.getProcessor(stackIndex));
				byte[] red   = cp.getChannel(1);
				byte[] green = cp.getChannel(2);
				byte[] blue  = cp.getChannel(3);

				byte[][] color_target = new byte[3][red.length];
                color_target[0] = red;
                color_target[1] = green;
                color_target[2] = blue;

				try {
					if (dataspace_id >= 0)    
					{
						H5Sclose(dataspace_id);
						dataspace_id = -1;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				if (z==0 ){
					try {
						if (dataset_id >= 0)
							H5Dset_extent(dataset_id, channelDimsRGB);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

				for (int c=0; c<3; c++){
					try {
						if (dataset_id >= 0) {
							dataspace_id = H5Dget_space(dataset_id);

							long[] start = {t,z,0,0,c};
							long[] iniDims = {0,0,nCols,nRows,1};

							H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims, null);
							int memspace = H5Screate_simple(5, iniDims, null);

							if (dataspace_id >= 0)
								H5Dwrite(dataset_id, H5T_NATIVE_UINT8, memspace, dataspace_id, H5P_DEFAULT, color_target[c]);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		log.info("write uint8 RGB HDF5");
		log.info("compressionLevel: " + String.valueOf(compressionLevel));
		log.info("Done");
	}


	private void writeIndividualChannels(int hdf5DataType)
	{
		long[] channel_Dims = null;
		if (nLevs < 1) 
		{
			log.error("got less than 1 z?");
			nLevs = 1;
		} 
		else 
		{
			channel_Dims = new long[5];
			channel_Dims[0] = nFrames; // t
			channel_Dims[1] = nLevs; // z
			channel_Dims[2] = nRows; //y
			channel_Dims[3] = nCols; //x
			channel_Dims[4] = nChannels; // c
		}

		long[] iniDims = new long[5];
		iniDims[0] = 1;
		iniDims[1] = 1;
		iniDims[2] = nRows;
		iniDims[3] = nCols;
		iniDims[4] = 1;

		try {
			dataspace_id = H5Screate_simple(5, iniDims, maxdims);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if ((file_id >= 0) && (dataspace_id >= 0))
				dataset_id =  H5Dcreate(file_id, dataset, hdf5DataType, dataspace_id, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		for (int stackIndex = 1; stackIndex <= stack.getSize(); stackIndex++){
			int[] slicePosition = image.convertIndexToPosition(stackIndex); // contains (channel, slice, frame) indices
			log.info("Slice " + stackIndex + " position is: [c:" + slicePosition[0] + ", s:" + slicePosition[1] + ", f:" + slicePosition[2] + "]");

			if (stackIndex == 1){
				try {
					if (dataset_id >= 0) {
						if(hdf5DataType == H5T_NATIVE_UINT8) {
							H5Dwrite(dataset_id, hdf5DataType, H5S_ALL, H5S_ALL, H5P_DEFAULT, (byte[])stack.getPixels(stackIndex));
						}
						else if(hdf5DataType == H5T_NATIVE_UINT16) {
							H5Dwrite(dataset_id, hdf5DataType, H5S_ALL, H5S_ALL, H5P_DEFAULT, (short[])stack.getPixels(stackIndex));
						}
						else if(hdf5DataType == H5T_NATIVE_FLOAT) {
							H5Dwrite(dataset_id, hdf5DataType, H5S_ALL, H5S_ALL, H5P_DEFAULT, (float[])stack.getPixels(stackIndex));
						}
                        else {
                            throw new IllegalArgumentException("Trying to save dataset of unknown datatype");
                        }
					}
					else
						throw new HDF5Exception("No active dataset for writing first chunk");
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				try {
					if (dataspace_id >= 0)    
					{
						H5Sclose(dataspace_id);
						dataspace_id = -1;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

                try {
                    if (dataset_id >= 0)
                        H5Dset_extent(dataset_id, channel_Dims);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
			}
			else{
				try {
					if (dataset_id >= 0) {
						dataspace_id = H5Dget_space(dataset_id);
						long[] start = {slicePosition[2]-1, slicePosition[1]-1, 0, 0, slicePosition[0]-1}; // tzyxc
						H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims, null);

						int memspace = H5Screate_simple(5, iniDims, null);
						if (dataspace_id >= 0) {
							if(hdf5DataType == H5T_NATIVE_UINT8) {
								H5Dwrite(dataset_id, hdf5DataType, memspace, dataspace_id, H5P_DEFAULT, (byte[])stack.getPixels(stackIndex));
							}
							else if(hdf5DataType == H5T_NATIVE_UINT16) {
								H5Dwrite(dataset_id, hdf5DataType, memspace, dataspace_id, H5P_DEFAULT, (short[])stack.getPixels(stackIndex));
							}
							else if(hdf5DataType == H5T_NATIVE_FLOAT) {
								H5Dwrite(dataset_id, hdf5DataType, memspace, dataspace_id, H5P_DEFAULT, (float[])stack.getPixels(stackIndex));
							}
						}
						else
							throw new HDF5Exception("No active dataset for writing remaining");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		log.info("write hdf5");
		log.info("Done");
	}
}
