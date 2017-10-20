package org.ilastik.ilastik4ij.util;

public class ComboBoxDimensions {

    private String name;
    private String valid;

    public ComboBoxDimensions(String name, String valid) {
        this.name = name;
        this.valid = valid;
    }

    public String getEntry() {
        return this.valid + ": " + this.name;
    }

    public String getName() {
        return this.name;
    }

    public String isValid() {
        return this.valid;
    }

}
