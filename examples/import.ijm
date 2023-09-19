#@ File (label = "Input file", style = "file") input
// input = "/absolute/path/to/some/directory/src/test/resources/2d_cells_apoptotic_1channel.h5";

datasetname = "/data";
axisorder = "tzyxc";

run("Import HDF5", "select=[" + input + "] datasetname=[" + datasetname + "] axisorder=[" + axisorder + "]");
