// Lines that start with "//" are comments.

// Lines that start with "#@" declare input parameters that interactively ask the user for values.
// You can remove those lines and set input parameters explicitly in order to run the macro without further user interaction.

#@ File (label = "Project file", style = "file") project
// project = "/absolute/path/to/some/directory/tracking.ilp";

#@ File (label = "Input file", style = "file") input
// input = "/absolute/path/to/some/directory/raw_data.tif";

#@ File (label = "Second input file", style = "file") secondInput
// secondInput = "/absolute/path/to/some/directory/probabilities.tif";

type = "Probabilities";

open(input);
open(secondInput);

run("Run Tracking", "projectfilename=[" + project + "] input=[" + input + "] inputproborsegimage=[" + secondInput + "] secondinputtype=[" + type + "]");
