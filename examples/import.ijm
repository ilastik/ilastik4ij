// Lines that start with "//" are comments.

// Lines that start with "#@" declare input parameters that interactively ask the user for values.
// You can remove those lines and set input parameters explicitly in order to run the macro without further user interaction.

#@ File (label = "Input file", style = "directory") dataDir
// dataDir = "/absolute/path/to/some/directory/with/h5-files/";

datasetname = "/data";
axisorder = "tzyxc";

fileList = getFileList(dataDir);
for (i = 0; i < fileList.length; i++) {
  // import image from the H5
  fileName = dataDir + fileList[i];
  run("Import HDF5", "select=[" + fileName + "] datasetname=[" + datasetname + "] axisorder=[" + axisorder + "]");
}
