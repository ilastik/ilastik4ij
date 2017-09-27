package org.ilastik.ilastik4ij;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import java.util.HashMap;
import java.util.Map;

/**
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

	/**
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

        numFrames = axisExtents.get('t');
        dimX = axisExtents.get('x');
        dimY = axisExtents.get('y');
        dimZ = axisExtents.get('z');
        numChannels = axisExtents.get('c');

		// datatype
		typeInfo = getTypeInfo(dsInfo);
	}
    
	public Hdf5DataSetConfig(IHDF5Reader reader, String dataset)
	{
		this(reader, dataset, "txyzc");
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

