package org.ilastik.ilastik4ij;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

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

        /**
         * @brief [brief description]
         * @details [long description]
         * 
         * @param reader
         *                [description]
         * @param dataset
         *                [description]
         * @param axesorder
         *                A string containing t,x,y,z,c in the order they are
         *                present in the dataset. If an axis is omitted, it is
         *                assumed to have dimensionality 1.
         * @return [description]
         */
        public Hdf5DataSetConfig(IHDF5Reader reader, String dataset, String axesorder) {
                // get shape info
                dimX = 1;
                dimY = 1;
                dimZ = 1;
                numChannels = 1;
                numFrames = 1;
                HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation(dataset);

                for (int index = 0; index < axesorder.length(); index++) {
                        char axis = axesorder.charAt(index);

                        if (axis == 't') {
                                numFrames = (int) dsInfo.getDimensions()[index];
                        }
                        if (axis == 'x') {
                                dimX = (int) dsInfo.getDimensions()[index];
                        }
                        if (axis == 'y') {
                                dimY = (int) dsInfo.getDimensions()[index];
                        }
                        if (axis == 'z') {
                                dimZ = (int) dsInfo.getDimensions()[index];
                        }
                        if (axis == 'c') {
                                numChannels = (int) dsInfo.getDimensions()[index];
                        }
                }

                // datatype
                typeInfo = getTypeInfo(dsInfo);
        }

        public Hdf5DataSetConfig(IHDF5Reader reader, String dataset) {
                this(reader, dataset, "txyzc");
        }

        private String getTypeInfo(HDF5DataSetInformation dsInfo) {
                HDF5DataTypeInformation dsType = dsInfo.getTypeInformation();
                String type = "";

                if (dsType.isSigned() == false) {
                        type += "u";
                }

                switch (dsType.getDataClass()) {
                case INTEGER:
                        type += "int" + 8 * dsType.getElementSize();
                        break;
                case FLOAT:
                        type += "float" + 8 * dsType.getElementSize();
                        break;
                default:
                        type += dsInfo.toString();
                }
                return type;
        }
}
