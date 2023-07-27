project = "src/test/resources/pixel_class_2d_cells_apoptotic.ilp";
input = "2d_cells_apoptotic.tif";

open(input);
run("Run Pixel Classification Prediction", "projectfilename=" + project + " input=" + input + " pixelclassificationtype=Probabilities");
