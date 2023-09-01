project = "src/test/resources/pixel_class_2d_cells_apoptotic.ilp";
input = "src/test/resources/2d_cells_apoptotic.tif";
type = "Probabilities";

open(input);
run("Run Pixel Classification Prediction", "projectfilename=[" + project + "] input=[" + input + "] pixelclassificationtype=[" + type + "]");
