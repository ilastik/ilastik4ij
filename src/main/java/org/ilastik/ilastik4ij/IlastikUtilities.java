/**
 * MIT License
 * 
 * Copyright (c) 2017 ilastik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Author: Carsten Haubold
 */

package org.ilastik.ilastik4ij;

import java.io.File;
import java.io.IOException;

import org.scijava.log.LogService;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants;

import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pset_chunk;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5P.H5Pset_deflate;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DATASET_CREATE;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Screate_simple;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5F.H5Fcreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5F.H5Fclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dwrite;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Acreate;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Awrite;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dset_extent;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_all;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_hyperslab;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sget_simple_extent_dims;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5F_ACC_TRUNC;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5S_ALL;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT16;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_STD_I8BE;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_STRING;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class IlastikUtilities {
	/**
	 * Utility method to obtain a unique filename
	 * @param extension
	 * @return
	 * @throws IOException
	 */
	public static String getTemporaryFileName(String extension) throws IOException
	{
		File f = File.createTempFile("ilastik4j", extension, null);
		String filename = f.getAbsolutePath();
		f.delete();
		return filename;
	}
	
	public static ImagePlus readFloatHdf5VolumeIntoImage(String filename, String dataset, String axesorder)
	{
		IHDF5Reader reader = HDF5Factory.openForReading(filename);
		Hdf5DataSetConfig dsConfig = new Hdf5DataSetConfig(reader, dataset, axesorder);
		if(!dsConfig.typeInfo.equals("float32"))
		{
			throw new IllegalArgumentException("Dataset is not of float datatype!");
		}

		MDFloatArray rawdata = reader.float32().readMDArray(dataset);
		float[] flat_data = rawdata.getAsFlatArray();
		float maxGray = 1;

		ImagePlus image = IJ.createHyperStack(dataset, dsConfig.dimX, dsConfig.dimY, dsConfig.numChannels, dsConfig.dimZ, dsConfig.numFrames, 32);

		// TODO: make this work with hyperslabs instead of reading the full HDF5 volume into memory at once!
		for (int frame = 0; frame < dsConfig.numFrames; ++frame)
		{
			for( int lev = 0; lev < dsConfig.dimZ; ++lev)
			{
				for (int c = 0; c < dsConfig.numChannels; ++c)
				{
					ImageProcessor ip = image.getStack().getProcessor( image.getStackIndex(c +1, lev+1, frame+1));
					float[] destData = (float[])ip.getPixels();

					for (int x = 0; x < dsConfig.dimX; x++)
					{
						for (int y = 0; y < dsConfig.dimY; y++)
						{
							int scrIndex = c + lev * dsConfig.numChannels 
											 + y * dsConfig.dimZ * dsConfig.numChannels
											 + x * dsConfig.dimZ * dsConfig.dimY * dsConfig.numChannels 
											 + frame * dsConfig.dimZ * dsConfig.dimX * dsConfig.dimY * dsConfig.numChannels ;
							int destIndex = y*dsConfig.dimX + x;
							destData[destIndex] = flat_data[scrIndex];
						}
					}
				}
			}
		}

		// find largest value to automatically adjust the display range
		for (int i = 0; i < flat_data.length; ++i) {
			if (flat_data[i] > maxGray) maxGray = flat_data[i];
		}

		// configure options of image
		for( int c = 1; c <= dsConfig.numChannels; ++c)
		{
			image.setC(c);
			image.setDisplayRange(0, maxGray);
		}

		return image;
	}

	public static void writeImageToHDF5Volume(ImagePlus image, String filename, String dataset, int compressionLevel, LogService log)
	{
		ImageStack stack;
		int nFrames   = image.getNFrames();
		int nChannels = image.getNChannels();
		int nLevs     = image.getNSlices();
		int nRows     = image.getHeight();
		int nCols     = image.getWidth();
		
		int file_id = -1;
		int dataspace_id = -1;
		int dataset_id = -1;
		int dcpl_id = -1 ;
		int dataspace_id_color = -1;
		int attribute_id = -1;
		long[] maxdims = { HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED,HDF5Constants.H5S_UNLIMITED };
		int memspace = -1;
		long[] chunk_dims = {1, nCols/8, nRows/8, nLevs, nChannels};

		log.info("Export Dimensions: " + String.valueOf(nFrames) + "x" + String.valueOf(nCols) + "x" 
				+ String.valueOf(nRows) + "x" + String.valueOf(nLevs) + "x" + String.valueOf(nChannels));

		try
		{
			long[] half_Dims = null;
			int[] channelDims = null;
			long[] channel_Dims = null;
			if (nLevs > 1) 
			{
				channelDims = new int[5];
				channelDims[0] = nFrames; //t
				channelDims[1] = nCols; //x
				channelDims[2] = nRows; //y
				channelDims[3] = nLevs ; //z
				channelDims[4] = nChannels;

				channel_Dims = new long[5];
				channel_Dims[0] = nFrames;
				channel_Dims[1] = nCols; //x
				channel_Dims[2] = nRows; //y
				channel_Dims[3] = nLevs ;
				channel_Dims[4] = nChannels;

				half_Dims = new long[5];
				half_Dims[0] = Math.round(nFrames/2);
				half_Dims[1] = nCols; //x
				half_Dims[2] = nRows; //y
				half_Dims[3] = nLevs; //z
				half_Dims[4] = nChannels;

				log.info("half_Dim:" + String.valueOf(Math.round(nFrames/2)-1));
			} 
			else 
			{
				channelDims = new int[5];
				channelDims[0] = nFrames; //t
				channelDims[1] = nCols; //x
				channelDims[2] = nRows; //y
				channelDims[3] = 1 ;
				channelDims[4] = nChannels;

				channel_Dims = new long[5];
				channel_Dims[0] = nFrames;
				channel_Dims[1] = nCols; //x
				channel_Dims[2] = nRows; //y
				channel_Dims[3] = 1 ;
				channel_Dims[4] = nChannels;

				half_Dims = new long[5];
				half_Dims[0] = Math.round(nFrames/2);
				half_Dims[1] = nCols; //x
				half_Dims[2] = nRows; //y
				half_Dims[3] = 1; //z
				half_Dims[4] = nChannels;
			}

			long[] iniDims = new long[5];
			iniDims[0] = 1;
			iniDims[1] = nCols;
			iniDims[2] = nRows;
			iniDims[3] = 1;
			iniDims[4] = 1;

			long[] color_iniDims = new long[5];
			color_iniDims[0] = 1;
			color_iniDims[1] = nCols;
			color_iniDims[2] = nRows;
			color_iniDims[3] = 1;
			color_iniDims[4] = 3;

			try {
				file_id = H5Fcreate(filename, H5F_ACC_TRUNC, H5P_DEFAULT, H5P_DEFAULT);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try {
				dataspace_id = H5Screate_simple(5, iniDims, maxdims);
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

			log.info("chunksize: " + "1" + "x" + String.valueOf(nCols/8) + "x" + String.valueOf(nRows/8) + "x" +  String.valueOf(nLevs) + "x" 
					+  String.valueOf(nChannels));
			int imgColorType = image.getType();

			if (imgColorType == ImagePlus.GRAY8 || imgColorType == ImagePlus.COLOR_256 )
			{	
				byte[] pixels = null;
				stack = image.getStack();

				int timestep = 0;
				int z_axis = 0;

				try {
					if ((file_id >= 0) && (dataspace_id >= 0))
						dataset_id =  H5Dcreate(file_id, dataset, H5T_NATIVE_UINT8, dataspace_id, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				log.info(String.valueOf(stack.getSize()));

				for (int i=1; i<=stack.getSize();i++){ // stack size: levels*t 500
					pixels = (byte[]) stack.getPixels(i);
					byte[] pixels_target = new byte[pixels.length];

					for (int y=0; y<nRows; y++){
						for(int x=0; x<nCols; x++){
							pixels_target[y + x*(nRows)] = pixels[x + y*(nCols)];
						}
					}

					if (i== 1){
						try {
							if (dataset_id >= 0) 
								H5Dwrite(dataset_id, H5T_NATIVE_UINT8, H5S_ALL, H5S_ALL, H5P_DEFAULT, pixels_target);
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						try {
							if (dataspace_id >= 0)
								H5Sclose(dataspace_id);
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						if (nLevs>1){
							z_axis += 1;
						}
					}
					else{
						if (i==2){
							long[] extdims = new long[5];
							extdims = channel_Dims ;

							try {
								if (dataset_id >= 0)
									H5Dset_extent(dataset_id, extdims);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
						try {
							if (dataspace_id >= 0) {
								dataspace_id = H5Dget_space(dataset_id);

								//								log.info(String.valueOf(i) + " " + String.valueOf(timestep) + " " +String.valueOf(z_axis) );

								long[] start = {timestep,0,0,z_axis,0};
								H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims, null);
								memspace = H5Screate_simple(5, iniDims, null);

								if (dataset_id >= 0)
									H5Dwrite(dataset_id, H5T_NATIVE_UINT8, memspace, dataspace_id, H5P_DEFAULT, pixels_target);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}

					z_axis += 1;

					if ((i % (nLevs*nChannels))==0){
						timestep += 1;
						z_axis = 0;
					}
				}
				log.info("write uint8 hdf5");
				log.info("compressionLevel: " + String.valueOf(compressionLevel));
				log.info("Done");
			}

			else if (imgColorType == ImagePlus.GRAY16) 
			{
				short[] pixels = null;
				stack = image.getStack();

				try {
					if ((file_id >= 0) && (dataspace_id >= 0))
						dataset_id =  H5Dcreate(file_id, dataset, H5T_NATIVE_UINT16, dataspace_id, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				for (int stackIndex = 1; stackIndex <= stack.getSize(); stackIndex++){
					pixels = (short[]) stack.getPixels(stackIndex);
					int[] slicePosition = image.convertIndexToPosition(stackIndex); // contains (channel, slice, frame) indices
					log.info("Slice " + stackIndex + " position is: [c:" + slicePosition[0] + ", s:" + slicePosition[1] + ", f:" + slicePosition[2] + "]");
					short[] pixels_target = new short[pixels.length];

					for (int y = 0; y < nRows; y++){
						for(int x = 0; x < nCols; x++){
							pixels_target[y + x * nRows] = pixels[x + y * nCols];
						}
					}

					if (stackIndex == 1){
						try {
							if (dataset_id >= 0) 
								H5Dwrite(dataset_id, H5T_NATIVE_UINT16, H5S_ALL, H5S_ALL, H5P_DEFAULT, pixels_target);
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						try {
							if (dataspace_id >= 0)
								H5Sclose(dataspace_id);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					else{
						if (stackIndex == 2)
						{
							long[] extdims = new long[5];
							extdims = channel_Dims;

							try {
								if (dataset_id >= 0)
									H5Dset_extent(dataset_id, extdims);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}

						try {
							if (dataspace_id >= 0) {
								dataspace_id = H5Dget_space(dataset_id);
								long[] start = {slicePosition[2]-1, 0, 0, slicePosition[1]-1, slicePosition[0]-1};
								H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims, null);

								memspace = H5Screate_simple(5, iniDims, null);
								if (dataset_id >= 0)
									H5Dwrite(dataset_id, H5T_NATIVE_UINT16, memspace, dataspace_id, H5P_DEFAULT, pixels_target);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}

					}
				}


				log.info("write uint16 hdf5");
				log.info("compressionLevel: " + String.valueOf(compressionLevel));
				log.info("Done");
			}
			else if (imgColorType == ImagePlus.GRAY32)
			{
				float[] pixels = null;
				stack = image.getStack();

				int timestep = 0;
				int z_axis = 0;

				try {
					if ((file_id >= 0) && (dataspace_id >= 0))
						dataset_id =  H5Dcreate(file_id, dataset, H5T_NATIVE_FLOAT, dataspace_id, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				for (int i=1;i<=stack.getSize();i++){
					pixels = (float[]) stack.getPixels(i);
					float[] pixels_target = new float[pixels.length];

					for (int y=0; y<nRows; y++){
						for(int x=0; x<nCols; x++){
							pixels_target[y + x*(nRows)] = pixels[x + y*(nCols)];
						}
					}

					if (i== 1){
						try {
							if (dataset_id >= 0) 
								H5Dwrite(dataset_id, H5T_NATIVE_FLOAT, H5S_ALL, H5S_ALL, H5P_DEFAULT, pixels_target);
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						try {
							if (dataspace_id >= 0)
								H5Sclose(dataspace_id);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						if (nLevs>1){
							z_axis += 1;
						}
					}
					else{
						if (i==2){
							long[] extdims = new long[5];
							extdims = channel_Dims ;

							try {
								if (dataset_id >= 0)
									H5Dset_extent(dataset_id, extdims);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}

						try {
							if (dataspace_id >= 0) {
								dataspace_id = H5Dget_space(dataset_id);
								long[] start = {timestep,0,0,z_axis,0};
								H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims, null);
								memspace = H5Screate_simple(5, iniDims, null);

								if (dataset_id >= 0)
									H5Dwrite(dataset_id, H5T_NATIVE_FLOAT, memspace, dataspace_id, H5P_DEFAULT, pixels_target);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					z_axis += 1;

					if ((i % (nLevs*nChannels))==0){
						timestep += 1;
						z_axis = 0;
					}
				}

				log.info("write float32 hdf5");
				log.info("compressionLevel: " + String.valueOf(compressionLevel));
				log.info("Done");	
			} 
			else if (imgColorType == ImagePlus.COLOR_RGB){

				long[] channelDimsRGB = null;
				if (nLevs > 1) 
				{
					channelDimsRGB = new long[5];
					channelDimsRGB[0] = nFrames; //t
					channelDimsRGB[1] = nCols; //x
					channelDimsRGB[2] = nRows; //y
					channelDimsRGB[3] = nLevs ; //z
					channelDimsRGB[4] = 3;
				} 
				else 
				{
					channelDimsRGB = new long[5];
					channelDimsRGB[0] = nFrames; //t
					channelDimsRGB[1] = nCols; //x
					channelDimsRGB[2] = nRows; //y
					channelDimsRGB[3] = 1 ;
					channelDimsRGB[4] = 3;
				}

				try {
					dataspace_id_color = H5Screate_simple(5, color_iniDims, maxdims);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				stack = image.getStack();

				try {
					if ((file_id >= 0) && (dataspace_id >= 0))
						dataset_id =  H5Dcreate(file_id, dataset, H5T_NATIVE_UINT8, dataspace_id_color, H5P_DEFAULT, dcpl_id, H5P_DEFAULT);
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

						for (int y=0; y<nRows; y++){
							for(int x=0; x<nCols; x++){
								color_target[0][y + x*(nRows)] = red[x + y*(nCols)];
								color_target[2][y + x*(nRows)] = blue[x + y*(nCols)];
								color_target[1][y + x*(nRows)] = green[x + y*(nCols)];
							}
						}

						try {
							if (dataspace_id >= 0)
								H5Sclose(dataspace_id_color);
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
								if (dataspace_id >= 0) {
									dataspace_id = H5Dget_space(dataset_id);

									long[] start = {t,0,0,z,c};
									long[] iniDims2 = {0,nCols,nRows,0,c};

									H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, start, null, iniDims2, null);
									memspace = H5Screate_simple(5, iniDims, null);

									if (dataset_id >= 0)
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
			else {
				log.error("Type Not handled yet!");
			}

			try {
				if (attribute_id >= 0)
					H5Aclose(attribute_id);
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
				if (dataspace_id >= 0)    
					H5Sclose(dataspace_id);
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
}
