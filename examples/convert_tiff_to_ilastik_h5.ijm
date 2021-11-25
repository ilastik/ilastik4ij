/*
 * Macro to convert multiple images in a folder to hdf5 for ilastik
 */

#@ File (label = "Input directory", style = "directory") input
#@ File (label = "Output directory", style = "directory") output
#@ String (label = "File suffix", value = ".tif") suffix

processFolder(input);

// function to scan folder to find files with correct suffix
function processFolder(input) {
	list = getFileList(input);
	list = Array.sort(list);
	for (i = 0; i < list.length; i++) {
		if(endsWith(list[i], suffix))
			processFile(input, output, list[i]);
	}
}

function processFile(input, output, file) {
	inputFilePath = input + File.separator + file;
	// print("Processing: " + inputFilePath);
	outputFileName = replace(file, suffix, ".h5");
	outputFilePath = output + File.separator + outputFileName;
	// print("Saving to: " + outputFilePath);
	open(inputFilePath);
	select = output + ".h5";
    exportArgs = "select=[" + select + "] exportpath=[" + outputFilePath + "] datasetname=data compressionlevel=0 input=[" + file +"]";
    run("Export HDF5", exportArgs);
    close();
}
