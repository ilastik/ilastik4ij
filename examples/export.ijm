// Lines that start with "//" are comments.

// Lines that start with "#@" declare input parameters that interactively ask the user for values.
// If you want to run a macro in batch or headless mode, remove those lines and set input parameters explicitly.

#@ File (label = "Input file", style = "file") input
// input = "/absolute/path/to/some/directory/src/test/resources/2d_cells_apoptotic.tif";

#@ File (label = "Output file", style = "save") exportPath
// exportPath = "/path/to/some/directory/output.h5";

datasetName = "/data";
compressionLevel = 0;

run("Export HDF5", "input=[" + input + "] datasetname=[" + datasetName + "] compressionlevel=[" + compressionLevel + "] exportpath=[" + exportPath + "]");
