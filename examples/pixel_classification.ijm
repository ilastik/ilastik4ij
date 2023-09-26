// Lines that start with "//" are comments.

// Lines that start with "#@" declare input parameters that interactively ask the user for values.
// You can remove those lines and set input parameters explicitly in order to run the macro without further user interaction.

#@ File (label = "Project file", style = "file") pixelClassificationProject
// project = "/absolute/path/to/some/directory/pixel_class_2d_cells_apoptotic.ilp";

#@ File (label = "Input file", style = "directory") dataDir
// dataDir = "/absolute/path/to/some/directory/";

// set global variables
outputType = "Probabilities"; //  or "Segmentation"
inputDataset = "data";
outputDataset = "exported_data";
axisOrder = "tzyxc";
compressionLevel = 0;

// process all H5 files in a given directory
fileList = getFileList(dataDir);
for (i = 0; i < fileList.length; i++) {
    // import image from the H5
    fileName = dataDir + fileList[i];   
    importArgs = "select=" + fileName + " datasetname=" + inputDataset + " axisorder=" + axisOrder;             
    run("Import HDF5", importArgs);

    // run pixel classification
    inputImage = fileName + "/" + inputDataset;
    pixelClassificationArgs = "projectfilename=" + pixelClassificationProject + " saveonly=false inputimage=" + inputImage + " pixelclassificationtype=" + outputType;
    run("Run Pixel Classification Prediction", pixelClassificationArgs);

    // export probability maps to H5
    outputFile = dataDir + "output" + i + ".h5";
    exportArgs = "select=" + outputFile + " datasetname=" + outputDataset + " compressionlevel=" + compressionLevel;
    run("Export HDF5", exportArgs);
}