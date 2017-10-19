package org.ilastik.ilastik4ij.hdf5;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import java.util.HashMap;
import java.util.Map;

/*
 * @brief Dimensionality and datatype information of a dataset in a HDF5 File
 * 
 */
public class Hdf5DataSetConfig {
	public int numFrames;
	public int dimX;
	public int dimY;
	public int dimZ;
	public int numChannels;
	public String typeInfo;
    public int bitdepth;
    public Map<Character, Integer> axisIndices;
    public Map<Character, Integer> axisExtents;
    public String axesorder;

	/*
	 * @brief [brief description]
	 * @details [long description]
	 * 
	 * @param reader [description]
	 * @param dataset [description]
	 * @param axesorder A string containing t,x,y,z,c in the order they are present in the dataset. 
	 * 					If an axis is omitted, it is assumed to have dimensionality 1.
	 * @return [description]
	 */
	public Hdf5DataSetConfig(IHDF5Reader reader, String dataset, String axesorder)
	{
		// init
		dimX = 1;
		dimY = 1;
		dimZ = 1;
		numChannels = 1;
		numFrames = 1;
		
		// get shape info
		HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(dataset);
		axisIndices = new HashMap<Character, Integer>();
        axisExtents = new HashMap<Character, Integer>();
        this.axesorder = axesorder;
        
		for(int index = 0; index < axesorder.length(); index++)
		{
			char axis = axesorder.charAt(index);
            axisIndices.put(axis, index);
            axisExtents.put(axis, (int)dsInfo.getDimensions()[index]);
		}

        if(dsInfo.getRank() != axesorder.length())
            throw new IllegalArgumentException("Provided axesorder and dataset have different numbers of axes!");
        
        if(!(axisIndices.containsKey('x') && axisIndices.containsKey('y')))
            throw new IllegalArgumentException("Provided file and dataset must contain x and y axes!");
        
        numFrames = tryToExtract('t');
        dimX = tryToExtract('x');
        dimY = tryToExtract('y');
        dimZ = tryToExtract('z');
        numChannels = tryToExtract('c');

		// datatype
		typeInfo = getTypeInfo(dsInfo);
	}
    
	public Hdf5DataSetConfig(IHDF5Reader reader, String dataset)
	{
		this(reader, dataset, "txyzc");
	}
    
    public long[] getSliceOffset(int t, int z, int c)
    {
        int numAxes = axisIndices.size();
        long[] result = new long[numAxes];
        for(Character axis : axisIndices.keySet())
        {
            int index = axisIndices.get(axis);
            
            switch(axis){
                case 't': 
                    result[index] = t;
                    break;
                case 'z':
                    result[index] = z;
                    break;
                case 'c': 
                    result[index] = c;
                    break;
                default:
                    result[index] = 0;
                    break;
            }
        }
        
        return result;
    }
    
    public int[] getXYSliceExtent()
    {
        int numAxes = axisIndices.size();
        int[] result = new int[numAxes];
        for(Character axis : axisIndices.keySet())
        {
            int index = axisIndices.get(axis);
            
            switch(axis){
                case 'x': 
                    result[index] = dimX;
                    break;
                case 'y':
                    result[index] = dimY;
                    break;
                default:
                    result[index] = 1;
                    break;
            }
        }
        
        return result;
    }
    
    private int tryToExtract(char axis)
    {
        try{
            return axisExtents.get(axis);
        }
        catch(NullPointerException e){
            return 1;
        }
    }
	
	private String getTypeInfo(HDF5DataSetInformation dsInfo)
	{
		HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
                bitdepth = 8 * dsType.getElementSize();
		String type = "";

		if (dsType.isSigned() == false) {
			type += "u";
		}

		switch( dsType.getDataClass())
		{
		case INTEGER:
			type += "int" + bitdepth;
			break;
		case FLOAT:
			type += "float" + bitdepth;
			break;
		default:
			type += dsInfo.toString();
		}
		return type;
	}
}

