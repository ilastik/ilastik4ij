input = "src/test/resources/2d_cells_apoptotic.tif";
datasetName = "/data";
compressionLevel = 0;
exportPath = "output.h5";

run("Export HDF5", "input=[" + input + "] datasetname=[" + datasetName + "] compressionlevel=[" + compressionLevel + "] exportpath=[" + exportPath + "]");
