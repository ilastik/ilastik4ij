project = "src/test/resources/pixel_class_2d_cells_apoptotic.ilp";
input = "src/test/resources/2d_cells_apoptotic.tif";
secondInput = "src/test/resources/2d_cells_apoptotic_1channel-data_Probabilities.tif";
type = "Probabilities";

open(input);
run("Run Object Classification Prediction", "projectfilename=[" + project + "] input=[" + input + "] inputproborsegimage=[" + secondInput + "] secondinputtype=[" + type + "]");
