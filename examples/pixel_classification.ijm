#@ File (label = "Project file", style = "file") project
// project = "/absolute/path/to/some/directory/pixel_class_2d_cells_apoptotic.ilp";

#@ File (label = "Input file", style = "file") input
// input = "/absolute/path/to/some/directory/2d_cells_apoptotic.tif";

type = "Probabilities";

open(input);
run("Run Pixel Classification Prediction", "projectfilename=[" + project + "] input=[" + input + "] pixelclassificationtype=[" + type + "]");
